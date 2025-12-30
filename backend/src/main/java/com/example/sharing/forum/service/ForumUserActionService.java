package com.example.sharing.forum.service;

import com.example.sharing.core.entity.User;
import com.example.sharing.forum.dto.PageResponse;
import com.example.sharing.forum.entity.ActionType;
import com.example.sharing.forum.entity.ForumUserAction;
import com.example.sharing.forum.repository.ForumUserActionRepository;
import com.example.sharing.profile.UserProfile;
import com.example.sharing.profile.UserProfileDTO;
import com.example.sharing.profile.UserProfileRepository;
import com.example.sharing.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumUserActionService {

    private final ForumUserActionRepository actionRepository;
    private final UserRepository userRepository;
    private final TopicService topicService;
    private final CommentService commentService;
    private final UserProfileRepository userProfileRepository;

    /**
     * 执行用户操作（点赞、收藏、关注）
     */
    @Transactional
    public boolean performAction(Long userId, Integer actionType, Long targetId) {
        try {
            // 检查用户是否存在
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

            // 查找现有记录
            Optional<ForumUserAction> existingAction = actionRepository
                    .findByUserIdAndActionTypeAndTargetId(userId, actionType, targetId);

            if (existingAction.isPresent()) {
                ForumUserAction action = existingAction.get();
                if (Boolean.TRUE.equals(action.getIsCancel())) {
                    // 如果已取消，重新激活
                    action.setIsCancel(false);
                    actionRepository.save(action);
                    log.info("重新激活用户操作: userId={}, actionType={}, targetId={}",
                            userId, actionType, targetId);
                    updateTargetCounter(actionType, targetId, true);
                    return true;
                }
                // 如果已存在且未取消，不做任何操作
                return false;
            } else {
                // 创建新记录
                ForumUserAction action = new ForumUserAction();
                action.setUser(user);
                action.setUserId(userId);
                action.setActionType(actionType);
                action.setTargetId(targetId);
                action.setIsCancel(false);

                actionRepository.save(action);
                log.info("创建用户操作: userId={}, actionType={}, targetId={}",
                        userId, actionType, targetId);
                updateTargetCounter(actionType, targetId, true);
                return true;
            }
        } catch (Exception e) {
            log.error("执行用户操作失败: userId={}, actionType={}, targetId={}",
                    userId, actionType, targetId, e);
            throw new RuntimeException("执行操作失败: " + e.getMessage());
        }
    }

    /**
     * 取消用户操作
     */
    @Transactional
    public boolean cancelAction(Long userId, Integer actionType, Long targetId) {
        try {
            // 检查记录是否存在且未取消
            Optional<ForumUserAction> existingAction = actionRepository
                    .findByUserIdAndActionTypeAndTargetId(userId, actionType, targetId);

            if (existingAction.isPresent() && !Boolean.TRUE.equals(existingAction.get().getIsCancel())) {
                ForumUserAction action = existingAction.get();
                action.setIsCancel(true);
                actionRepository.save(action);

                log.info("取消用户操作: userId={}, actionType={}, targetId={}",
                        userId, actionType, targetId);
                updateTargetCounter(actionType, targetId, false);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("取消用户操作失败: userId={}, actionType={}, targetId={}",
                    userId, actionType, targetId, e);
            throw new RuntimeException("取消操作失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否执行了某种操作
     */
    public boolean hasAction(Long userId, Integer actionType, Long targetId) {
        return actionRepository.existsByUserIdAndActionTypeAndTargetIdAndIsCancelFalse(
                userId, actionType, targetId);
    }

    /**
     * 获取目标的某种操作数量
     */
    public long getActionCount(Long targetId, Integer actionType) {
        return actionRepository.countByTargetIdAndActionTypeAndIsCancelFalse(targetId, actionType);
    }

    /**
     * 获取用户的操作历史（分页）
     */
    public Page<ForumUserAction> getUserActions(Long userId, Integer actionType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return actionRepository.findByUserIdAndActionTypeAndIsCancelFalse(userId, actionType, pageable);
    }

    /**
     * 获取用户关注的人
     */
    public List<Long> getFollowingUsers(Long userId) {
        return actionRepository.findFollowingByUserId(userId);
    }

    /**
     * 获取用户的粉丝
     */
    public List<Long> getFollowers(Long userId) {
        return actionRepository.findFollowersByUserId(userId);
    }

    /**
     * 更新目标的计数器
     */
    private void updateTargetCounter(Integer actionType, Long targetId, boolean increment) {
        try {
            switch (actionType) {
                case ActionType.ACTION_TYPE_LIKE_TOPIC:
                    if (increment) {
                        topicService.incrementLikeCount(targetId);
                    } else {
                        topicService.decrementLikeCount(targetId);
                    }
                    break;
                case ActionType.ACTION_TYPE_LIKE_COMMENT:
                    if (increment) {
                        commentService.incrementLikeCount(targetId);
                    } else {
                        commentService.decrementLikeCount(targetId);
                    }
                    break;
                case ActionType.ACTION_TYPE_COLLECT_TOPIC:
                    if (increment) {
                        topicService.incrementCollectCount(targetId);
                    } else {
                        topicService.decrementCollectCount(targetId);
                    }
                    break;
                // 关注用户不需要更新计数器
                case ActionType.ACTION_TYPE_FOLLOW_USER:
                    break;
                default:
                    log.warn("未知的操作类型: {}", actionType);
            }
        } catch (Exception e) {
            log.error("更新目标计数器失败: actionType={}, targetId={}", actionType, targetId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 切换操作状态（如果存在则取消，不存在则创建）
     */
    @Transactional
    public boolean toggleAction(Long userId, Integer actionType, Long targetId) {
        boolean hasAction = hasAction(userId, actionType, targetId);
        if (hasAction) {
            return cancelAction(userId, actionType, targetId);
        } else {
            return performAction(userId, actionType, targetId);
        }
    }

    /**
     * 批量检查用户对多个目标的操作状态
     */
    public List<Long> getTargetsWithAction(Long userId, Integer actionType, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return List.of();
        }
        return actionRepository.findTargetIdsByUserIdAndActionTypeAndTargetIds(
                userId, actionType, targetIds);
    }

    /**
     * 获取话题的点赞用户列表
     */
    public Page<ForumUserAction> getTopicLikes(Long topicId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return actionRepository.findByTargetIdAndActionTypeAndIsCancelFalse(
                topicId, ActionType.ACTION_TYPE_LIKE_TOPIC, pageable);
    }

    /**
     * 获取话题的收藏用户列表
     */
    public Page<ForumUserAction> getTopicCollectors(Long topicId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return actionRepository.findByTargetIdAndActionTypeAndIsCancelFalse(
                topicId, ActionType.ACTION_TYPE_COLLECT_TOPIC, pageable);
    }

    /**
     * 检查用户是否点赞了话题
     */
    public boolean hasLikedTopic(Long userId, Long topicId) {
        return hasAction(userId, ActionType.ACTION_TYPE_LIKE_TOPIC, topicId);
    }

    /**
     * 检查用户是否收藏了话题
     */
    public boolean hasCollectedTopic(Long userId, Long topicId) {
        return hasAction(userId, ActionType.ACTION_TYPE_COLLECT_TOPIC, topicId);
    }

    /**
     * 检查用户是否点赞了评论
     */
    public boolean hasLikedComment(Long userId, Long commentId) {
        return hasAction(userId, ActionType.ACTION_TYPE_LIKE_COMMENT, commentId);
    }

    /**
     * 检查用户是否关注了另一个用户
     */
    public boolean hasFollowedUser(Long userId, Long targetUserId) {
        return hasAction(userId, ActionType.ACTION_TYPE_FOLLOW_USER, targetUserId);
    }

    /**
     * 批量获取用户资料（返回列表） - 适配你的UserProfileDTO
     */
    private List<UserProfileDTO> getUsersProfiles(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<UserProfileDTO> result = new ArrayList<>();

        // 批量查询用户
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 批量查询用户资料
        List<UserProfile> profiles = userProfileRepository.findAllById(userIds);
        Map<Long, UserProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));

        // 构建DTO列表
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            UserProfile profile = profileMap.get(userId);

            if (user != null) {
                UserProfileDTO dto = new UserProfileDTO();
                dto.setUserId(userId);
                dto.setUsername(user.getUsername());
                dto.setEmail(user.getEmail());
                dto.setRole(user.getRole()); // 假设User有getRole()方法

                if (profile != null) {
                    dto.setNickname(profile.getNickname());
                    dto.setBio(profile.getBio());
                    dto.setMajor(profile.getMajor());
                    // 如果有其他字段也可以设置
                    // dto.setAvatar(profile.getAvatar()); // 如果你需要这个字段
                } else {
                    // 如果没有用户资料，使用用户名作为昵称
                    dto.setNickname(user.getUsername() != null ? user.getUsername() : "用户" + userId);
                    dto.setBio("");
                    dto.setMajor("");
                }

                result.add(dto);
            }
        }

        return result;
    }

    /**
     * 获取用户关注的人（分页）
     */
    public PageResponse<UserProfileDTO> getFollowingUsers(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 获取关注记录
        Page<ForumUserAction> actionPage = actionRepository.findByUserIdAndActionTypeAndIsCancelFalse(
                userId, ActionType.ACTION_TYPE_FOLLOW_USER, pageable);

        // 提取被关注的用户ID
        List<Long> followingUserIds = actionPage.getContent().stream()
                .map(ForumUserAction::getTargetId)
                .collect(Collectors.toList());

        // 批量获取用户资料
        List<UserProfileDTO> followingList = getUsersProfiles(followingUserIds);

        return createPageResponse(followingList, actionPage);
    }

    /**
     * 获取用户的粉丝（分页）
     */
    public PageResponse<UserProfileDTO> getFollowers(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 获取粉丝记录
        Page<ForumUserAction> actionPage = actionRepository.findByTargetIdAndActionTypeAndIsCancelFalse(
                userId, ActionType.ACTION_TYPE_FOLLOW_USER, pageable);

        // 提取粉丝的用户ID
        List<Long> followerUserIds = actionPage.getContent().stream()
                .map(ForumUserAction::getUserId)
                .collect(Collectors.toList());

        // 批量获取用户资料
        List<UserProfileDTO> followerList = getUsersProfiles(followerUserIds);

        return createPageResponse(followerList, actionPage);
    }

    /**
     * 创建分页响应
     */
    private <T> PageResponse<T> createPageResponse(List<T> content, Page<?> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        return response;
    }
}
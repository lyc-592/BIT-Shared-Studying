package com.example.sharing.forum.controller;

import com.example.sharing.core.dto.ApiResponse;
import com.example.sharing.forum.dto.CommentStatusRequest;
import com.example.sharing.forum.dto.PageResponse;
import com.example.sharing.forum.dto.TopicStatusRequest;
import com.example.sharing.forum.entity.ActionType;
import com.example.sharing.forum.service.ForumUserActionService;
import com.example.sharing.profile.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
public class ForumUserActionController {

    private final ForumUserActionService forumUserActionService;

    /**
     * 批量查询用户对话题的状态
     */
    @PostMapping("/topic-status/batch")
    public ApiResponse<Map<String, Object>> getBatchTopicStatus(
            @RequestParam Long userId,
            @RequestBody TopicStatusRequest request) {

        try {
            Map<String, Object> result = new HashMap<>();

            // 批量查询点赞状态
            List<Long> likedTopicIds = forumUserActionService.getTargetsWithAction(
                    userId, ActionType.ACTION_TYPE_LIKE_TOPIC, request.getTopicIds());
            Map<Long, Boolean> likeStatus = new HashMap<>();
            for (Long topicId : request.getTopicIds()) {
                likeStatus.put(topicId, likedTopicIds.contains(topicId));
            }
            result.put("liked", likeStatus);

            // 批量查询收藏状态
            List<Long> collectedTopicIds = forumUserActionService.getTargetsWithAction(
                    userId, ActionType.ACTION_TYPE_COLLECT_TOPIC, request.getTopicIds());
            Map<Long, Boolean> collectStatus = new HashMap<>();
            for (Long topicId : request.getTopicIds()) {
                collectStatus.put(topicId, collectedTopicIds.contains(topicId));
            }
            result.put("collected", collectStatus);

            // 批量查询关注状态（关注的是作者）
            Map<Long, Boolean> followStatus = new HashMap<>();
            if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
                List<Long> followedAuthorIds = forumUserActionService.getTargetsWithAction(
                        userId, ActionType.ACTION_TYPE_FOLLOW_USER, request.getAuthorIds());
                for (Long authorId : request.getAuthorIds()) {
                    followStatus.put(authorId, followedAuthorIds.contains(authorId));
                }
            }
            result.put("followed", followStatus);

            return ApiResponse.success("批量查询状态成功", result);
        } catch (Exception e) {
            return ApiResponse.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 批量查询用户对评论的状态
     */
    @PostMapping("/comment-status/batch")
    public ApiResponse<Map<String, Object>> getBatchCommentStatus(
            @RequestParam Long userId,
            @RequestBody CommentStatusRequest request) {

        try {
            Map<String, Object> result = new HashMap<>();

            // 批量查询点赞状态
            List<Long> likedCommentIds = forumUserActionService.getTargetsWithAction(
                    userId, ActionType.ACTION_TYPE_LIKE_COMMENT, request.getCommentIds());
            Map<Long, Boolean> likeStatus = new HashMap<>();
            for (Long commentId : request.getCommentIds()) {
                likeStatus.put(commentId, likedCommentIds.contains(commentId));
            }
            result.put("liked", likeStatus);

            // 批量查询关注状态（关注的是评论作者）
            Map<Long, Boolean> followStatus = new HashMap<>();
            if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
                List<Long> followedAuthorIds = forumUserActionService.getTargetsWithAction(
                        userId, ActionType.ACTION_TYPE_FOLLOW_USER, request.getAuthorIds());
                for (Long authorId : request.getAuthorIds()) {
                    followStatus.put(authorId, followedAuthorIds.contains(authorId));
                }
            }
            result.put("followed", followStatus);

            return ApiResponse.success("批量查询状态成功", result);
        } catch (Exception e) {
            return ApiResponse.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个话题的所有状态
     */
    @GetMapping("/topic-status/{topicId}")
    public ApiResponse<Map<String, Boolean>> getTopicStatus(
            @PathVariable Long topicId,
            @RequestParam Long userId) {

        try {
            Map<String, Boolean> result = new HashMap<>();

            // 查询点赞状态
            result.put("liked", forumUserActionService.hasLikedTopic(userId, topicId));

            // 查询收藏状态
            result.put("collected", forumUserActionService.hasCollectedTopic(userId, topicId));

            return ApiResponse.success("查询状态成功", result);
        } catch (Exception e) {
            return ApiResponse.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个评论的所有状态
     */
    @GetMapping("/comment-status/{commentId}")
    public ApiResponse<Map<String, Boolean>> getCommentStatus(
            @PathVariable Long commentId,
            @RequestParam Long userId) {

        try {
            Map<String, Boolean> result = new HashMap<>();

            // 查询点赞状态
            result.put("liked", forumUserActionService.hasLikedComment(userId, commentId));

            return ApiResponse.success("查询状态成功", result);
        } catch (Exception e) {
            return ApiResponse.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow/{targetUserId}")
    public ApiResponse<Void> followUser(
            @PathVariable Long targetUserId,
            @RequestParam Long userId) {
        try {
            if (userId.equals(targetUserId)) {
                return ApiResponse.fail("不能关注自己");
            }

            boolean success = forumUserActionService.performAction(
                    userId, ActionType.ACTION_TYPE_FOLLOW_USER, targetUserId);
            return success ?
                    ApiResponse.success("关注成功", null) :
                    ApiResponse.fail("关注失败");
        } catch (Exception e) {
            return ApiResponse.fail("关注失败: " + e.getMessage());
        }
    }

    /**
     * 取消关注用户
     */
    @PostMapping("/unfollow/{targetUserId}")
    public ApiResponse<Void> unfollowUser(
            @PathVariable Long targetUserId,
            @RequestParam Long userId) {
        try {
            boolean success = forumUserActionService.cancelAction(
                    userId, ActionType.ACTION_TYPE_FOLLOW_USER, targetUserId);
            return success ?
                    ApiResponse.success("取消关注成功", null) :
                    ApiResponse.fail("取消关注失败");
        } catch (Exception e) {
            return ApiResponse.fail("取消关注失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否关注了用户
     */
    @GetMapping("/check/{targetUserId}")
    public ApiResponse<Boolean> checkFollow(
            @PathVariable Long targetUserId,
            @RequestParam Long userId) {
        try {
            boolean isFollowing = forumUserActionService.hasAction(
                    userId, ActionType.ACTION_TYPE_FOLLOW_USER, targetUserId);
            return ApiResponse.success("查询成功", isFollowing);
        } catch (Exception e) {
            return ApiResponse.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户关注的人
     */
    @GetMapping("/following/{userId}")
    public ApiResponse<PageResponse<UserProfileDTO>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageResponse<UserProfileDTO> following = forumUserActionService.getFollowingUsers(userId, page, size);
            return ApiResponse.success("获取关注列表成功", following);
        } catch (Exception e) {
            return ApiResponse.fail("获取关注列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的粉丝
     */
    @GetMapping("/followers/{userId}")
    public ApiResponse<PageResponse<UserProfileDTO>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageResponse<UserProfileDTO> followers = forumUserActionService.getFollowers(userId, page, size);
            return ApiResponse.success("获取粉丝列表成功", followers);
        } catch (Exception e) {
            return ApiResponse.fail("获取粉丝列表失败: " + e.getMessage());
        }
    }
}
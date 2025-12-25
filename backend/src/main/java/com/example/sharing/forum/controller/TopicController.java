package com.example.sharing.forum.controller;

import com.example.sharing.dto.ApiResponse;
import com.example.sharing.forum.dto.*;
import com.example.sharing.forum.entity.ActionType;
import com.example.sharing.forum.service.ForumUserActionService;
import com.example.sharing.forum.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final ForumUserActionService forumUserActionService;

    /**
     * 按论坛ID查找论坛下的话题，支持分页查询
     */
    @GetMapping("/by-forum/{forumNo}")
    public ApiResponse<PageResponse<TopicDTO>> getTopicsByForumId(
            @PathVariable Long forumNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) Long currentUserId) {

        try {
            Sort sort = getSortFromParams(sortBy, direction);
            PageResponse<TopicDTO> response = topicService.getTopicsByForumNo(forumNo, page, size, sort);
            return ApiResponse.success("获取话题列表成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("获取话题列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/by-topic/{topicId}")
    public ApiResponse<TopicDTO> getTopicById(@PathVariable Long topicId) {
        try {
            Optional<TopicDTO> topicOpt = topicService.getTopicByIdWithAuthor(topicId);
            return topicOpt.map(topic -> ApiResponse.success("获取话题详情成功", topic))
                    .orElse(ApiResponse.fail("话题不存在"));
        } catch (Exception e) {
            return ApiResponse.fail("获取话题详情失败: " + e.getMessage());
        }
    }

    /**
     * 允许用户创建话题
     */
    @PostMapping(value = "/create/{userId}", consumes = {"multipart/form-data"})
    public ApiResponse<TopicDTO> createTopic(
            @Valid @ModelAttribute CreateTopicRequest request,
            @PathVariable Long userId) {

        try {
            // 验证用户ID
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }

            TopicDTO topic = topicService.createTopic(request, userId);
            return ApiResponse.success("创建话题成功", topic);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("参数错误: " + e.getMessage());
        } catch (RuntimeException e) {
            return ApiResponse.fail("创建失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("系统错误，请稍后重试");
        }
    }

    /**
     * 更新话题 - 支持附件上传和删除
     */
    @PutMapping(value = "/update/{topicId}", consumes = {"multipart/form-data"})
    public ApiResponse<TopicDTO> updateTopic(
            @PathVariable Long topicId,
            @Valid @ModelAttribute UpdateTopicRequest request,
            @RequestParam Long userId) {

        try {
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }

            Optional<TopicDTO> topicOpt = topicService.updateTopic(topicId, request, userId);
            return topicOpt.map(topic -> ApiResponse.success("更新话题成功", topic))
                    .orElse(ApiResponse.fail("话题不存在"));
        } catch (RuntimeException e) {
            return ApiResponse.fail("更新失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("系统错误，请稍后重试");
        }
    }

    @DeleteMapping("/delete/{topicId}")
    public ApiResponse<Void> deleteTopic(
            @PathVariable Long topicId,
            @RequestParam Long userId) {

        try {
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }

            boolean deleted = topicService.deleteTopic(topicId, userId);
            return deleted ?
                    ApiResponse.success("删除话题成功", null) :
                    ApiResponse.fail("话题不存在");
        } catch (RuntimeException e) {
            return ApiResponse.fail("删除失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("系统错误，请稍后重试");
        }
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<PageResponse<TopicDTO>> getUserTopics(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageResponse<TopicDTO> response = topicService.getUserTopics(userId, page, size);
            return ApiResponse.success("获取用户话题列表成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("获取用户话题列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<TopicDTO>> searchTopics(
            @RequestParam(required = false) Long forumId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageResponse<TopicDTO> response = topicService.searchTopicsWithAuthor(forumId, keyword, page, size);
            return ApiResponse.success("搜索成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("搜索失败: " + e.getMessage());
        }
    }

    @GetMapping("/hot")
    public ApiResponse<PageResponse<TopicDTO>> getHotTopics(
            @RequestParam(required = false) Long forumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageResponse<TopicDTO> response = topicService.getHotTopics(forumId, page, size);
            return ApiResponse.success("获取热门话题成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("获取热门话题失败: " + e.getMessage());
        }
    }

    /**
     * 点赞话题
     */
    @PostMapping("/like/{topicId}")
    public ApiResponse<Void> likeTopic(
            @PathVariable Long topicId,
            @RequestParam Long userId) {
        try {
            boolean success = forumUserActionService.performAction(
                    userId, ActionType.ACTION_TYPE_LIKE_TOPIC, topicId);
            return success ?
                    ApiResponse.success("点赞成功", null) :
                    ApiResponse.fail("点赞失败");
        } catch (Exception e) {
            return ApiResponse.fail("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 取消点赞话题
     */
    @PostMapping("/unlike/{topicId}")
    public ApiResponse<Void> unlikeTopic(
            @PathVariable Long topicId,
            @RequestParam Long userId) {
        try {
            boolean success = forumUserActionService.cancelAction(
                    userId, ActionType.ACTION_TYPE_LIKE_TOPIC, topicId);
            return success ?
                    ApiResponse.success("取消点赞成功", null) :
                    ApiResponse.fail("取消点赞失败");
        } catch (Exception e) {
            return ApiResponse.fail("取消点赞失败: " + e.getMessage());
        }
    }

    /**
     * 收藏话题
     */
    @PostMapping("/collect/{topicId}")
    public ApiResponse<Void> collectTopic(
            @PathVariable Long topicId,
            @RequestParam Long userId) {
        try {
            boolean success = forumUserActionService.performAction(
                    userId, ActionType.ACTION_TYPE_COLLECT_TOPIC, topicId);
            return success ?
                    ApiResponse.success("收藏成功", null) :
                    ApiResponse.fail("收藏失败");
        } catch (Exception e) {
            return ApiResponse.fail("收藏失败: " + e.getMessage());
        }
    }

    /**
     * 取消收藏话题
     */
    @PostMapping("/uncollect/{topicId}")
    public ApiResponse<Void> uncollectTopic(
            @PathVariable Long topicId,
            @RequestParam Long userId) {
        try {
            boolean success = forumUserActionService.cancelAction(
                    userId, ActionType.ACTION_TYPE_COLLECT_TOPIC, topicId);
            return success ?
                    ApiResponse.success("取消收藏成功", null) :
                    ApiResponse.fail("取消收藏失败");
        } catch (Exception e) {
            return ApiResponse.fail("取消收藏失败: " + e.getMessage());
        }
    }

    /**
     * 根据参数构建排序对象
     */
    private Sort getSortFromParams(String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy.toLowerCase()) {
            case "viewcount" -> Sort.by(sortDirection, "viewCount");
            case "likecount" -> Sort.by(sortDirection, "likeCount");
            case "replycount" -> Sort.by(sortDirection, "replyCount");
            case "collectcount" -> Sort.by(sortDirection, "collectCount");
            default -> Sort.by(sortDirection, "createdAt");
        };
    }
}
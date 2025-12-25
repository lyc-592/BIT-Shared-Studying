package com.example.sharing.forum.controller;

import com.example.sharing.dto.ApiResponse;
import com.example.sharing.forum.dto.*;
import com.example.sharing.forum.entity.ActionType;
import com.example.sharing.forum.service.CommentService;
import com.example.sharing.forum.service.ForumUserActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final ForumUserActionService forumUserActionService;

    /**
     * 获取评论详情
     */
    @GetMapping("/{commentId}")
    public ApiResponse<CommentDTO> getCommentById(@PathVariable Long commentId) {
        try {
            CommentDTO response = commentService.getCommentById(commentId);
            return ApiResponse.success("获取评论: ", response);
        } catch (Exception e) {
            return ApiResponse.fail("获取评论详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取话题的一级评论（分页）
     */
    @GetMapping("/root/{topicId}")
    public ApiResponse<PageResponse<CommentDTO>> getRootComments(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort sort = getSortFromParams(sortBy, direction);
            PageResponse<CommentDTO> response = commentService.getRootCommentsByTopicId(topicId, page, size, sort);
            return ApiResponse.success("获取评论成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("获取评论失败: " + e.getMessage());
        }
    }

    /**
     * 获取评论详情（包含统计信息）
     */
    @GetMapping("/detail/{commentId}")
    public ApiResponse<CommentDetailDTO> getCommentDetail(@PathVariable Long commentId) {
        try {
            CommentDetailDTO detail = commentService.getCommentDetail(commentId);
            return ApiResponse.success("获取评论详情成功", detail);
        } catch (Exception e) {
            return ApiResponse.fail("获取评论详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取一级评论的二级评论（分页）
     */
    @GetMapping("/{rootId}/replies")
    public ApiResponse<PageResponse<CommentDTO>> getReplies(
            @PathVariable Long rootId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            PageResponse<CommentDTO> response = commentService.getRepliesByRootId(rootId, page, size);
            return ApiResponse.success("获取回复成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("获取回复失败: " + e.getMessage());
        }
    }

    /**
     * 获取评论统计信息（不包含评论内容）
     */
    @GetMapping("/{topicId}/stats")
    public ApiResponse<CommentStatsDTO> getCommentStats(@PathVariable Long topicId) {
        try {
            CommentStatsDTO stats = commentService.getCommentStats(topicId);
            return ApiResponse.success("获取评论统计成功", stats);
        } catch (Exception e) {
            return ApiResponse.fail("获取评论统计失败: " + e.getMessage());
        }
    }

    /**
     * 创建评论
     */
    @PostMapping(value = "/create/{userId}", consumes = {"multipart/form-data"})
    public ApiResponse<CommentDTO> createComment(
            @Valid @ModelAttribute CreateCommentRequest request,
            @PathVariable Long userId) {

        try {
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }

            CommentDTO comment = commentService.createComment(request, userId);
            return ApiResponse.success("创建评论成功", comment);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("参数错误: " + e.getMessage());
        } catch (RuntimeException e) {
            return ApiResponse.fail("创建失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("系统错误，请稍后重试");
        }
    }

    /**
     * 更新评论
     */
    @PutMapping(value = "/update/{commentId}", consumes = {"multipart/form-data"})
    public ApiResponse<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @Valid @ModelAttribute UpdateCommentRequest request,
            @RequestParam Long userId) {

        try {
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }

            Optional<CommentDTO> commentOpt = commentService.updateComment(commentId, request, userId);
            return commentOpt.map(comment -> ApiResponse.success("更新评论成功", comment))
                    .orElse(ApiResponse.fail("评论不存在"));
        } catch (RuntimeException e) {
            return ApiResponse.fail("更新失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("系统错误，请稍后重试");
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/delete/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {

        try {
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }

            boolean deleted = commentService.deleteComment(commentId, userId);
            return deleted ?
                    ApiResponse.success("删除评论成功", null) :
                    ApiResponse.fail("评论不存在");
        } catch (RuntimeException e) {
            return ApiResponse.fail("删除失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("系统错误，请稍后重试");
        }
    }

    /**
     * 获取用户的评论列表
     */
    @GetMapping("/by-user/{userId}")
    public ApiResponse<PageResponse<CommentDTO>> getUserComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageResponse<CommentDTO> response = commentService.getUserComments(userId, page, size);
            return ApiResponse.success("获取用户评论列表成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("获取用户评论列表失败: " + e.getMessage());
        }
    }

    /**
     * 点赞评论
     */
    @PostMapping("/like/{commentId}")
    public ApiResponse<Void> likeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            boolean success = forumUserActionService.performAction(
                    userId, ActionType.ACTION_TYPE_LIKE_COMMENT, commentId);
            return success ?
                    ApiResponse.success("点赞成功", null) :
                    ApiResponse.fail("点赞失败");
        } catch (Exception e) {
            return ApiResponse.fail("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 取消点赞评论
     */
    @PostMapping("/unlike/{commentId}")
    public ApiResponse<Void> unlikeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            boolean success = forumUserActionService.cancelAction(
                    userId, ActionType.ACTION_TYPE_LIKE_COMMENT, commentId);
            return success ?
                    ApiResponse.success("取消点赞成功", null) :
                    ApiResponse.fail("取消点赞失败");
        } catch (Exception e) {
            return ApiResponse.fail("取消点赞失败: " + e.getMessage());
        }
    }

    /**
     * 搜索评论
     */
    @GetMapping("/search")
    public ApiResponse<PageResponse<CommentDTO>> searchComments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageResponse<CommentDTO> response = commentService.searchComments(keyword, userId, topicId, page, size);
            return ApiResponse.success("搜索评论成功", response);
        } catch (Exception e) {
            return ApiResponse.fail("搜索评论失败: " + e.getMessage());
        }
    }

    /**
     * 根据参数构建排序对象
     */
    private Sort getSortFromParams(String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy.toLowerCase()) {
            case "likecount" -> Sort.by(sortDirection, "likeCount");
            case "replycount" -> Sort.by(sortDirection, "replyCount");
            default -> Sort.by(sortDirection, "createdAt");
        };
    }
}
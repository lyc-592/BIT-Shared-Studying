package com.example.sharing.forum.dto;

import com.example.sharing.profile.UserProfileDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CommentDTO: 基本评论信息，用于列表展示、创建/更新返回等通用场景
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private Long topicId;
    private String content;
    private Long parentId;

    // 评论者信息
    private UserProfileDTO author;

    // 被回复的用户信息（新增）
    private Long targetUserId;
    private String targetUsername;

    // 统计信息
    private Integer likeCount;

    // 状态
    private Integer status;

    // 层级（0: 一级评论，1: 二级评论）
    private Integer level;

    // 时间信息
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Integer replyCount;

    private List<AttachmentDTO> attachments;
}
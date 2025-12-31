package com.example.sharing.forum.dto;

import lombok.Data;

import java.util.List;

/**
 * CommentDetailDTO: 评论详情，包含额外信息（回复统计、预览回复等），用于详情页
 */
@Data
public class CommentDetailDTO {
    // 评论基本信息
    private CommentDTO comment;

    // 如果是父评论，包含子评论统计
    private Integer replyCount;

    // 子评论预览（前几条）
    private List<CommentDTO> previewReplies;

    // 子评论分页信息
    private Integer replyPageNumber = 0;
    private Integer replyPageSize = 10;
    private Integer replyTotalPages = 0;
    private Long replyTotalElements = 0L;
}
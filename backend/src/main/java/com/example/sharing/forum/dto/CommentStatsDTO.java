package com.example.sharing.forum.dto;

import com.example.sharing.profile.UserProfileDTO;
import lombok.Data;

@Data
public class CommentStatsDTO {
    // 话题的评论总数
    private Long totalComments;

    // 一级评论数量
    private Long rootCommentCount;

    // 二级评论数量
    private Long replyCount;

    // 最新评论时间
    private String latestCommentTime;

    // 最热评论（点赞最多）
    private CommentDTO hottestComment;

    // 最活跃用户
    private UserProfileDTO mostActiveUser;
    private Integer userCommentCount;
}
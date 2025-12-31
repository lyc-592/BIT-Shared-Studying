package com.example.sharing.forum.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommentStatusRequest {
    private List<Long> commentIds;
    private List<Long> authorIds; // 作者ID列表，用于查询关注状态
}
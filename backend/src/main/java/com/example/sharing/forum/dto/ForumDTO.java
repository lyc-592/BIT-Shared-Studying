package com.example.sharing.forum.dto;

import lombok.Data;

@Data
public class ForumDTO {
    private Long forumNo; // 论坛编号（课程号）
    private String courseName; // 课程名称（从 Course 对象获取）
    private Integer topicCount;
}
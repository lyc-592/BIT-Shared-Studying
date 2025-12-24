package com.example.sharing.forum.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Forum {
    private Long forumNo; // 论坛编号（课程号）
    private Integer topicCount = 0;

    // 构造函数
    public Forum(Long forumNo) {
        this.forumNo = forumNo;
        this.topicCount = 0;
    }

    public Forum(Long forumNo, Integer topicCount) {
        this.forumNo = forumNo;
        this.topicCount = topicCount != null ? topicCount : 0;
    }
}
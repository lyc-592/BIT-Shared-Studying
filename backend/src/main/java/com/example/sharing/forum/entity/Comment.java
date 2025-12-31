package com.example.sharing.forum.entity;

import com.example.sharing.profile.UserProfile;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "forum_comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", referencedColumnName = "id",
            insertable = false, updatable = false)
    private Topic topic;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id",
            insertable = false, updatable = false)
    private UserProfile userProfile;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 父评论ID
    @Column(name = "parent_id")
    private Long parentId;

    // 根评论ID（用于快速查询整个评论树）
    @Column(name = "root_id")
    private Long rootId;

    // 评论层级（0: 一级评论，1: 二级评论，以此类推）
    @Column(name = "level")
    private Integer level = 0;

    // 排序字段（用于同一父评论下的排序）
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "status")
    private Integer status = Status.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("comment_id is not null")
    private List<Attachment> attachments;

    // 自关联，用于获取子评论
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    @OrderBy("sortOrder ASC")
    private List<Comment> childComments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (parentId == null) {
            rootId = null;
            level = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
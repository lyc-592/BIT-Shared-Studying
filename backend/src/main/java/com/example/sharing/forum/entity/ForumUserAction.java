package com.example.sharing.forum.entity;

import com.example.sharing.core.entity.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "forum_user_action",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "action_type", "target_id"}))
@EqualsAndHashCode(callSuper = false)
public class ForumUserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId; // 为了方便查询，添加冗余字段

    @Column(name = "action_type", nullable = false)
    private Integer actionType; // 1-点赞话题，2-点赞评论，3-收藏话题，4-关注用户

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "is_cancel")
    private Boolean isCancel = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
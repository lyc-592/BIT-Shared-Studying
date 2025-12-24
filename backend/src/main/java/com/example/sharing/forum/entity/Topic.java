package com.example.sharing.forum.entity;

import com.example.sharing.profile.UserProfile;
import com.example.sharing.entity.Course;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "forum_topic")
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 直接存储课程号，并建立外键约束到Course表
    @Column(name = "forum_no", nullable = false)
    private Long forumNo;

    // 与Course表建立外键关联，确保课程存在
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_no", referencedColumnName = "course_no",
            insertable = false, updatable = false)
    private Course course;

    // 添加实际的userId字段用于存储外键
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 关联UserProfile对象
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id",
            insertable = false, updatable = false)
    private UserProfile userProfile;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "reference_path")
    private String referencePath;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "reply_count")
    private Integer replyCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "collect_count")
    private Integer collectCount = 0;

    @Column(name = "status")
    private Integer status = Status.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 关联附件
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments;

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
package com.example.sharing.forum.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "forum_attachment")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private Topic topic;

    @Column(name = "topic_id")
    private Long topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", insertable = false, updatable = false)
    private Comment comment;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "original_name", nullable = false)
    private String originalName;  // 原始文件名

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;  // 存储标识（现在是本地路径，未来可以是OSS Key）

    @Column(name = "file_type", length = 100)
    private String fileType;  // MIME类型

    @Column(name = "file_size")
    private Long fileSize;  // 文件大小（字节）

    @Column(name = "storage_type", length = 20)
    private String storageType = "local";  // 存储类型：local/oss/cos等

    @Column(name = "access_url", length = 1000)
    private String accessUrl;  // 访问URL（可动态生成，也可存储）

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
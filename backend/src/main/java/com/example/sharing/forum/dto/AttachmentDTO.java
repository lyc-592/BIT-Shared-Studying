package com.example.sharing.forum.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentDTO {
    private Long id;

    // 文件信息
    private String originalName;  // 原始文件名
    private String accessUrl;     // 文件访问URL（前端直接使用）
    private String fileType;      // 文件MIME类型
    private Long fileSize;        // 文件大小（字节）

    // 存储信息（可用于调试或管理）
    private String storageType;   // 存储类型：local/oss/cos等

    // 关联信息
    private Long userId;          // 上传者ID
    private Long topicId;         // 关联的话题ID（如果属于话题）
    private Long commentId;       // 关联的评论ID（如果属于评论）

    // 时间信息
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // ========== 新增预览相关字段 ==========
    // 是否为图片
    private Boolean isImage;

    // 是否为可预览文件（图片、PDF等）
    private Boolean isPreviewable;

    // 预览类型（image/pdf/other）
    private String previewType;

    // 预览URL（与accessUrl相同，但更明确）
    private String previewUrl;

    // 缩略图URL（如果有的话）
    private String thumbnailUrl;
}
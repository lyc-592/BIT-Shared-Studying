package com.example.sharing.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentPreviewInfoDTO {
    private Long id;
    private String originalName;
    private String accessUrl;
    private String fileType;
    private Long fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // 文件类型标识
    private Boolean isImage;
    private Boolean isPdf;
    private Boolean isText;
    private Boolean isOffice;

    // 图片尺寸信息（如果有）
    private Integer width;
    private Integer height;
}
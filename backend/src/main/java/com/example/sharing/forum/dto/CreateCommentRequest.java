package com.example.sharing.forum.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateCommentRequest {

    @NotNull(message = "话题ID不能为空")
    private Long topicId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    // 父评论ID（如果是回复评论）
    private Long parentId;

    // 根评论ID（用于快速构建评论树）
    private Long rootId;

    // 附件列表
    private List<MultipartFile> attachments;
}
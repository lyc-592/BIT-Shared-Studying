package com.example.sharing.forum.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class UpdateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    // 新上传的附件
    private List<MultipartFile> attachments;

    // 要删除的附件ID（JSON数组或逗号分隔）
    private String attachmentIdsToDelete;
}

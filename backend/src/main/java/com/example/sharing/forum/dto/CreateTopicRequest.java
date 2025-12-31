package com.example.sharing.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CreateTopicRequest {

    @NotNull(message = "论坛ID不能为空")
    private Long forumNo;

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度在1-200字符之间")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(min = 1, message = "内容不能为空")
    private String content;

    private String referencePath;

    // 附件文件列表
    private List<MultipartFile> attachments;
}
package com.example.sharing.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UpdateTopicRequest {

    @Size(min = 1, max = 200, message = "标题长度在1-200字符之间")
    private String title;

    @Size(min = 1, message = "内容不能为空")
    private String content;

    private String referencePath;

    // 新增：上传的附件文件列表
    private List<MultipartFile> attachments;

    // 新增：要删除的附件ID列表
    private String attachmentIdsToDelete;
}
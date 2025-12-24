package com.example.sharing.forum.dto;

import com.example.sharing.profile.UserProfileDTO;
import com.example.sharing.dto.CourseDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicDTO {
    private Long id;
    private String title;
    private String content;
    private Long forumNo; // 改为论坛编号（课程号）

    private String referencePath;

    private CourseDto course;
    // 作者信息
    private UserProfileDTO author;

    // 统计信息
    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer collectCount;

    // 附件列表
    private List<AttachmentDTO> attachments;

    // 状态
    private Integer status;

    // 时间信息
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

}
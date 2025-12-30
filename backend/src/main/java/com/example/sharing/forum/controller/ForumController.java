package com.example.sharing.forum.controller;

import com.example.sharing.core.dto.ApiResponse;
import com.example.sharing.core.entity.Course;
import com.example.sharing.forum.dto.ForumDTO;
import com.example.sharing.forum.entity.Forum;
import com.example.sharing.forum.service.ForumService;
import com.example.sharing.core.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/forums")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final CourseRepository courseRepository;

    /**
     * 根据课程号获取论坛信息
     */
    @GetMapping("/by-course/{courseNo}")
    public ApiResponse<ForumDTO> getForumByCourseNo(@PathVariable Long courseNo) {
        // 获取课程名称
        String courseName = courseRepository.findById(courseNo)
                .map(Course::getCourseName)
                .orElse(null);

        if (courseName == null) {
            return ApiResponse.fail("课程不存在");
        }

        // 获取论坛信息
        Optional<Forum> forumOpt = forumService.getForumByCourseNo(courseNo);

        if (forumOpt.isEmpty()) {
            return ApiResponse.fail("论坛信息获取失败");
        }

        // 转换为DTO
        ForumDTO forumDTO = forumService.convertToDTO(forumOpt.get(), courseName);

        return ApiResponse.success("论坛信息获取成功", forumDTO);
    }
}
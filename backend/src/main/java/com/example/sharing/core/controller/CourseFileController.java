package com.example.sharing.core.controller;


import com.example.sharing.core.dto.FileNode;
import com.example.sharing.core.service.CourseFileService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/course")
public class CourseFileController {

    private final CourseFileService courseFileService;

    public CourseFileController(CourseFileService courseFileService) {
        this.courseFileService = courseFileService;
    }

    /**
     * GET /api/course/{courseId}/file-tree
     */
    @GetMapping("/{courseId}/file-tree")
    public List<FileNode> getCourseFileTree(@PathVariable String courseId) throws IOException {
        return courseFileService.getCourseFileTree(courseId);
    }
}
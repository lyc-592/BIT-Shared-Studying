package com.example.sharing.controller;

import com.example.sharing.dto.ApiResponse;
import com.example.sharing.dto.CourseDto;
import com.example.sharing.dto.MajorDto;
import com.example.sharing.service.MajorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/majors")
public class MajorController {

    private final MajorService majorService;

    public MajorController(MajorService majorService) {
        this.majorService = majorService;
    }

    /**
     * GET /api/majors
     * 登录后前端立刻调用，获取所有专业
     */
    @GetMapping
    public ApiResponse<List<MajorDto>> getAllMajors() {
        List<MajorDto> majors = majorService.getAllMajors();
        return ApiResponse.success("获取专业列表成功", majors);
    }

    /**
     * GET /api/majors/{majorNo}/courses
     * 用户点击某个专业后调用，获取该专业下所有课程
     */
    @GetMapping("/{majorNo}/courses")
    public ApiResponse<List<CourseDto>> getCoursesByMajor(
            @PathVariable("majorNo") Long majorNo) {

        List<CourseDto> courses = majorService.getCoursesByMajorNo(majorNo);
        return ApiResponse.success("获取课程列表成功", courses);
    }
}
package com.example.sharing.core.dto;

public class CourseDto {

    private Long courseNo;
    private String courseName;

    public CourseDto() {
    }

    public CourseDto(Long courseNo, String courseName) {
        this.courseNo = courseNo;
        this.courseName = courseName;
    }

    public Long getCourseNo() {
        return courseNo;
    }

    public String getCourseName() {
        return courseName;
    }
}
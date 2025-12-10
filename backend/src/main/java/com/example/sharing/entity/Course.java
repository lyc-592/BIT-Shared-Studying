package com.example.sharing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "course")
public class Course {

    // 课程号：主键，纯数字（Long）
    @Id
    @Column(name = "course_no")
    private Long courseNo;

    // 课程名称
    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    public Course() {
    }

    public Course(Long courseNo, String courseName) {
        this.courseNo = courseNo;
        this.courseName = courseName;
    }

    public Long getCourseNo() {
        return courseNo;
    }

    public void setCourseNo(Long courseNo) {
        this.courseNo = courseNo;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
}
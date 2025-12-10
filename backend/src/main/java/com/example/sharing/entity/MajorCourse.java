package com.example.sharing.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "major_course",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_no", "major_no"})
)
public class MajorCourse {

    // 联系序号：自增主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_id")
    private Long relationId;

    // 课程号外键
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_no", nullable = false)
    private Course course;

    // 专业号外键
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_no", nullable = false)
    private Major major;

    public MajorCourse() {
    }

    public MajorCourse(Course course, Major major) {
        this.course = course;
        this.major = major;
    }

    public Long getRelationId() {
        return relationId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Major getMajor() {
        return major;
    }

    public void setMajor(Major major) {
        this.major = major;
    }
}
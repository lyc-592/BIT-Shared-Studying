package com.example.sharing.role;


import com.example.sharing.core.entity.Course;
import com.example.sharing.core.entity.User;
import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Table(
        name = "user_course_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_no"})
)
public class UserCoursePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 对应 user 表
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 对应 course 表，主键是 course_no
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_no", referencedColumnName = "course_no", nullable = false)
    private Course course;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Course getCourse() {
        return course;
    }

}
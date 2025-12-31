package com.example.sharing.role;


import com.example.sharing.core.entity.User;
import com.example.sharing.core.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCoursePermissionRepository
        extends JpaRepository<UserCoursePermission, Long> {

    boolean existsByUserAndCourse(User user, Course course);

    List<UserCoursePermission> findByUser(User user);

    void deleteByUser(User user);

    void deleteByUserAndCourse(User user, Course course);
}
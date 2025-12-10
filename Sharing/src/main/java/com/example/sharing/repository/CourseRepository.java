// CourseRepository.java
package com.example.sharing.repository;

import com.example.sharing.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // 主键类型 Long（courseNo）
}
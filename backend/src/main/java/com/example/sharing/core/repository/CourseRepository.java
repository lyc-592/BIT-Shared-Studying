// CourseRepository.java
package com.example.sharing.core.repository;

import com.example.sharing.core.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // 主键类型 Long（courseNo）
}
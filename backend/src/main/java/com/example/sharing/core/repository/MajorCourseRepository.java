// MajorCourseRepository.java
package com.example.sharing.core.repository;

import com.example.sharing.core.entity.Major;
import com.example.sharing.core.entity.MajorCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MajorCourseRepository extends JpaRepository<MajorCourse, Long> {

    // 根据专业查出所有关系记录
    List<MajorCourse> findByMajor(Major major);
}
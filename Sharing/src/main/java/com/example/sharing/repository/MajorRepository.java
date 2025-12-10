// MajorRepository.java
package com.example.sharing.repository;

import com.example.sharing.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MajorRepository extends JpaRepository<Major, Long> {
    // 主键类型 Long（majorNo）
}
package com.example.sharing.message.repository;

import com.example.sharing.message.entity.FileUploadRequest;
import com.example.sharing.message.enums.FileUploadStatus;
import com.example.sharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileUploadRequestRepository extends JpaRepository<FileUploadRequest, Long> {

    Optional<FileUploadRequest> findByIdAndStatus(Long id, FileUploadStatus status);

    // 可扩展按课程+状态查询
}
package com.example.sharing.message.repository;

import com.example.sharing.message.entity.AdminApply;
import com.example.sharing.message.enums.ApplyStatus;
import com.example.sharing.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminApplyRepository extends JpaRepository<AdminApply, Long> {

    // 防止重复申请：一个用户对一个专业只允许存在一个 PENDING
    Optional<AdminApply> findByApplicantAndMajorNoAndStatus(User applicant, Long majorNo, ApplyStatus status);
}
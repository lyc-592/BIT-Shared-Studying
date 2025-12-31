package com.example.sharing.message.dto;

import com.example.sharing.message.enums.ApplyStatus;

import java.time.LocalDateTime;

public class AdminApplyDTO {

    private Long id;
    private Long applicantId;
    private String applicantName;
    private Long majorNo;
    private ApplyStatus status;
    private String wordFilePath;
    private LocalDateTime createdAt;
    private String rejectReason;

    public AdminApplyDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Long getMajorNo() {
        return majorNo;
    }

    public void setMajorNo(Long majorNo) {
        this.majorNo = majorNo;
    }

    public ApplyStatus getStatus() {
        return status;
    }

    public void setStatus(ApplyStatus status) {
        this.status = status;
    }

    public String getWordFilePath() {
        return wordFilePath;
    }

    public void setWordFilePath(String wordFilePath) {
        this.wordFilePath = wordFilePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
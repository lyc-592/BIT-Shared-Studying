package com.example.sharing.message.dto;

import com.example.sharing.message.enums.FileUploadStatus;

import java.time.LocalDateTime;

public class FileUploadRequestDTO {

    private Long id;
    private Long requesterId;
    private String requesterName;
    private Long courseNo;
    private String originalFilename;
    private String pendingFilePath;
    private String finalFilePath;
    private FileUploadStatus status;
    private LocalDateTime createdAt;
    private String rejectReason;

    private String aiSuggestAction;
    private String aiSuggestReason;

    public FileUploadRequestDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public Long getCourseNo() {
        return courseNo;
    }

    public void setCourseNo(Long courseNo) {
        this.courseNo = courseNo;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getPendingFilePath() {
        return pendingFilePath;
    }

    public void setPendingFilePath(String pendingFilePath) {
        this.pendingFilePath = pendingFilePath;
    }

    public String getFinalFilePath() {
        return finalFilePath;
    }

    public void setFinalFilePath(String finalFilePath) {
        this.finalFilePath = finalFilePath;
    }

    public FileUploadStatus getStatus() {
        return status;
    }

    public void setStatus(FileUploadStatus status) {
        this.status = status;
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

    public String getAiSuggestAction() {
        return aiSuggestAction;
    }

    public void setAiSuggestAction(String aiSuggestAction) {
        this.aiSuggestAction = aiSuggestAction;
    }

    public String getAiSuggestReason() {
        return aiSuggestReason;
    }

    public void setAiSuggestReason(String aiSuggestReason) {
        this.aiSuggestReason = aiSuggestReason;
    }
}
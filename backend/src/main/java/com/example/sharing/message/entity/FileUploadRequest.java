package com.example.sharing.message.entity;

import com.example.sharing.message.enums.FileUploadStatus;
import com.example.sharing.core.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_upload_request")
public class FileUploadRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 申请人
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // 课程编号
    @Column(name = "course_no", nullable = false)
    private Long courseNo;

    // 原始文件名
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    // 待确认目录路径
    @Column(name = "pending_file_path", nullable = false, length = 512)
    private String pendingFilePath;

    // 审核通过后正式保存路径
    @Column(name = "final_file_path", length = 512)
    private String finalFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileUploadStatus status = FileUploadStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 审核人
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    // AI 建议（预留）
    @Column(name = "ai_suggest_action", length = 20)
    private String aiSuggestAction;

    @Column(name = "ai_suggest_reason", columnDefinition = "TEXT")
    private String aiSuggestReason;

    // 用户在申请时指定的最终绝对路径
    @Column(name = "target_absolute_path", length = 512)
    private String targetAbsolutePath;

    public String getTargetAbsolutePath() {
        return targetAbsolutePath;
    }

    public void setTargetAbsolutePath(String targetAbsolutePath) {
        this.targetAbsolutePath = targetAbsolutePath;
    }

    public FileUploadRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
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

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
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
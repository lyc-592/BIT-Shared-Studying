package com.example.sharing.message.dto;

public class MessageDetailDTO {

    private MessageDTO message;

    private AdminApplyDTO adminApply;

    private FileUploadRequestDTO fileUploadRequest;

    public MessageDetailDTO() {
    }

    public MessageDTO getMessage() {
        return message;
    }

    public void setMessage(MessageDTO message) {
        this.message = message;
    }

    public AdminApplyDTO getAdminApply() {
        return adminApply;
    }

    public void setAdminApply(AdminApplyDTO adminApply) {
        this.adminApply = adminApply;
    }

    public FileUploadRequestDTO getFileUploadRequest() {
        return fileUploadRequest;
    }

    public void setFileUploadRequest(FileUploadRequestDTO fileUploadRequest) {
        this.fileUploadRequest = fileUploadRequest;
    }
}
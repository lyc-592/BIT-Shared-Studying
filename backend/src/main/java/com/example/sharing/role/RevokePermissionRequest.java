package com.example.sharing.role;

public class RevokePermissionRequest {

    // 撤销者ID（仍然用ID）
    private Long revokerId;

    // 被撤销者用户名
    private String targetUsername;

    public Long getRevokerId() {
        return revokerId;
    }

    public void setRevokerId(Long revokerId) {
        this.revokerId = revokerId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }
}
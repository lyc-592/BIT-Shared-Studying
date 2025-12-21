package com.example.sharing.role;

public class GrantPermissionRequest {

    // 授权者ID
    private Long grantorId;

    // 被授权者用户名
    private String targetUsername;

    // 目标角色等级（1~4）
    private Integer targetRole;

    // 如果 targetRole = 2，需要指定专业号
    private Long majorNo;

    public Long getGrantorId() {
        return grantorId;
    }

    public void setGrantorId(Long grantorId) {
        this.grantorId = grantorId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public Integer getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(Integer targetRole) {
        this.targetRole = targetRole;
    }

    public Long getMajorNo() {
        return majorNo;
    }

    public void setMajorNo(Long majorNo) {
        this.majorNo = majorNo;
    }
}
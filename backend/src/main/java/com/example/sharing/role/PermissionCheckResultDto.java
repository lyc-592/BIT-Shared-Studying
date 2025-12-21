package com.example.sharing.role;

public class PermissionCheckResultDto {

    private boolean hasPermission;

    public PermissionCheckResultDto() {
    }

    public PermissionCheckResultDto(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }
}

package com.example.sharing.message.enums;

public enum MessageType {

    // 1：管理员申请请求（普通用户 → 3/4 级）
    ADMIN_APPLY_REQUEST,

    // 2：管理员申请结果（系统/管理员 → 普通用户）
    ADMIN_APPLY_RESULT,

    // 3：文件上传请求（普通用户 → 课程管理员）
    FILE_UPLOAD_REQUEST,

    // 4：文件上传结果（系统/管理员 → 普通用户）
    FILE_UPLOAD_RESULT
}
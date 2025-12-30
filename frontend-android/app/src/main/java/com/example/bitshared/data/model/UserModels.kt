package com.example.bitshared.data.model

// Auth 相关请求
data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val password: String, val email: String)

// 用户基本信息 (Auth 接口返回)
data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val role: Int
)

// 详细个人信息 (Profile 接口返回)
data class ProfileDto(
    val userId: Long,
    val username: String,
    val nickname: String?,
    val bio: String?,
    val major: String?,
    val email: String,
    val role: Int
)

// 创建/更新 Profile 请求
data class ProfileRequest(
    val userId: Long? = null, // 创建时需要，更新时不需要
    val nickname: String,
    val bio: String,
    val major: String
)

data class PermissionResult(val hasPermission: Boolean)
data class GrantRequest(val grantorId: Long, val targetUsername: String, val targetRole: Int, val majorNo: Long?)
data class RevokeRequest(val revokerId: Long, val targetUsername: String)
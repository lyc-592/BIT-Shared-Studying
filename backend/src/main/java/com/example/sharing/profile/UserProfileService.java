package com.example.sharing.profile;

public interface UserProfileService {

    // 1. 设置 / 创建个人信息（首次）
    UserProfileDTO createProfile(Long userId, String nickname, String bio, String major);

    // 2. 更新个人信息
    UserProfileDTO updateProfile(Long userId, String nickname, String bio, String major);

    // 3. 获取个人信息
    UserProfileDTO getProfile(Long userId);
}
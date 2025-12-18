package com.example.sharing.Profile;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    // 1. 首次设置个人信息
    @PostMapping
    public UserProfileDTO createProfile(@RequestBody UserProfileRequest request) {
        if (request.getUserId() == null) {
            throw new RuntimeException("userId 不能为空");
        }
        return userProfileService.createProfile(
                request.getUserId(),
                request.getNickname(),
                request.getBio(),
                request.getMajor()
        );
    }

    // 2. 更新个人信息
    @PutMapping("/{userId}")
    public UserProfileDTO updateProfile(@PathVariable Long userId,
                                        @RequestBody UserProfileRequest request) {
        return userProfileService.updateProfile(
                userId,
                request.getNickname(),
                request.getBio(),
                request.getMajor()
        );
    }

    // 3. 获取个人信息
    @GetMapping("/{userId}")
    public UserProfileDTO getProfile(@PathVariable Long userId) {
        return userProfileService.getProfile(userId);
    }
}
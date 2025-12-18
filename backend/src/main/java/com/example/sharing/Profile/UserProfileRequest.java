package com.example.sharing.Profile;

public class UserProfileRequest {

    // 首次创建时用；如果你从登录信息里拿 userId，可以不放在请求体里
    private Long userId;

    private String nickname;
    private String bio;
    private String major;

    // ===== 无参构造（反序列化需要）=====
    public UserProfileRequest() {
    }

    // ===== 可选：带全部参数的构造方法 =====
    public UserProfileRequest(Long userId, String nickname, String bio, String major) {
        this.userId = userId;
        this.nickname = nickname;
        this.bio = bio;
        this.major = major;
    }

    // ===== Getter / Setter 全写出来 =====

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }
}
package com.example.sharing.Profile;


public class UserProfileDTO {

    private Long userId;
    private String username;
    private String nickname;
    private String bio;
    private String major;
    private String email;
    private Integer role;

    // ===== 无参构造 =====
    public UserProfileDTO() {
    }

    // ===== 全参构造（可选，看你习惯）=====
    public UserProfileDTO(Long userId, String username, String nickname,
                          String bio, String major, String email, Integer role) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.bio = bio;
        this.major = major;
        this.email = email;
        this.role = role;
    }

    // ===== Getter / Setter =====

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }
}
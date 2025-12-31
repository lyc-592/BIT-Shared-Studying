package com.example.sharing.profile;

import com.example.sharing.core.entity.User;
import jakarta.persistence.*;


@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 自己的主键，对应表里的 id

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;   // 外键到 users.id

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "major", length = 100)
    private String major;

    // ===== 构造方法 =====
    public UserProfile() {
    }

    public UserProfile(User user, String nickname, String bio, String major) {
        this.user = user;
        this.nickname = nickname;
        this.bio = bio;
        this.major = major;
    }

    // ===== Getter / Setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
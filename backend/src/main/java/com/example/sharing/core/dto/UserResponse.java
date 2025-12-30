package com.example.sharing.core.dto;


/**
 * 返回给前端的用户信息：不包含密码
 */
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Integer role;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, Integer role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
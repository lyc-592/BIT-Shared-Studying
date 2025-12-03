package com.example.sharing.controller;


import com.example.sharing.dto.ApiResponse;
import com.example.sharing.dto.LoginRequest;
import com.example.sharing.dto.RegisterRequest;
import com.example.sharing.dto.UserResponse;
import com.example.sharing.entity.User;
import com.example.sharing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 注册
     * 请求体：{ "username": "...", "password": "...", "email": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail()
            );

            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole()
            );

            return ResponseEntity.ok(
                    ApiResponse.success("注册成功", userResponse)
            );
        } catch (RuntimeException e) {
            // 失败：返回 400 + 错误信息
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail("注册失败：" + e.getMessage()));
        }
    }

    /**
     * 登录
     * 请求体：{ "username": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(
                    request.getUsername(),
                    request.getPassword()
            );

            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole()
            );

            return ResponseEntity.ok(
                    ApiResponse.success("登录成功", userResponse)
            );
        } catch (RuntimeException e) {
            // 比如：用户名不存在 / 密码错误
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail("登录失败：" + e.getMessage()));
        }
    }
}
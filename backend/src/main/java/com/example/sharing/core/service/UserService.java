package com.example.sharing.core.service;


import com.example.sharing.core.entity.User;
import com.example.sharing.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    // 默认角色：普通用户，先写死为 1，将来可提取为枚举或常量类
    private static final int DEFAULT_ROLE = 1;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 注册：只接收 username / password / email
     * id：数据库自增
     * role：后端统一设置为默认角色
     */
    @Transactional
    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        // TODO: 真实项目建议这里对密码做加密（比如 BCrypt），目前为了演示先明文存
        User user = new User(username, password, email, DEFAULT_ROLE);
        return userRepository.save(user);
    }

    /**
     * 登录：用户名 + 密码
     */
    public User login(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("用户名不存在");
        }

        User user = optionalUser.get();

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        return user;
    }
}
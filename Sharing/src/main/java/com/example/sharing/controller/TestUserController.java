package com.example.sharing.controller;

import com.example.sharing.entity.User;
import com.example.sharing.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestUserController {

    private final UserRepository userRepository;

    public TestUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/test-insert")
    public String testInsert() {
        User u = new User();
        u.setEmail("test3@example.com");
        u.setUsername("testuser3");
        u.setPassword("123456");
        u.setRole(1);

        User saved = userRepository.save(u);
        return "插入成功，id=" + saved.getId();
    }
}
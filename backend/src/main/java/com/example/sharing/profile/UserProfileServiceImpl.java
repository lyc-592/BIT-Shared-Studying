package com.example.sharing.profile;

import com.example.sharing.core.entity.User;
import com.example.sharing.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileServiceImpl(UserRepository userRepository,
                                  UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional
    public UserProfileDTO createProfile(Long userId, String nickname, String bio, String major) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

        if (userProfileRepository.existsByUser_Id(userId)) {
            throw new RuntimeException("该用户已设置过个人信息");
        }

        // 一定要 new 一个新的 UserProfile，而不是自己手动 set Id=1
        UserProfile profile = new UserProfile(user, nickname, bio, major);

        UserProfile saved = userProfileRepository.save(profile);

        return toDTO(saved);
    }

    @Override
    @Transactional
    public UserProfileDTO updateProfile(Long userId, String nickname, String bio, String major) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("该用户还未设置个人信息"));

        profile.setNickname(nickname);
        profile.setBio(bio);
        profile.setMajor(major);

        UserProfile saved = userProfileRepository.save(profile);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("该用户还未设置个人信息"));
        return toDTO(profile);
    }

    private UserProfileDTO toDTO(UserProfile profile) {
        User user = profile.getUser();
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(profile.getNickname());
        dto.setBio(profile.getBio());
        dto.setMajor(profile.getMajor());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}
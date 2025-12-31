package com.example.sharing.role;


import com.example.sharing.core.entity.Course;
import com.example.sharing.core.entity.Major;
import com.example.sharing.core.entity.MajorCourse;
import com.example.sharing.core.entity.User;
import com.example.sharing.core.repository.CourseRepository;
import com.example.sharing.core.repository.MajorCourseRepository;
import com.example.sharing.core.repository.MajorRepository;
import com.example.sharing.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCoursePermissionRepository userCoursePermissionRepository;
    private final MajorRepository majorRepository;
    private final MajorCourseRepository majorCourseRepository;

    public PermissionService(UserRepository userRepository,
                             CourseRepository courseRepository,
                             UserCoursePermissionRepository userCoursePermissionRepository,
                             MajorRepository majorRepository,
                             MajorCourseRepository majorCourseRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.userCoursePermissionRepository = userCoursePermissionRepository;
        this.majorRepository = majorRepository;
        this.majorCourseRepository = majorCourseRepository;
    }

    /**
     * 授权
     */
    @Transactional
    public String grantPermission(GrantPermissionRequest req) {
        User grantor = userRepository.findById(req.getGrantorId())
                .orElse(null);
        if (grantor == null) {
            return "授权者不存在";
        }

        User target = userRepository.findByUsername(req.getTargetUsername())
                .orElse(null);
        if (target == null) {
            return "被授权用户不存在";
        }

        Integer grantorRole = grantor.getRole();
        Integer targetRole = req.getTargetRole();
        Integer currentTargetRole = target.getRole();

        if (grantorRole == null || targetRole == null || grantorRole <= targetRole) {
            return "无权授予该等级的权限";
        }

        if (currentTargetRole != null && currentTargetRole != 1) {
            return "只能对当前为普通用户的账号进行授权";
        }

        // 授予角色
        target.setRole(targetRole);
        userRepository.save(target);

        // 如果是二级，处理专业课程
        if (targetRole == 2) {
            if (req.getMajorNo() == null) {
                return "授予二级权限时必须指定专业号";
            }

            Major major = majorRepository.findById(req.getMajorNo())
                    .orElse(null);
            if (major == null) {
                return "专业不存在";
            }

            List<MajorCourse> relations = majorCourseRepository.findByMajor(major);
            for (MajorCourse mc : relations) {
                Course c = mc.getCourse();
                if (!userCoursePermissionRepository.existsByUserAndCourse(target, c)) {
                    UserCoursePermission perm = new UserCoursePermission();
                    perm.setUser(target);
                    perm.setCourse(c);
                    userCoursePermissionRepository.save(perm);
                }
            }
        }

        return null; // null 表示成功
    }

    /**
     * 撤销权限
     */
    @Transactional
    public String revokePermission(RevokePermissionRequest req) {
        User revoker = userRepository.findById(req.getRevokerId())
                .orElse(null);
        if (revoker == null) {
            return "撤销者不存在";
        }

        User target = userRepository.findByUsername(req.getTargetUsername())
                .orElse(null);
        if (target == null) {
            return "被撤销用户不存在";
        }

        Integer revokerRole = revoker.getRole();
        Integer targetRole = target.getRole();

        if (revokerRole == null || targetRole == null || revokerRole <= targetRole) {
            return "无权撤销该用户的权限";
        }

        if (targetRole == 2) {
            userCoursePermissionRepository.deleteByUser(target);
        }

        target.setRole(1);
        userRepository.save(target);

        return null;
    }

    /**
     * 查询用户是否有某课程的管理权限
     */
    @Transactional(readOnly = true)
    public PermissionCheckResultDto checkCoursePermission(Long userId, Long courseNo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Integer role = user.getRole();
        if (role == null) {
            return new PermissionCheckResultDto(false);
        }

        // 4: 超级管理员，3: 全专业管理员 -> 直接有权限
        if (role == 4 || role == 3) {
            return new PermissionCheckResultDto(true);
        }

        // 1: 普通用户 -> 没权限
        if (role == 1) {
            return new PermissionCheckResultDto(false);
        }

        // 2: 课程管理员 -> 查 user_course_permission
        if (role == 2) {
            Course course = courseRepository.findById(courseNo)
                    .orElseThrow(() -> new RuntimeException("课程不存在"));

            boolean exists = userCoursePermissionRepository.existsByUserAndCourse(user, course);
            return new PermissionCheckResultDto(exists);
        }

        // 其他未知角色，按无权限处理
        return new PermissionCheckResultDto(false);
    }
}
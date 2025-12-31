package com.example.sharing.role;


import com.example.sharing.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // 授权
    @PostMapping("/grant")
    public ApiResponse<Void> grant(@RequestBody GrantPermissionRequest request) {
        String error = permissionService.grantPermission(request);
        if (error != null) {
            // 失败
            return ApiResponse.fail(error);
        }
        // 成功
        return ApiResponse.success("授权成功", null);
    }

    // 撤销
    @PostMapping("/revoke")
    public ApiResponse<Void> revoke(@RequestBody RevokePermissionRequest request) {
        String error = permissionService.revokePermission(request);
        if (error != null) {
            return ApiResponse.fail(error);
        }
        return ApiResponse.success("撤销权限成功", null);
    }

    // 查询是否对某课程有权限
    @GetMapping("/check")
    public ApiResponse<PermissionCheckResultDto> check(
            @RequestParam Long userId,
            @RequestParam Long courseNo) {

        PermissionCheckResultDto result = permissionService.checkCoursePermission(userId, courseNo);
        return ApiResponse.success("查询成功", result);
    }
}
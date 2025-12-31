package com.example.sharing.role;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RevokePermissionRequest {

    // 撤销者ID（仍然用ID）
    private Long revokerId;

    // 被撤销者用户名
    private String targetUsername;

}
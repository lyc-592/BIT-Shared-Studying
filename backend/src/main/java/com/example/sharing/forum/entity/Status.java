package com.example.sharing.forum.entity;

public interface Status {
    int ACTIVE = 1;      // 正常
    int DELETED = 0;     // 已删除（软删除）
//    int HIDDEN = 2;      // 隐藏
//    int PENDING = 3;     // 审核中
//    int LOCKED = 4;      // 锁定
}
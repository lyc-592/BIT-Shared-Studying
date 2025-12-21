// src/utils/role.js

export const ROLE_MAP = {
    1: { label: '普通用户', color: '#909399', bgColor: '#f4f4f5' }, // 灰色
    2: { label: '专业管理员', color: '#e6a23c', bgColor: '#fdf6ec' }, // 浅橙色
    3: { label: '通用管理员', color: '#ff9900', bgColor: '#fff3e0' }, // 橙色 (更深)
    4: { label: '超级管理员', color: '#f56c6c', bgColor: '#fef0f0' }  // 红色
}

export function getRoleInfo(roleId) {
    // 默认为普通用户
    return ROLE_MAP[roleId] || ROLE_MAP[1]
}

// 检查是否拥有管理员权限 (>=2)
export function isAdmin(roleId) {
    return parseInt(roleId) >= 2
}

// 检查是否有全局文件操作权限 (>=3)
export function hasGlobalPermission(roleId) {
    return parseInt(roleId) >= 3
}
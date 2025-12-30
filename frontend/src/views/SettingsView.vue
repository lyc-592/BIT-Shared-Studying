<template>
  <div class="home-container">
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <button class="nav-item" @click="goToPage('/courses')">课程</button>
        <button class="nav-item" @click="goToPage('/inbox')">信箱</button>
        <button class="nav-item active" @click="goToPage('/settings')">设置</button>

      </nav>
    </aside>

    <div class="main-content">
      <div class="page-header">
        <div class="header-left">
          <h2>个人信息设置</h2>
          <span class="sub-title">管理您的个人资料</span>
        </div>

        <div class="header-right" v-if="isLoggedIn">
          <span class="role-badge" :style="roleBadgeStyle">{{ roleLabel }}</span>
          <button class="btn-logout" @click="handleLogout">退出登录</button>
        </div>
      </div>

      <div v-if="!isLoggedIn" class="guest-state">
        <div class="guest-box">
          <h2>🚫 访问受限</h2>
          <p>您处于游客状态，或者登录信息不完整。</p>
          <button class="btn-primary" @click="goToPage('/login')">去登录</button>
        </div>
      </div>

      <div v-else>
        <div v-if="loading" class="loading-state">
          正在同步用户信息...
        </div>

        <div v-else>
          <div class="profile-card">
            <h3 class="card-title">基本资料</h3>
            <div class="form-body">
              <div class="form-row">
                <div class="form-group">
                  <label>用户名 (不可修改)</label>
                  <input type="text" :value="form.username || localUsername" disabled class="input-disabled" />
                </div>
                <div class="form-group">
                  <label>邮箱 (不可修改)</label>
                  <input type="text" :value="form.email || localEmail" disabled class="input-disabled" placeholder="暂无邮箱" />
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label>昵称</label>
                  <input type="text" v-model="form.nickname" placeholder="设置一个昵称" />
                </div>
                <div class="form-group">
                  <label>专业 / 学院</label>
                  <select v-model="form.major">
                    <option value="" disabled>请选择专业</option>
                    <option v-for="m in majorList" :key="m.majorNo" :value="m.majorName">
                      {{ m.majorName }}
                    </option>
                  </select>
                </div>
              </div>

              <div class="form-group full-width">
                <label>个人简介</label>
                <textarea v-model="form.bio" rows="4" placeholder="介绍一下你自己..."></textarea>
              </div>

              <div class="action-footer">
                <button class="btn-save" @click="handleSave">保存修改</button>
                <span v-if="message" :class="['msg-tip', isError ? 'error' : 'success']">
                  {{ message }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="userRole >= 3" class="admin-panel">
            <h3 class="card-title admin-title">管理员授权中心</h3>
            <p class="admin-desc">
              您是 <strong>{{ roleLabel }}</strong>。
              <span v-if="userRole === 3">您可以任命/撤销“专业管理员”。</span>
              <span v-if="userRole === 4">您可以任命/撤销“通用管理员”及以下权限。</span>
            </p>

            <div class="admin-actions">
              <div class="admin-form-box">
                <h4>授予权限</h4>
                <div class="form-group">
                  <label>目标用户名</label>
                  <input v-model="grantForm.targetUsername" placeholder="输入用户名" />
                </div>

                <div class="form-group">
                  <label>授予角色</label>
                  <select v-model="grantForm.targetRole">
                    <option value="" disabled>请选择角色</option>
                    <option value="2">专业管理员 (权限 2)</option>
                    <option value="3" v-if="userRole === 4">通用管理员 (权限 3)</option>
                  </select>
                </div>

                <div class="form-group" v-if="grantForm.targetRole == 2">
                  <label>所属专业</label>
                  <select v-model="grantForm.majorNo">
                    <option value="" disabled>请选择专业</option>
                    <option v-for="m in majorList" :key="m.majorNo" :value="m.majorNo">
                      {{ m.majorName }}
                    </option>
                  </select>
                </div>

                <button class="btn-grant" @click="handleGrant">确认授权</button>
              </div>

              <div class="admin-form-box revoke-box">
                <h4>剥夺权限</h4>
                <div class="form-group">
                  <label>目标用户名</label>
                  <input v-model="revokeForm.targetUsername" placeholder="输入要撤销的用户" />
                </div>
                <p class="tip-text">注意：此操作将把该用户重置为“普通用户 (1)”。</p>
                <button class="btn-revoke" @click="handleRevoke">确认撤销</button>
              </div>
            </div>

            <div v-if="adminMessage" :class="['admin-msg', adminSuccess ? 'success' : 'error']">
              {{ adminMessage }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'SettingsView' })

import { ref, reactive, onMounted, computed, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { getRoleInfo } from '@/utils/role'

const router = useRouter()

const isLoggedIn = ref(false)
const loading = ref(false)
const userId = ref(null)
const userRole = ref(1)
const localUsername = ref('')
const localEmail = ref('')
const majorList = ref([])

const message = ref('')
const isError = ref(false)
const form = reactive({ username: '', email: '', nickname: '', bio: '', major: '', role: 0 })

const adminMessage = ref('')
const adminSuccess = ref(false)
const grantForm = reactive({ targetUsername: '', targetRole: '', majorNo: '' })
const revokeForm = reactive({ targetUsername: '' })

const roleLabel = computed(() => getRoleInfo(userRole.value).label)
const roleBadgeStyle = computed(() => {
  const info = getRoleInfo(userRole.value)
  return {
    color: info.color,
    backgroundColor: info.bgColor,
    border: `1px solid ${info.color}`,
    padding: '4px 10px',
    borderRadius: '4px',
    fontWeight: 'bold',
    fontSize: '13px'
  }
})

async function initData() {
  // 修改点：sessionStorage
  const token = sessionStorage.getItem('token')
  const uid = sessionStorage.getItem('userId')
  const role = sessionStorage.getItem('role')

  localUsername.value = sessionStorage.getItem('username') || ''
  localEmail.value = sessionStorage.getItem('email') || ''

  if (!token || !uid) {
    isLoggedIn.value = false
    userId.value = null
    return
  }

  userId.value = uid
  userRole.value = parseInt(role || '1')
  isLoggedIn.value = true

  loading.value = true
  message.value = ''

  try {
    await ensureProfileCreated()
    await Promise.all([loadProfile(), loadMajors()])
  } catch (e) {
    console.error(e)
    isError.value = true
    message.value = '初始化数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => initData())
onActivated(() => initData())

async function ensureProfileCreated() {
  try {
    await axios.post('/api/profile', { userId: Number(userId.value) })
  } catch (error) { /* ignore */ }
}

async function loadProfile() {
  const res = await axios.get(`/api/profile/${userId.value}`)
  const data = res.data.data || res.data
  if (data) {
    form.username = data.username || localUsername.value
    form.email = data.email || localEmail.value
    form.nickname = data.nickname || ''
    form.bio = data.bio || ''
    form.major = data.major || ''
    form.role = data.role

    if (data.role && parseInt(data.role) !== userRole.value) {
      userRole.value = parseInt(data.role)
      sessionStorage.setItem('role', data.role)
      if (userRole.value < 2) sessionStorage.removeItem('auth_major_no')
    }
  }
}

async function loadMajors() {
  const res = await axios.get('/api/majors')
  if (res.data.success) majorList.value = res.data.data || []
}

async function handleSave() {
  message.value = ''; isError.value = false
  const payload = {
    userId: Number(userId.value),
    nickname: form.nickname || null,
    bio: form.bio || null,
    major: form.major || null
  }
  try {
    const res = await axios.put(`/api/profile/${userId.value}`, payload)
    if (res.data && (res.data.success || res.data.userId || res.data.data)) {
      message.value = '保存成功！'
      isError.value = false
    } else throw new Error(res.data.message || '保存失败')
  } catch (e) {
    isError.value = true
    message.value = e.response?.data?.message || '保存失败'
  }
}

async function handleGrant() {
  adminMessage.value = ''
  if (!grantForm.targetUsername || !grantForm.targetRole) return alert('请填写完整')

  const targetRoleInt = parseInt(grantForm.targetRole)
  if (targetRoleInt >= userRole.value) {
    adminMessage.value = '错误：无法授予同级或更高级权限'
    adminSuccess.value = false
    return
  }
  if (targetRoleInt === 2 && !grantForm.majorNo) return alert('必须指定专业')

  try {
    const payload = {
      grantorId: parseInt(userId.value),
      targetUsername: grantForm.targetUsername,
      targetRole: targetRoleInt,
      majorNo: targetRoleInt === 2 ? grantForm.majorNo : null
    }
    const res = await axios.post('/api/permissions/grant', payload)
    if (res.data.success) {
      adminSuccess.value = true
      adminMessage.value = `成功授权 "${grantForm.targetUsername}"`
      grantForm.targetUsername = ''
    } else {
      adminSuccess.value = false
      adminMessage.value = res.data.message
    }
  } catch (e) {
    adminSuccess.value = false
    adminMessage.value = '请求失败'
  }
}

async function handleRevoke() {
  adminMessage.value = ''
  if (!revokeForm.targetUsername) return alert('请输入用户名')
  try {
    const payload = { revokerId: parseInt(userId.value), targetUsername: revokeForm.targetUsername }
    const res = await axios.post('/api/permissions/revoke', payload)
    if (res.data.success) {
      adminSuccess.value = true
      adminMessage.value = `成功撤销 "${revokeForm.targetUsername}" 的权限`
      revokeForm.targetUsername = ''
    } else {
      adminSuccess.value = false
      adminMessage.value = res.data.message
    }
  } catch (e) {
    adminSuccess.value = false
    adminMessage.value = '请求失败'
  }
}

function handleLogout() {
  if (confirm('确定要退出登录吗？')) {
    sessionStorage.clear() // 修改点
    isLoggedIn.value = false
    router.push('/login')
  }
}
const goToPage = (path) => router.push(path)
</script>

<style scoped>
/* 首页整体容器 */
.home-container {
  height: 100vh;
  width: 100%;
  display: flex;
  background-color: #f5f7fa;
}

/* 侧边栏样式 */
.sidebar {
  width: 220px;
  background-color: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  flex-shrink: 0;
}

/* 侧边栏 Logo 样式 */
.logo {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 30px;
  text-align: center;
  color: #fff;
}

/* 侧边栏导航菜单容器 */
.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 侧边栏导航项基础样式 */
.nav-item {
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #ccc;
  text-align: left;
  cursor: pointer;
  font-size: 15px;
  transition: all 0.3s;
}

/* 侧边栏导航项 hover 状态 */
.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

/* 侧边栏激活态导航项 */
.nav-item.active {
  background-color: #409eff;
  color: #fff;
  font-weight: bold;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  padding: 30px 50px;
  overflow-y: auto;
}

/* 页面头部区域 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid #e8e8e8;
  padding-bottom: 15px;
}

/* 页面头部标题 */
.page-header h2 {
  font-size: 24px;
  color: #303133;
  margin: 0 0 5px 0;
}

/* 页面副标题 */
.sub-title {
  color: #909399;
  font-size: 14px;
}

/* 页面头部右侧区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 退出登录按钮 */
.btn-logout {
  background: #f56c6c;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

/* 个人资料卡片 & 管理员面板通用样式 */
.profile-card,
.admin-panel {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  max-width: 850px;
  margin-bottom: 30px;
}

/* 卡片标题样式 */
.card-title {
  margin-top: 0;
  margin-bottom: 20px;
  border-left: 4px solid #409eff;
  padding-left: 10px;
  color: #303133;
}

/* 表单主体容器 */
.form-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 表单行（横向排列多个表单组） */
.form-row {
  display: flex;
  gap: 30px;
}

/* 表单组容器 */
.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 全宽样式（用于表单组/控件） */
.full-width {
  width: 100%;
}

/* 表单标签样式 */
label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

/* 表单输入控件通用样式（输入框/下拉/文本域） */
input,
select,
textarea {
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  background-color: #fff;
}

/* 文本域可垂直调整大小 */
textarea {
  resize: vertical;
}

/* 禁用状态的输入控件 */
.input-disabled {
  background-color: #f5f7fa;
  color: #909399;
  cursor: not-allowed;
}

/* 表单操作页脚（按钮+提示区） */
.action-footer {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 保存按钮样式 */
.btn-save {
  padding: 10px 24px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  background-color: #409eff;
  color: #fff;
  font-size: 14px;
}

/* 保存按钮 hover 状态 */
.btn-save:hover {
  background-color: #66b1ff;
}

/* 提示文本基础样式 */
.msg-tip {
  font-size: 14px;
}

/* 成功提示文本 */
.msg-tip.success {
  color: #67c23a;
}

/* 错误提示文本 */
.msg-tip.error {
  color: #f56c6c;
}

/* 管理员面板标题（修改左侧边框色） */
.admin-title {
  border-color: #e6a23c;
}

/* 管理员面板描述文本 */
.admin-desc {
  color: #606266;
  margin-bottom: 25px;
  background: #fdf6ec;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #faecd8;
}

/* 管理员操作区域（授权+撤销横向排列） */
.admin-actions {
  display: flex;
  gap: 30px;
}

/* 管理员表单盒子通用样式 */
.admin-form-box {
  flex: 1;
  background: #f9f9f9;
  padding: 20px;
  border-radius: 6px;
  border: 1px solid #eee;
}

/* 管理员表单盒子标题 */
.admin-form-box h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #303133;
}

/* 授权按钮样式 */
.btn-grant {
  background: #67c23a;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
  margin-top: 10px;
}

/* 撤销操作盒子（修改边框/背景色） */
.revoke-box {
  border-color: #fde2e2;
  background: #fef0f0;
}

/* 撤销操作盒子标题（修改文字色） */
.revoke-box h4 {
  color: #f56c6c;
}

/* 小提示文本 */
.tip-text {
  font-size: 12px;
  color: #909399;
  margin: 0 0 10px 0;
}

/* 撤销按钮样式 */
.btn-revoke {
  background: #f56c6c;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
  margin-top: 10px;
}

/* 管理员操作提示信息基础样式 */
.admin-msg {
  margin-top: 20px;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
}

/* 管理员操作成功提示 */
.admin-msg.success {
  background: #f0f9eb;
  color: #67c23a;
}

/* 管理员操作错误提示 */
.admin-msg.error {
  background: #fef0f0;
  color: #f56c6c;
}

/* 访客状态容器（居中展示） */
.guest-state {
  display: flex;
  justify-content: center;
  margin-top: 100px;
}

/* 访客提示盒子 */
.guest-box {
  background: white;
  padding: 40px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

/* 访客盒子标题 */
.guest-box h2 {
  color: #f56c6c;
  margin-bottom: 10px;
}

/* 主按钮样式（访客页用） */
.btn-primary {
  background: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 15px;
}

/* 加载状态样式 */
.loading-state {
  text-align: center;
  color: #999;
  margin-top: 50px;
}
</style>
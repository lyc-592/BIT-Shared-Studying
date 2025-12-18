<template>
  <div class="home-container">
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <button class="nav-item" @click="goToPage('/courses')">课程</button>
        <button class="nav-item active" @click="goToPage('/settings')">设置</button>
      </nav>
    </aside>

    <div class="main-content">
      <!-- 游客拦截 -->
      <div v-if="!isLoggedIn" class="guest-state">
        <div class="guest-box">
          <h2>🚫 访问受限</h2>
          <p>您处于游客状态，或者登录信息不完整。</p>
          <button class="btn-primary" @click="goToPage('/login')">去登录</button>
        </div>
      </div>

      <!-- 登录状态 -->
      <div v-else>
        <div class="page-header">
          <div class="header-left">
            <h2>个人信息设置</h2>
            <span class="sub-title">管理您的个人资料</span>
          </div>
          <button class="btn-logout" @click="handleLogout">退出登录</button>
        </div>

        <div v-if="loading" class="loading-state">
          正在同步用户信息...
        </div>

        <div v-else class="profile-card">
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
              <!-- 这里永远是保存修改，因为进入页面时已经确保创建了 -->
              <button class="btn-save" @click="handleSave">保存修改</button>

              <span v-if="message" :class="['msg-tip', isError ? 'error' : 'success']">
                {{ message }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'SettingsView' })

import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// 状态
const isLoggedIn = ref(false)
const loading = ref(false)
const message = ref('')
const isError = ref(false)

// 数据
const userId = ref(null)
const localUsername = ref('')
const localEmail = ref('')
const majorList = ref([])

const form = reactive({
  username: '', email: '', nickname: '', bio: '', major: '', role: 0
})

// --- 初始化逻辑 ---
async function initData() {
  const token = localStorage.getItem('token')
  const uid = localStorage.getItem('userId')
  localUsername.value = localStorage.getItem('username') || ''
  localEmail.value = localStorage.getItem('email') || ''

  if (!token || !uid) {
    isLoggedIn.value = false
    return
  }

  userId.value = uid
  isLoggedIn.value = true

  loading.value = true
  message.value = ''

  try {
    // 1. 懒加载策略：先尝试创建
    await ensureProfileCreated()

    // 2. 创建/确认存在后，并行拉取详情和专业列表
    await Promise.all([loadProfile(), loadMajors()])

  } catch (e) {
    console.error(e)
    isError.value = true
    message.value = '初始化数据失败，请刷新重试'
  } finally {
    loading.value = false
  }
}

// 生命周期
onMounted(() => initData())
onActivated(() => initData())

// --- 核心：懒创建函数 ---
async function ensureProfileCreated() {
  try {
    // 发送一个只带 userId 的请求，尝试创建
    // 后端如果不存在 -> 创建成功 (200)
    // 后端如果已存在 -> 返回错误 (400/409等) -> 进入 catch -> 我们忽略错误
    console.log('[Lazy Create] 尝试初始化用户档案...')
    await axios.post('/api/profile', { userId: Number(userId.value) })
    console.log('[Lazy Create] 创建成功 (新用户)')
  } catch (error) {
    // 只要是请求发通了，不管后端返回什么错误(比如"已存在")，都视为成功
    // 只有网络错误才需要关注，但为了体验，这里也先静默处理，交给后面的 loadProfile 去兜底
    console.log('[Lazy Create] 创建请求结束 (用户可能已存在或后端报错)，继续加载...')
  }
}

// --- 加载数据 ---
async function loadProfile() {
  // 此时理论上用户档案一定存在了
  const res = await axios.get(`/api/profile/${userId.value}`)
  const data = res.data
  // 兼容后端结构
  const profile = data.data || data

  if (profile) {
    form.username = profile.username || localUsername.value
    form.email = profile.email || localEmail.value
    form.nickname = profile.nickname || ''
    form.bio = profile.bio || ''
    form.major = profile.major || ''
    form.role = profile.role
  }
}

async function loadMajors() {
  try {
    const res = await axios.get('/api/majors')
    if (res.data.success) majorList.value = res.data.data || []
  } catch (e) { console.error(e) }
}

// --- 保存数据 ---
async function handleSave() {
  message.value = ''
  isError.value = false

  // 构造 payload
  const payload = {
    userId: Number(userId.value),
    nickname: form.nickname || null,
    bio: form.bio || null,
    major: form.major || null
  }

  try {
    // 永远只调用 PUT，因为我们保证了用户档案在进入页面时已创建
    console.log('[Save] 发送 PUT 请求更新数据...')
    const res = await axios.put(`/api/profile/${userId.value}`, payload)

    if (res.data && (res.data.success || res.data.userId || res.data.data)) {
      message.value = '保存成功！'
      isError.value = false

      // 更新一下视图
      const d = res.data.data || res.data
      if (d.username) form.username = d.username
      if (d.email) form.email = d.email
    } else {
      throw new Error(res.data.message || '保存失败')
    }
  } catch (e) {
    console.error(e)
    isError.value = true
    message.value = e.response?.data?.message || '保存失败'
  }
}

function handleLogout() {
  if (confirm('确定要退出登录吗？')) {
    localStorage.clear()
    isLoggedIn.value = false
    router.push('/login')
  }
}

const goToPage = (path) => router.push(path)
</script>

<style scoped>
/* 样式保持不变 */
.home-container { height: 100vh; width: 100%; display: flex; background-color: #f5f7fa; }
.sidebar { width: 220px; background-color: #001529; color: #fff; display: flex; flex-direction: column; padding: 20px 16px; flex-shrink: 0; }
.logo { font-size: 20px; font-weight: bold; margin-bottom: 30px; text-align: center; color: #fff; }
.nav-menu { display: flex; flex-direction: column; gap: 10px; }
.nav-item { width: 100%; padding: 10px 12px; border: none; border-radius: 4px; background: transparent; color: #ccc; text-align: left; cursor: pointer; font-size: 15px; transition: all 0.3s; }
.nav-item:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }
.nav-item.active { background-color: #409eff; color: #fff; font-weight: bold; }
.main-content { flex: 1; padding: 30px 50px; overflow-y: auto; }
.guest-state { display: flex; justify-content: center; margin-top: 100px; }
.guest-box { background: white; padding: 40px; border-radius: 8px; text-align: center; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.guest-box h2 { color: #f56c6c; margin-bottom: 10px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; border-bottom: 1px solid #e8e8e8; padding-bottom: 15px; }
.page-header h2 { font-size: 24px; color: #303133; margin: 0 0 5px 0; }
.sub-title { color: #909399; font-size: 14px; }
.btn-logout { background: #f56c6c; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
.btn-logout:hover { background: #f78989; }
.profile-card { background: #fff; border-radius: 8px; padding: 40px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05); max-width: 800px; }
.form-body { display: flex; flex-direction: column; gap: 20px; }
.form-row { display: flex; gap: 30px; }
.form-group { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.full-width { width: 100%; }
label { font-size: 14px; font-weight: 500; color: #606266; }
input, select, textarea { padding: 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 14px; background-color: #fff; color: #606266; }
textarea { resize: vertical; }
.input-disabled { background-color: #f5f7fa; color: #909399; cursor: not-allowed; }
.action-footer { margin-top: 20px; display: flex; align-items: center; gap: 15px; }
.btn-save { padding: 10px 24px; border-radius: 4px; border: none; cursor: pointer; background-color: #409eff; color: #fff; }
.btn-save:hover { background-color: #66b1ff; }
.msg-tip { font-size: 14px; }
.msg-tip.success { color: #67c23a; }
.msg-tip.error { color: #f56c6c; }
.loading-state { text-align: center; color: #999; margin-top: 50px; }
.btn-primary { background: #409eff; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; margin-top: 15px; }
</style>
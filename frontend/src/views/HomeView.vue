<template>
  <div class="home-container">
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item active" @click="goToPage('/')">首页</button>
        <button class="nav-item" @click="goToPage('/courses')">课程</button>
        <button class="nav-item" @click="goToPage('/inbox')">信箱</button>
        <button class="nav-item" @click="goToPage('/settings')">设置</button>

      </nav>
    </aside>

    <div class="main-content">
      <div class="main-header">
        <div class="search-bar-wrapper">
          <div class="search-input-container">
            <input
                v-model="searchQuery"
                type="text"
                placeholder="请输入并选择专业..."
                class="search-input"
                @input="handleInput"
                @focus="handleFocus"
                @blur="handleBlur"
            />
            <div v-if="showDropdown && filteredMajors.length > 0" class="dropdown">
              <div
                  v-for="major in filteredMajors"
                  :key="major.majorNo"
                  class="dropdown-item"
                  @mousedown.prevent="selectMajor(major)"
              >
                {{ major.majorName }}
              </div>
            </div>
          </div>
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>

        <div class="top-right">
          <button v-if="!isLoggedIn" @click="goToLogin" class="nav-btn">
            登录 / 注册
          </button>
          <div v-else class="welcome-user">
            <span class="role-badge" :style="badgeStyle">{{ roleName }}</span>
            <span class="welcome-text">{{ currentUsername }}</span>
            <button @click="logout" class="logout-btn">退出</button>
          </div>
        </div>
      </div>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
      <div v-if="infoMessage" class="info-banner">{{ infoMessage }}</div>

      <div class="content-body">
        <h2 v-if="currentCourseList.length > 0" class="section-title">
          “{{ selectedMajorName }}” 的课程列表
        </h2>

        <div v-if="currentCourseList.length > 0" class="course-grid">
          <div
              v-for="course in currentCourseList"
              :key="course.courseNo"
              class="course-card"
              @click="goToCourseDetail(course.courseNo)"
          >
            <div class="course-icon">📚</div>
            <div class="course-info">
              <h3 class="course-name">{{ course.courseName }}</h3>
              <p class="course-no">课程号: {{ course.courseNo }}</p>
            </div>
          </div>
        </div>

        <div v-else-if="hasSearched" class="empty-state">
          该专业下暂无课程数据。
        </div>
        <div v-else class="empty-state">
          请在上方搜索并选择专业以查看课程。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'HomeView' })

import { ref, onMounted, onActivated, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { getRoleInfo } from '@/utils/role'

const router = useRouter()

const isLoggedIn = ref(false)
const currentUsername = ref('')
const currentRole = ref(1)
const errorMessage = ref('')
const infoMessage = ref('')

const searchQuery = ref('')
const showDropdown = ref(false)
const allMajors = ref([])
const filteredMajors = ref([])
const currentSelectedMajor = ref(null)
const currentCourseList = ref([])
const selectedMajorName = ref('')
const hasSearched = ref(false)
const lastUserId = ref(null)

const roleName = computed(() => getRoleInfo(currentRole.value).label)
const badgeStyle = computed(() => {
  const info = getRoleInfo(currentRole.value)
  return { color: info.color, backgroundColor: info.bgColor, borderColor: info.color }
})

function checkLoginStatus() {
  // 修改点：使用 sessionStorage
  const token = sessionStorage.getItem('token')
  const username = sessionStorage.getItem('username')
  const uid = sessionStorage.getItem('userId')
  const role = sessionStorage.getItem('role')

  if (token) {
    isLoggedIn.value = true
    currentUsername.value = username || '用户'
    currentRole.value = parseInt(role || '1')
    return uid
  } else {
    isLoggedIn.value = false
    currentUsername.value = ''
    currentRole.value = 1
    return null
  }
}

// 同步权限
async function syncUserRole() {
  const uid = sessionStorage.getItem('userId')
  if (!uid) return
  try {
    const res = await axios.get(`/api/profile/${uid}`)
    const data = res.data.data || res.data
    if (data && data.role !== undefined) {
      const remoteRole = parseInt(data.role)
      const localRole = currentRole.value
      if (remoteRole !== localRole) {
        currentRole.value = remoteRole
        sessionStorage.setItem('role', remoteRole)
        if (remoteRole < 2) sessionStorage.removeItem('auth_major_no')
      }
    }
  } catch (e) { /* ignore */ }
}

function resetSearchState() {
  searchQuery.value = ''
  currentSelectedMajor.value = null
  currentCourseList.value = []
  selectedMajorName.value = ''
  hasSearched.value = false
  errorMessage.value = ''
  infoMessage.value = ''
}

onMounted(async () => {
  const uid = checkLoginStatus()
  lastUserId.value = uid
  if (uid) await syncUserRole()
  await loadAllMajors()
})

onActivated(async () => {
  const currentUid = checkLoginStatus()
  if (lastUserId.value !== currentUid) {
    resetSearchState()
    lastUserId.value = currentUid
  }
  if (currentUid) await syncUserRole()
})

async function loadAllMajors() {
  try {
    const res = await axios.get('/api/majors')
    if (res.data.success) {
      allMajors.value = res.data.data || []
    }
  } catch (err) { console.error(err) }
}

function handleInput() {
  const keyword = searchQuery.value.trim()
  currentSelectedMajor.value = null
  if (!keyword) {
    filteredMajors.value = []
    showDropdown.value = false
    return
  }
  showDropdown.value = true
  filteredMajors.value = allMajors.value.filter(m => m.majorName.includes(keyword))
}

function handleFocus() { if (searchQuery.value.trim()) handleInput() }
function handleBlur() { setTimeout(() => { showDropdown.value = false }, 200) }

function selectMajor(major) {
  searchQuery.value = major.majorName
  currentSelectedMajor.value = major
  showDropdown.value = false
  errorMessage.value = ''
}

async function handleSearch() {
  errorMessage.value = ''; infoMessage.value = ''; hasSearched.value = true
  currentCourseList.value = []

  if (!currentSelectedMajor.value) {
    const exact = allMajors.value.find(m => m.majorName === searchQuery.value.trim())
    if (exact) currentSelectedMajor.value = exact
    else { errorMessage.value = '请先从下拉列表中选择一个有效的专业'; return }
  }

  selectedMajorName.value = currentSelectedMajor.value.majorName

  try {
    infoMessage.value = '加载中...'
    const res = await axios.get(`/api/majors/${currentSelectedMajor.value.majorNo}/courses`)
    infoMessage.value = ''
    if (res.data.success) {
      currentCourseList.value = res.data.data || []
      if (currentCourseList.value.length === 0) infoMessage.value = '该专业暂无课程'
    } else {
      errorMessage.value = res.data.message
    }
  } catch (err) { errorMessage.value = '请求失败' }
}

const goToLogin = () => router.push('/login')
const logout = () => {
  sessionStorage.clear() // 修改点
  resetSearchState()
  lastUserId.value = null
  isLoggedIn.value = false
  router.push('/login')
}
const goToPage = (path) => router.push(path)

const goToCourseDetail = (id) => {
  if (!currentSelectedMajor.value) return
  router.push({
    name: 'CourseDetail',
    params: { courseNo: id },
    query: { majorNo: currentSelectedMajor.value.majorNo }
  })
}
</script>

<style scoped>
/* 样式不变 */
.home-container {
  height: 100vh;
  width: 100%;
  display: flex;
  background-color: #f5f7fa;
}
.sidebar {
  width: 220px;
  background-color: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  flex-shrink: 0;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 30px;
  text-align: center;
  color: #fff;
}
.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
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
.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}
.nav-item.active {
  background-color: #409eff;
  color: #fff; font-weight: bold;
}
.main-content {
  flex: 1; padding: 20px 40px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  background: #fff;
  padding: 15px 25px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  height: 70px;
  box-sizing: border-box;
}
.search-bar-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
}
.search-input-container {
  position: relative;
  width: 300px;
  max-width: 100%;
}
.search-input {
  width: 100%;
  padding: 10px 15px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  outline: none;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.search-input:focus {
  border-color: #409eff;
}
.search-btn {
  flex-shrink: 0;
  padding: 10px 24px;
  border: none;
  border-radius: 4px;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.3s;
  height: 38px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-btn:hover {
  background-color: #66b1ff;
}
.dropdown {
  width: 100%;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  position: absolute;
  top: 105%; left: 0;
  max-height: 240px;
  overflow-y: auto;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.dropdown-item {
  padding: 10px 15px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}
.dropdown-item:hover {
  background-color: #ecf5ff;
  color: #409eff;
}
/* 右上角容器样式 */
.top-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 120px;
}

/* 欢迎用户区域样式 */
.welcome-user {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #606266;
}

/* 角色徽章样式 */
.role-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid;
  font-weight: bold;
}

/* 导航按钮样式 */
.nav-btn {
  padding: 8px 20px;
  font-size: 14px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* 退出登录按钮基础样式 */
.logout-btn {
  padding: 5px 12px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  font-size: 12px;
  color: #606266;
}

/* 退出登录按钮 hover 状态 */
.logout-btn:hover {
  border-color: #c6e2ff;
  color: #409eff;
}

/* 提示横幅通用样式（错误/信息） */
.error-banner, .info-banner {
  margin-bottom: 20px;
  padding: 10px 15px;
  border-radius: 4px;
  font-size: 14px;
}

/* 错误横幅样式 */
.error-banner {
  border: 1px solid #fde2e2;
  background-color: #fef0f0;
  color: #f56c6c;
}

/* 信息横幅样式 */
.info-banner {
  border: 1px solid #e1f3d8;
  background-color: #f0f9eb;
  color: #67c23a;
}

/* 内容主体容器 */
.content-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 章节标题样式 */
.section-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 4px solid #409eff;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 数量徽章样式 */
.count-badge {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  color: #909399;
  margin-top: 50px;
  font-size: 16px;
}

/* 加载状态样式 */
.loading-state {
  text-align: center;
  color: #909399;
  margin-top: 30px;
  font-size: 14px;
}

/* 课程网格布局 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

/* 课程卡片样式 */
.course-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;

  /* 添加以下代码 */
  min-height: 100px; /* 根据你的 UI 调整具体数值，建议在 90px - 110px 之间 */
  box-sizing: border-box; /* 确保 padding 不会额外撑大高度 */
}

/* 课程卡片 hover 状态 */
.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
}

/* 课程图标样式 */
.course-icon {
  font-size: 24px;
  background: #f0f7ff;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}

/* 课程信息容器 */
.course-info {
  display: flex;
  flex-direction: column;
}

/* 课程名称样式 */
.course-name {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

/* 课程编号样式 */
.course-no {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

/* 课程所属部门样式 */
.course-dept {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
}
</style>
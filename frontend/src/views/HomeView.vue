<template>
  <div class="home-container">
    <!-- 左侧侧边导航栏 -->
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <button class="nav-item" @click="goToPage('/settings')">设置</button>
      </nav>
    </aside>

    <!-- 右侧主内容 -->
    <div class="main-content">
      <!-- 顶部区域：搜索栏 + 登录按钮 -->
      <div class="main-header">

        <!-- 1. 搜索栏区域 (居中显示) -->
        <div class="search-bar-wrapper">
          <!-- 输入框容器 (相对定位，用于容纳下拉菜单) -->
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
            <!-- 搜索下拉结果 -->
            <div
                v-if="showDropdown && filteredMajors.length > 0"
                class="dropdown"
            >
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

          <!-- 搜索按钮 (利用 flex gap 与输入框隔开) -->
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>

        <!-- 2. 右上角登录/用户区域 (靠右显示) -->
        <div class="top-right">
          <button v-if="!isLoggedIn" @click="goToLogin" class="nav-btn">
            登录 / 注册
          </button>
          <div v-else class="welcome-user">
            <span class="welcome-text">欢迎回来！</span>
            <button @click="logout" class="logout-btn">退出</button>
          </div>
        </div>

      </div>

      <!-- 提示信息 -->
      <div v-if="errorMessage" class="error-banner">
        {{ errorMessage }}
      </div>
      <div v-if="infoMessage" class="info-banner">
        {{ infoMessage }}
      </div>

      <!-- 课程内容展示区 -->
      <div class="content-body">
        <h2 v-if="currentCourseList.length > 0" class="section-title">
          “{{ selectedMajorName }}” 的课程列表
        </h2>

        <!-- 课程列表网格 -->
        <div v-if="currentCourseList.length > 0" class="course-grid">
          <div
              v-for="course in currentCourseList"
              :key="course.courseNo"
              class="course-card"
          >
            <div class="course-icon">📚</div>
            <div class="course-info">
              <h3 class="course-name">{{ course.courseName }}</h3>
              <p class="course-no">课程号: {{ course.courseNo }}</p>
            </div>
          </div>
        </div>

        <!-- 空状态提示 -->
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// --- 状态变量 ---
const isLoggedIn = ref(false)
const errorMessage = ref('')
const infoMessage = ref('')

// 搜索框相关
const searchQuery = ref('')
const showDropdown = ref(false)

// 数据存储
const allMajors = ref([])          // 存储从后端获取的所有专业
const filteredMajors = ref([])     // 存储模糊搜索后的专业
const currentSelectedMajor = ref(null) // 当前选中的专业对象

// 结果展示
const currentCourseList = ref([])  // 存储从后端查询到的课程列表
const selectedMajorName = ref('')
const hasSearched = ref(false)

// --- 生命周期 ---
onMounted(async () => {
  const token = localStorage.getItem('token')
  if (token) isLoggedIn.value = true
  await loadAllMajors()
})

// --- 核心逻辑 ---

async function loadAllMajors() {
  errorMessage.value = ''
  try {
    // 假设配置了 vite proxy 转发
    const res = await axios.get('/api/majors')
    if (res.data && res.data.success) {
      allMajors.value = res.data.data || []
    } else {
      errorMessage.value = res.data?.message || '获取专业列表失败'
    }
  } catch (err) {
    console.error(err)
    errorMessage.value = '无法连接到服务器获取专业列表'
  }
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
  filteredMajors.value = allMajors.value.filter(major =>
      major.majorName.includes(keyword)
  )
}

function handleFocus() {
  if (searchQuery.value.trim()) {
    handleInput()
  }
}

function handleBlur() {
  // 延时关闭，确保点击下拉项有效
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

function selectMajor(major) {
  searchQuery.value = major.majorName
  currentSelectedMajor.value = major
  showDropdown.value = false
  errorMessage.value = ''
}

async function handleSearch() {
  errorMessage.value = ''
  infoMessage.value = ''
  currentCourseList.value = []
  hasSearched.value = true

  if (!currentSelectedMajor.value) {
    // 尝试精确匹配
    const exactMatch = allMajors.value.find(m => m.majorName === searchQuery.value.trim())
    if (exactMatch) {
      currentSelectedMajor.value = exactMatch
    } else {
      errorMessage.value = '请先从下拉列表中选择一个有效的专业，再进行搜索。'
      return
    }
  }

  const majorNo = currentSelectedMajor.value.majorNo
  selectedMajorName.value = currentSelectedMajor.value.majorName

  try {
    infoMessage.value = '正在加载课程...'
    const res = await axios.get(`/api/majors/${majorNo}/courses`)
    infoMessage.value = ''

    if (res.data && res.data.success) {
      currentCourseList.value = res.data.data || []
      if (currentCourseList.value.length === 0) {
        infoMessage.value = '该专业下暂时没有课程数据。'
      }
    } else {
      errorMessage.value = res.data?.message || '获取课程失败'
    }
  } catch (err) {
    console.error(err)
    infoMessage.value = ''
    errorMessage.value = '请求服务器失败，请稍后再试。'
  }
}

const goToLogin = () => router.push('/login')
const logout = () => {
  localStorage.removeItem('token')
  isLoggedIn.value = false
  location.reload()
}
const goToPage = (path) => router.push(path)
</script>

<style scoped>
/* 容器布局 */
.home-container {
  height: 100vh;
  width: 100%;
  display: flex;
  background-color: #f5f7fa;
}

/* 左侧侧边栏 */
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

/* 右侧主内容区 */
.main-content {
  flex: 1;
  padding: 20px 40px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* --- 顶部 Header 关键样式 --- */
.main-header {
  display: flex;
  align-items: center;
  /* 关键：两端对齐，左边可以是搜索栏(或空)，中间搜索栏，右边是登录 */
  /* 这里为了让搜索栏居中，使用了特殊的 flex 布局技巧，或者直接用 space-between */
  justify-content: space-between;
  margin-bottom: 20px;
  background: #fff;
  padding: 15px 25px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  height: 70px; /* 给一个固定高度确保对齐 */
  box-sizing: border-box;
}

/* 1. 搜索栏区域 */
.search-bar-wrapper {
  /* 让搜索栏占据中间位置 (可选：如果想绝对居中，可以去掉 flex:1 改用 margin: 0 auto 并调整父容器) */
  flex: 1;
  display: flex;
  justify-content: center; /* 内容水平居中 */
  align-items: center;     /* 内容垂直居中 */
  gap: 15px;               /* 关键：输入框和按钮之间的间距，防止重合 */
}

/* 输入框容器 */
.search-input-container {
  position: relative;
  width: 300px; /* 限制宽度，不要太宽 */
  max-width: 100%;
}

.search-input {
  width: 100%;
  padding: 10px 15px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  outline: none;
  font-size: 14px;
  box-sizing: border-box; /* 确保 padding 不撑大 */
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #409eff;
}

.search-btn {
  /* 防止按钮被挤压 */
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
  height: 38px; /* 确保高度和 input 一致 (input默认高+padding约等于40) */
  display: flex;
  align-items: center;
}

.search-btn:hover {
  background-color: #66b1ff;
}

/* 下拉菜单 */
.dropdown {
  width: 100%;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  position: absolute;
  top: 105%; /* 放在输入框正下方 */
  left: 0;
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

/* 2. 右上角登录 / 退出区域 */
.top-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 120px; /* 预留一点空间，防止被挤压 */
}

.welcome-user {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #606266;
}

.nav-btn {
  padding: 8px 20px;
  font-size: 14px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.logout-btn {
  padding: 5px 12px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  font-size: 12px;
  color: #606266;
}

.logout-btn:hover {
  border-color: #c6e2ff;
  color: #409eff;
}

/* 提示条 */
.error-banner, .info-banner {
  margin-bottom: 20px;
  padding: 10px 15px;
  border-radius: 4px;
  font-size: 14px;
}
.error-banner {
  border: 1px solid #fde2e2;
  background-color: #fef0f0;
  color: #f56c6c;
}
.info-banner {
  border: 1px solid #e1f3d8;
  background-color: #f0f9eb;
  color: #67c23a;
}

/* 内容展示区 */
.content-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 4px solid #409eff;
}

.empty-state {
  text-align: center;
  color: #909399;
  margin-top: 50px;
  font-size: 16px;
}

/* 课程网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.course-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: transform 0.2s, box-shadow 0.2s;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
}

.course-icon {
  font-size: 24px;
  background: #f0f7ff;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-info {
  display: flex;
  flex-direction: column;
}

.course-name {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.course-no {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
</style>
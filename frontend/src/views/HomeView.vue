<template>
  <div class="home-container">
    <!-- 左侧侧边导航栏 -->
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <button class="nav-item" @click="goToPage('/courses')">课程</button>
        <button class="nav-item" @click="goToPage('/settings')">设置</button>
      </nav>
    </aside>

    <!-- 右侧主内容 -->
    <div class="main-content">
      <!-- 顶部区域 -->
      <div class="main-header">
        <!-- 搜索栏 -->
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

        <!-- 右上角登录/用户区域 -->
        <div class="top-right">
          <button v-if="!isLoggedIn" @click="goToLogin" class="nav-btn">
            登录 / 注册
          </button>
          <div v-else class="welcome-user">
            <span class="welcome-text">欢迎，{{ currentUsername }}</span>
            <button @click="logout" class="logout-btn">退出</button>
          </div>
        </div>
      </div>

      <!-- 提示信息 -->
      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
      <div v-if="infoMessage" class="info-banner">{{ infoMessage }}</div>

      <!-- 课程内容展示区 -->
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
            <!-- 保持原有的 Emoji 图标 -->
            <div class="course-icon">📚</div>
            <div class="course-info">
              <h3 class="course-name">{{ course.courseName }}</h3>
              <p class="course-no">课程号: {{ course.courseNo }}</p>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
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
// 1. 必须定义 name，KeepAlive 才能缓存它
defineOptions({
  name: 'HomeView'
})

import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// 状态
const isLoggedIn = ref(false)
const currentUsername = ref('')
const errorMessage = ref('')
const infoMessage = ref('')

// 搜索状态 (这些会被 KeepAlive 缓存)
const searchQuery = ref('')
const showDropdown = ref(false)
const allMajors = ref([])
const filteredMajors = ref([])
const currentSelectedMajor = ref(null)
const currentCourseList = ref([])
const selectedMajorName = ref('')
const hasSearched = ref(false)

// 2. 记录上一次的用户ID，用于检测是否切换了账号
const lastUserId = ref(null)

// --- 核心逻辑 ---

function checkLoginStatus() {
  const token = localStorage.getItem('token')
  const username = localStorage.getItem('username')
  const uid = localStorage.getItem('userId')

  if (token) {
    isLoggedIn.value = true
    currentUsername.value = username || '用户'
    return uid // 返回当前 LocalStorage 里的用户ID
  } else {
    isLoggedIn.value = false
    currentUsername.value = ''
    return null
  }
}

// 清空搜索结果 (当检测到用户状态变化时调用)
function resetSearchState() {
  console.log('检测到用户状态变化，重置 Home 页面...')
  searchQuery.value = ''
  currentSelectedMajor.value = null
  currentCourseList.value = []
  selectedMajorName.value = ''
  hasSearched.value = false
  errorMessage.value = ''
  infoMessage.value = ''
}

// --- 生命周期 ---

onMounted(async () => {
  const uid = checkLoginStatus()
  lastUserId.value = uid // 记录初始加载时的用户
  await loadAllMajors()
})

// 3. onActivated: 每次从缓存回到页面时触发
onActivated(() => {
  // 获取当前最新的登录状态
  const currentUid = checkLoginStatus()

  // 对比：如果缓存里的用户(lastUserId) 和 现在的用户(currentUid) 不一样
  // 说明用户进行了“退出登录”或“切换账号”的操作
  if (lastUserId.value !== currentUid) {
    resetSearchState() // 强制清空之前的搜索结果
    lastUserId.value = currentUid // 更新记录
  }
  // 如果一样，什么都不做，KeepAlive 会自动显示之前缓存的内容
})

// --- 业务函数 (保持不变) ---

async function loadAllMajors() {
  errorMessage.value = ''
  try {
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

function handleFocus() { if (searchQuery.value.trim()) handleInput() }
function handleBlur() { setTimeout(() => { showDropdown.value = false }, 200) }

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
  localStorage.clear()
  isLoggedIn.value = false
  currentUsername.value = ''

  // 退出时不仅要清空本地，也要清空当前页面的数据状态
  resetSearchState()
  lastUserId.value = null

  router.push('/login')
}
const goToPage = (path) => router.push(path)
const goToCourseDetail = (courseNo) => {
  router.push({
    name: 'CourseDetail',
    params: { courseNo: courseNo }
  })
}
</script>

<style scoped>
/* 样式保持原样 (Emoji 风格) */
.home-container { height: 100vh; width: 100%; display: flex; background-color: #f5f7fa; }
.sidebar { width: 220px; background-color: #001529; color: #fff; display: flex; flex-direction: column; padding: 20px 16px; flex-shrink: 0; }
.logo { font-size: 20px; font-weight: bold; margin-bottom: 30px; text-align: center; color: #fff; }
.nav-menu { display: flex; flex-direction: column; gap: 10px; }
.nav-item { width: 100%; padding: 10px 12px; border: none; border-radius: 4px; background: transparent; color: #ccc; text-align: left; cursor: pointer; font-size: 15px; transition: all 0.3s; }
.nav-item:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }
.main-content { flex: 1; padding: 20px 40px; display: flex; flex-direction: column; overflow-y: auto; }
.main-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; background: #fff; padding: 15px 25px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); height: 70px; box-sizing: border-box; }
.search-bar-wrapper { flex: 1; display: flex; justify-content: center; align-items: center; gap: 15px; }
.search-input-container { position: relative; width: 300px; max-width: 100%; }
.search-input { width: 100%; padding: 10px 15px; border-radius: 4px; border: 1px solid #dcdfe6; outline: none; font-size: 14px; box-sizing: border-box; transition: border-color 0.2s; }
.search-input:focus { border-color: #409eff; }
.search-btn { flex-shrink: 0; padding: 10px 24px; border: none; border-radius: 4px; background-color: #409eff; color: #fff; cursor: pointer; font-size: 14px; font-weight: 500; transition: background-color 0.3s; height: 38px; display: flex; align-items: center; }
.search-btn:hover { background-color: #66b1ff; }
.dropdown { width: 100%; background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; position: absolute; top: 105%; left: 0; max-height: 240px; overflow-y: auto; z-index: 100; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
.dropdown-item { padding: 10px 15px; cursor: pointer; font-size: 14px; color: #606266; }
.dropdown-item:hover { background-color: #ecf5ff; color: #409eff; }
.top-right { display: flex; align-items: center; justify-content: flex-end; min-width: 120px; }
.welcome-user { display: flex; align-items: center; gap: 10px; font-size: 14px; color: #606266; }
.nav-btn { padding: 8px 20px; font-size: 14px; background-color: #409eff; color: white; border: none; border-radius: 4px; cursor: pointer; }
.logout-btn { padding: 5px 12px; cursor: pointer; border-radius: 4px; border: 1px solid #dcdfe6; background-color: #fff; font-size: 12px; color: #606266; }
.logout-btn:hover { border-color: #c6e2ff; color: #409eff; }
.error-banner, .info-banner { margin-bottom: 20px; padding: 10px 15px; border-radius: 4px; font-size: 14px; }
.error-banner { border: 1px solid #fde2e2; background-color: #fef0f0; color: #f56c6c; }
.info-banner { border: 1px solid #e1f3d8; background-color: #f0f9eb; color: #67c23a; }
.content-body { flex: 1; display: flex; flex-direction: column; }
.section-title { font-size: 18px; color: #303133; margin-bottom: 15px; padding-left: 10px; border-left: 4px solid #409eff; }
.empty-state { text-align: center; color: #909399; margin-top: 50px; font-size: 16px; }
.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.course-card { background: #fff; border-radius: 8px; padding: 20px; display: flex; align-items: center; gap: 15px; box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05); transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; }
.course-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1); }
.course-icon { font-size: 24px; background: #f0f7ff; width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.course-info { display: flex; flex-direction: column; }
.course-name { margin: 0 0 5px 0; font-size: 16px; color: #303133; font-weight: 600; }
.course-no { margin: 0; font-size: 12px; color: #909399; }
</style>
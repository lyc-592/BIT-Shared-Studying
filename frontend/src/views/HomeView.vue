<template>
  <div class="home-container">
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <button class="nav-item" @click="goToPage('/courses')">课程</button>
        <button class="nav-item" @click="goToPage('/settings')">设置</button>
      </nav>
    </aside>

    <div class="main-content">
      <div class="main-header">
        <!-- 搜索栏 -->
        <div class="search-bar-wrapper">
          <div class="search-input-container">
            <input v-model="searchQuery" type="text" placeholder="请输入并选择专业..." class="search-input" @input="handleInput" @focus="handleFocus" @blur="handleBlur"/>
            <div v-if="showDropdown && filteredMajors.length > 0" class="dropdown">
              <div v-for="major in filteredMajors" :key="major.majorNo" class="dropdown-item" @mousedown.prevent="selectMajor(major)">
                {{ major.majorName }}
              </div>
            </div>
          </div>
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>

        <!-- 右上角登录/用户区域 -->
        <div class="top-right">
          <button v-if="!isLoggedIn" @click="goToLogin" class="nav-btn">登录 / 注册</button>
          <div v-else class="welcome-user">
            <span class="welcome-text">欢迎，{{ currentUsername }}</span>
            <button @click="logout" class="logout-btn">退出</button>
          </div>
        </div>
      </div>

      <!-- 内容区 -->
      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
      <div v-if="infoMessage" class="info-banner">{{ infoMessage }}</div>

      <div class="content-body">
        <h2 v-if="currentCourseList.length > 0" class="section-title">“{{ selectedMajorName }}” 的课程列表</h2>
        <div v-if="currentCourseList.length > 0" class="course-grid">
          <div v-for="course in currentCourseList" :key="course.courseNo" class="course-card" @click="goToCourseDetail(course.courseNo)">
            <div class="course-icon">📚</div>
            <div class="course-info">
              <h3 class="course-name">{{ course.courseName }}</h3>
              <p class="course-no">课程号: {{ course.courseNo }}</p>
            </div>
          </div>
        </div>
        <div v-else-if="hasSearched" class="empty-state">该专业下暂无课程数据。</div>
        <div v-else class="empty-state">请在上方搜索并选择专业以查看课程。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// 状态
const isLoggedIn = ref(false)
const currentUsername = ref('')
const errorMessage = ref('')
const infoMessage = ref('')

// 搜索数据
const searchQuery = ref('')
const showDropdown = ref(false)
const allMajors = ref([])
const filteredMajors = ref([])
const currentSelectedMajor = ref(null)
const currentCourseList = ref([])
const selectedMajorName = ref('')
const hasSearched = ref(false)

// --- 生命周期 ---
// 因为没有 KeepAlive，每次路由跳转到这里都会触发 onMounted
onMounted(async () => {
  // 1. 检查登录
  checkLoginStatus()
  // 2. 加载数据
  await loadAllMajors()
})

function checkLoginStatus() {
  const token = localStorage.getItem('token')
  const username = localStorage.getItem('username')

  if (token) {
    isLoggedIn.value = true
    currentUsername.value = username || '用户'
  } else {
    isLoggedIn.value = false
    currentUsername.value = ''
  }
}

// --- 业务逻辑 ---
async function loadAllMajors() {
  try {
    const res = await axios.get('/api/majors')
    if (res.data.success) allMajors.value = res.data.data || []
  } catch (err) { console.error(err) }
}

function handleInput() {
  const keyword = searchQuery.value.trim()
  currentSelectedMajor.value = null
  if (!keyword) { filteredMajors.value = []; showDropdown.value = false; return }
  showDropdown.value = true
  filteredMajors.value = allMajors.value.filter(m => m.majorName.includes(keyword))
}
function handleFocus() { if(searchQuery.value.trim()) handleInput() }
function handleBlur() { setTimeout(() => showDropdown.value = false, 200) }
function selectMajor(major) {
  searchQuery.value = major.majorName
  currentSelectedMajor.value = major
  showDropdown.value = false
}
async function handleSearch() {
  errorMessage.value = ''; infoMessage.value = ''; currentCourseList.value = []; hasSearched.value = true
  if (!currentSelectedMajor.value) {
    const exact = allMajors.value.find(m => m.majorName === searchQuery.value.trim())
    if(exact) currentSelectedMajor.value = exact
    else { errorMessage.value = '请选择有效专业'; return }
  }
  try {
    infoMessage.value = '加载中...'
    const res = await axios.get(`/api/majors/${currentSelectedMajor.value.majorNo}/courses`)
    infoMessage.value = ''
    if(res.data.success) {
      currentCourseList.value = res.data.data || []
      if(currentCourseList.value.length === 0) infoMessage.value = '暂无课程'
    } else errorMessage.value = res.data.message
  } catch(e) { errorMessage.value = '请求失败' }
}
function goToCourseDetail(id) { router.push({ name: 'CourseDetail', params: { courseNo: id } }) }
function goToLogin() { router.push('/login') }
function goToPage(path) { router.push(path) }

// 退出登录
function logout() {
  localStorage.clear() // 简单粗暴，清除所有
  isLoggedIn.value = false
  currentUsername.value = ''
  // 刷新页面确保状态彻底更新
  location.reload()
}
</script>

<style scoped>
/* 保持原有样式，省略以复用 */
.home-container { height: 100vh; width: 100%; display: flex; background-color: #f5f7fa; }
.sidebar { width: 220px; background-color: #001529; color: #fff; display: flex; flex-direction: column; padding: 20px 16px; flex-shrink: 0; }
.logo { font-size: 20px; font-weight: bold; margin-bottom: 30px; text-align: center; color: #fff; }
.nav-menu { display: flex; flex-direction: column; gap: 10px; }
.nav-item { width: 100%; padding: 10px 12px; border: none; border-radius: 4px; background: transparent; color: #ccc; text-align: left; cursor: pointer; font-size: 15px; transition: all 0.3s; }
.nav-item:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }
.main-content { flex: 1; padding: 20px 40px; display: flex; flex-direction: column; overflow-y: auto; }
.main-header { display: flex; justify-content: space-between; margin-bottom: 20px; background: #fff; padding: 15px 25px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); height: 70px; box-sizing: border-box; }
.search-bar-wrapper { flex: 1; display: flex; justify-content: center; align-items: center; gap: 15px; }
.search-input-container { position: relative; width: 300px; }
.search-input { width: 100%; padding: 10px 15px; border-radius: 4px; border: 1px solid #dcdfe6; outline: none; box-sizing: border-box; }
.search-input:focus { border-color: #409eff; }
.search-btn { padding: 10px 24px; border: none; border-radius: 4px; background-color: #409eff; color: #fff; cursor: pointer; }
.dropdown { width: 100%; background: #fff; border: 1px solid #e4e7ed; position: absolute; top: 105%; left: 0; max-height: 240px; overflow-y: auto; z-index: 100; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.dropdown-item { padding: 10px 15px; cursor: pointer; color: #606266; }
.dropdown-item:hover { background-color: #ecf5ff; color: #409eff; }
.top-right { display: flex; align-items: center; justify-content: flex-end; min-width: 120px; }
.welcome-user { display: flex; align-items: center; gap: 10px; font-size: 14px; color: #606266; }
.nav-btn { padding: 8px 20px; background-color: #409eff; color: white; border: none; border-radius: 4px; cursor: pointer; }
.logout-btn { padding: 5px 12px; cursor: pointer; border-radius: 4px; border: 1px solid #dcdfe6; background-color: #fff; font-size: 12px; }
.error-banner { margin-bottom: 20px; padding: 10px 15px; background-color: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; border-radius: 4px; }
.info-banner { margin-bottom: 20px; padding: 10px 15px; background-color: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; border-radius: 4px; }
.content-body { flex: 1; display: flex; flex-direction: column; }
.section-title { font-size: 18px; margin-bottom: 15px; border-left: 4px solid #409eff; padding-left: 10px; }
.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.course-card { background: #fff; padding: 20px; border-radius: 8px; display: flex; align-items: center; gap: 15px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); cursor: pointer; }
.course-icon { font-size: 24px; background: #f0f7ff; width: 48px; height: 48px; border-radius: 50%; display: flex; justify-content: center; align-items: center; }
.course-name { margin: 0 0 5px 0; font-size: 16px; font-weight: 600; }
.course-no { margin: 0; font-size: 12px; color: #999; }
.empty-state { text-align: center; color: #999; margin-top: 50px; }
</style>
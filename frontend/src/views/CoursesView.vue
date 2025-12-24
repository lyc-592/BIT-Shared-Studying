<template>
  <div class="home-container">
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <button class="nav-item active" @click="goToPage('/courses')">课程</button>
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
                placeholder="输入课程名称或ID在本地过滤..."
                class="search-input"
                @input="handleLocalSearch"
            />
          </div>
          <button class="search-btn" @click="handleLocalSearch">搜索</button>
        </div>

        <div class="top-right">
          <button v-if="!isLoggedIn" @click="goToLogin" class="nav-btn">登录 / 注册</button>
          <div v-else class="welcome-user">
            <span
                class="role-badge"
                :style="{
                color: getRoleInfo(currentRole).color,
                backgroundColor: getRoleInfo(currentRole).bgColor,
                borderColor: getRoleInfo(currentRole).color
              }"
            >
              {{ getRoleInfo(currentRole).label }}
            </span>
            <span class="welcome-text">{{ currentUsername }}</span>
            <button @click="logout" class="logout-btn">退出</button>
          </div>
        </div>
      </div>

      <div class="content-body">
        <h2 v-if="loading" class="section-title">正在从服务器获取所有课程...</h2>
        <h2 v-else class="section-title">
          {{ searchQuery ? `“${searchQuery}” 的检索结果` : '所有课程列表' }}
          <span class="count-badge">({{ displayList.length }})</span>
        </h2>

        <!-- 加载动画 -->
        <div v-if="loading" class="loading-state">数据同步中...</div>

        <!-- 课程列表 -->
        <div v-else-if="displayList.length > 0" class="course-grid">
          <div
              v-for="course in displayList"
              :key="course.courseNo"
              class="course-card"
              @click="goToCourseDetail(course.courseNo)"
          >
            <div class="course-icon">📚</div>
            <div class="course-info">
              <h3 class="course-name">{{ course.courseName }}</h3>
              <p class="course-no">ID: {{ course.courseNo }}</p>
              <p v-if="course.majorName" class="course-dept">{{ course.majorName }}</p>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          没有找到匹配的课程。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'CoursesView' })

import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { getRoleInfo } from '@/utils/role'

const router = useRouter()

// 状态
const isLoggedIn = ref(false)
const currentUsername = ref('')
const currentRole = ref(1)
const loading = ref(false)

const searchQuery = ref('')
const allCourses = ref([])
const displayList = ref([])
const lastUserId = ref(null)

function checkLoginStatus() {
  const token = localStorage.getItem('token')
  const username = localStorage.getItem('username')
  const uid = localStorage.getItem('userId')
  const role = localStorage.getItem('role')

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

function resetPageState() {
  console.log('CoursesView: 用户状态变化，重置页面...')
  searchQuery.value = ''
  allCourses.value = []
  displayList.value = []
  fetchAllData()
}

onMounted(async () => {
  const uid = checkLoginStatus()
  lastUserId.value = uid
  await fetchAllData()
})

onActivated(() => {
  const currentUid = checkLoginStatus()
  if (lastUserId.value !== currentUid) {
    resetPageState()
    lastUserId.value = currentUid
  }
})

// --- 核心修改：API 路径更改 ---
async function fetchAllData() {
  loading.value = true
  try {
    // 按照要求改为 /api/majors/all/courses
    const res = await axios.get('/api/majors/all/courses')
    if (res.data && res.data.success) {
      allCourses.value = res.data.data || []
      handleLocalSearch()
    }
  } catch (err) { console.error(err) } finally { loading.value = false }
}

function handleLocalSearch() {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (!keyword) {
    displayList.value = allCourses.value
    return
  }
  displayList.value = allCourses.value.filter(course => {
    const nameMatch = course.courseName && course.courseName.toLowerCase().includes(keyword)
    const noMatch = course.courseNo && String(course.courseNo).toLowerCase().includes(keyword)
    return nameMatch || noMatch
  })
}

const goToCourseDetail = (id) => router.push({ name: 'CourseDetail', params: { courseNo: id } })
const goToLogin = () => router.push('/login')
const logout = () => {
  localStorage.clear()
  resetPageState()
  lastUserId.value = null
  isLoggedIn.value = false
  router.push('/login')
}
const goToPage = (path) => router.push(path)
</script>

<style scoped>
/* 保持原有样式，与 HomeView 风格一致 */
.home-container { height: 100vh; width: 100%; display: flex; background-color: #f5f7fa; }
.sidebar { width: 220px; background-color: #001529; color: #fff; display: flex; flex-direction: column; padding: 20px 16px; flex-shrink: 0; }
.logo { font-size: 20px; font-weight: bold; margin-bottom: 30px; text-align: center; color: #fff; }
.nav-menu { display: flex; flex-direction: column; gap: 10px; }
.nav-item { width: 100%; padding: 10px 12px; border: none; border-radius: 4px; background: transparent; color: #ccc; text-align: left; cursor: pointer; font-size: 15px; transition: all 0.3s; }
.nav-item:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }
.nav-item.active { background-color: #409eff; color: #fff; font-weight: bold; }
.main-content { flex: 1; padding: 20px 40px; display: flex; flex-direction: column; overflow-y: auto; }
.main-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; background: #fff; padding: 15px 25px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); height: 70px; box-sizing: border-box; }
.search-bar-wrapper { flex: 1; display: flex; justify-content: center; align-items: center; gap: 15px; }
.search-input-container { position: relative; width: 400px; max-width: 100%; }
.search-input { width: 100%; padding: 10px 15px; border-radius: 4px; border: 1px solid #dcdfe6; outline: none; font-size: 14px; box-sizing: border-box; transition: border-color 0.2s; }
.search-input:focus { border-color: #409eff; }
.search-btn { flex-shrink: 0; padding: 10px 24px; border: none; border-radius: 4px; background-color: #409eff; color: #fff; cursor: pointer; font-size: 14px; font-weight: 500; transition: background-color 0.3s; height: 38px; display: flex; align-items: center; gap: 8px; }
.search-btn:hover { background-color: #66b1ff; }
.top-right { display: flex; align-items: center; justify-content: flex-end; min-width: 120px; }
.welcome-user { display: flex; align-items: center; gap: 10px; font-size: 14px; color: #606266; }
.role-badge { font-size: 12px; padding: 2px 8px; border-radius: 4px; border: 1px solid; font-weight: bold; }
.nav-btn { padding: 8px 20px; font-size: 14px; background-color: #409eff; color: white; border: none; border-radius: 4px; cursor: pointer; }
.logout-btn { padding: 5px 12px; cursor: pointer; border-radius: 4px; border: 1px solid #dcdfe6; background-color: #fff; font-size: 12px; color: #606266; }
.logout-btn:hover { border-color: #c6e2ff; color: #409eff; }
.content-body { flex: 1; display: flex; flex-direction: column; }
.section-title { font-size: 18px; color: #303133; margin-bottom: 15px; padding-left: 10px; border-left: 4px solid #409eff; display: flex; align-items: center; gap: 10px; }
.count-badge { font-size: 14px; color: #909399; font-weight: normal; }
.empty-state { text-align: center; color: #909399; margin-top: 50px; font-size: 16px; }
.loading-state { text-align: center; color: #909399; margin-top: 30px; font-size: 14px; }
.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.course-card { background: #fff; border-radius: 8px; padding: 20px; display: flex; align-items: center; gap: 15px; box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05); transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; }
.course-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1); }
.course-icon { font-size: 24px; background: #f0f7ff; width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #409eff; }
.course-info { display: flex; flex-direction: column; }
.course-name { margin: 0 0 5px 0; font-size: 16px; color: #303133; font-weight: 600; }
.course-no { margin: 0; font-size: 12px; color: #909399; }
.course-dept { margin: 2px 0 0 0; font-size: 12px; color: #409eff; background: #ecf5ff; padding: 2px 6px; border-radius: 4px; display: inline-block; width: fit-content;}
</style>
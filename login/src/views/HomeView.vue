<!-- src/views/HomeView.vue -->
<template>
  <div class="home-container">
    <!-- 右上角按钮 -->
    <div class="top-right">
      <!-- 如果已登录 (isLoggedIn 为 true)，按钮消失（或者显示退出，这里按你要求直接不显示登录按钮） -->
      <button v-if="!isLoggedIn" @click="goToLogin" class="nav-btn">
        登录 / 注册
      </button>
      <div v-else class="welcome-user">
        欢迎回来！ <button @click="logout" class="logout-btn">退出</button>
      </div>
    </div>

    <!-- 中间内容 -->
    <div class="center-content">
      <h1>Hello， BITShared！</h1>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isLoggedIn = ref(false)

// 页面加载时检查是否有 token
onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    isLoggedIn.value = true
  }
})

const goToLogin = () => {
  router.push('/login')
}

const logout = () => {
  localStorage.removeItem('token')
  isLoggedIn.value = false
  // 可选：刷新页面
  location.reload()
}
</script>

<style scoped>
.home-container {
  height: 100vh;
  width: 100%;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.top-right {
  position: absolute;
  top: 20px;
  right: 30px;
}

.nav-btn {
  padding: 10px 20px;
  font-size: 16px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.nav-btn:hover {
  background-color: #66b1ff;
}

.center-content h1 {
  font-size: 3em;
  color: #333;
}

.logout-btn {
  margin-left: 10px;
  padding: 5px 10px;
  cursor: pointer;
}
</style>
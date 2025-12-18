<!-- src/views/LoginView.vue -->
<template>
  <div class="auth-container">
    <button class="back-home-btn" @click="goToHome">← 回到主页</button>

    <div class="auth-box">
      <h2>用户登录</h2>
      <div class="form-group">
        <label>用户名</label>
        <input type="text" v-model="form.username" placeholder="请输入用户名" @keyup.enter="handleLogin"/>
      </div>
      <div class="form-group">
        <label>密码</label>
        <input type="password" v-model="form.password" placeholder="请输入密码" @keyup.enter="handleLogin"/>
      </div>
      <button class="submit-btn" @click="handleLogin" :disabled="isLoading">
        {{ isLoading ? '登录中...' : '登录' }}
      </button>
      <div class="link-text" @click="goToRegister">还没有账号？点击去注册</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const isLoading = ref(false)
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码')
    return
  }

  isLoading.value = true

  try {
    const res = await axios.post('/api/auth/login', form)
    const body = res.data

    console.log("🔥 [Login Debug] 后端返回数据:", body)

    if (body.success) {
      // 1. 存 Token
      const token = body.token || body.data?.token || 'session-token'
      localStorage.setItem('token', token)

      // 2. 存用户基础信息 (ID, Username, Email, Role)
      const data = body.data || {}

      // 必须拿到 ID
      const uid = data.id || body.userId
      if (uid) {
        localStorage.setItem('userId', uid)

        // --- 关键修改 START：保存所有基础信息 ---
        if (data.username) localStorage.setItem('username', data.username)
        if (data.email) localStorage.setItem('email', data.email) // 👈 存邮箱！
        if (data.role !== undefined) localStorage.setItem('role', data.role)
        // --- 关键修改 END ---

        alert('登录成功！')
        router.push('/')
      } else {
        console.error("❌ 无法找到用户 ID")
        alert('登录异常：未获取到用户ID')
      }
    } else {
      alert(body.message || '登录失败')
    }
  } catch (error) {
    console.error(error)
    alert('请求失败: ' + (error.response?.data?.message || error.message))
  } finally {
    isLoading.value = false
  }
}

const goToRegister = () => router.push('/register')
const goToHome = () => router.push('/')
</script>

<style scoped>
/* 保持原样 */
.auth-container { display: flex; justify-content: center; align-items: center; height: 100vh; background-color: #eef1f6; position: relative; }
.back-home-btn { position: absolute; top: 30px; left: 30px; padding: 10px 20px; background: white; border: 1px solid #dcdfe6; cursor: pointer; border-radius: 4px; color: #606266; }
.auth-box { width: 350px; padding: 40px; background: white; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); text-align: center; }
.form-group { margin-bottom: 20px; text-align: left; }
.form-group label { display: block; margin-bottom: 5px; font-weight: bold; }
.form-group input { width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
.submit-btn { width: 100%; padding: 12px; background-color: #42b983; color: white; border: none; border-radius: 4px; cursor: pointer; margin-top: 10px; }
.submit-btn:disabled { background-color: #a0cfff; }
.link-text { margin-top: 20px; color: #409eff; cursor: pointer; font-size: 14px; }
</style>
<template>
  <div class="auth-container">
    <button class="back-home-btn" @click="goToHome">
      ← 回到主页
    </button>

    <div class="auth-box">
      <h2>用户登录</h2>

      <div class="form-group">
        <label>用户名</label>
        <div class="input-wrapper">
          <input
              type="text"
              v-model="form.username"
              placeholder="请输入用户名"
              @keyup.enter="handleLogin"
          />
        </div>
      </div>

      <div class="form-group">
        <label>密码</label>
        <div class="input-wrapper">
          <input
              type="password"
              v-model="form.password"
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
          />
        </div>
      </div>

      <button class="submit-btn" @click="handleLogin" :disabled="isLoading">
        {{ isLoading ? '登录中...' : '登录' }}
      </button>

      <div class="link-text" @click="goToRegister">
        还没有账号？点击去注册 →
      </div>
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

    console.log("🔥 [Login Debug] Response:", body)

    if (body.success) {
      // 1. Token
      const token = body.token || body.data?.token || 'session-token-placeholder'
      // 修改点：使用 sessionStorage
      sessionStorage.setItem('token', token)

      // 2. User Info
      const data = body.data || {}
      const uid = data.id || data.userId || body.userId

      if (uid) {
        sessionStorage.setItem('userId', uid)

        if (data.username) sessionStorage.setItem('username', data.username)
        if (data.email) sessionStorage.setItem('email', data.email)

        // 3. Role
        const role = data.role || '1'
        sessionStorage.setItem('role', role)

        // 4. MajorNo (专业管理员)
        if (parseInt(role) === 2) {
          const userMajor = data.majorNo || data.major_no || data.majorId
          if (userMajor) {
            sessionStorage.setItem('auth_major_no', userMajor)
          } else {
            sessionStorage.removeItem('auth_major_no')
          }
        } else {
          sessionStorage.removeItem('auth_major_no')
        }

        alert('登录成功！')
        router.push('/')
      } else {
        console.error("❌ 登录成功但未找到用户ID字段", body)
        alert('登录异常：无法获取用户身份信息')
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
/* 样式不变 */
/* 认证页面整体容器（登录/注册等） */
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #eef1f6;
  position: relative;
}

/* 返回首页按钮 */
.back-home-btn {
  position: absolute;
  top: 30px;
  left: 30px;
  padding: 10px 20px;
  background: white;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  border-radius: 4px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

/* 返回首页按钮 hover 状态 */
.back-home-btn:hover {
  color: #409eff;
  border-color: #c6e2ff;
}

/* 认证卡片（登录/注册表单容器） */
.auth-box {
  width: 350px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  text-align: center;
}

/* 认证卡片标题 */
.auth-box h2 {
  margin-bottom: 30px;
  color: #303133;
}

/* 表单组容器 */
.form-group {
  margin-bottom: 20px;
  text-align: left;
}

/* 表单标签 */
.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #606266;
}

/* 输入框包装器（用于定位图标等扩展） */
.input-wrapper {
  position: relative;
}

/* 表单输入框基础样式 */
.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}

/* 表单输入框聚焦状态 */
.form-group input:focus {
  border-color: #409eff;
  outline: none;
}

/* 提交按钮基础样式 */
.submit-btn {
  width: 100%;
  padding: 12px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 10px;
  font-size: 16px;
  transition: background 0.3s;
}

/* 提交按钮 hover 状态 */
.submit-btn:hover {
  background-color: #66b1ff;
}

/* 提交按钮禁用状态 */
.submit-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

/* 链接文本（如“忘记密码”/“注册账号”） */
.link-text {
  margin-top: 20px;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.3s;
}

/* 链接文本 hover 状态 */
.link-text:hover {
  color: #409eff;
  text-decoration: underline;
}
</style>
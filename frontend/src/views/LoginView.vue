<!-- src/views/LoginView.vue -->
<template>
  <div class="auth-container">
    <div class="auth-box">
      <h2>用户登录</h2>

      <div class="form-group">
        <label>用户名</label>
        <input type="text" v-model="form.username" placeholder="请输入用户名" />
      </div>

      <div class="form-group">
        <label>密码</label>
        <input type="password" v-model="form.password" placeholder="请输入密码" />
      </div>

      <button class="submit-btn" @click="handleLogin">登录</button>

      <div class="link-text" @click="goToRegister">
        还没有账号？点击去注册
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// 定义表单数据
const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码')
    return
  }

  try {
    // ==========================================================
    // TODO: 根据你txt文件里的后端接口修改下面的 URL 和 数据结构
    // 假设后端地址是 http://localhost:8080/api/login
    // ==========================================================

    // 发送请求
    // 这里的 url 替换成你后端的真实地址
    console.log("Login Request")
    const response = await axios.post('/api/auth/login', {
      username: form.username,
      password: form.password
    })

    const body = response.data
    // 假设后端返回的数据里有一个 code: 200 表示成功，或者直接返回 token
    // 请根据实际 JSON 调整判断逻辑
    if (body.success) {
      alert('登录成功！')

      // 保存 token (假设后端返回了 token字段)
      // 如果后端没返回token，只返回成功，随便存个标记也行
      const token = response.data.token || 'mock-token-123'
      localStorage.setItem('token', token)

      // 跳转回主页
      router.push('/')
    } else {
      alert(response.data.message || '登录失败，请检查用户名或密码')
    }

  } catch (error) {
    if (error.response && error.response.data) {
      const body = error.response.data
      // 比如：{ success:false, message:"登录失败：用户名不存在", data:null }
      alert(body.message || '登录失败')
    }
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
/* 简单的居中和样式，Login和Register页面可以共用类似的样式 */
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #eef1f6;
}
.auth-box {
  width: 350px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  text-align: center;
}
.form-group {
  margin-bottom: 20px;
  text-align: left;
}
.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}
.form-group input {
  width: 100%;
  padding: 10px;
  box-sizing: border-box; /* 关键：防止padding撑大宽度 */
  border: 1px solid #ccc;
  border-radius: 4px;
}
.submit-btn {
  width: 100%;
  padding: 12px;
  background-color: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 10px;
}
.submit-btn:hover {
  background-color: #3aa876;
}
.link-text {
  margin-top: 20px;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}
.link-text:hover {
  text-decoration: underline;
}
</style>
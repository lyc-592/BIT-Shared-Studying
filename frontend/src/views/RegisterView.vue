<!-- src/views/RegisterView.vue -->
<template>
  <div class="auth-container">
    <div class="auth-box">
      <h2>用户注册</h2>

      <div class="form-group">
        <label>用户名</label>
        <input type="text" v-model="form.username" placeholder="请输入用户名" />
      </div>

      <div class="form-group">
        <label>邮箱</label>
        <input type="email" v-model="form.email" placeholder="请输入邮箱" />
      </div>

      <div class="form-group">
        <label>密码</label>
        <input type="password" v-model="form.password" placeholder="请输入密码" />
      </div>

      <div class="form-group">
        <label>确认密码</label>
        <input type="password" v-model="form.confirmPassword" placeholder="请再次输入密码" />
      </div>

      <button class="submit-btn" @click="handleRegister">注册</button>

      <div class="link-text" @click="goToLogin">
        跳转回登录页面
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const handleRegister = async () => {
  // 1. 基础非空校验
  if (!form.username || !form.email || !form.password || !form.confirmPassword) {
    alert('请填写所有必填项')
    return
  }

  // 2. 前端完成密码一致性校验
  if (form.password !== form.confirmPassword) {
    alert('两次输入的密码不一致！')
    return
  }

  try {
    // ==========================================================
    // TODO: 根据你txt文件里的后端接口修改下面的 URL 和 字段
    // 用户名和邮箱重复的判断由后端完成，前端只管发送请求并处理报错
    // ==========================================================

    const response = await axios.post('/api/auth/register', {
      username: form.username,
      email: form.email,
      password: form.password
      // 通常不需要传 confirmPassword 给后端
    })

    if (response.data.code === 200 || response.status === 200) {
      alert('注册成功！请登录。')
      router.push('/login')
    } else {
      // 后端可能会返回 "用户名已存在" 或 "邮箱已注册"
      alert(response.data.message || '注册失败')
    }

  } catch (error) {
    console.error(error)
    if (error.response && error.response.data) {
      const body = error.response.data
      // 比如：{ success:false, message:"登录失败：用户名不存在", data:null }
      alert(body.message || '注册失败')
    }
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
/* 复用之前的样式，或者直接复制上面的style */
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
  margin-bottom: 15px;
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
  box-sizing: border-box;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.submit-btn {
  width: 100%;
  padding: 12px;
  background-color: #409eff; /* 注册按钮换个颜色 */
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 10px;
}
.submit-btn:hover {
  background-color: #66b1ff;
}
.link-text {
  margin-top: 20px;
  color: #909399;
  cursor: pointer;
  font-size: 14px;
}
.link-text:hover {
  text-decoration: underline;
  color: #333;
}
</style>
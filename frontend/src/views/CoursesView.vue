<template>
  <div class="home-container">
    <!-- 左侧侧边导航栏 (保持与主页一致) -->
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <!-- 当前页面高亮 -->
        <button class="nav-item active" @click="goToPage('/courses')">课程</button>
        <button class="nav-item" @click="goToPage('/settings')">设置</button>
      </nav>
    </aside>

    <!-- 右侧主内容 -->
    <div class="main-content">
      <div class="page-header">
        <h2>课程资源管理 & 上传测试</h2>
      </div>

      <!-- 上传测试区域 -->
      <div class="upload-card">
        <h3 class="card-title">文件上传测试 (API Test)</h3>

        <div class="form-group">
          <label class="form-label">1. 服务器目标路径 (Target Dir)</label>
          <input
              v-model="targetDir"
              type="text"
              class="form-input"
              placeholder="例如: /home/data 或 D:/temp (目录必须已存在)"
          />
          <p class="form-tip">注意：后端不会自动创建目录，请确保服务器上该路径已存在。</p>
        </div>

        <div class="form-group">
          <label class="form-label">2. 选择文件 (File)</label>
          <input
              type="file"
              class="file-input"
              @change="handleFileChange"
              ref="fileInputRef"
          />
        </div>

        <div class="action-area">
          <button
              class="upload-btn"
              @click="handleUpload"
              :disabled="isUploading"
          >
            {{ isUploading ? '正在上传...' : '开始上传' }}
          </button>
        </div>

        <!-- 结果反馈 -->
        <div v-if="resultMessage" :class="['result-box', resultSuccess ? 'success' : 'error']">
          <p><strong>状态:</strong> {{ resultSuccess ? '成功' : '失败' }}</p>
          <p><strong>消息:</strong> {{ resultMessage }}</p>
          <p v-if="resultData"><strong>返回数据:</strong> {{ resultData }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

// --- 状态变量 ---
const targetDir = ref('')       // 目标目录
const selectedFile = ref(null)  // 选中的文件对象
const isUploading = ref(false)  // 上传加载状态

// 结果展示
const resultSuccess = ref(false)
const resultMessage = ref('')
const resultData = ref(null)
const fileInputRef = ref(null)

// --- 路由跳转 ---
const goToPage = (path) => {
  if (path === '/courses') return
  router.push(path)
}

// --- 文件选择 ---
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    selectedFile.value = file
    // 重置之前的状态
    resultMessage.value = ''
    resultData.value = null
  }
}

// --- 上传逻辑 ---
async function handleUpload() {
  // 1. 本地校验
  if (!targetDir.value.trim()) {
    alert('请输入服务器目标目录路径 (targetDir)')
    return
  }
  if (!selectedFile.value) {
    alert('请选择要上传的文件')
    return
  }

  isUploading.value = true
  resultMessage.value = ''
  resultSuccess.value = false
  resultData.value = null

  // 2. 构建 FormData
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('targetDir', targetDir.value.trim())

  try {
    // 3. 发送请求
    // 注意：如果是跨域，需要在 vite.config.js 配置 proxy，或者后端开启 CORS
    // 这里直接使用提供的 IP 地址进行请求
    const apiUrl = '/api/files/upload'

    const res = await axios.post(apiUrl, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    // 4. 处理响应
    // 预期格式: { success: true/false, message: "...", data: "..." }
    if (res.data) {
      resultSuccess.value = res.data.success
      resultMessage.value = res.data.message
      resultData.value = res.data.data
    } else {
      resultSuccess.value = false
      resultMessage.value = '服务器返回格式异常'
    }

  } catch (error) {
    console.error(error)
    resultSuccess.value = false
    resultMessage.value = error.response?.data?.message || error.message || '请求发送失败'
  } finally {
    isUploading.value = false
  }
}
</script>

<style scoped>
/* --- 复用 Home.vue 的基础布局样式 --- */
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
  color: #fff;
  font-weight: bold;
}

.main-content {
  flex: 1;
  padding: 20px 40px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* --- 课程页面特定样式 --- */
.page-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 10px;
}

.page-header h2 {
  color: #303133;
  font-weight: 600;
}

.upload-card {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  max-width: 600px; /* 限制宽度更美观 */
}

.card-title {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #303133;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #606266;
}

.form-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #409eff;
  outline: none;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.file-input {
  font-size: 14px;
  color: #606266;
}

.action-area {
  margin-top: 30px;
}

.upload-btn {
  background-color: #67c23a;
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.upload-btn:hover {
  background-color: #85ce61;
}

.upload-btn:disabled {
  background-color: #b3e19d;
  cursor: not-allowed;
}

/* 结果反馈框样式 */
.result-box {
  margin-top: 25px;
  padding: 15px;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.6;
}

.result-box.success {
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
  color: #67c23a;
}

.result-box.error {
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
  color: #f56c6c;
}

.result-box p {
  margin: 5px 0;
}
</style>
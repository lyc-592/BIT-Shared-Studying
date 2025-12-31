<template>
  <div class="folder-view">
    <div class="header">
      <button class="back-btn" @click="$router.back()">← 返回树形图</button>
      <div class="header-info">
        <h2>📂 {{ folderName }}</h2>
        <span class="path-info">当前路径: {{ currentPath }}</span>
      </div>
    </div>

    <!-- 上传区域：使用 canEdit 严格控制权限 -->
    <div class="upload-section" v-if="canEdit">
      <span class="section-label">📤 上传文件到当前目录：</span>
      <div class="upload-controls">
        <input type="file" ref="folderFileInputRef" @change="handleFileSelect" class="file-input" />
        <button
            class="upload-btn"
            @click="handleFolderUpload"
            :disabled="!selectedFile || isUploading"
        >
          {{ isUploading ? '上传中...' : '点击上传' }}
        </button>
      </div>
    </div>

    <!-- 添加：针对普通用户（无canEdit权限）展示的“申请上传”区域 -->
    <div class="upload-section request-section" v-if="!canEdit">
      <span class="section-label">申请上传文件：</span>
      <div class="upload-controls">
        <input type="file" @change="handleRequestFileSelect" class="file-input" />
        <input v-model="requestRemark" placeholder="填写申请备注（必填）..." class="remark-input" />
        <button
            class="upload-btn request-btn"
            @click="handleRequestUpload"
            :disabled="!requestFile || isRequesting"
        >
          {{ isRequesting ? '提交中...' : '提交申请' }}
        </button>
      </div>
    </div>

    <div v-if="files.length > 0" class="batch-bar">
      <div class="batch-left">
        <label class="select-all-label">
          <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
          全选 ({{ selectedPaths.length }}/{{ files.length }})
        </label>
      </div>
      <div class="batch-right">
        <button
            class="batch-btn download"
            @click="handleBatchDownload"
            :disabled="selectedPaths.length === 0"
        >
          📦 批量下载选中项
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">正在加载文件列表...</div>

    <div v-else class="file-grid">
      <div v-if="files.length === 0" class="empty-state">
        <div class="empty-icon">📂</div>
        <p>此文件夹下暂无文件</p>
      </div>

      <div
          v-for="file in files"
          :key="file.name"
          :class="['file-card', { 'is-selected': isSelected(file) }]"
          @click.stop="toggleSelection(file)"
      >
        <div class="card-checkbox">
          <input
              type="checkbox"
              :checked="isSelected(file)"
              @click.stop="toggleSelection(file)"
          />
        </div>

        <div class="file-icon">{{ getFileIcon(file.name) }}</div>

        <div class="file-info">
          <div class="file-name" :title="file.name">{{ file.name }}</div>

          <div class="file-actions" @click.stop>
            <button class="action-btn preview-btn" @click="handlePreview(file)">
              👁️ 预览
            </button>
            <button class="action-btn download-btn" @click="handleDownload(file)">
              ⬇️ 下载
            </button>
            <!-- 添加：针对单个文件的讨论按钮 -->
            <button class="action-btn discuss-btn" @click="goToDiscuss(file)">
              💬 讨论
            </button>
            <!-- 删除按钮：使用 canEdit 严格控制 -->
            <button v-if="canEdit" class="action-btn delete-btn" @click="handleDeleteFile(file)">
              🗑️ 删除
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router' // 添加：引入 useRouter
import axios from 'axios'

const route = useRoute()
const router = useRouter() // 添加：初始化 router
const courseNo = route.params.courseNo
const currentPath = route.query.path
const folderName = route.query.folderName || '文件夹内容'

// 权限相关 (修改点：使用 sessionStorage)
const canEdit = ref(false)
const userRole = parseInt(sessionStorage.getItem('role') || '1')
const userId = sessionStorage.getItem('userId')

const files = ref([])
const loading = ref(true)
const selectedFile = ref(null)
const isUploading = ref(false)
const folderFileInputRef = ref(null)
const selectedPaths = ref([])

// 添加：“文件上传申请”所需响应式变量
const requestFile = ref(null)
const requestRemark = ref('')
const isRequesting = ref(false)

onMounted(async () => {
  // 并行检查权限和加载文件
  await Promise.all([loadFolderContent(), checkPermission()])
})

// 添加：跳转到发帖页面并携带 referencePath
function goToDiscuss(file) {
  const filePath = getFullFilePath(file.name)
  // 先获取论坛信息以确保能拿到正确的 forumNo 传给发帖页
  axios.get(`/api/forums/by-course/${courseNo}`).then(res => {
    if (res.data.success) {
      router.push({
        name: 'TopicPost',
        params: { courseNo: courseNo },
        query: {
          forumNo: res.data.data.forumNo,
          referencePath: filePath,
          fileName: file.name
        }
      })
    }
  })
}

// --- 权限检查 ---
async function checkPermission() {
  if (userRole >= 3) {
    canEdit.value = true
    return
  }
  if (userRole === 1) {
    canEdit.value = false
    return
  }
  // Role 2 调用后端接口
  try {
    const res = await axios.get('/api/permissions/check', {
      params: { userId: userId, courseNo: courseNo }
    })
    if (res.data.success && res.data.data) {
      canEdit.value = res.data.data.hasPermission
    } else {
      canEdit.value = false
    }
  } catch (e) {
    console.error('Permission check failed', e)
    canEdit.value = false
  }
}

function getFileIcon(filename) {
  if (!filename) return '📄'
  const ext = filename.split('.').pop().toLowerCase()
  switch (ext) {
    case 'pdf': return '📕'
    case 'ppt': case 'pptx': return '📊'
    case 'doc': case 'docx': return '📝'
    case 'jpg': case 'jpeg': case 'png': case 'gif': case 'bmp': case 'svg': case 'webp': return '🖼️'
    case 'mp4': case 'avi': case 'mov': case 'mkv': case 'webm': return '🎬'
    case 'zip': case 'rar': case 'tar': case '7z': return '📦'
    default: return '📄'
  }
}

async function loadFolderContent() {
  loading.value = true
  files.value = []
  selectedPaths.value = []
  try {
    const res = await axios.get(`/api/course/${courseNo}/file-tree`)
    const rootData = Array.isArray(res.data) ? res.data[0] : res.data
    if (!rootData) return
    const targetNode = findNodeByPathRecursive(rootData, '', currentPath)
    if (targetNode && targetNode.children) {
      files.value = targetNode.children.filter(item => item.type === 'file')
    }
  } catch (err) {
    console.error(err)
    alert('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

function findNodeByPathRecursive(node, parentPath, targetPath) {
  let currentFullPath = node.name
  if (parentPath) {
    currentFullPath = `${parentPath}/${node.name}`
  }
  if (currentFullPath === targetPath) return node
  if (node.children) {
    for (const child of node.children) {
      if (child.type === 'directory') {
        const found = findNodeByPathRecursive(child, currentFullPath, targetPath)
        if (found) return found
      }
    }
  }
  return null
}

function getFullFilePath(fileName) {
  const separator = currentPath.endsWith('/') ? '' : '/'
  return `${currentPath}${separator}${fileName}`
}

function isSelected(file) {
  return selectedPaths.value.includes(getFullFilePath(file.name))
}
function toggleSelection(file) {
  const fullPath = getFullFilePath(file.name)
  const index = selectedPaths.value.indexOf(fullPath)
  if (index > -1) selectedPaths.value.splice(index, 1)
  else selectedPaths.value.push(fullPath)
}
const isAllSelected = computed(() => { // 引入 computed
  if (files.value.length === 0) return false
  return selectedPaths.value.length === files.value.length
})
function toggleSelectAll() {
  if (selectedPaths.value.length === files.value.length) selectedPaths.value = []
  else selectedPaths.value = files.value.map(f => getFullFilePath(f.name))
}

function handleBatchDownload() {
  if (selectedPaths.value.length === 0) {
    alert('请至少选择一个文件')
    return
  }
  const pathsParam = selectedPaths.value.join(',')
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = `/api/files/batch-download?paths=${encodeURIComponent(pathsParam)}`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handlePreview(file) {
  window.open(`/api/files/preview?path=${encodeURIComponent(getFullFilePath(file.name))}`, '_blank')
}

function handleDownload(file) {
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = `/api/files/download?path=${encodeURIComponent(getFullFilePath(file.name))}`
  link.setAttribute('download', file.name)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

async function handleDeleteFile(file) {
  if (!confirm(`确定要删除文件 "${file.name}" 吗？`)) return
  try {
    const formData = new FormData()
    formData.append('dir', getFullFilePath(file.name))
    const res = await axios.post('/api/files/delete', formData)
    if (res.data.success) {
      alert('删除成功')
      await loadFolderContent()
    } else {
      alert('删除失败: ' + res.data.message)
    }
  } catch (err) {
    alert('请求失败: ' + err.message)
  }
}

function handleFileSelect(event) {
  selectedFile.value = event.target.files[0]
}

async function handleFolderUpload() {
  if (!selectedFile.value) return
  isUploading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('targetDir', currentPath)
  try {
    const res = await axios.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.success) {
      alert('上传成功')
      selectedFile.value = null
      if (folderFileInputRef.value) folderFileInputRef.value.value = ''
      await loadFolderContent()
    } else {
      alert('上传失败: ' + res.data.message)
    }
  } catch (err) {
    alert('请求失败: ' + err.message)
  } finally {
    isUploading.value = false
  }
}

// 添加：申请上传的相关处理逻辑
function handleRequestFileSelect(event) {
  requestFile.value = event.target.files[0]
}

async function handleRequestUpload() {
  if (!requestFile.value) {
    alert('请选择要上传的文件')
    return
  }
  if (!requestRemark.value.trim()) {
    alert('请填写申请备注')
    return
  }

  isRequesting.value = true
  const formData = new FormData()
  formData.append('requesterId', userId)
  formData.append('courseNo', courseNo)
  formData.append('file', requestFile.value)
  formData.append('remark', requestRemark.value)
  formData.append('targetAbsolutePath', "/root/sharing_files/" + currentPath)

  try {
    const res = await axios.post('/api/file-upload-requests/submit', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.success) {
      alert('上传申请已提交，等待管理员审核')
      requestFile.value = null
      requestRemark.value = ''
      // 清空文件 input (如果是通过 e.target.files 拿到的话通常需要手动清空 DOM 或刷新状态)
    } else {
      alert('申请失败: ' + res.data.message)
    }
  } catch (err) {
    alert('请求失败: ' + err.message)
  } finally {
    isRequesting.value = false
  }
}
</script>

<style scoped>
/* 文件夹视图整体容器 */
.folder-view {
  padding: 30px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 头部区域样式 */
.header {
  margin-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 返回按钮样式 */
.back-btn {
  align-self: flex-start;
  cursor: pointer;
  background: white;
  border: 1px solid #dcdfe6;
  padding: 6px 15px;
  border-radius: 4px;
  color: #606266;
}

/* 头部信息标题 */
.header-info h2 {
  margin: 0;
  color: #303133;
  font-size: 22px;
}

/* 路径信息文本 */
.path-info {
  color: #909399;
  font-size: 13px;
  margin-top: 5px;
  display: block;
}

/* 上传区域容器 */
.upload-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 区域标签样式 */
.section-label {
  font-weight: bold;
  color: #606266;
}

/* 上传控制按钮组 */
.upload-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 上传按钮样式 */
.upload-btn {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

/* 批量操作栏 */
.batch-bar {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  padding: 10px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 全选标签样式 */
.select-all-label {
  cursor: pointer;
  font-weight: bold;
  color: #1890ff;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 批量操作按钮基础样式 */
.batch-btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: white;
}

/* 批量下载按钮样式 */
.batch-btn.download {
  background-color: #1890ff;
}

/* 文件网格布局容器 */
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

/* 空状态样式 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  color: #909399;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
}

/* 空状态图标 */
.empty-icon {
  font-size: 48px;
}

/* 文件卡片基础样式 */
.file-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: all 0.2s;
  border: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  cursor: pointer;
}

/* 文件卡片选中状态 */
.file-card.is-selected {
  border-color: #409eff;
  background-color: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 文件卡片 hover 状态 */
.file-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

/* 卡片复选框容器 */
.card-checkbox {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 5;
}

/* 卡片复选框输入框 */
.card-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 文件图标样式 */
.file-icon {
  font-size: 40px;
  margin-bottom: 15px;
}

/* 文件信息容器 */
.file-info {
  width: 100%;
}

/* 文件名样式 */
.file-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* 文件操作按钮组 */
.file-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

/* 操作按钮基础样式 */
.action-btn {
  font-size: 13px;
  border: none;
  padding: 6px 10px;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
}

/* 预览按钮样式 */
.preview-btn {
  background-color: #e6a23c;
  color: white;
}

/* 下载按钮样式 */
.download-btn {
  background-color: #409eff;
  color: white;
}

/* 添加：申请上传按钮及输入框样式 */
.request-section {
  border: 1px solid #e6a23c;
  background-color: #fdf6ec;
}

/* 备注输入框样式 */
.remark-input {
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  width: 250px;
}

/* 申请上传按钮样式 */
.request-btn {
  background-color: #e6a23c;
}

/* 申请上传按钮 hover 状态 */
.request-btn:hover {
  background-color: #ebb563;
}

/* 讨论按钮样式 */
.discuss-btn {
  background-color: #67c23a;
  color: white;
}

/* 讨论按钮 hover 状态 */
.discuss-btn:hover {
  background-color: #85ce61;
}

/* 删除按钮样式 */
.delete-btn {
  background-color: #f56c6c;
  color: white;
}

/* 加载状态样式 */
.loading {
  text-align: center;
  color: #909399;
  padding-top: 50px;
}
</style>
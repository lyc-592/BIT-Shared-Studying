<template>
  <div class="folder-view">
    <div class="header">
      <button class="back-btn" @click="$router.back()">← 返回树形图</button>
      <div class="header-info">
        <h2>📁 {{ folderName }}</h2>
        <span class="path-info">当前路径: {{ currentPath }}</span>
      </div>
    </div>

    <!-- 上传区域 -->
    <div class="upload-section">
      <span class="section-label">上传文件到当前目录：</span>
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

    <!-- 批量操作栏 (当有文件时显示) -->
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

    <!-- 文件列表 -->
    <div v-if="loading" class="loading">正在加载文件列表...</div>

    <div v-else class="file-grid">
      <!-- 空状态 -->
      <div v-if="files.length === 0" class="empty-state">
        此文件夹下暂无文件 (仅显示文件，不显示子文件夹)
      </div>

      <!-- 文件卡片 -->
      <div
          v-for="file in files"
          :key="file.name"
          :class="['file-card', { 'is-selected': isSelected(file) }]"
          @click.stop="toggleSelection(file)"
      >
        <!-- 复选框 (阻止冒泡，防止触发卡片点击) -->
        <div class="card-checkbox">
          <input
              type="checkbox"
              :checked="isSelected(file)"
              @click.stop="toggleSelection(file)"
          />
        </div>

        <div class="file-icon">📄</div>
        <div class="file-info">
          <div class="file-name" :title="file.name">{{ file.name }}</div>

          <!-- 按钮组 (阻止冒泡，防止触选选中逻辑) -->
          <div class="file-actions" @click.stop>
            <button class="action-btn preview-btn" @click="handlePreview(file)">
              👁️ 预览
            </button>
            <button class="action-btn download-btn" @click="handleDownload(file)">
              ⬇️ 下载
            </button>
            <button class="action-btn delete-btn" @click="handleDeleteFile(file)">
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
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const courseNo = route.params.courseNo

// 接收从 TreeView 传过来的完整路径
const currentPath = route.query.path
const folderName = route.query.folderName || '文件夹内容'

const files = ref([])
const loading = ref(true)

// 上传相关
const selectedFile = ref(null)
const isUploading = ref(false)
const folderFileInputRef = ref(null)

// --- 批量下载相关状态 ---
const selectedPaths = ref([]) // 存储选中文件的完整路径字符串

onMounted(async () => {
  await loadFolderContent()
})

// --- 核心逻辑：加载并匹配节点 ---
async function loadFolderContent() {
  loading.value = true
  files.value = []
  selectedPaths.value = [] // 切换或刷新时清空选中项

  try {
    const res = await axios.get(`/api/course/${courseNo}/file-tree`)
    const rootData = Array.isArray(res.data) ? res.data[0] : res.data

    if (!rootData) {
      console.warn("树数据为空")
      return
    }

    const targetNode = findNodeByPathRecursive(rootData, '', currentPath)

    if (targetNode && targetNode.children) {
      files.value = targetNode.children.filter(item => item.type === 'file')
    } else {
      console.warn("未找到对应路径的节点:", currentPath)
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

  if (currentFullPath === targetPath) {
    return node
  }

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

// --- 辅助函数：构建完整路径 ---
function getFullFilePath(fileName) {
  const separator = currentPath.endsWith('/') ? '' : '/'
  return `${currentPath}${separator}${fileName}`
}

// ==========================================
//               批量选择逻辑
// ==========================================

// 判断是否选中
function isSelected(file) {
  const fullPath = getFullFilePath(file.name)
  return selectedPaths.value.includes(fullPath)
}

// 切换单个文件选中状态
function toggleSelection(file) {
  const fullPath = getFullFilePath(file.name)
  const index = selectedPaths.value.indexOf(fullPath)
  if (index > -1) {
    selectedPaths.value.splice(index, 1) // 取消选中
  } else {
    selectedPaths.value.push(fullPath) // 选中
  }
}

// 计算属性：是否全选
const isAllSelected = computed(() => {
  if (files.value.length === 0) return false
  return selectedPaths.value.length === files.value.length
})

// 全选/取消全选
function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedPaths.value = [] // 全不选
  } else {
    // 全选：把当前列表所有文件的完整路径都加进去
    selectedPaths.value = files.value.map(f => getFullFilePath(f.name))
  }
}

// ==========================================
//               批量下载逻辑
// ==========================================

function handleBatchDownload() {
  if (selectedPaths.value.length === 0) {
    alert('请至少选择一个文件')
    return
  }

  // 1. 将数组转为逗号分隔的字符串
  // 注意：如果路径中包含逗号，可能会有问题，但通常文件名不建议含逗号
  // 后端逻辑是 split(",")
  const pathsParam = selectedPaths.value.join(',')

  // 2. 构建 URL
  // 假设你的 FileController 都在 /api/files 下
  // 所以完整路径是 /api/files/batch-download
  const downloadUrl = `/api/files/batch-download?paths=${encodeURIComponent(pathsParam)}`

  // 3. 触发下载
  // 使用创建隐藏 a 标签的方式，体验更好
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = downloadUrl
  // 如果是多文件，后端通常返回 zip，浏览器会自动识别文件名
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// ==========================================
//             原有单文件操作
// ==========================================

function handlePreview(file) {
  const fullFilePath = getFullFilePath(file.name)
  const previewUrl = `/api/files/preview?path=${encodeURIComponent(fullFilePath)}`
  window.open(previewUrl, '_blank')
}

function handleDownload(file) {
  const fullFilePath = getFullFilePath(file.name)
  const downloadUrl = `/api/files/download?path=${encodeURIComponent(fullFilePath)}`

  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = downloadUrl
  link.setAttribute('download', file.name)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

async function handleDeleteFile(file) {
  const fullFilePath = getFullFilePath(file.name)
  if (!confirm(`确定要删除文件 "${file.name}" 吗？`)) return

  try {
    const formData = new FormData()
    formData.append('dir', fullFilePath)
    const res = await axios.post('/api/files/delete', formData)

    if (res.data && res.data.success) {
      alert('删除成功')
      await loadFolderContent()
    } else {
      alert('删除失败: ' + (res.data.message || '未知错误'))
    }
  } catch (err) {
    console.error(err)
    alert('请求失败: ' + err.message)
  }
}

function handleFileSelect(event) {
  selectedFile.value = event.target.files[0]
}

async function handleFolderUpload() {
  if (!selectedFile.value) return
  if (!currentPath) {
    alert('无法确定当前路径，上传失败')
    return
  }
  isUploading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('targetDir', currentPath)

  try {
    const res = await axios.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data && res.data.success) {
      alert('上传成功')
      selectedFile.value = null
      if (folderFileInputRef.value) folderFileInputRef.value.value = ''
      await loadFolderContent()
    } else {
      alert('上传失败: ' + (res.data.message || '未知错误'))
    }
  } catch (err) {
    alert('请求失败: ' + err.message)
  } finally {
    isUploading.value = false
  }
}
</script>

<style scoped>
.folder-view { padding: 30px; background-color: #f5f7fa; min-height: 100vh; }

.header { margin-bottom: 20px; border-bottom: 1px solid #e4e7ed; padding-bottom: 15px; display: flex; flex-direction: column; gap: 10px; }
.back-btn { align-self: flex-start; cursor: pointer; background: white; border: 1px solid #dcdfe6; padding: 6px 15px; border-radius: 4px; color: #606266; }
.back-btn:hover { color: #409eff; border-color: #c6e2ff; }
.header-info h2 { margin: 0; color: #303133; font-size: 22px; }
.path-info { color: #909399; font-size: 13px; margin-top: 5px; display: block; }

.upload-section { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); margin-bottom: 20px; display: flex; align-items: center; gap: 15px; }
.section-label { font-weight: bold; color: #606266; }
.upload-controls { display: flex; gap: 10px; align-items: center; }
.upload-btn { background-color: #409eff; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; transition: background 0.3s; }
.upload-btn:hover { background-color: #66b1ff; }
.upload-btn:disabled { background-color: #a0cfff; cursor: not-allowed; }

/* 批量操作栏样式 */
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
.select-all-label { cursor: pointer; font-weight: bold; color: #1890ff; display: flex; align-items: center; gap: 8px; }
.batch-btn { padding: 8px 16px; border-radius: 4px; border: none; cursor: pointer; font-size: 14px; color: white; transition: opacity 0.2s; }
.batch-btn:hover { opacity: 0.9; }
.batch-btn:disabled { background-color: #ccc; cursor: not-allowed; }
.batch-btn.download { background-color: #1890ff; }

.file-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.empty-state { grid-column: 1 / -1; text-align: center; color: #909399; padding: 40px; background: #fff; border-radius: 8px; }

/* 文件卡片样式调整 */
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
  position: relative; /* 为了定位 checkbox */
  cursor: pointer; /* 让整个卡片可点 */
}

/* 选中状态样式 */
.file-card.is-selected {
  border-color: #409eff;
  background-color: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.file-card:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

/* 复选框样式 */
.card-checkbox {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 5;
}
.card-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.file-icon { font-size: 40px; margin-bottom: 15px; color: #909399; }
.file-info { width: 100%; }
.file-name { font-weight: 500; color: #303133; margin-bottom: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }

.file-actions { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.action-btn { font-size: 13px; border: none; padding: 6px 10px; border-radius: 4px; cursor: pointer; width: 100%; transition: opacity 0.2s; }
.action-btn:hover { opacity: 0.9; }
.preview-btn { background-color: #e6a23c; color: white; }
.download-btn { background-color: #409eff; color: white; }
.delete-btn { background-color: #f56c6c; color: white; }

.loading { text-align: center; color: #909399; padding-top: 50px; }
</style>
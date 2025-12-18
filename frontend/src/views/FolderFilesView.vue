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

    <!-- 文件列表 -->
    <div v-if="loading" class="loading">正在加载文件列表...</div>

    <div v-else class="file-grid">
      <!-- 空状态 -->
      <div v-if="files.length === 0" class="empty-state">
        此文件夹下暂无文件 (仅显示文件，不显示子文件夹)
      </div>

      <!-- 文件卡片 -->
      <div v-for="file in files" :key="file.name" class="file-card">
        <div class="file-icon">📄</div>
        <div class="file-info">
          <div class="file-name" :title="file.name">{{ file.name }}</div>

          <!-- 按钮组 -->
          <div class="file-actions">
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
import { ref, onMounted } from 'vue'
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

onMounted(async () => {
  await loadFolderContent()
})

// --- 核心逻辑：加载并匹配节点 ---
async function loadFolderContent() {
  loading.value = true
  files.value = []

  try {
    // 重新获取最新的树结构
    const res = await axios.get(`/api/course/${courseNo}/file-tree`)
    const rootData = Array.isArray(res.data) ? res.data[0] : res.data

    if (!rootData) {
      console.warn("树数据为空")
      return
    }

    // 在树中找到当前 path 对应的节点
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

/**
 * 递归查找辅助函数
 */
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
  // 确保路径拼接正确，防止出现双斜杠或缺失斜杠
  const separator = currentPath.endsWith('/') ? '' : '/'
  return `${currentPath}${separator}${fileName}`
}

// --- 1. 预览逻辑 ---
function handlePreview(file) {
  const fullFilePath = getFullFilePath(file.name)
  // 使用 window.open 打开预览接口，通常后端会返回文件流或浏览器可识别的内容
  const previewUrl = `/api/files/preview?path=${encodeURIComponent(fullFilePath)}`
  window.open(previewUrl, '_blank')
}

// --- 2. 下载逻辑 ---
function handleDownload(file) {
  const fullFilePath = getFullFilePath(file.name)
  const downloadUrl = `/api/files/download?path=${encodeURIComponent(fullFilePath)}`

  // 方法 A: 如果后端配置正确 (Content-Disposition: attachment)，直接 open 即可
  // window.open(downloadUrl, '_blank')

  // 方法 B: 前端强制下载技巧 (创建隐藏的 a 标签)
  // 这种方式对于同源请求 (我们用了 /api 代理，属于同源) 通常能生效
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = downloadUrl
  // 设置 download 属性，告诉浏览器这是一个下载操作，并指定文件名
  link.setAttribute('download', file.name)

  document.body.appendChild(link)
  link.click()

  // 清理 DOM
  document.body.removeChild(link)
}

// --- 3. 删除逻辑 ---
async function handleDeleteFile(file) {
  const fullFilePath = getFullFilePath(file.name)

  if (!confirm(`确定要删除文件 "${file.name}" 吗？\n此操作不可恢复！`)) {
    return
  }

  try {
    const formData = new FormData()
    // 后端删除接口复用 /api/files/delete，参数为 dir (或根据后端实际参数名可能是 path)
    // 根据之前的逻辑，后端接受 "dir" 作为路径参数
    formData.append('dir', fullFilePath)

    const res = await axios.post('/api/files/delete', formData)

    if (res.data && res.data.success) {
      alert('删除成功')
      // 刷新列表
      await loadFolderContent()
    } else {
      alert('删除失败: ' + (res.data.message || '未知错误'))
    }
  } catch (err) {
    console.error(err)
    alert('请求失败: ' + err.message)
  }
}

// --- 4. 上传逻辑 ---
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

.upload-section { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); margin-bottom: 30px; display: flex; align-items: center; gap: 15px; }
.section-label { font-weight: bold; color: #606266; }
.upload-controls { display: flex; gap: 10px; align-items: center; }
.upload-btn { background-color: #409eff; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; transition: background 0.3s; }
.upload-btn:hover { background-color: #66b1ff; }
.upload-btn:disabled { background-color: #a0cfff; cursor: not-allowed; }

.file-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.empty-state { grid-column: 1 / -1; text-align: center; color: #909399; padding: 40px; background: #fff; border-radius: 8px; }

.file-card { background: white; padding: 20px; border-radius: 8px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: transform 0.2s; border: 1px solid #ebeef5; display: flex; flex-direction: column; align-items: center; }
.file-card:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.file-icon { font-size: 40px; margin-bottom: 15px; color: #909399; }
.file-info { width: 100%; }
.file-name { font-weight: 500; color: #303133; margin-bottom: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }

/* 按钮组样式 */
.file-actions { display: flex; flex-direction: column; gap: 8px; width: 100%; }

.action-btn { font-size: 13px; border: none; padding: 6px 10px; border-radius: 4px; cursor: pointer; width: 100%; transition: opacity 0.2s; }
.action-btn:hover { opacity: 0.9; }

.preview-btn { background-color: #e6a23c; color: white; } /* 黄色预览 */
.download-btn { background-color: #409eff; color: white; } /* 蓝色下载 */
.delete-btn { background-color: #f56c6c; color: white; }   /* 红色删除 */

.loading { text-align: center; color: #909399; padding-top: 50px; }
</style>
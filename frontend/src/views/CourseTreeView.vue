<template>
  <div class="tree-container">
    <div class="header">
      <button class="nav-btn" @click="$router.push('/')">返回首页</button>
      <h3>课程资源结构图 ({{ courseNo }})</h3>
      <span class="tip">提示：滚轮缩放，左键拖动，点击节点操作</span>
    </div>

    <div ref="chartRef" class="chart-box"></div>

    <div v-if="showModal" class="action-modal" :style="{ top: modalPos.y + 'px', left: modalPos.x + 'px' }">
      <div class="modal-title">{{ selectedNode.name }}</div>

      <div class="btn-group" v-if="!showCreateInput && !showUploadInput">
        <button class="btn-primary" @click="handleOpenFolder">打开文件夹</button>

        <!-- 使用 canEdit 状态控制显示 -->
        <template v-if="canEdit">
          <button class="btn-warning" @click="showUploadInput = true">上传文件</button>
          <button class="btn-success" @click="showCreateInput = true">新建子文件夹</button>
          <button class="btn-danger" @click="handleDeleteNode">删除此节点</button>
        </template>
      </div>

      <div v-if="showCreateInput" class="sub-action-box">
        <input v-model="newFolderName" placeholder="输入文件夹名" class="modal-input" />
        <div class="action-buttons">
          <button @click="handleCreateFolder" class="confirm-btn">确认</button>
          <button @click="showCreateInput = false" class="cancel-btn">取消</button>
        </div>
      </div>

      <div v-if="showUploadInput" class="sub-action-box">
        <input type="file" ref="treeFileInputRef" @change="handleTreeFileSelect" class="file-input" />
        <div class="action-buttons">
          <button @click="handleTreeUpload" class="confirm-btn" :disabled="isUploading">
            {{ isUploading ? '上传中...' : '开始上传' }}
          </button>
          <button @click="cancelUpload" class="cancel-btn">取消</button>
        </div>
      </div>

      <button class="close-btn" @click="closeModal">×</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const courseNo = route.params.courseNo

// 权限状态
const canEdit = ref(false)
const userRole = parseInt(localStorage.getItem('role') || '1')
const userId = localStorage.getItem('userId')

const chartRef = ref(null)
let myChart = null
const rawTreeData = ref([])
const showModal = ref(false)
const modalPos = ref({ x: 0, y: 0 })
const selectedNode = ref(null)
const showCreateInput = ref(false)
const newFolderName = ref('')
const showUploadInput = ref(false)
const treeSelectedFile = ref(null)
const isUploading = ref(false)

onMounted(async () => {
  initChart()
  // 并行加载：文件树 + 权限检查
  await Promise.all([fetchTreeData(), checkPermission()])
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (myChart) myChart.dispose()
})

// --- 核心修改：权限检查 ---
async function checkPermission() {
  // 1. 角色判断
  if (userRole >= 3) {
    canEdit.value = true // 通用/超级管理员直接有权限
    return
  }
  if (userRole === 1) {
    canEdit.value = false // 普通用户直接无权限
    return
  }

  // 2. 专业管理员(2) -> 调用接口询问后端
  try {
    const res = await axios.get('/api/permissions/check', {
      params: {
        userId: userId,
        courseNo: courseNo
      }
    })
    // 后端返回 { success: true, data: { hasPermission: true/false } }
    if (res.data.success && res.data.data) {
      canEdit.value = res.data.data.hasPermission
      console.log(`[Permission Check] 课程 ${courseNo} 权限: ${canEdit.value}`)
    } else {
      canEdit.value = false
    }
  } catch (e) {
    console.error('[Permission Check] 检查权限失败', e)
    canEdit.value = false
  }
}

const initChart = () => {
  myChart = echarts.init(chartRef.value)
  myChart.on('click', (params) => { if (params.data) openModal(params.data, params.event.event) })
  myChart.getZr().on('click', (params) => { if (!params.target) closeModal() })
}
const resizeChart = () => myChart && myChart.resize()

async function fetchTreeData() {
  try {
    const res = await axios.get(`/api/course/${courseNo}/file-tree`)
    const data = res.data
    rawTreeData.value = Array.isArray(data) ? data : [data]
    if (rawTreeData.value.length > 0) {
      const echartsData = transformToECharts(rawTreeData.value[0])
      renderChart(echartsData)
    }
  } catch (error) { console.error('获取文件树失败', error); alert('无法加载课程文件树') }
}

function transformToECharts(node, parentPath = '') {
  let currentFullPath = ''
  if (!parentPath && node.name === 'root') currentFullPath = ''
  else currentFullPath = parentPath ? `${parentPath}/${node.name}` : node.name

  let fileCount = 0
  if (node.children) fileCount = node.children.filter(child => child.type === 'file').length

  let nodeColor = '#67C23A', textColor = '#fff'
  if (fileCount >= 10) { nodeColor = '#F56C6C'; textColor = '#fff' }
  else if (fileCount > 0) { nodeColor = '#E6A23C'; textColor = '#333' }

  const formatted = {
    name: node.name,
    path: currentFullPath,
    type: node.type,
    children: [],
    symbol: 'roundRect',
    symbolSize: [180, 55],
    itemStyle: { color: nodeColor, borderColor: nodeColor, borderWidth: 1 },
    label: { color: textColor, fontSize: 16, fontWeight: 'bold', formatter: function(params) {
        let str = params.name
        if (str.length > 10) str = str.substring(0, 10) + '...'
        return `${str} (${fileCount})`
      }}
  }
  if (node.children) {
    const folderChildren = node.children.filter(child => child.type === 'directory')
    formatted.children = folderChildren.map(child => transformToECharts(child, currentFullPath))
  }
  return formatted
}

function renderChart(data) {
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}' },
    series: [{
      type: 'tree', data: [data], top: '5%', bottom: '5%', left: '5%', right: '20%',
      layout: 'orthogonal', orient: 'LR', expandAndCollapse: false, initialTreeDepth: -1, roam: true,
      label: { position: 'inside', verticalAlign: 'middle', align: 'center' },
      leaves: { label: { position: 'inside', verticalAlign: 'middle', align: 'center' } },
      lineStyle: { color: '#ccc', width: 2, curveness: 0.5 }
    }]
  }
  myChart.setOption(option)
}

function openModal(nodeData, event) {
  selectedNode.value = nodeData
  const x = Math.min(event.clientX + 10, window.innerWidth - 220)
  const y = Math.min(event.clientY + 10, window.innerHeight - 250)
  modalPos.value = { x, y }
  showModal.value = true; showCreateInput.value = false; newFolderName.value = ''; showUploadInput.value = false; treeSelectedFile.value = null
}
function closeModal() { showModal.value = false; selectedNode.value = null }
function handleOpenFolder() {
  if (!selectedNode.value) return
  router.push({
    name: 'FolderFiles',
    params: { courseNo: courseNo },
    query: {
      path: selectedNode.value.path,
      folderName: selectedNode.value.name
    }
  })
  closeModal()
}
function handleTreeFileSelect(event) { treeSelectedFile.value = event.target.files[0] }
function cancelUpload() { showUploadInput.value = false; treeSelectedFile.value = null }
async function handleTreeUpload() {
  if (!treeSelectedFile.value) return; isUploading.value = true
  const formData = new FormData(); formData.append('file', treeSelectedFile.value); formData.append('targetDir', selectedNode.value.path)
  try {
    const res = await axios.post('/api/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (res.data.success) { alert('上传成功'); closeModal(); await fetchTreeData() } else alert('上传失败: ' + res.data.message)
  } catch (err) { alert('请求失败: ' + err.message) } finally { isUploading.value = false }
}
async function handleCreateFolder() {
  if (!newFolderName.value) return
  const parentPath = selectedNode.value.path; const separator = parentPath.endsWith('/') ? '' : '/'; const targetPath = `${parentPath}${separator}${newFolderName.value}`
  try {
    const formData = new FormData(); formData.append('dir', targetPath)
    const res = await axios.post('/api/files/create_dir', formData)
    if (res.data.success) { alert('创建成功'); closeModal(); await fetchTreeData() } else alert('创建失败: ' + res.data.message)
  } catch (err) { alert('请求失败: ' + err.message) }
}
async function handleDeleteNode() {
  if (!confirm(`确定要删除文件夹 "${selectedNode.value.name}" 吗？`)) return
  try {
    const formData = new FormData(); formData.append('dir', selectedNode.value.path)
    const res = await axios.post('/api/files/delete', formData)
    if (res.data.success) { alert('删除成功'); closeModal(); await fetchTreeData() } else alert('删除失败: ' + res.data.message)
  } catch (err) { alert('请求失败: ' + err.message) }
}
</script>

<style scoped>
.tree-container { width: 100%; height: 100vh; position: relative; background: #fdfdfd; display: flex; flex-direction: column; }
.header { padding: 10px 20px; background: #fff; border-bottom: 1px solid #eee; display: flex; align-items: center; gap: 15px; z-index: 10; }
.chart-box { flex: 1; width: 100%; }
.nav-btn { cursor: pointer; padding: 6px 12px; background: white; border: 1px solid #dcdfe6; border-radius: 4px; }
.tip { color: #999; font-size: 12px; margin-left: auto; }
.action-modal { position: fixed; background: white; border: 1px solid #ebeef5; box-shadow: 0 4px 12px rgba(0,0,0,0.15); border-radius: 6px; padding: 15px; width: 220px; z-index: 1000; }
.modal-title { font-weight: bold; margin-bottom: 10px; color: #333; border-bottom: 1px solid #eee; padding-bottom: 5px; text-align: center; }
.btn-group { display: flex; flex-direction: column; gap: 8px; }
button { cursor: pointer; padding: 8px 10px; border-radius: 4px; font-size: 13px; transition: opacity 0.2s; }
button:hover { opacity: 0.9; }
.btn-primary { background: #409eff; color: white; border: none; }
.btn-success { background: #67c23a; color: white; border: none; }
.btn-warning { background: #E6A23C; color: white; border: none; }
.btn-danger { background: #f56c6c; color: white; border: none; }
.sub-action-box { margin-top: 5px; padding-top: 10px; border-top: 1px solid #eee; display: flex; flex-direction: column; gap: 8px; }
.modal-input { width: 100%; padding: 6px; box-sizing: border-box; border: 1px solid #dcdfe6; border-radius: 4px; }
.file-input { font-size: 12px; width: 100%; }
.action-buttons { display: flex; gap: 5px; }
.confirm-btn { background: #409eff; color: white; border: none; flex: 1; }
.cancel-btn { background: #909399; color: white; border: none; flex: 1; }
.close-btn { position: absolute; top: 5px; right: 8px; border: none; background: transparent; font-size: 18px; color: #999; }
</style>
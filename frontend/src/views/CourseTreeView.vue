<template>
  <div class="tree-container">
    <div class="header">
      <button class="nav-btn" @click="$router.push('/')">返回首页</button>
      <h3>课程资源结构图 ({{ courseNo }})</h3>
      <span class="tip">提示：滚轮缩放，鼠标左键拖动画布，点击节点进行操作</span>
    </div>

    <!-- ECharts 挂载点 -->
    <div ref="chartRef" class="chart-box"></div>

    <!-- 操作弹窗 (点击节点后显示) -->
    <div v-if="showModal" class="action-modal" :style="{ top: modalPos.y + 'px', left: modalPos.x + 'px' }">
      <div class="modal-title">{{ selectedNode.name }}</div>

      <!-- 初始菜单按钮组 -->
      <div class="btn-group" v-if="!showCreateInput && !showUploadInput">
        <button class="btn-primary" @click="handleOpenFolder">📂 打开文件夹</button>
        <button class="btn-warning" @click="showUploadInput = true">⬆️ 上传文件</button>
        <button class="btn-success" @click="showCreateInput = true">➕ 新建子文件夹</button>
        <button class="btn-danger" @click="handleDeleteNode">🗑️ 删除此节点</button>
      </div>

      <!-- 1. 新建文件夹输入框 -->
      <div v-if="showCreateInput" class="sub-action-box">
        <input v-model="newFolderName" placeholder="输入文件夹名" class="modal-input" />
        <div class="action-buttons">
          <button @click="handleCreateFolder" class="confirm-btn">确认</button>
          <button @click="showCreateInput = false" class="cancel-btn">取消</button>
        </div>
      </div>

      <!-- 2. 上传文件区域 -->
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

const chartRef = ref(null)
let myChart = null

// 数据与状态
const rawTreeData = ref([])
const showModal = ref(false)
const modalPos = ref({ x: 0, y: 0 })
const selectedNode = ref(null)

// 子操作状态
const showCreateInput = ref(false)
const newFolderName = ref('')
const showUploadInput = ref(false)
const treeSelectedFile = ref(null)
const isUploading = ref(false)

// --- 生命周期 ---
onMounted(async () => {
  initChart()
  await fetchTreeData()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (myChart) myChart.dispose()
})

// --- ECharts 初始化 ---
const initChart = () => {
  myChart = echarts.init(chartRef.value)

  // 节点点击事件
  myChart.on('click', (params) => {
    if (params.data) {
      openModal(params.data, params.event.event)
    }
  })

  // 空白处点击关闭弹窗
  myChart.getZr().on('click', (params) => {
    if (!params.target) {
      closeModal()
    }
  })
}

const resizeChart = () => myChart && myChart.resize()

// --- 数据获取与处理 ---
async function fetchTreeData() {
  try {
    const res = await axios.get(`/api/course/${courseNo}/file-tree`)
    const data = res.data
    rawTreeData.value = Array.isArray(data) ? data : [data]

    // 核心：转换数据 (传入 undefined 作为初始 parentPath)
    // 假设 rawTreeData[0] 是根节点 (课程文件夹)
    if (rawTreeData.value.length > 0) {
      const echartsData = transformToECharts(rawTreeData.value[0])
      renderChart(echartsData)
    }
  } catch (error) {
    console.error('获取文件树失败', error)
    alert('无法加载课程文件树')
  }
}

/**
 * 递归转换数据：
 * 1. 修复路径：强制根据父子层级拼接完整路径
 * 2. 计算文件数、设置颜色、设置矩形形状
 */
function transformToECharts(node, parentPath = '') {
  // --- 路径修复逻辑 ---
  // 如果 parentPath 为空，说明是根节点，直接用 node.name
  // 否则，拼接 parentPath + '/' + node.name
  let currentFullPath = node.name
  if (parentPath) {
    currentFullPath = `${parentPath}/${node.name}`
  }

  // 计算文件数
  let fileCount = 0
  if (node.children && node.children.length > 0) {
    fileCount = node.children.filter(child => child.type === 'file').length
  }

  // 决定颜色
  let nodeColor = '#67C23A' // 绿
  let textColor = '#fff'
  if (fileCount >= 10) {
    nodeColor = '#F56C6C'   // 红
    textColor = '#fff'
  } else if (fileCount > 0) {
    nodeColor = '#E6A23C'   // 黄
    textColor = '#333'
  }

  const formatted = {
    name: node.name,
    path: currentFullPath, // 使用拼接好的全路径
    type: node.type,
    children: [],

    // 样式
    symbol: 'roundRect',
    symbolSize: [140, 40],
    itemStyle: {
      color: nodeColor,
      borderColor: nodeColor,
      borderWidth: 1
    },
    label: {
      color: textColor,
      formatter: function(params) {
        let str = params.name
        if (str.length > 8) str = str.substring(0, 8) + '...'
        return `${str} (${fileCount})`
      }
    }
  }

  // 递归处理子文件夹
  if (node.children && node.children.length > 0) {
    const folderChildren = node.children.filter(child => child.type === 'directory')
    // 传递 currentFullPath 给子节点
    formatted.children = folderChildren.map(child => transformToECharts(child, currentFullPath))
  }

  return formatted
}

function renderChart(data) {
  const option = {
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      formatter: '{b}'
    },
    series: [
      {
        type: 'tree',
        data: [data],
        top: '1%', bottom: '1%', left: '5%', right: '20%',
        layout: 'orthogonal',
        orient: 'LR',         // 从左到右
        expandAndCollapse: false, // 禁止点击收缩
        initialTreeDepth: -1,     // 默认展开所有
        roam: true,
        label: {
          position: 'inside',
          verticalAlign: 'middle',
          align: 'center',
          fontSize: 14,
          fontWeight: 'bold'
        },
        leaves: {
          label: {
            position: 'inside',
            verticalAlign: 'middle',
            align: 'center'
          }
        },
        lineStyle: {
          color: '#ccc',
          width: 2,
          curveness: 0.5
        }
      }
    ]
  }
  myChart.setOption(option)
}

// --- 交互逻辑 ---

function openModal(nodeData, event) {
  selectedNode.value = nodeData
  // 防止弹窗溢出
  const x = Math.min(event.clientX + 10, window.innerWidth - 220)
  const y = Math.min(event.clientY + 10, window.innerHeight - 250)
  modalPos.value = { x, y }

  showModal.value = true
  // 重置子状态
  showCreateInput.value = false
  newFolderName.value = ''
  showUploadInput.value = false
  treeSelectedFile.value = null
}

function closeModal() {
  showModal.value = false
  selectedNode.value = null
}

// 1. 打开文件夹
function handleOpenFolder() {
  if (!selectedNode.value) return
  router.push({
    name: 'FolderFiles',
    params: { courseNo: courseNo },
    query: {
      path: selectedNode.value.path, // 这是完整路径
      folderName: selectedNode.value.name
    }
  })
  closeModal()
}

// 2. 上传文件 (Tree View)
function handleTreeFileSelect(event) {
  treeSelectedFile.value = event.target.files[0]
}
function cancelUpload() {
  showUploadInput.value = false
  treeSelectedFile.value = null
}
async function handleTreeUpload() {
  if (!treeSelectedFile.value) return
  isUploading.value = true

  const formData = new FormData()
  formData.append('file', treeSelectedFile.value)
  formData.append('targetDir', selectedNode.value.path) // 使用完整路径

  try {
    const res = await axios.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data && res.data.success) {
      alert('上传成功')
      closeModal()
      await fetchTreeData() // 刷新树状态（比如颜色变化）
    } else {
      alert('上传失败: ' + (res.data.message || '未知错误'))
    }
  } catch (err) {
    alert('请求失败: ' + err.message)
  } finally {
    isUploading.value = false
  }
}

// 3. 新建文件夹
async function handleCreateFolder() {
  if (!newFolderName.value) return

  const parentPath = selectedNode.value.path
  // 拼接路径
  const separator = parentPath.endsWith('/') ? '' : '/'
  const targetPath = `${parentPath}${separator}${newFolderName.value}`

  try {
    const formData = new FormData()
    formData.append('dir', targetPath)

    const res = await axios.post('/api/files/create_dir', formData)
    if (res.data && res.data.success) {
      alert('创建成功')
      closeModal()
      await fetchTreeData()
    } else {
      alert('创建失败: ' + res.data.message)
    }
  } catch (err) {
    alert('请求失败: ' + err.message)
  }
}

// 4. 删除节点
async function handleDeleteNode() {
  const nodeName = selectedNode.value.name
  if (!confirm(`确定要删除文件夹 "${nodeName}" 及其所有内容吗？\n此操作不可恢复！`)) return

  try {
    const formData = new FormData()
    formData.append('dir', selectedNode.value.path)

    const res = await axios.post('/api/files/delete', formData)
    if (res.data && res.data.success) {
      alert('删除成功')
      closeModal()
      await fetchTreeData()
    } else {
      alert('删除失败: ' + res.data.message)
    }
  } catch (err) {
    alert('请求失败: ' + err.message)
  }
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
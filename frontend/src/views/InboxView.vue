<template>
  <div class="home-container">
    <!-- 侧边栏：风格与 SettingsView 完全一致 -->
    <aside class="sidebar">
      <div class="logo">BITShared</div>
      <nav class="nav-menu">
        <button class="nav-item" @click="goToPage('/')">首页</button>
        <button class="nav-item" @click="goToPage('/courses')">课程</button>
        <button class="nav-item active" @click="goToPage('/inbox')">
          信箱 <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
        </button>
        <button class="nav-item" @click="goToPage('/settings')">设置</button>
      </nav>
    </aside>

    <div class="main-content">
      <div class="page-header">
        <div class="header-left">
          <h2>我的信箱</h2>
          <span class="sub-title">收纳您的申请记录与系统通知</span>
        </div>
        <div class="header-right">
          <!-- 修改点：仅当 role 为 1 (普通用户) 时显示申请按钮 -->
          <button v-if="userRole === 1" class="btn-apply-admin" @click="showApplyModal = true">
            申请成为管理员
          </button>
        </div>
      </div>

      <div class="mailbox-wrapper">
        <!-- 左侧：邮件列表 -->
        <div class="message-list-panel shadow-card">
          <div v-if="loadingList" class="loading-state">加载中...</div>
          <div v-else-if="messages.length === 0" class="empty-state">暂无邮件</div>
          <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['message-item', { 'is-unread': !msg.isRead, 'is-active': activeMsgId === msg.id }]"
              @click="fetchMessageDetail(msg.id)"
          >
            <div class="msg-main">
              <div class="msg-title">{{ msg.title }}</div>
              <div class="msg-info">
                <span class="msg-sender">来自: {{ msg.senderName }}</span>
                <span class="msg-date">{{ formatDate(msg.createdAt) }}</span>
              </div>
            </div>
            <div v-if="!msg.isRead" class="unread-dot"></div>
          </div>
        </div>

        <!-- 右侧：详情展示区 -->
        <div class="message-detail-panel shadow-card">
          <div v-if="!detail" class="detail-placeholder">
            <div class="icon">✉️</div>
            <p>请在左侧选择一封邮件查看详情</p>
          </div>

          <div v-else class="detail-content">
            <div class="detail-header-info">
              <h3>{{ detail.message.title }}</h3>
              <div class="meta">
                <span>发件人：{{ detail.message.senderName }} (ID: {{ detail.message.senderId }})</span>
                <span>时间：{{ formatDate(detail.message.createdAt) }}</span>
              </div>
            </div>

            <div class="detail-body">
              <p class="content-text">{{ detail.message.content }}</p>

              <!-- 类型 1：管理员申请请求 -->
              <div v-if="detail.message.type === 'ADMIN_APPLY_REQUEST' && detail.adminApply" class="action-box admin-apply">
                <h4>⚖️ 待处理申请详情</h4>
                <div class="info-grid">
                  <p><strong>申请人：</strong>{{ detail.adminApply.applicantName }}</p>
                  <p><strong>申请专业：</strong>{{ detail.adminApply.majorNo }}</p>
                  <p><strong>当前状态：</strong><span class="status-tag">{{ detail.adminApply.status }}</span></p>
                </div>
                <button class="btn-download-word" @click="downloadResume(detail.adminApply.id)">
                  📥 下载简历 (Word)
                </button>
              </div>

              <!-- 类型 2：文件上传请求 -->
              <div v-if="detail.message.type === 'FILE_UPLOAD_REQUEST' && detail.fileUploadRequest" class="action-box upload-request">
                <h4>📁 文件上传审批</h4>
                <div class="info-grid">
                  <p><strong>申请人：</strong>{{ detail.fileUploadRequest.requesterName }}</p>
                  <p><strong>文件名：</strong>{{ detail.fileUploadRequest.originalFilename }}</p>
                  <p><strong>课程号：</strong>{{ detail.fileUploadRequest.courseNo }}</p>
                </div>

                <!-- AI 核查结果区域 -->
                <div class="ai-audit-results">
                  <p class="ai-title">🤖 AI 智能核查结果：</p>
                  <div class="ai-content">
                    <template v-if="!detail.fileUploadRequest.aiSuggestAction">
                      <span class="ai-failed">AI调用失败</span>
                    </template>
                    <template v-else>
                      <span :class="['ai-badge', detail.fileUploadRequest.aiSuggestAction]">
                        {{ detail.fileUploadRequest.aiSuggestAction === 'manual_review' ? 'AI只读取了文件目录并未读取文件内容' : 'AI已阅读文件内容并做出判断' }}
                      </span>
                      <p v-if="detail.fileUploadRequest.aiSuggestReason" class="ai-reason">
                        <strong>理由：</strong>{{ detail.fileUploadRequest.aiSuggestReason }}
                      </p>
                    </template>
                  </div>
                </div>

                <div class="button-group">
                  <button class="btn-action preview" @click="previewFile(detail.fileUploadRequest.id)">👁️ 预览文件</button>
                  <button class="btn-action download" @click="downloadFile(detail.fileUploadRequest.id)">⬇️ 下载</button>

                  <!-- 管理员审批按钮 -->
                  <template v-if="detail.fileUploadRequest.status === 'PENDING'">
                    <button class="btn-action approve" @click="handleApprove(detail.message.id)">✅ 通过</button>
                    <button class="btn-action reject" @click="handleReject(detail.message.id)">❌ 拒绝</button>
                  </template>
                </div>
              </div>

              <!-- 类型 3：结果通知 -->
              <div v-if="detail.message.type === 'FILE_UPLOAD_RESULT'" class="action-box result-info">
                <p v-if="detail.fileUploadRequest && detail.fileUploadRequest.status === 'REJECTED'" class="reject-reason">
                  <strong>拒绝原因：</strong>{{ detail.fileUploadRequest.rejectReason }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 申请成为管理员弹窗 -->
    <div v-if="showApplyModal" class="modal-overlay">
      <div class="modal-box">
        <h3>申请管理员权限</h3>
        <div class="form-body">
          <div class="form-group">
            <label>目标专业</label>
            <select v-model="applyForm.majorNo">
              <option value="" disabled>请选择专业</option>
              <option v-for="m in majorList" :key="m.majorNo" :value="m.majorNo">
                {{ m.majorName }} ({{ m.majorNo }})
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>简历文件 (.docx)</label>
            <input type="file" @change="handleFileSelect" accept=".doc,.docx" />
          </div>
          <div class="form-group">
            <label>备注说明</label>
            <textarea v-model="applyForm.remark" rows="3" placeholder="简要说明您的申请理由"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showApplyModal = false">取消</button>
          <button class="btn-submit" @click="submitAdminApply">提交申请</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const userId = sessionStorage.getItem('userId')
const userRole = parseInt(sessionStorage.getItem('role') || '1')

// 状态管理
const messages = ref([])
const activeMsgId = ref(null)
const detail = ref(null)
const loadingList = ref(false)
const unreadCount = computed(() => messages.value.filter(m => !m.isRead).length)

// 申请弹窗相关
const showApplyModal = ref(false)
const majorList = ref([])
const applyForm = ref({ majorNo: '', remark: '', wordFile: null })

onMounted(() => {
  if (!userId) {
    alert('请先登录')
    router.push('/login')
    return
  }
  fetchInbox()
  loadMajors()
})

// 修改 InboxView.vue 中的 fetchInbox 函数
async function fetchInbox() {
  loadingList.value = true
  try {
    const res = await axios.get('/api/messages/inbox', {
      params: {
        userId: userId,
        // 添加下面这两个参数
        page: 0,     // 获取第 1 页
        size: 100    // 每页获取 100 条（根据需要调整，比如 1000）
      }
    })
    if (res.data.success) {
      // 后端返回的是 Page 对象，数据在 data.content
      const rawContent = res.data.data.content || []

      // 排序：最新在前
      messages.value = rawContent.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

      // 如果你发现发了新邮件还是不显示，请检查后端 SQL 是否真的查到了最新数据
      console.log("当前获取到的邮件总数:", messages.value.length)
    }
  } catch (e) {
    console.error('获取信箱失败', e)
  } finally {
    loadingList.value = false
  }
}

async function fetchMessageDetail(id) {
  activeMsgId.value = id
  try {
    const res = await axios.get(`/api/messages/${id}`, { params: { userId } })
    if (res.data.success) {
      detail.value = res.data.data
      const msg = messages.value.find(m => m.id === id)
      if (msg) msg.isRead = true
    }
  } catch (e) {
    alert('获取详情失败')
  }
}

async function loadMajors() {
  const res = await axios.get('/api/majors')
  if (res.data.success) majorList.value = res.data.data
}

function handleFileSelect(e) {
  applyForm.value.wordFile = e.target.files[0]
}

function downloadResume(id) {
  window.open(`/api/admin-apply/${id}/word/download`)
}

function previewFile(requestId) {
  window.open(`/api/file-upload-requests/${requestId}/preview`)
}

function downloadFile(requestId) {
  window.open(`/api/file-upload-requests/${requestId}/download`)
}

async function handleApprove(msgId) {
  if (!confirm('确定通过该申请吗？')) return
  try {
    const res = await axios.post('/api/file-upload-requests/approve', null, {
      params: { messageId: msgId, reviewerId: userId }
    })
    if (res.data.success) {
      alert('已通过申请')
      fetchMessageDetail(msgId)
    }
  } catch (e) { alert('审批失败') }
}

async function handleReject(msgId) {
  const reason = prompt('请输入拒绝理由：')
  if (reason === null) return
  try {
    const res = await axios.post('/api/file-upload-requests/reject', null, {
      params: { messageId: msgId, reviewerId: userId, reason: reason }
    })
    if (res.data.success) {
      alert('已拒绝该申请')
      fetchMessageDetail(msgId)
    }
  } catch (e) { alert('操作失败') }
}

async function submitAdminApply() {
  if (!applyForm.value.majorNo || !applyForm.value.wordFile) return alert('请填全信息')

  const formData = new FormData()
  formData.append('applicantId', userId)
  formData.append('majorNo', applyForm.value.majorNo)
  formData.append('wordFile', applyForm.value.wordFile)
  formData.append('remark', applyForm.value.remark)

  try {
    const res = await axios.post('/api/admin-apply/submit', formData)
    if (res.data.success) {
      alert('申请成功，正在审核中')
      showApplyModal.value = false
      fetchInbox()
    }
  } catch (e) { alert('申请提交失败') }
}

const goToPage = (path) => router.push(path)
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}-${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
/* 首页整体容器 */
.home-container {
  height: 100vh;
  width: 100%;
  display: flex;
  background-color: #f5f7fa;
}

/* 侧边栏样式 */
.sidebar {
  width: 220px;
  background-color: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  flex-shrink: 0;
}

/* 侧边栏 Logo 样式 */
.logo {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 30px;
  text-align: center;
  color: #fff;
}

/* 侧边栏导航菜单容器 */
.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 侧边栏导航项基础样式 */
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
  position: relative;
}

/* 侧边栏导航项 hover 状态 */
.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

/* 侧边栏激活态导航项 */
.nav-item.active {
  background-color: #409eff;
  color: #fff;
  font-weight: bold;
}

/* 未读消息徽章 */
.unread-badge {
  background: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  margin-left: 5px;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  padding: 30px 50px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 页面头部区域 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  border-bottom: 1px solid #e8e8e8;
  padding-bottom: 15px;
}

/* 页面头部标题 */
.page-header h2 {
  font-size: 24px;
  color: #303133;
  margin: 0;
}

/* 页面副标题 */
.sub-title {
  color: #909399;
  font-size: 14px;
}

/* 申请管理员按钮 */
.btn-apply-admin {
  background: #67c23a;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

/* 邮箱/消息容器包装器 */
.mailbox-wrapper {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 0;
}

/* 通用阴影卡片样式 */
.shadow-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 消息列表面板 */
.message-list-panel {
  width: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 消息项基础样式 */
.message-item {
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

/* 消息项 hover 状态 */
.message-item:hover {
  background: #f9f9f9;
}

/* 消息项激活态（选中） */
.message-item.is-active {
  background: #ecf5ff;
  border-left: 4px solid #409eff;
}

/* 未读消息项基础样式 */
.message-item.is-unread {
  background: #fff;
}

/* 未读消息项标题样式 */
.message-item.is-unread .msg-title {
  font-weight: bold;
  color: #303133;
}

/* 未读消息红点 */
.unread-dot {
  position: absolute;
  top: 18px;
  right: 15px;
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
}

/* 消息标题 */
.msg-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.4;
}

/* 消息元信息（时间/发送方等） */
.msg-info {
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: space-between;
}

/* 消息详情面板 */
.message-detail-panel {
  flex: 1;
  overflow-y: auto;
  padding: 30px;
}

/* 详情面板占位符（未选中消息时） */
.detail-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

/* 占位符图标 */
.detail-placeholder .icon {
  font-size: 60px;
  margin-bottom: 10px;
}

/* 详情头部信息区域 */
.detail-header-info {
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

/* 详情头部标题 */
.detail-header-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

/* 详情元信息（时间/来源等） */
.meta {
  font-size: 13px;
  color: #999;
  display: flex;
  gap: 20px;
}

/* 消息内容文本 */
.content-text {
  font-size: 15px;
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
  margin-bottom: 30px;
}

/* 操作框容器 */
.action-box {
  background: #f8f9fa;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
}

/* 操作框标题 */
.action-box h4 {
  margin-top: 0;
  color: #303133;
  border-bottom: 1px solid #ddd;
  padding-bottom: 10px;
}

/* 信息网格文本 */
.info-grid p {
  margin: 8px 0;
  font-size: 14px;
}

/* 状态标签 */
.status-tag {
  background: #e6a23c;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/* AI 结果样式 */
.ai-audit-results {
  margin-top: 15px;
  padding: 12px;
  background-color: #f0f9ff;
  border: 1px solid #bae7ff;
  border-radius: 6px;
}

/* AI 结果标题 */
.ai-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #0050b3;
  font-size: 14px;
}

/* AI 结果失败提示 */
.ai-failed {
  color: #8c8c8c;
  font-style: italic;
}

/* AI 结果徽章基础样式 */
.ai-badge {
  padding: 3px 10px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 13px;
}

/* AI 徽章 - 人工审核状态 */
.ai-badge.manual_review {
  background-color: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}

/* AI 徽章 - 驳回状态 */
.ai-badge.reject {
  background-color: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}

/* AI 驳回原因 */
.ai-reason {
  margin-top: 8px;
  font-size: 13px;
  color: #595959;
  background: white;
  padding: 5px;
  border-radius: 4px;
}

/* 按钮组容器 */
.button-group {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* 操作按钮基础样式 */
.btn-action {
  border: none;
  padding: 8px 15px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: white;
}

/* 预览按钮 */
.btn-action.preview {
  background: #409eff;
}

/* 下载按钮 */
.btn-action.download {
  background: #909399;
}

/* 批准按钮 */
.btn-action.approve {
  background: #67c23a;
}

/* 驳回按钮 */
.btn-action.reject {
  background: #f56c6c;
}

/* 下载Word按钮 */
.btn-download-word {
  background: #e6a23c;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
}

/* 模态框遮罩层 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 模态框容器 */
.modal-box {
  background: white;
  border-radius: 8px;
  width: 450px;
  padding: 30px;
}

/* 模态框底部按钮区 */
.modal-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 取消按钮 */
.btn-cancel {
  background: #eee;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  cursor: pointer;
}

/* 提交按钮 */
.btn-submit {
  background: #409eff;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  cursor: pointer;
}

/* 表单组容器 */
.form-group {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 表单标签 */
.form-group label {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
}

/* 表单输入控件（下拉/文本域/输入框） */
.form-group select,
.form-group textarea,
.form-group input {
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
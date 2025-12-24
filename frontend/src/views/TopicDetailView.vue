<template>
  <div class="detail-page" v-if="topic">
    <div class="content-card">
      <!-- 话题主体部分 (保持原有) -->
      <div class="detail-header">
        <button @click="$router.back()" class="back-btn">← 返回列表</button>
        <h1>{{ topic.title }}</h1>
        <div class="meta-row">
          <span class="user-badge">{{ topic.author.nickname || topic.author.username }}</span>
          <span class="time-text">发布于 {{ topic.createdAt }}</span>

          <div class="stats-group">
            <span class="stat-item">👁️ {{ topic.viewCount || 0 }}</span>
            <span class="stat-item">💬 {{ topic.replyCount || 0 }}</span>
            <span class="stat-item like-count" :class="{ 'liked': hasLiked }">
              ❤️ {{ topic.likeCount || 0 }}
            </span>
          </div>

          <div v-if="isOwner" class="owner-ops">
            <button class="delete-link" @click="handleDelete">删除话题</button>
          </div>
        </div>

        <div v-if="topic.referencePath" class="path-jump-box">
          <span class="path-label">🔗 关联资源：</span>
          <a href="javascript:void(0)" @click="jumpToResource(topic.referencePath)" class="path-link">
            {{ topic.referencePath }}
          </a>
        </div>
      </div>

      <div class="main-body">
        {{ topic.content }}
      </div>

      <!-- 话题点赞交互 -->
      <div class="interaction-bar">
        <button class="like-button" :class="{ 'active': hasLiked }" @click="handleLike">
          <span class="heart-icon">{{ hasLiked ? '❤️' : '🤍' }}</span>
          <span class="like-text">{{ hasLiked ? '已点赞' : '点个赞吧' }}</span>
          <span class="count-bubble" v-if="topic.likeCount > 0">{{ topic.likeCount }}</span>
        </button>
      </div>

      <!-- 话题附件 (保持原有) -->
      <div class="attachments-area" v-if="topic.attachments && topic.attachments.length > 0">
        <h3 class="section-title">附件资料 ({{ topic.attachments.length }})</h3>
        <div class="attachment-list">
          <div v-for="file in topic.attachments" :key="file.id" class="attachment-item">
            <div class="file-icon">{{ getIcon(file.originalName) }}</div>
            <div class="file-info-text">
              <div class="file-name" :title="file.originalName">{{ file.originalName }}</div>
              <div class="file-size">{{ (file.fileSize / 1024).toFixed(1) }} KB</div>
            </div>
            <div class="file-btns">
              <button class="btn-att download" @click="downloadFile(file)">下载</button>
              <button class="btn-att preview" @click="previewFile(file)" v-if="canPreview(file.fileType)">预览</button>
            </div>
          </div>
        </div>
      </div>


      <!-- ================= 评论区开始 ================= -->
      <div class="comments-section">
        <h3 class="section-title">全部评论 ({{ topic.replyCount || 0 }})</h3>

        <!-- 发表一级评论 -->
        <div class="comment-editor">
          <textarea v-model="newComment.content" placeholder="发表你的友善评论..." rows="3"></textarea>
          <div class="editor-footer">
            <div class="upload-btn-wrapper">
              <button class="btn-upload">📎 添加附件</button>
              <input type="file" multiple @change="handleCommentFileChange" />
              <span class="file-count" v-if="newComment.files.length">{{ newComment.files.length }}个文件</span>
            </div>
            <button class="btn-submit-comment" @click="submitComment(null)">发布评论</button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list" v-if="comments.length > 0">
          <div v-for="comment in comments" :key="comment.id" class="comment-item-container">
            <!-- 一级评论主体 -->
            <div class="comment-main">
              <div class="comment-author-info">
                <span class="c-user">{{ comment.author.nickname || comment.author.username }}</span>
                <span class="c-date">{{ comment.createdAt }}</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>

              <!-- 评论附件 -->
              <div class="comment-attachments" v-if="comment.attachments?.length">
                <div v-for="att in comment.attachments" :key="att.id" class="c-att-tag" @click="previewFile(att)">
                  📎 {{ att.originalName }}
                </div>
              </div>

              <div class="comment-footer">
                <span class="c-action" @click="handleCommentLike(comment)">
                  👍 {{ comment.likeCount }}
                </span>
                <span class="c-action" @click="showReplyInput(comment.id)">回复</span>
                <span class="c-action delete" v-if="comment.author.userId == currentUserId" @click="deleteComment(comment.id)">删除</span>
              </div>
            </div>

            <!-- 二级评论展示 (回复) -->
            <div class="replies-container" v-if="comment.replyCount > 0 || comment.replies?.length">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="comment-author-info">
                  <span class="c-user">{{ reply.author.nickname || reply.author.username }}</span>
                  <span class="c-date">{{ reply.createdAt }}</span>
                </div>
                <div class="comment-text">{{ reply.content }}</div>
                <div class="comment-footer">
                  <span class="c-action" @click="handleCommentLike(reply)">👍 {{ reply.likeCount }}</span>
                  <span class="c-action delete" v-if="reply.author.userId == currentUserId" @click="deleteComment(reply.id)">删除</span>
                </div>
              </div>
              <button class="btn-load-more" v-if="comment.replyCount > (comment.replies?.length || 0)" @click="loadReplies(comment)">
                查看全部 {{ comment.replyCount }} 条回复
              </button>
            </div>

            <!-- 二级回复输入框 -->
            <div class="reply-editor" v-if="activeReplyId === comment.id">
              <input v-model="newReply.content" :placeholder="'回复 @' + comment.author.username + '...'" />
              <button @click="submitComment(comment.id)">发送</button>
            </div>
          </div>
        </div>

        <!-- 加载更多一级评论 -->
        <div class="pagination-area" v-if="!commentLast">
          <button class="btn-load-main" @click="fetchRootComments(commentPage + 1)">加载更多评论</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const topicId = route.params.topicId
const currentUserId = sessionStorage.getItem('userId')

const topic = ref(null)
const hasLiked = ref(false)

// 评论相关状态
const comments = ref([])
const commentPage = ref(0)
const commentLast = ref(true)
const activeReplyId = ref(null)
const newComment = reactive({ content: '', files: [] })
const newReply = reactive({ content: '' })

onMounted(() => {
  fetchDetail()
  fetchRootComments(0)
})

const isOwner = computed(() => {
  return topic.value && String(topic.value.author.userId) === String(currentUserId)
})

async function fetchDetail() {
  try {
    const res = await axios.get(`/api/topics/by-topic/${topicId}`)
    if (res.data.success) topic.value = res.data.data
  } catch (err) { console.error(err) }
}

// ================= 评论逻辑 =================

// 获取一级评论
async function fetchRootComments(page) {
  try {
    const res = await axios.get(`/api/comments/root/${topicId}`, {
      params: { page, size: 10, sortBy: 'createdAt', direction: 'desc' }
    })
    if (res.data.success) {
      if (page === 0) comments.value = res.data.data.content
      else comments.value.push(...res.data.data.content)
      commentPage.value = res.data.data.pageNumber
      commentLast.value = res.data.data.last
    }
  } catch (err) { console.error(err) }
}

// 加载二级回复
async function loadReplies(rootComment) {
  try {
    const res = await axios.get(`/api/comments/${rootComment.id}/replies`, { params: { size: 50 } })
    if (res.data.success) {
      rootComment.replies = res.data.data.content
    }
  } catch (err) { console.error(err) }
}

// 处理评论文件选择
function handleCommentFileChange(e) {
  newComment.files = Array.from(e.target.files)
}

// 显示回复框
function showReplyInput(id) {
  activeReplyId.value = activeReplyId.value === id ? null : id
  newReply.content = ''
}

// 提交评论 (一级或二级)
async function submitComment(parentId) {
  const content = parentId ? newReply.content : newComment.content
  if (!content.trim()) return alert('请输入内容')

  const formData = new FormData()
  formData.append('topicId', topicId)
  formData.append('content', content)
  if (parentId) formData.append('parentId', parentId)

  if (!parentId && newComment.files.length > 0) {
    newComment.files.forEach(file => formData.append('attachments', file))
  }

  try {
    const res = await axios.post(`/api/comments/create/${currentUserId}`, formData)
    if (res.data.success) {
      if (!parentId) {
        comments.value.unshift(res.data.data)
        newComment.content = ''
        newComment.files = []
      } else {
        const parent = comments.value.find(c => c.id === parentId)
        if (!parent.replies) parent.replies = []
        parent.replies.push(res.data.data)
        parent.replyCount++
        activeReplyId.value = null
      }
    }
  } catch (err) { alert('发布失败') }
}

// 点赞评论
async function handleCommentLike(comment) {
  try {
    const url = `/api/comments/like/${comment.id}`
    await axios.post(url)
    comment.likeCount++
  } catch (err) { console.error(err) }
}

// 删除评论
async function deleteComment(commentId) {
  if (!confirm('确定删除评论吗？')) return
  try {
    await axios.delete(`/api/comments/delete/${commentId}`, { params: { userId: currentUserId } })
    // 前端移除
    comments.value = comments.value.filter(c => {
      if (c.id === commentId) return false
      if (c.replies) c.replies = c.replies.filter(r => r.id !== commentId)
      return true
    })
  } catch (err) { alert('删除失败') }
}

// ================= 原有工具逻辑 =================

async function handleLike() {
  if (hasLiked.value) {
    topic.value.likeCount--; hasLiked.value = false
  } else {
    topic.value.likeCount++; hasLiked.value = true
  }
}

async function handleDelete() {
  if (!confirm('确定要删除这个话题吗？')) return
  try {
    const res = await axios.delete(`/api/topics/delete/${topicId}`, { params: { userId: currentUserId } })
    if (res.data.success) { alert('话题已删除'); router.back() }
  } catch (err) { alert('删除失败') }
}

function jumpToResource(path) {
  const pathParts = path.split('/'); pathParts.pop()
  const parentPath = pathParts.join('/')
  router.push({
    name: 'FolderFiles',
    params: { courseNo: topic.value.course.courseNo },
    query: { path: parentPath, folderName: '关联目录' }
  })
}

function getIcon(name) {
  const ext = name.split('.').pop().toLowerCase()
  if (['jpg','png','jpeg'].includes(ext)) return '🖼️'
  if (ext === 'pdf') return '📕'
  return '📄'
}

function canPreview(type) {
  return type?.includes('image') || type?.includes('pdf') || type?.includes('text')
}

function downloadFile(file) {
  const filename = file.accessUrl.split('/').pop()
  window.open(`/api/attachments/download/${topic.value.forumNo}/${filename}?download=true`, '_blank')
}

function previewFile(file) {
  // 如果附件对象中有直接的 previewUrl 则使用，否则拼接
  const url = file.previewUrl || `/api/attachments/preview/${topic.value.forumNo}/${file.accessUrl.split('/').pop()}`
  window.open(url, '_blank')
}
</script>

<style scoped>
/* 继承原有样式 */
.detail-page { background: #f0f2f5; min-height: 100vh; padding: 40px 20px; }
.content-card { max-width: 850px; margin: 0 auto; background: white; padding: 40px; border-radius: 16px; box-shadow: 0 4px 25px rgba(0,0,0,0.06); }
.detail-header { border-bottom: 1px solid #f0f0f0; padding-bottom: 25px; margin-bottom: 30px; }
.back-btn { background: #fff; border: 1px solid #d9d9d9; padding: 6px 14px; border-radius: 6px; cursor: pointer; color: #666; transition: all 0.3s; margin-bottom: 15px; }
.back-btn:hover { color: #409eff; border-color: #409eff; }
.detail-header h1 { margin: 10px 0; color: #1f1f1f; font-size: 26px; font-weight: 700; line-height: 1.4; }

.meta-row { display: flex; align-items: center; gap: 12px; color: #8c8c8c; font-size: 14px; flex-wrap: wrap; }
.user-badge { background: #e6f7ff; padding: 4px 12px; border-radius: 20px; color: #1890ff; font-weight: 600; }
.stats-group { display: flex; gap: 15px; margin-left: auto; }
.stat-item { display: flex; align-items: center; gap: 4px; }
.like-count.liked { color: #f5222d; font-weight: bold; }

.path-jump-box { margin-top: 18px; background: #f6ffed; padding: 10px 16px; border-radius: 8px; border: 1px solid #b7eb8f; display: flex; align-items: center; }
.path-label { font-size: 13px; color: #52c41a; font-weight: bold; }
.path-link { color: #1890ff; text-decoration: none; font-family: monospace; font-size: 13px; margin-left: 8px; }

.main-body { line-height: 1.8; font-size: 16px; color: #434343; white-space: pre-wrap; min-height: 120px; margin-bottom: 40px; }

.interaction-bar { display: flex; justify-content: center; margin: 40px 0; padding: 20px 0; border-top: 1px solid #f5f5f5; }
.like-button {
  display: flex; align-items: center; gap: 10px; padding: 10px 24px;
  border-radius: 30px; border: 1px solid #d9d9d9; background: #fff;
  cursor: pointer; transition: all 0.3s;
}
.like-button.active { background: #fff1f0; border-color: #ffccc7; color: #ff4d4f; }

/* 附件区域 */
.attachments-area { border-top: 2px solid #f0f0f0; padding-top: 30px; margin-top: 40px; }
.section-title { font-size: 18px; color: #262626; margin-bottom: 20px; font-weight: 600; border-left: 4px solid #1890ff; padding-left: 12px; }

/* 附件资料框布局优化：拉长资料框 */
.attachment-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); /* 将最小宽度从 260px 增加到 380px */
  gap: 16px;
}

.attachment-item { display: flex; align-items: center; padding: 12px; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 10px; }
.file-info-text { flex: 1; overflow: hidden; }
.file-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.btn-att { border: none; padding: 3px 8px; border-radius: 4px; font-size: 11px; cursor: pointer; margin-left: 5px; color: #fff; }
.btn-att.download { background: #1890ff; }
.btn-att.preview { background: #faad14; }

/* ================= 评论区样式 ================= */
.comments-section { margin-top: 50px; border-top: 2px solid #f0f0f0; padding-top: 30px; }

/* 编辑器 */
.comment-editor { background: #fafafa; padding: 20px; border-radius: 12px; margin-bottom: 30px; }
.comment-editor textarea {
  width: 100%; border: 1px solid #d9d9d9; border-radius: 8px; padding: 12px;
  resize: none; font-family: inherit; margin-bottom: 12px; box-sizing: border-box;
}
.editor-footer { display: flex; justify-content: space-between; align-items: center; }
.upload-btn-wrapper { position: relative; overflow: hidden; display: inline-block; }
.btn-upload { border: 1px solid #d9d9d9; background: #fff; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 13px; }
.upload-btn-wrapper input[type=file] { position: absolute; left: 0; top: 0; opacity: 0; cursor: pointer; }
.file-count { font-size: 12px; color: #52c41a; margin-left: 8px; }
.btn-submit-comment { background: #1890ff; color: #fff; border: none; padding: 8px 20px; border-radius: 6px; cursor: pointer; font-weight: 600; }

/* 列表项 */
.comment-item-container { border-bottom: 1px solid #f0f0f0; padding: 20px 0; }
.comment-author-info { display: flex; gap: 10px; align-items: center; margin-bottom: 8px; }
.c-user { font-weight: 600; color: #262626; font-size: 14px; }
.c-date { color: #bfbfbf; font-size: 12px; }
.comment-text { font-size: 14px; color: #434343; line-height: 1.6; margin-bottom: 10px; }

.comment-footer { display: flex; gap: 20px; }
.c-action { font-size: 13px; color: #8c8c8c; cursor: pointer; transition: color 0.2s; }
.c-action:hover { color: #1890ff; }
.c-action.delete { color: #ff7875; }

/* 附件标签 */
.comment-attachments { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.c-att-tag { background: #f5f5f5; padding: 4px 10px; border-radius: 4px; font-size: 12px; color: #595959; cursor: pointer; border: 1px solid #d9d9d9; }
.c-att-tag:hover { border-color: #1890ff; color: #1890ff; }

/* 二级回复样式 */
.replies-container { background: #f9f9f9; border-radius: 8px; padding: 15px; margin-top: 15px; margin-left: 20px; }
.reply-item { padding: 10px 0; border-bottom: 1px solid #eee; }
.reply-item:last-child { border-bottom: none; }
.btn-load-more { background: none; border: none; color: #1890ff; font-size: 13px; cursor: pointer; padding: 10px 0; }

.reply-editor { margin-top: 15px; margin-left: 20px; display: flex; gap: 10px; }
.reply-editor input { flex: 1; border: 1px solid #d9d9d9; border-radius: 4px; padding: 8px 12px; }
.reply-editor button { background: #1890ff; color: #fff; border: none; padding: 0 15px; border-radius: 4px; cursor: pointer; }

.btn-load-main {
  width: 100%; padding: 12px; background: #fff; border: 1px solid #d9d9d9;
  border-radius: 8px; color: #8c8c8c; cursor: pointer; margin-top: 20px;
}
.btn-load-main:hover { color: #1890ff; border-color: #1890ff; }

.delete-link { color: #ff4d4f; background: none; border: none; cursor: pointer; font-size: 13px; margin-left: 20px; }
</style>
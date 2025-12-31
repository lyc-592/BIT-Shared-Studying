<template>
  <div class="detail-page" v-if="topic">
    <div class="content-card">
      <div class="detail-header">
        <button @click="$router.back()" class="back-btn">← 返回列表</button>
        <h1>{{ topic.title }}</h1>
        <div class="meta-row">
          <span class="user-badge">{{ topic.author.nickname || topic.author.username }}</span>

          <!-- 添加：关注按钮 -->
          <button
              v-if="topic.author.userId != currentUserId"
              class="follow-btn"
              :class="{ 'is-following': isFollowing }"
              @click="handleFollow"
          >
            {{ isFollowing ? '已关注' : '+ 关注' }}
          </button>

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

      <!-- 交互区：点赞 + 收藏 -->
      <div class="interaction-bar">
        <!-- 点赞按钮 -->
        <button class="like-button" :class="{ 'active': hasLiked }" @click="handleLike">
          <span class="heart-icon">{{ hasLiked ? '❤️' : '🤍' }}</span>
          <span class="like-text">{{ hasLiked ? '已点赞' : '点个赞吧' }}</span>
          <span class="count-bubble" v-if="topic.likeCount > 0">{{ topic.likeCount }}</span>
        </button>

        <!-- 添加：收藏按钮 -->
        <button class="collect-button" :class="{ 'active': isCollected }" @click="handleCollect">
          <span class="star-icon">{{ isCollected ? '⭐' : '☆' }}</span>
          <span class="collect-text">{{ isCollected ? '已收藏' : '收藏' }}</span>
        </button>
      </div>

      <!-- 话题附件 -->
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

      <!-- 评论区部分 -->
      <div class="comments-section">
        <h3 class="section-title">全部评论 ({{ topic.replyCount || 0 }})</h3>
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

        <div class="comment-list" v-if="comments.length > 0">
          <div v-for="comment in comments" :key="comment.id" class="comment-item-container">
            <div class="comment-main">
              <div class="comment-author-info">
                <span class="c-user">{{ comment.author.nickname || comment.author.username }}</span>
                <span class="c-date">{{ comment.createdAt }}</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
              <div class="comment-attachments" v-if="comment.attachments?.length">
                <div v-for="att in comment.attachments" :key="att.id" class="c-att-tag" @click="previewFile(att)">
                  📎 {{ att.originalName }}
                </div>
              </div>
              <div class="comment-footer">
                <!-- 评论点赞交互 -->
                <span class="c-action" :class="{ 'is-liked': comment.isLiked }" @click="handleCommentLike(comment)">
                  {{ comment.isLiked ? '❤️' : '👍' }} {{ comment.likeCount }}
                </span>
                <span class="c-action" @click="showReplyInput(comment.id)">回复</span>
                <span class="c-action delete" v-if="comment.author.userId == currentUserId" @click="deleteComment(comment.id)">删除</span>
              </div>
            </div>

            <!-- 二级回复 -->
            <div class="replies-container" v-if="comment.replyCount > 0 || comment.replies?.length">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="comment-author-info">
                  <span class="c-user">{{ reply.author.nickname || reply.author.username }}</span>
                  <span class="c-date">{{ reply.createdAt }}</span>
                </div>
                <div class="comment-text">{{ reply.content }}</div>
                <div class="comment-footer">
                  <span class="c-action" :class="{ 'is-liked': reply.isLiked }" @click="handleCommentLike(reply)">
                    {{ reply.isLiked ? '❤️' : '👍' }} {{ reply.likeCount }}
                  </span>
                  <span class="c-action delete" v-if="reply.author.userId == currentUserId" @click="deleteComment(reply.id)">删除</span>
                </div>
              </div>
              <button class="btn-load-more" v-if="comment.replyCount > (comment.replies?.length || 0)" @click="loadReplies(comment)">
                查看全部 {{ comment.replyCount }} 条回复
              </button>
            </div>

            <div class="reply-editor" v-if="activeReplyId === comment.id">
              <input v-model="newReply.content" :placeholder="'回复 @' + comment.author.username + '...'" />
              <button @click="submitComment(comment.id)">发送</button>
            </div>
          </div>
        </div>
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
const isCollected = ref(false)
const isFollowing = ref(false)

const comments = ref([])
const commentPage = ref(0)
const commentLast = ref(true)
const activeReplyId = ref(null)
const newComment = reactive({ content: '', files: [] })
const newReply = reactive({ content: '' })

onMounted(async () => {
  await fetchDetail()
  if (currentUserId) {
    fetchTopicStatus()
    fetchRootComments(0)
  }
})

const isOwner = computed(() => topic.value && String(topic.value.author.userId) === String(currentUserId))

async function fetchDetail() {
  try {
    const res = await axios.get(`/api/topics/by-topic/${topicId}`)
    if (res.data.success) {
      topic.value = res.data.data
      checkFollowStatus() // 获取作者关注状态
    }
  } catch (err) { console.error(err) }
}

// 获取话题交互状态（点赞、收藏）
async function fetchTopicStatus() {
  try {
    const res = await axios.get(`/api/actions/topic-status/${topicId}`, { params: { userId: currentUserId } })
    if (res.data.success) {
      hasLiked.value = res.data.data.liked
      isCollected.value = res.data.data.collected
    }
  } catch (err) { console.error(err) }
}

// 检查关注状态
async function checkFollowStatus() {
  if (!topic.value) return
  try {
    const res = await axios.get(`/api/actions/check/${topic.value.author.userId}`, { params: { userId: currentUserId } })
    isFollowing.value = res.data.data
  } catch (err) { console.error(err) }
}

// --- 话题点赞/取消点赞 ---
async function handleLike() {
  const url = hasLiked.value ? `/api/topics/unlike/${topicId}` : `/api/topics/like/${topicId}`
  try {
    const res = await axios.post(url, null, { params: { userId: currentUserId } })
    if (res.data.success) {
      hasLiked.value = !hasLiked.value
      hasLiked.value ? topic.value.likeCount++ : topic.value.likeCount--
    }
  } catch (err) { alert('操作失败') }
}

// --- 话题收藏/取消收藏 ---
async function handleCollect() {
  const url = isCollected.value ? `/api/topics/uncollect/${topicId}` : `/api/topics/collect/${topicId}`
  try {
    const res = await axios.post(url, null, { params: { userId: currentUserId } })
    if (res.data.success) {
      isCollected.value = !isCollected.value
    }
  } catch (err) { alert('收藏操作失败') }
}

// --- 关注/取消关注作者 ---
async function handleFollow() {
  const url = isFollowing.value ? `/api/actions/unfollow/${topic.value.author.userId}` : `/api/actions/follow/${topic.value.author.userId}`
  try {
    const res = await axios.post(url, null, { params: { userId: currentUserId } })
    if (res.data.success) {
      isFollowing.value = !isFollowing.value
    }
  } catch (err) { alert(err.response?.data?.message || '关注操作失败') }
}

// --- 评论交互逻辑 ---

async function fetchRootComments(page) {
  try {
    const res = await axios.get(`/api/comments/root/${topicId}`, {
      params: { page, size: 10 }
    })
    if (res.data.success) {
      const newComments = res.data.data.content
      // 批量查询评论的点赞状态
      if (newComments.length > 0) {
        const commentIds = newComments.map(c => c.id)
        const statusRes = await axios.post('/api/actions/comment-status/batch', { commentIds }, { params: { userId: currentUserId } })
        newComments.forEach(c => { c.isLiked = statusRes.data.data.liked[c.id] })
      }
      page === 0 ? comments.value = newComments : comments.value.push(...newComments)
      commentPage.value = res.data.data.pageNumber
      commentLast.value = res.data.data.last
    }
  } catch (err) { console.error(err) }
}

async function handleCommentLike(comment) {
  const url = comment.isLiked ? `/api/comments/unlike/${comment.id}` : `/api/comments/like/${comment.id}`
  try {
    const res = await axios.post(url, null, { params: { userId: currentUserId } })
    if (res.data.success) {
      comment.isLiked = !comment.isLiked
      comment.isLiked ? comment.likeCount++ : comment.likeCount--
    }
  } catch (err) { console.error(err) }
}

// 其他工具方法（保持原有逻辑）
async function loadReplies(comment) {
  const res = await axios.get(`/api/comments/${comment.id}/replies`)
  if (res.data.success) {
    const replies = res.data.data.content
    const commentIds = replies.map(r => r.id)
    const statusRes = await axios.post('/api/actions/comment-status/batch', { commentIds }, { params: { userId: currentUserId } })
    replies.forEach(r => { r.isLiked = statusRes.data.data.liked[r.id] })
    comment.replies = replies
  }
}

function handleCommentFileChange(e) { newComment.files = Array.from(e.target.files) }
function showReplyInput(id) { activeReplyId.value = activeReplyId.value === id ? null : id; newReply.content = '' }

async function submitComment(parentId) {
  const content = parentId ? newReply.content : newComment.content
  if (!content.trim()) return alert('请输入内容')
  const formData = new FormData()
  formData.append('topicId', topicId)
  formData.append('content', content)
  if (parentId) formData.append('parentId', parentId)
  if (!parentId) newComment.files.forEach(f => formData.append('attachments', f))
  try {
    const res = await axios.post(`/api/comments/create/${currentUserId}`, formData)
    if (res.data.success) {
      parentId ? loadReplies(comments.value.find(c => c.id === parentId)) : fetchRootComments(0)
      newComment.content = ''; newComment.files = []; activeReplyId.value = null
    }
  } catch (err) { alert('发布失败') }
}

async function deleteComment(id) {
  if (!confirm('确定删除?')) return
  await axios.delete(`/api/comments/delete/${id}`, { params: { userId: currentUserId } })
  fetchRootComments(0)
}

async function handleDelete() {
  if (!confirm('确定删除话题?')) return
  await axios.delete(`/api/topics/delete/${topicId}`, { params: { userId: currentUserId } })
  router.back()
}

function jumpToResource(path) {
  const pathParts = path.split('/'); pathParts.pop()
  router.push({ name: 'FolderFiles', params: { courseNo: topic.value.course.courseNo }, query: { path: pathParts.join('/'), folderName: '关联目录' } })
}

function getIcon(n) {
  const e = n.split('.').pop().toLowerCase();
  return e === 'pdf' ? '📕' : ['jpg', 'png', 'jpeg'].includes(e) ? '🖼️' : '📄';
}

function canPreview(t) {
  return t?.includes('image') || t?.includes('pdf') || t?.includes('text');
}

function downloadFile(f) {
  window.open(`/api/attachments/download/${topic.value.forumNo}/${f.accessUrl.split('/').pop()}?download=true`, '_blank');
}

function previewFile(f) {
  window.open(f.previewUrl || `/api/attachments/preview/${topic.value.forumNo}/${f.accessUrl.split('/').pop()}`, '_blank');
}
</script>

<style scoped>
/* 详情页整体容器 */
.detail-page {
  background: #f0f2f5;
  min-height: 100vh;
  padding: 40px 20px;
}

/* 内容卡片（核心内容容器） */
.content-card {
  max-width: 850px;
  margin: 0 auto;
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 25px rgba(0,0,0,0.06);
}

/* 详情页头部区域 */
.detail-header {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 25px;
  margin-bottom: 30px;
}

/* 返回按钮样式 */
.back-btn {
  background: #fff;
  border: 1px solid #d9d9d9;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.3s;
  margin-bottom: 15px;
}

/* 返回按钮 hover 状态 */
.back-btn:hover {
  color: #409eff;
  border-color: #409eff;
}

/* 详情页主标题 */
.detail-header h1 {
  margin: 10px 0;
  color: #1f1f1f;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.4;
}

/* 元信息行（作者/时间/状态等） */
.meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #8c8c8c;
  font-size: 14px;
  flex-wrap: wrap;
}

/* 用户徽章样式 */
.user-badge {
  background: #e6f7ff;
  padding: 4px 12px;
  border-radius: 20px;
  color: #1890ff;
  font-weight: 600;
}

/* 关注按钮样式 */
.follow-btn {
  border: 1px solid #1890ff;
  color: #1890ff;
  background: #fff;
  padding: 2px 10px;
  border-radius: 15px;
  cursor: pointer;
  font-size: 12px;
  transition: 0.3s;
}

/* 关注按钮 hover 状态 */
.follow-btn:hover {
  background: #e6f7ff;
}

/* 已关注状态的关注按钮 */
.follow-btn.is-following {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #8c8c8c;
}

/* 统计信息组（点赞/收藏/评论数） */
.stats-group {
  display: flex;
  gap: 15px;
  margin-left: auto;
}

/* 单个统计项 */
.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 已点赞的点赞数样式 */
.like-count.liked {
  color: #f5222d;
  font-weight: bold;
}

/* 路径跳转盒子 */
.path-jump-box {
  margin-top: 18px;
  background: #f6ffed;
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #b7eb8f;
  display: flex;
  align-items: center;
}

/* 路径标签文本 */
.path-label {
  font-size: 13px;
  color: #52c41a;
  font-weight: bold;
}

/* 路径链接样式 */
.path-link {
  color: #1890ff;
  text-decoration: none;
  font-family: monospace;
  font-size: 13px;
  margin-left: 8px;
}

/* 主内容区域（正文） */
.main-body {
  line-height: 1.8;
  font-size: 16px;
  color: #434343;
  white-space: pre-wrap;
  min-height: 120px;
  margin-bottom: 40px;
}

/* 交互栏样式（点赞/收藏按钮容器） */
.interaction-bar {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin: 40px 0;
  padding: 20px 0;
  border-top: 1px solid #f5f5f5;
}

/* 点赞按钮 & 收藏按钮基础样式 */
.like-button,
.collect-button {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 24px;
  border-radius: 30px;
  border: 1px solid #d9d9d9;
  background: #fff;
  cursor: pointer;
  transition: 0.3s;
}

/* 点赞按钮激活态（已点赞） */
.like-button.active {
  background: #fff1f0;
  border-color: #ffccc7;
  color: #ff4d4f;
}

/* 收藏按钮激活态（已收藏） */
.collect-button.active {
  background: #fffbe6;
  border-color: #ffe58f;
  color: #faad14;
}

/* 附件区域容器 */
.attachments-area {
  border-top: 2px solid #f0f0f0;
  padding-top: 30px;
  margin-top: 40px;
}

/* 区域标题通用样式（附件/评论区标题） */
.section-title {
  font-size: 18px;
  color: #262626;
  margin-bottom: 20px;
  font-weight: 600;
  border-left: 4px solid #1890ff;
  padding-left: 12px;
}

/* 附件列表网格布局 */
.attachment-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 16px;
}

/* 单个附件项 */
.attachment-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}

/* 附件文件信息文本容器 */
.file-info-text {
  flex: 1;
  overflow: hidden;
}

/* 附件文件名 */
.file-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 附件操作按钮基础样式 */
.btn-att {
  border: none;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  margin-left: 5px;
  color: #fff;
}

/* 附件下载按钮 */
.btn-att.download {
  background: #1890ff;
}

/* 附件预览按钮 */
.btn-att.preview {
  background: #faad14;
}

/* 评论区容器 */
.comments-section {
  margin-top: 50px;
  border-top: 2px solid #f0f0f0;
  padding-top: 30px;
}

/* 评论编辑器容器 */
.comment-editor {
  background: #fafafa;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 30px;
}

/* 评论编辑器文本域 */
.comment-editor textarea {
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  resize: none;
  margin-bottom: 12px;
  box-sizing: border-box;
}

/* 编辑器底部（上传/提交按钮区） */
.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 上传按钮包装器（用于隐藏原生文件输入框） */
.upload-btn-wrapper {
  position: relative;
  overflow: hidden;
  display: inline-block;
}

/* 上传按钮样式 */
.btn-upload {
  border: 1px solid #d9d9d9;
  background: #fff;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

/* 原生文件上传输入框（隐藏） */
.upload-btn-wrapper input[type=file] {
  position: absolute;
  left: 0;
  top: 0;
  opacity: 0;
  cursor: pointer;
}

/* 提交评论按钮 */
.btn-submit-comment {
  background: #1890ff;
  color: #fff;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

/* 单个评论项容器 */
.comment-item-container {
  border-bottom: 1px solid #f0f0f0;
  padding: 20px 0;
}

/* 评论作者信息区域 */
.comment-author-info {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

/* 评论作者名称 */
.c-user {
  font-weight: 600;
  color: #262626;
  font-size: 14px;
}

/* 评论发布时间 */
.c-date {
  color: #bfbfbf;
  font-size: 12px;
}

/* 评论文本内容 */
.comment-text {
  font-size: 14px;
  color: #434343;
  line-height: 1.6;
  margin-bottom: 10px;
}

/* 评论页脚（点赞/回复/删除操作） */
.comment-footer {
  display: flex;
  gap: 20px;
}

/* 评论操作按钮（点赞/回复） */
.c-action {
  font-size: 13px;
  color: #8c8c8c;
  cursor: pointer;
}

/* 已点赞的评论操作按钮 */
.c-action.is-liked {
  color: #f5222d;
  font-weight: bold;
}

/* 评论操作按钮 hover 状态 */
.c-action:hover {
  color: #1890ff;
}

/* 评论删除操作按钮 */
.c-action.delete {
  color: #ff7875;
}

/* 回复列表容器 */
.replies-container {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
  margin-top: 15px;
  margin-left: 20px;
}

/* 单个回复项 */
.reply-item {
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

/* 回复编辑器 */
.reply-editor {
  margin-top: 15px;
  margin-left: 20px;
  display: flex;
  gap: 10px;
}

/* 回复编辑器输入框 */
.reply-editor input {
  flex: 1;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 8px 12px;
}

/* 回复编辑器提交按钮 */
.reply-editor button {
  background: #1890ff;
  color: #fff;
  border: none;
  padding: 0 15px;
  border-radius: 4px;
  cursor: pointer;
}

/* 删除链接（评论/回复删除） */
.delete-link {
  color: #ff4d4f;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  margin-left: 20px;
}
</style>
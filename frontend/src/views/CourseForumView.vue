<template>
  <div class="forum-container">
    <div class="header">
      <button @click="$router.back()" class="back-btn">← 返回</button>
      <div class="forum-info">
        <h2>{{ forumInfo.courseName }} 讨论区</h2>
        <span class="topic-count">共 {{ forumInfo.topicCount }} 条讨论</span>
      </div>
      <button class="btn-create" @click="goToPost">发起新话题</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else class="topic-list">
      <div v-if="topics.length === 0" class="empty">暂无讨论话题</div>

      <div v-for="topic in topics" :key="topic.id" class="topic-card" @click="goToDetail(topic.id)">
        <div class="topic-main">
          <h3 class="topic-title">
            <span v-if="topic.referencePath" class="ref-icon">🔗</span>
            {{ topic.title }}
          </h3>
          <p class="topic-preview">{{ topic.content.substring(0, 100) }}...</p>
          <div class="topic-meta">
            <span class="author">👤 {{ topic.author.nickname || topic.author.username }}</span>
            <span class="time">🕒 {{ topic.createdAt }}</span>
            <span v-if="topic.referencePath" class="ref-path">关联文件: {{ topic.referencePath }}</span>
          </div>
        </div>
        <div class="topic-stats">
          <div class="stat-item">👁️ {{ topic.viewCount }}</div>
          <div class="stat-item">💬 {{ topic.replyCount }}</div>

          <!-- 修改：增加点赞和收藏的状态显示 -->
          <div class="stat-item" :class="{ 'highlight-like': topic.isLiked }">
            {{ topic.isLiked ? '❤️' : '👍' }} {{ topic.likeCount }}
          </div>
          <div v-if="topic.isCollected" class="stat-item highlight-collect">⭐ 已收藏</div>
        </div>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="currentPage === 0" @click="fetchTopics(currentPage - 1)">上一页</button>
        <span>第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
        <button :disabled="currentPage >= totalPages - 1" @click="fetchTopics(currentPage + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const courseNo = route.params.courseNo
const currentUserId = sessionStorage.getItem('userId')

const loading = ref(true)
const forumInfo = ref({ forumNo: null, courseName: '', topicCount: 0 })
const topics = ref([])
const currentPage = ref(0)
const totalPages = ref(1)

onMounted(async () => {
  try {
    const res = await axios.get(`/api/forums/by-course/${courseNo}`)
    if (res.data.success) {
      forumInfo.value = res.data.data
      await fetchTopics(0)
    }
  } catch (e) { console.error(e) } finally { loading.value = false }
})

async function fetchTopics(page) {
  try {
    const res = await axios.get(`/api/topics/by-forum/${forumInfo.value.forumNo}`, {
      params: { page, size: 20 }
    })
    if (res.data.success) {
      const content = res.data.data.content

      // --- 关键：批量查询点赞/收藏状态 ---
      if (currentUserId && content.length > 0) {
        const topicIds = content.map(t => t.id)
        const authorIds = content.map(t => t.author.userId)
        const statusRes = await axios.post('/api/actions/topic-status/batch',
            { topicIds, authorIds },
            { params: { userId: currentUserId } }
        )
        const statusData = statusRes.data.data
        content.forEach(t => {
          t.isLiked = statusData.liked[t.id]
          t.isCollected = statusData.collected[t.id]
        })
      }

      topics.value = content
      totalPages.value = res.data.data.totalPages
      currentPage.value = res.data.data.pageNumber
    }
  } catch (e) { alert('获取失败') }
}

function goToPost() {
  router.push({ name: 'TopicPost', params: { courseNo }, query: { forumNo: forumInfo.value.forumNo } })
}

function goToDetail(id) {
  router.push({ name: 'TopicDetail', params: { topicId: id } })
}
</script>

<style scoped>
/* 论坛主容器样式 */
.forum-container {
  padding: 30px;
  max-width: 1000px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f9fafb;
}

/* 头部容器样式 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 25px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

/* 话题卡片基础样式 */
.topic-card {
  background: white;
  padding: 20px;
  margin-bottom: 15px;
  border-radius: 8px;
  display: flex;
  cursor: pointer;
  border: 1px solid #eee;
  transition: all 0.2s;
}

/* 话题卡片 hover 状态 */
.topic-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
}

/* 话题主体内容区域 */
.topic-main {
  flex: 1;
}

/* 话题标题样式 */
.topic-title {
  margin: 0 0 10px 0;
  color: #303133;
}

/* 引用图标样式 */
.ref-icon {
  color: #67c23a;
  margin-right: 5px;
}

/* 话题预览文本样式 */
.topic-preview {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
}

/* 话题元信息（作者/时间等）样式 */
.topic-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 15px;
}

/* 引用路径样式 */
.ref-path {
  color: #409eff;
  background: #ecf5ff;
  padding: 0 5px;
  border-radius: 3px;
}

/* 话题统计信息（点赞/收藏/回复数）区域 */
.topic-stats {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 90px;
  padding-left: 20px;
  border-left: 1px solid #f0f0f0;
  gap: 5px;
  color: #909399;
  font-size: 13px;
}

/* 状态高亮样式 */
.highlight-like {
  color: #f5222d;
  font-weight: bold;
}

.highlight-collect {
  color: #faad14;
  font-weight: bold;
}

/* 创建话题按钮样式 */
.btn-create {
  background: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
}

/* 返回按钮样式 */
.back-btn {
  padding: 8px 16px;
  cursor: pointer;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
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
          <!-- 添加：点赞展示 -->
          <div class="stat-item like-count">👍 {{ topic.likeCount }}</div>
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
      topics.value = res.data.data.content
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
.forum-container { padding: 30px; max-width: 1000px; margin: 0 auto; min-height: 100vh; background: #f9fafb; }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 25px; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.topic-card { background: white; padding: 20px; margin-bottom: 15px; border-radius: 8px; display: flex; cursor: pointer; border: 1px solid #eee; transition: all 0.2s; }
.topic-card:hover { border-color: #409eff; transform: translateY(-2px); }
.topic-main { flex: 1; }
.topic-title { margin: 0 0 10px 0; color: #303133; }
.ref-icon { color: #67c23a; margin-right: 5px; }
.topic-preview { color: #606266; font-size: 14px; margin-bottom: 12px; }
.topic-meta { font-size: 12px; color: #999; display: flex; gap: 15px; }
.ref-path { color: #409eff; background: #ecf5ff; padding: 0 5px; border-radius: 3px; }
.topic-stats { display: flex; flex-direction: column; justify-content: center; min-width: 70px; padding-left: 20px; border-left: 1px solid #f0f0f0; gap: 5px; color: #909399; font-size: 13px; }
.like-count { color: #f56c6c; font-weight: bold; }
.btn-create { background: #409eff; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; }
.back-btn { padding: 8px 16px; cursor: pointer; background: white; border: 1px solid #dcdfe6; border-radius: 4px; }
</style>
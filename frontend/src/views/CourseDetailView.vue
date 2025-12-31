<!-- src/views/CourseDetailView.vue -->
<template>
  <div class="detail-container">
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回列表</button>
      <h2>课程详情</h2>
    </div>

    <div class="content">
      <!-- 这里展示课程ID，证明所有同号课程都跳到了这里 -->
      <div class="info-card">
        <h3>当前课程号: {{ courseNo }}</h3>
        <p>这里将通过 API 根据课程号加载该课程的详细资料...</p>
        <!-- 以后在这里写 axios.get(`/api/courses/${courseNo}`) -->

        <!-- 添加：进入论坛的入口 -->
        <div class="forum-section">
          <p class="forum-tip">对课程有疑问或想法？点击下方按钮进入交流社区</p>
          <button @click="goToForum" class="forum-btn">💬 进入课程论坛</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ref, onMounted } from 'vue'

const route = useRoute()
const router = useRouter()

// 获取路由参数中的 courseNo
const courseNo = ref(route.params.courseNo)

const goBack = () => {
  router.back() // 这样返回时，配合 KeepAlive 就能保留上一页的搜索结果
}

// 添加：跳转到论坛页面的方法
const goToForum = () => {
  router.push({
    name: 'CourseForum',
    params: { courseNo: courseNo.value }
  })
}

onMounted(() => {
  console.log("正在请求课程数据，课程号:", courseNo.value)
  // TODO: 调用后端接口获取详情
})
</script>

<style scoped>
.detail-container {
  padding: 40px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 20px;
}
.back-btn {
  padding: 8px 16px;
  cursor: pointer;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.info-card {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

/* 添加：论坛入口样式 */
.forum-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed #dcdfe6;
  text-align: center;
}
.forum-tip {
  color: #909399;
  font-size: 14px;
  margin-bottom: 15px;
}
.forum-btn {
  padding: 10px 24px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}
.forum-btn:hover {
  background-color: #66b1ff;
}
</style>
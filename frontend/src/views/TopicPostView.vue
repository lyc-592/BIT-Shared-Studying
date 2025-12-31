<template>
  <div class="post-page">
    <div class="post-container">
      <h2>发表新讨论</h2>

      <!-- 引用资源展示区 -->
      <div v-if="form.referencePath" class="reference-info">
        <span class="ref-label">🔗 引用资源路径:</span>
        <code class="ref-path">{{ form.referencePath }}</code>
        <button class="btn-clear-ref" @click="form.referencePath = ''">清除引用</button>
      </div>

      <div class="form-item">
        <label>标题</label>
        <input v-model="form.title" class="input-title" placeholder="请输入话题标题..." />
      </div>

      <div class="form-item">
        <label>正文内容</label>
        <textarea v-model="form.content" rows="12" class="input-text" placeholder="请输入详细讨论内容..."></textarea>
      </div>

      <div class="form-item">
        <label>添加附件</label>
        <input type="file" multiple @change="handleFileChange" class="input-file" />
        <div class="file-list">
          <div v-for="(file, index) in selectedFiles" :key="index" class="file-tag">
            {{ file.name }} <span class="remove-file" @click="removeFile(index)">×</span>
          </div>
        </div>
      </div>

      <div class="post-actions">
        <button class="btn-cancel" @click="$router.back()">取消</button>
        <button class="btn-submit" :disabled="submitting" @click="submitTopic">
          {{ submitting ? '正在发布...' : '确认发布' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const userId = sessionStorage.getItem('userId')

const form = reactive({
  forumNo: route.query.forumNo,
  title: route.query.fileName ? `关于 ${route.query.fileName} 的讨论` : '',
  content: '',
  referencePath: route.query.referencePath || ''
})

const selectedFiles = ref([])
const submitting = ref(false)

function handleFileChange(e) {
  selectedFiles.value = Array.from(e.target.files)
}

function removeFile(index) {
  selectedFiles.value.splice(index, 1)
}

async function submitTopic() {
  if (!form.title || !form.content) return alert('标题和内容必填')
  submitting.value = true

  const formData = new FormData()
  formData.append('forumNo', form.forumNo)
  formData.append('title', form.title)
  formData.append('content', form.content)
  formData.append('referencePath', form.referencePath)
  selectedFiles.value.forEach(file => formData.append('attachments', file))

  try {
    const res = await axios.post(`/api/topics/create/${userId}`, formData)
    if (res.data.success) {
      alert('发布话题成功')
      router.back()
    }
  } catch (err) { alert('发布失败') } finally { submitting.value = false }
}
</script>

<style scoped>
.post-page {
  padding: 40px;
  background: #f0f2f5;
  min-height: 100vh;
}

.post-container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.reference-info {
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  padding: 12px;
  margin-bottom: 25px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.ref-label {
  font-weight: bold;
  color: #67c23a;
  font-size: 14px;
}

.ref-path {
  margin: 0 10px;
  background: #fff;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 13px;
  color: #409eff;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-clear-ref {
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #333;
}

.input-title {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-sizing: border-box;
}

.input-text {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-sizing: border-box;
  resize: vertical;
}

.file-list {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-tag {
  background: #ecf5ff;
  color: #409eff;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.remove-file {
  margin-left: 5px;
  cursor: pointer;
  font-weight: bold;
}

.post-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 30px;
}

.btn-submit {
  background: #409eff;
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.btn-cancel {
  background: #909399;
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 4px;
  cursor: pointer;
}
</style>
<!-- src/App.vue -->
<template>
  <router-view v-slot="{ Component }">
    <!-- 增加一个渐隐过渡效果，让切换页面不再生硬 -->
    <transition name="fade" mode="out-in">
      <keep-alive include="HomeView,CoursesView">
        <component :is="Component" />
      </keep-alive>
    </transition>
  </router-view>
</template>

<script setup>
import { RouterView } from 'vue-router'
</script>

<style>
/* 1. 定义全局设计变量 - 现代感的核心 */
:root {
  /* 品牌色：使用更有活力的靛蓝色 */
  --primary-color: #6366f1;
  --primary-hover: #4f46e5;
  --primary-light: #e0e7ff;

  /* 辅助色：深色主题背景 */
  --sidebar-bg: #0f172a;
  --sidebar-hover: #1e293b;

  /* 中性色：文字与背景 */
  --text-main: #1e293b;
  --text-muted: #64748b;
  --bg-app: #f8fafc; /* 极淡的灰蓝色，比纯白更高级 */

  /* 物理属性：圆角与阴影 */
  --radius-lg: 16px;
  --radius-md: 12px;
  --radius-sm: 8px;
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  --shadow-lg: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);

  /* 毛玻璃边框 */
  --glass-border: rgba(255, 255, 255, 0.3);
}

/* 2. 全局基础重置 */
body, html {
  margin: 0;
  padding: 0;
  height: 100%;
  /* 使用现代字体栈，优先选择 Inter 或系统默认字体 */
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: var(--bg-app);
  color: var(--text-main);
}

#app {
  height: 100%;
}

/* 3. 全局滚动条美化 (针对 Chrome/Edge/Safari) */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 4. 通用组件样式覆盖 (让现有代码自动变美) */

/* 所有的卡片自动获得阴影和圆角 */
.info-card, .course-card, .file-card, .auth-box, .profile-card, .admin-panel {
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--shadow-md) !important;
  border-radius: var(--radius-lg) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.info-card:hover, .course-card:hover, .file-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg) !important;
}

/* 统一按钮样式：增加微动效 */
button {
  transition: all 0.2s ease !important;
  border-radius: var(--radius-sm) !important;
  font-weight: 500 !important;
  letter-spacing: 0.025em !important;
}

button:active {
  transform: scale(0.96);
}

/* 所有的输入框聚焦时更有呼吸感 */
input, select, textarea {
  border: 1px solid #e2e8f0 !important;
  border-radius: var(--radius-sm) !important;
  transition: all 0.2s ease !important;
}

input:focus, select:focus, textarea:focus {
  outline: none !important;
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1) !important;
}

/* 5. 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
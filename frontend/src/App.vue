<!-- src/App.vue -->
<template>
  <router-view v-slot="{ Component }">
    <!-- 仅缓存 HomeView，其他页面（如详情页、设置页）每次进入都刷新 -->
    <keep-alive include="HomeView,CoursesView">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<script setup>
import { RouterView } from 'vue-router'
</script>

<style>
/* 全局重置一些样式，让页面更好看 */
body, html {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: Avenir, Helvetica, Arial, sans-serif;
}
#app {
  height: 100%;
}
</style>
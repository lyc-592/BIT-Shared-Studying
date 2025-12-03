<!-- src/App.vue -->
<template>
  <RouterView />
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
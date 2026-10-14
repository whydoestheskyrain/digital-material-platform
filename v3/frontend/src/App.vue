<template>
  <div id="app">
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter v-if="showFooter" />
    <AiPet />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AiPet from './components/AiPet.vue'
import AppFooter from './components/AppFooter.vue'

const route = useRoute()

// 登录/注册/管理后台为独立全屏布局，不显示全局页尾
const noFooterPaths = ['/login', '/register']
const showFooter = computed(() => {
  return !route.path.startsWith('/admin') && !noFooterPaths.includes(route.path)
})

// 全局动效开关：优先读取用户在「个人中心」保存的偏好，其次跟随系统 prefers-reduced-motion
const applyReduceMotion = () => {
  const stored = localStorage.getItem('ui_reduce_motion')
  const systemReduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const reduce = stored === '1' || (stored === null && systemReduce)
  document.documentElement.classList.toggle('reduce-motion', reduce)
}

onMounted(() => {
  applyReduceMotion()
  // 监听系统偏好变化
  if (window.matchMedia) {
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)')
    const handler = () => {
      if (localStorage.getItem('ui_reduce_motion') === null) applyReduceMotion()
    }
    mq.addEventListener ? mq.addEventListener('change', handler) : mq.addListener(handler)
  }
})
</script>

<style>
/* 全局布局：粘性页尾（内容不足一屏时页尾贴底，内容超出一屏时跟随文档流） */
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1 0 auto;
}

.app-footer {
  flex-shrink: 0;
}
</style>

<template>
  <div id="app">
    <router-view v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <AiPet />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AiPet from './components/AiPet.vue'

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

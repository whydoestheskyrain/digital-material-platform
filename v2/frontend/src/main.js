import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './assets/css/common.css'

const app = createApp(App)
app.use(router)
app.use(ElementPlus)

// 全局注册 Element Plus 图标，模板中直接 <el-icon><ShoppingCart /></el-icon> 使用
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')

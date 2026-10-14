<template>
  <div class="login-page">
    <div class="login-background">
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
      </div>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">🎮</div>
        <h1 class="login-title">虚拟交易平台</h1>
        <p class="login-subtitle">安全可靠的虚拟商品交易</p>
      </div>

      <el-form :model="loginForm" class="login-form" @keyup.enter="login">
        <el-form-item>
          <div class="input-wrapper">
            <el-icon class="input-icon"><User /></el-icon>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入账号"
              class="form-input"
              size="large"
              clearable
            />
          </div>
        </el-form-item>

        <el-form-item>
          <div class="input-wrapper">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-input
              v-model="loginForm.password"
              placeholder="请输入密码"
              type="password"
              class="form-input"
              size="large"
              show-password
            />
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="login-btn" size="large" @click="login" :loading="loading">
            <span v-if="!loading">🚀</span>
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span class="footer-text">还没有账号？</span>
        <el-button type="success" text @click="$router.push('/register')" class="register-link">
          立即注册 →
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { setAccount, migrateFromLegacy } from '@/utils/accountManager'

const router = useRouter()
const loading = ref(false)

const loginForm = ref({
  username: '',
  password: ''
})

const login = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  try {
    
  loading.value = true
  const res = await request.post('/user/login', loginForm.value)
  if (res.code === 200) {
    // 后端返回结构：{ code:200, data:{ user: {...}, token: "xxx" } }
    const userData = res.data.user || res.data   // 兼容两种返回
    const token = res.data.token || res.token   // 取 token
    
    // 确保用户 ID 为数字类型（后端返回可能是整数或字符串）
    if (userData.id) {
      userData.id = Number(userData.id)
    }
    
    console.log("当前登录用户信息：", userData)

    // 账号必须先同步写入 localStorage，再跳转。
    // 否则路由守卫可能读到旧账号，错误地把管理员送回首页。
    migrateFromLegacy()
    const accountId = 'user_' + userData.id
    setAccount(accountId, token, userData)

    ElMessage.success('登录成功')
    console.log('完整响应:', res)
console.log('res.data:', res.data)
    if (String(userData.role || '').toUpperCase() === 'ADMIN') {
      router.push('/admin/home')
    } else {
      router.push('/')
    }
  } else {
    ElMessage.error(res.msg || '登录失败')
  }
} catch (err) {
  console.error('登录异常', err)
  ElMessage.error('服务器异常，请稍后重试')
} finally {
  loading.value = false
} 
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 12% 12%, rgba(99, 91, 255, 0.13), transparent 30%),
    radial-gradient(circle at 88% 82%, rgba(34, 211, 238, 0.12), transparent 32%),
    #f1f1f1;
  position: relative;
  overflow: hidden;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.floating-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.16;
  animation: float 20s infinite ease-in-out;
  will-change: transform;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: #a5b4fc;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: #99f6e4;
  bottom: -50px;
  right: -50px;
  animation-delay: -5s;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: #c4b5fd;
  top: 50%;
  right: 10%;
  animation-delay: -10s;
}

.shape-4 {
  width: 150px;
  height: 150px;
  background: #bae6fd;
  bottom: 20%;
  left: 5%;
  animation-delay: -15s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(20px, 20px) rotate(5deg);
  }
  50% {
    transform: translate(0, 40px) rotate(0deg);
  }
  75% {
    transform: translate(-20px, 20px) rotate(-5deg);
  }
}

.login-card {
  width: 420px;
  padding: 50px 40px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 24px;
  backdrop-filter: blur(20px);
  box-shadow: 0 18px 44px rgba(16, 24, 40, 0.12);
  position: relative;
  z-index: 10;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-icon {
  font-size: 64px;
  margin-bottom: 16px;
  display: inline-block;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
  background: var(--color-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.login-form {
  margin-bottom: 20px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: var(--color-bg-card-hover);
  border-radius: 12px;
  padding: 0 16px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.input-wrapper:focus-within {
  background: var(--color-bg-card-hover);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(99, 91, 255, 0.14);
}

.input-icon {
  font-size: 18px;
  margin-right: 12px;
  color: var(--color-primary);
  flex-shrink: 0;
}

.login-form :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none !important;
  padding: 0;
}

.login-form :deep(.el-input__inner) {
  height: 50px;
  font-size: 15px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: var(--color-primary-gradient);
  border: none;
  box-shadow: 0 8px 18px rgba(99, 91, 255, 0.24);
  transition: all 0.3s ease;
  letter-spacing: 2px;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(99, 91, 255, 0.3);
}

.login-btn:active {
  transform: translateY(0);
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
}

.footer-text {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.register-link {
  font-size: 14px;
  font-weight: 500;
  padding: 0;
  color: var(--color-link) !important;
}

.register-link:hover {
  color: var(--color-primary-end) !important;
}

@media (max-width: 768px) {
  .login-card {
    width: 92%;
    max-width: 380px;
    padding: 30px 20px;
    margin: 16px auto;
  }

  .login-title {
    font-size: 22px;
  }

  .logo-icon {
    font-size: 44px;
  }

  .login-form .el-input__inner {
    height: 42px;
    font-size: 15px;
  }

  .login-btn {
    height: 44px;
    font-size: 15px;
  }

  /* 小屏/移动设备通常 GPU 较弱：减少漂浮几何数量并放慢周期，降低掉帧 */
  .shape-3,
  .shape-4 {
    display: none;
  }
  .shape {
    animation-duration: 32s;
  }
}

@media (max-width: 480px) {
  .login-card {
    width: 94%;
    padding: 24px 16px;
  }
  .login-title {
    font-size: 20px;
  }
}
</style>

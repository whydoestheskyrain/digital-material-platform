<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="form-wrapper">
        <h2>用户注册</h2>

        <el-input v-model="form.username" placeholder="请输入用户名" class="form-input" />
        <el-input v-model="form.password" placeholder="请输入密码" type="password" class="form-input" show-password />
        <el-input v-model="form.nickname" placeholder="请输入昵称" class="form-input" />
        <el-input v-model="form.phone" placeholder="请输入手机号" class="form-input" />

        <div class="form-actions">
          <el-button type="primary" @click="register">注册</el-button>
          <el-button type="success" @click="$router.push('/login')">已有账号？去登录</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const form = ref({
  username: '',
  password: '',
  nickname: '',
  phone: ''
})

const register = async () => {
  // 非空校验
  if (!form.value.username || !form.value.password || !form.value.nickname || !form.value.phone) {
    ElMessage.warning('请填写完整信息')
    return
  }
  // 手机号格式校验
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
    ElMessage.error('手机号格式不正确')
    return
  }
  // 密码强度校验（6-20位字母和数字组合）
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/.test(form.value.password)) {
    ElMessage.error('密码必须为6-20位字母和数字组合')
    return
  }

  try {
    const res = await request.post('/user/register', form.value)
    if (res.code === 200) {
      // 成功提示并跳转
      ElMessageBox.alert('注册成功！即将跳转到登录页', '提示', {
        confirmButtonText: '确定',
        callback: () => {
          router.push('/login')
        }
      })
    } else {
      // 针对用户名已存在的特殊提示
      if (res.msg && res.msg.includes('用户名已存在')) {
        ElMessage.error('该用户名已被注册，请更换')
      } else {
        ElMessage.error(res.msg || '注册失败')
      }
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('注册失败，请稍后重试')
  }
}
</script>

<style scoped>
.form-wrapper {
  max-width: 400px;
  margin: 80px auto;
  padding: 40px 30px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}

.form-wrapper h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
}

.form-input {
  margin-bottom: 20px;
}

.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 10px;
}

.form-actions .el-button {
  flex: 1;
}

@media (max-width: 768px) {
  .form-wrapper {
    margin: 40px 20px;
    padding: 30px 20px;
  }
}
</style>

<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="form-wrapper">
        <h2 class="form-title">新增用户</h2>
        
        <el-form :model="form" label-width="80px" class="user-form">
          <el-form-item label="用户名">
            <el-input 
              v-model="form.username" 
              placeholder="请输入用户名" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="密码">
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="昵称">
            <el-input 
              v-model="form.nickname" 
              placeholder="请输入昵称" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="手机号">
            <el-input 
              v-model="form.phone" 
              placeholder="请输入手机号" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="角色">
            <el-select v-model="form.role" class="form-input">
              <el-option :value="0" label="普通用户" />
              <el-option :value="1" label="管理员" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submit" class="btn-submit">保存</el-button>
            <el-button @click="back" class="btn-back">返回</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const form = ref({ username: '', password: '', nickname: '', phone: '', role: 0 })

const submit = async () => {
  try {
    if (!form.value.username) {
      ElMessage.warning('请输入用户名')
      return
    }
    if (!form.value.password) {
      ElMessage.warning('请输入密码')
      return
    }
    
    await request.post('/user/add', form.value)
    ElMessage.success('新增成功')
    router.push('/user/list')
  } catch (err) {
    ElMessage.error('新增失败')
  }
}

const back = () => router.push('/user/list')
</script>

<style scoped>
.form-title {
  text-align: center;
  margin-bottom: 25px;
  font-size: 22px;
  color: var(--color-text-primary);
}

.user-form {
  max-width: 100%;
}

.form-input {
  width: 100%;
}

.btn-submit,
.btn-back {
  width: 48%;
}

.btn-back {
  margin-left: 4%;
}

@media (max-width: 768px) {
  .btn-submit,
  .btn-back {
    width: 100%;
    margin-left: 0;
    margin-top: 10px;
  }
}
</style>

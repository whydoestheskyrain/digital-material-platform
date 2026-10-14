<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="form-wrapper">
        <h2 class="form-title">编辑商品</h2>
        
        <el-form :model="form" label-width="80px" class="product-form">
          <el-form-item label="商品标题">
            <el-input 
              v-model="form.title" 
              placeholder="请输入商品标题" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="游戏区服">
            <el-input 
              v-model="form.serverName" 
              placeholder="如：官服、B服" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="角色等级">
            <el-input 
              v-model.number="form.roleLevel" 
              type="number" 
              placeholder="请输入角色等级" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="商品价格">
            <el-input 
              v-model.number="form.price" 
              type="number" 
              placeholder="请输入价格" 
              class="form-input"
              prefix-icon="¥"
            />
          </el-form-item>
          
          <el-form-item label="商品描述">
            <el-textarea 
              v-model="form.desc" 
              :rows="4" 
              placeholder="请输入商品描述" 
              class="form-input"
            />
          </el-form-item>

          <el-form-item label="商品详情">
            <el-textarea 
              v-model="form.detailDesc" 
              :rows="6" 
              placeholder="详细描述商品信息，如：英雄数量、皮肤数量、段位、亮点等" 
              class="form-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitUpdate" class="btn-submit">保存修改</el-button>
            <el-button @click="goBack" class="btn-back">返回列表</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const form = ref({})

async function loadData() {
  const id = route.params.id
  const res = await request.get('/product/get/' + id)
  form.value = res.data
}

async function submitUpdate() {
  try {
    if (!form.value.title) {
      ElMessage.warning('请输入商品标题')
      return
    }
    if (!form.value.price || form.value.price <= 0) {
      ElMessage.warning('请输入有效价格')
      return
    }
    
    const submitData = {
      ...form.value,
      description: form.value.desc
    }
    await request.post('/product/update', submitData)
    ElMessage.success('修改成功！')
    router.push('/product/list')
  } catch (err) {
    ElMessage.error('修改失败')
  }
}

function goBack() {
  router.push('/product/list')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.form-title {
  text-align: center;
  margin-bottom: 25px;
  font-size: 22px;
  color: var(--color-text-primary);
}

.product-form {
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

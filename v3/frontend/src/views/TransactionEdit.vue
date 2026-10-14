<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="form-wrapper">
        <h2 class="form-title">编辑交易记录</h2>
        
        <el-form :model="form" label-width="100px" class="transaction-form">
          <el-form-item label="订单编号">
            <el-input 
              v-model="form.orderNo" 
              placeholder="请输入订单编号" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="买家ID">
            <el-input 
              v-model="form.buyerId" 
              type="number" 
              placeholder="请输入买家ID" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="卖家ID">
            <el-input 
              v-model="form.sellerId" 
              type="number" 
              placeholder="请输入卖家ID" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="交易金额">
            <el-input 
              v-model="form.money" 
              type="number" 
              placeholder="请输入交易金额" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="交易类型">
            <el-select v-model="form.type" class="form-input">
              <el-option :value="1" label="支付" />
              <el-option :value="2" label="退款" />
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const form = ref({})

const loadData = async () => {
  try {
    const res = await request.get('/transaction/get/' + route.params.id)
    form.value = res.data
  } catch (err) {
    ElMessage.error('加载数据失败')
  }
}

const submit = async () => {
  try {
    await request.post('/transaction/update', form.value)
    ElMessage.success('修改成功')
    router.push('/transaction/list')
  } catch (err) {
    ElMessage.error('修改失败')
  }
}

const back = () => router.push('/transaction/list')

onMounted(loadData)
</script>

<style scoped>
.form-title {
  text-align: center;
  margin-bottom: 25px;
  font-size: 22px;
  color: var(--color-text-primary);
}

.transaction-form {
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

<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="form-wrapper">
        <h2 class="form-title">编辑订单</h2>
        
        <el-form :model="form" label-width="100px" class="order-form">
          <el-form-item label="订单编号">
            <el-input 
              v-model="form.orderNo" 
              placeholder="请输入订单编号" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="商品ID">
            <el-input 
              v-model.number="form.productId" 
              type="number" 
              placeholder="请输入商品ID" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="买家ID">
            <el-input 
              v-model.number="form.buyerId" 
              type="number" 
              placeholder="请输入买家ID" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="卖家ID">
            <el-input 
              v-model.number="form.sellerId" 
              type="number" 
              placeholder="请输入卖家ID" 
              class="form-input"
            />
          </el-form-item>
          
          <el-form-item label="价格">
            <el-input 
              v-model.number="form.price" 
              type="number" 
              placeholder="请输入价格" 
              class="form-input"
              prefix-icon="¥"
            />
          </el-form-item>
          
          <el-form-item label="状态">
            <el-select v-model="form.status" class="form-input">
              <el-option value="UNPAID" label="待支付" />
              <el-option value="PAID" label="已支付" />
              <el-option value="DELIVERED" label="已发货" />
              <el-option value="COMPLETED" label="已完成" />
              <el-option value="CANCELLED" label="已取消" />
            </el-select>
          </el-form-item>

          <el-form-item label="账号信息">
            <el-textarea 
              v-model="form.accountInfo" 
              :rows="3" 
              placeholder="卖家填写的账号信息" 
              class="form-input"
            />
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
    const res = await request.get('/order/detail/' + route.params.id)
    form.value = res.data
  } catch (err) {
    ElMessage.error('加载数据失败')
  }
}

const submit = async () => {
  try {
    await request.post('/order/update', form.value)
    ElMessage.success('保存成功')
    router.push('/order/list')
  } catch (err) {
    ElMessage.error('保存失败')
  }
}

const back = () => router.push('/order/list')

onMounted(loadData)
</script>

<style scoped>
.form-title {
  text-align: center;
  margin-bottom: 25px;
  font-size: 22px;
  color: var(--color-text-primary);
}

.order-form {
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

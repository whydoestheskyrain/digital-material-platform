<template>
  <div class="recharge-container">
    <h2>账户充值</h2>

    <el-input-number
      v-model="amount"
      :min="0.01"
      :max="9999"
      step="0.01"
      placeholder="充值金额"
    ></el-input-number>

    <el-button type="primary" @click="handleRecharge" :loading="loading">
      立即充值
    </el-button>

    <div ref="alipayFormRef" style="display:none;"></div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const amount = ref(10)
const loading = ref(false)
const alipayFormRef = ref(null)

// 轮询定时器
let timer = null

const handleRecharge = async () => {
  if (!amount.value || amount.value <= 0) {
    ElMessage.warning('请输入正确金额')
    return
  }

  try {
    loading.value = true

    // 1. 创建订单
    const res = await request.post('/recharge/create', {
      amount: amount.value
    })

    if (res.code !== 200) {
      ElMessage.error(res.msg || '创建订单失败')
      return
    }

    const formHtml = res.data
    alipayFormRef.value.innerHTML = formHtml

    // 提取订单号
    const match = formHtml.match(/out_trade_no":"([^"&]+)/)
    const orderNo = match ? match[1] : null

    if (!orderNo) {
      ElMessage.error('获取订单失败')
      return
    }

    // 2. 新标签打开支付宝（关键！解决 iframe 报错）
    const newWindow = window.open('', '_blank')
    newWindow.document.write(formHtml)
    newWindow.document.close()

    // 3. 开始轮询订单是否支付成功
    startPollOrder(orderNo)

  } catch (e) {
    console.error(e)
    ElMessage.error('充值请求失败')
  } finally {
    loading.value = false
  }
}

// 轮询查询订单状态 → 成功就跳个人中心
const startPollOrder = (orderNo) => {
  if (timer) clearInterval(timer)

  timer = setInterval(async () => {
    try {
      const res = await request.get(`/recharge/status/${orderNo}`)
      if (res.code === 200) {
        const status = res.data.status
        if (status === 1) {
          clearInterval(timer)
          ElMessage.success('充值成功！')
          // ✅ 跳回个人中心
          window.location.href = '/#/user/center'
        }
      }
    } catch (e) {e}
  }, 2000)
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.recharge-container {
  width: 300px;
  margin: 50px auto;
}
</style>
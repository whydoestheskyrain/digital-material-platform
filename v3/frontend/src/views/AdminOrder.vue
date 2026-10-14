<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>🛒 订单管理</h1>
          <p class="header-desc">管理所有买家订单，处理发货等操作</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" plain @click="$router.push('/admin/home')">
            <el-icon><ArrowLeft /></el-icon> 返回控制台
          </el-button>
        </div>
      </div>

      <div class="filter-section">
        <div class="filter-tabs">
          <el-button
            :type="statusFilter === 'all' ? 'primary' : 'default'"
            :class="{ 'active-tab': statusFilter === 'all' }"
            @click="statusFilter = 'all'"
          >
            全部 <span class="tab-count">{{ statusCounts.all }}</span>
          </el-button>
          <el-button
            :type="statusFilter === 'PAID' ? 'warning' : 'default'"
            :class="{ 'active-tab': statusFilter === 'PAID' }"
            @click="statusFilter = 'PAID'"
          >
            <el-icon><Timer /></el-icon> 待发货 <span class="tab-count" v-if="statusCounts.paid > 0">{{ statusCounts.paid }}</span>
          </el-button>
          <el-button
            :type="statusFilter === 'DELIVERED' ? 'info' : 'default'"
            :class="{ 'active-tab': statusFilter === 'DELIVERED' }"
            @click="statusFilter = 'DELIVERED'"
          >
            <el-icon><Van /></el-icon> 已发货
          </el-button>
          <el-button
            :type="statusFilter === 'COMPLETED' ? 'success' : 'default'"
            :class="{ 'active-tab': statusFilter === 'COMPLETED' }"
            @click="statusFilter = 'COMPLETED'"
          >
            <el-icon><CircleCheck /></el-icon> 已完成
          </el-button>
          <el-button
            :type="statusFilter === 'CANCELLED' ? 'danger' : 'default'"
            :class="{ 'active-tab': statusFilter === 'CANCELLED' }"
            @click="statusFilter = 'CANCELLED'"
          >
            <el-icon><CircleClose /></el-icon> 已取消
          </el-button>
        </div>
      </div>

      <div class="table-card">
        <el-table
          :data="filteredOrders"
          border
          stripe
          class="data-table"
          v-loading="loading"
          :header-cell-style="{ background: '#f8f9fc', color: '#475467', fontWeight: '600' }"
        >
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="orderNo" label="订单号" min-width="180">
            <template #default="scope">
              <span class="order-no">{{ scope.row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="productId" label="商品ID" width="100" align="center" />
          <el-table-column prop="buyerId" label="买家" width="100" align="center">
            <template #default="scope">
              <span class="person-cell"><el-icon><User /></el-icon> {{ scope.row.buyerId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sellerId" label="卖家" width="100" align="center">
            <template #default="scope">
              <span class="person-cell"><el-icon><User /></el-icon> {{ scope.row.sellerId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="金额" width="100" align="center">
            <template #default="scope">
              <span class="price-text">¥{{ scope.row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)" effect="dark" size="small">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="accountInfo" label="账号信息" width="120" align="center">
            <template #default="scope">
              <el-button
                v-if="scope.row.accountInfo"
                size="small"
                type="primary"
                plain
                @click="showAccount(scope.row)"
              >
                <el-icon><View /></el-icon> 查看
              </el-button>
              <span v-else-if="scope.row.status === 'PAID'" class="no-account">
                待发货
              </span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="160" align="center">
  <template #default="scope">
    <div class="action-buttons">
      <!-- 待发货（数字2）才显示发货按钮 -->
      <el-button
        v-if="scope.row.status === 2"
        size="small"
        type="success"
        @click="deliver(scope.row)"
      >
        <el-icon><Van /></el-icon> 发货
      </el-button>
      <el-button
        size="small"
        type="info"
        plain
        @click="showDetail(scope.row)"
      >
        <el-icon><Document /></el-icon> 详情
      </el-button>
    </div>
  </template>
</el-table-column>
        </el-table>
      </div>

      <!-- 账号信息弹窗 -->
      <el-dialog
        v-model="accountDialogVisible"
        width="450px"
      >
        <template #header>
          <span class="dialog-title"><el-icon><Lock /></el-icon> 账号信息</span>
        </template>
        <div class="account-info" v-if="currentOrder">
          <div class="info-item">
            <span class="label"><el-icon><Document /></el-icon> 订单号：</span>
            <span class="value">{{ currentOrder.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="label"><el-icon><Cellphone /></el-icon> 游戏账号：</span>
            <span class="value">{{ currentOrder.accountInfo || '暂无' }}</span>
          </div>
        </div>
        <template #footer>
          <el-button @click="accountDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 发货弹窗 -->
      <el-dialog
        v-model="deliverDialogVisible"
        width="500px"
      >
        <template #header>
          <span class="dialog-title"><el-icon><Van /></el-icon> 填写账号信息并发货</span>
        </template>
        <div class="deliver-form" v-if="currentOrder">
          <div class="order-summary">
            <span>订单号：{{ currentOrder.orderNo }}</span>
            <span>金额：¥{{ currentOrder.price }}</span>
          </div>
          <p class="deliver-tip">
            <el-icon><Warning /></el-icon> 请填写游戏账号信息，买家确认收货后即可看到
          </p>
          <el-input
            v-model="deliverAccountInfo"
            type="textarea"
            :rows="4"
            placeholder="请输入游戏账号、密码等信息..."
            maxlength="500"
            show-word-limit
          />
        </div>
        <template #footer>
          <el-button @click="deliverDialogVisible = false">取消</el-button>
          <el-button type="success" @click="confirmDeliver" :loading="delivering">
            <el-icon><Van /></el-icon> 确认发货
          </el-button>
        </template>
      </el-dialog>

      <!-- 订单详情弹窗 -->
      <el-dialog
        v-model="detailDialogVisible"
        width="500px"
      >
        <template #header>
          <span class="dialog-title"><el-icon><Document /></el-icon> 订单详情</span>
        </template>
        <div class="detail-info" v-if="currentOrder">
          <div class="info-row">
            <span class="info-label">订单ID：</span>
            <span class="info-value">{{ currentOrder.id }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">订单号：</span>
            <span class="info-value">{{ currentOrder.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">商品ID：</span>
            <span class="info-value">{{ currentOrder.productId }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">买家ID：</span>
            <span class="info-value">{{ currentOrder.buyerId }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">卖家ID：</span>
            <span class="info-value">{{ currentOrder.sellerId }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">订单金额：</span>
            <span class="info-value price">¥{{ currentOrder.price }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">订单状态：</span>
            <el-tag :type="getStatusType(currentOrder.status)" size="small">
              {{ getStatusText(currentOrder.status) }}
            </el-tag>
          </div>
          <div class="info-row">
            <span class="info-label">创建时间：</span>
            <span class="info-value">{{ currentOrder.createTime }}</span>
          </div>
          <div class="info-row" v-if="currentOrder.accountInfo">
            <span class="info-label">账号信息：</span>
            <span class="info-value">{{ currentOrder.accountInfo }}</span>
          </div>
        </div>
        <template #footer>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button
            v-if="currentOrder && currentOrder.status === 'PAID'"
            type="success"
            @click="deliver(currentOrder); detailDialogVisible = false"
          >
            <el-icon><Van /></el-icon> 去发货
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { ORDER_STATUS_TEXT, ORDER_STATUS_TYPE } from '@/utils/constants'

const loading = ref(false)
const allOrders = ref([])
const statusFilter = ref('all')
const delivering = ref(false)

const accountDialogVisible = ref(false)
const deliverDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const deliverAccountInfo = ref('')

const statusCounts = computed(() => {
  const counts = { all: 0, paid: 0, delivered: 0, completed: 0, cancelled: 0 }
  allOrders.value.forEach(order => {
    counts.all++
    if (order.status === 'PAID') counts.paid++
    else if (order.status === 'DELIVERED') counts.delivered++
    else if (order.status === 'COMPLETED') counts.completed++
    else if (order.status === 'CANCELLED') counts.cancelled++
  })
  return counts
})

const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/order/listAll')
    allOrders.value = res.data || []
  } catch (err) {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const filteredOrders = computed(() => {
  if (statusFilter.value === 'all') {
    return allOrders.value
  }
  if (statusFilter.value === 'PAID') {
    return allOrders.value.filter(o => o.status === 2) // 待发货
  }
  if (statusFilter.value === 'DELIVERED') {
    return allOrders.value.filter(o => o.status === 3)
  }
  if (statusFilter.value === 'COMPLETED') {
    return allOrders.value.filter(o => o.status === 4)
  }
  if (statusFilter.value === 'CANCELLED') {
    return allOrders.value.filter(o => o.status === 5)
  }
  return allOrders.value
})

const getStatusType = (status) => {
  return ORDER_STATUS_TYPE[status] || 'default'
}

const getStatusText = (status) => {
  return ORDER_STATUS_TEXT[status] || status
}

const showAccount = (row) => {
  currentOrder.value = row
  accountDialogVisible.value = true
}

const deliver = (row) => {
  currentOrder.value = row
  deliverAccountInfo.value = ''
  deliverDialogVisible.value = true
}

const confirmDeliver = async () => {
  if (!deliverAccountInfo.value) {
    ElMessage.warning('请填写账号信息')
    return
  }
  delivering.value = true
  try {
 await request.post(`/order/deliver/${currentOrder.value.id}`, {
  accountInfo: deliverAccountInfo.value
})
    ElMessage.success('发货成功')
    deliverDialogVisible.value = false
    getList()
  } catch (err) {
    ElMessage.error('发货失败')
  } finally {
    delivering.value = false
  }
}

const showDetail = (row) => {
  currentOrder.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  padding: 20px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: var(--color-bg-card);
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid var(--color-border);
}

.header-left h1 {
  font-size: 22px;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.header-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.header-actions .el-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: var(--color-bg-card);
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid var(--color-border);
}

.filter-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-tabs .el-button {
  border-radius: 20px;
  padding: 8px 16px;
  transition: all 0.3s ease;
}

.filter-tabs .active-tab {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.tab-count {
  display: inline-block;
  margin-left: 4px;
  padding: 0 6px;
  background: rgba(255,255,255,0.3);
  border-radius: 10px;
  font-size: 11px;
}

.table-card {
  background: var(--color-bg-card);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  padding: 20px;
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.order-no {
  font-family: monospace;
  color: var(--color-text-regular);
}

.price-text {
  color: #f56c6c;
  font-weight: bold;
  font-size: 15px;
}

.no-account {
  color: #e6a23c;
  font-size: 12px;
}

.text-muted {
  color: var(--color-text-secondary);
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.account-info,
.detail-info {
  padding: 10px 0;
}

.person-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dialog-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.info-item .label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: 110px;
  color: var(--color-text-regular);
  flex-shrink: 0;
}

.info-item .label .el-icon {
  font-size: 15px;
  color: var(--color-primary);
}

.info-item,
.info-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.info-item:last-child,
.info-row:last-child {
  border-bottom: none;
}

.info-label {
  width: 100px;
  color: var(--color-text-regular);
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  word-break: break-all;
}

.info-value.price {
  color: #f56c6c;
  font-weight: bold;
}

.order-summary {
  display: flex;
  gap: 20px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin-bottom: 15px;
  font-size: 13px;
  color: var(--color-text-regular);
}

.deliver-form {
  padding: 10px 0;
}

.deliver-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-warning);
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 15px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .filter-section {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

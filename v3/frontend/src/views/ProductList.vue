<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>📦 我发布的商品</h1>
          <p class="header-tip">管理您的商品，查看审核状态</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="$router.push('/product/add')">
            <el-icon><Plus /></el-icon> 发布新商品
          </el-button>
          <el-button @click="$router.push('/')">
            <el-icon><HomeFilled /></el-icon> 返回首页
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
            全部 <span class="tab-count" v-if="statusCounts.all > 0">{{ statusCounts.all }}</span>
          </el-button>
          <el-button
            :type="statusFilter === '0' ? 'warning' : 'default'"
            :class="{ 'active-tab': statusFilter === '0' }"
            @click="statusFilter = '0'"
          >
            <el-icon><Timer /></el-icon> 待审核 <span class="tab-count" v-if="statusCounts.pending > 0">{{ statusCounts.pending }}</span>
          </el-button>
          <el-button
            :type="statusFilter === '1' ? 'success' : 'default'"
            :class="{ 'active-tab': statusFilter === '1' }"
            @click="statusFilter = '1'"
          >
            <el-icon><CircleCheck /></el-icon> 已上架 <span class="tab-count" v-if="statusCounts.approved > 0">{{ statusCounts.approved }}</span>
          </el-button>
          <el-button
            :type="statusFilter === '2' ? 'danger' : 'default'"
            :class="{ 'active-tab': statusFilter === '2' }"
            @click="statusFilter = '2'"
          >
            <el-icon><CircleClose /></el-icon> 审核拒绝 <span class="tab-count" v-if="statusCounts.rejected > 0">{{ statusCounts.rejected }}</span>
          </el-button>
          <el-button
            :type="statusFilter === '3' ? 'info' : 'default'"
            :class="{ 'active-tab': statusFilter === '3' }"
            @click="statusFilter = '3'"
          >
            <el-icon><Bottom /></el-icon> 已下架 <span class="tab-count" v-if="statusCounts.sold > 0">{{ statusCounts.sold }}</span>
          </el-button>
        </div>
      </div>

      <el-table
        :data="filteredProducts"
        border
        stripe
        class="data-table"
        v-loading="loading"
        :header-cell-style="{ background: '#f8f9fc', color: '#475467', fontWeight: '600' }"
      >
        <el-table-column prop="title" label="商品标题" min-width="200">
          <template #default="scope">
            <div class="product-title-cell">
              <span class="product-icon">{{ getProductIcon(scope.row.gameTypeName || scope.row.gameType) }}</span>
              <span class="product-title">{{ scope.row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="gameTypeName" label="游戏类型" width="120">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.gameTypeName || scope.row.gameType || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serverName" label="区服" width="120" />
        <el-table-column prop="price" label="价格" width="100" align="center">
          <template #default="scope">
            <span class="price-text">¥{{ scope.row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" width="180">
          <template #default="scope">
            <span v-if="scope.row.status === 2" class="reject-reason">
              <el-icon class="reject-icon"><Warning /></el-icon>
              {{ scope.row.rejectReason || '无具体原因' }}
            </span>
            <span v-else class="no-reason">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="160" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                size="small"
                type="danger"
                plain
                v-if="scope.row.status === 0 || scope.row.status === 1"
                @click="handleCancel(scope.row)"
              >
                <el-icon><CircleClose /></el-icon> 撤销
              </el-button>
              <el-button
                size="small"
                type="primary"
                plain
                v-if="scope.row.status === 2"
                @click="$router.push(`/product/edit/${scope.row.id}`)"
              >
                <el-icon><EditPen /></el-icon> 重新编辑
              </el-button>
              <el-button
                size="small"
                type="info"
                plain
                v-if="scope.row.status === 3"
                disabled
              >
                已结束
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && filteredProducts.length === 0" class="empty-state">
        <div class="empty-illustration">
          <span class="empty-icon">{{ statusFilter === 'all' ? '📦' : '🔍' }}</span>
        </div>
        <h4 class="empty-title">
          {{ statusFilter === 'all' ? '暂无发布的商品' : '暂无符合条件的商品' }}
        </h4>
        <p class="empty-desc">
          {{ statusFilter === 'all' ? '开始发布您的第一个商品吧' : '换个筛选条件试试' }}
        </p>
        <div class="empty-actions">
          <el-button v-if="statusFilter !== 'all'" @click="statusFilter = 'all'">清除筛选</el-button>
          <el-button type="primary" @click="$router.push('/product/add')">去发布</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { PRODUCT_STATUS_TEXT, PRODUCT_STATUS_TYPE } from '@/utils/constants'

const loading = ref(false)
const allProducts = ref([])
const statusFilter = ref('all')

const statusCounts = computed(() => {
  const user = getUser()
  if (!user || !user.id) return { all: 0, pending: 0, approved: 0, rejected: 0, sold: 0 }

  const products = allProducts.value.filter(product => {
    const sellerId = typeof product.sellerId === 'string' ? parseInt(product.sellerId, 10) : product.sellerId
    return sellerId === user.id
  })

  return {
    all: products.length,
    pending: products.filter(p => p.status === 0).length,
    approved: products.filter(p => p.status === 1).length,
    rejected: products.filter(p => p.status === 2).length,
    sold: products.filter(p => p.status === 3).length
  }
})

const getProductIcon = (gameTypeName) => {
  if (!gameTypeName) return '🎮'
  const iconMap = {
    '王者荣耀': '👑',
    '英雄联盟': '⚔️',
    '和平精英': '🎯',
    '我的世界': '⛏️',
    '原神': '🌟',
    '崩坏': '💫'
  }
  return iconMap[gameTypeName] || '🎮'
}

import { getCurrentUser } from '@/utils/accountManager'

const getUser = () => {
  try {
    const user = getCurrentUser()
    if (user) {
      if (user.id) {
        user.id = Number(user.id)
      }
      return user
    }
    return null
  } catch (e) {
    return null
  }
}

const getList = async () => {
  loading.value = true
  try {
    const user = getUser()
    if (!user || !user.id) {
      allProducts.value = []
      return
    }
    const res = await request.get(`/product/myList/${user.id}`)
    allProducts.value = res.data || []
  } catch (err) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

const filteredProducts = computed(() => {
  const user = getUser()
  if (!user || !user.id) return []

  let result = allProducts.value.filter(product => {
    const sellerId = typeof product.sellerId === 'string' ? parseInt(product.sellerId, 10) : product.sellerId
    return sellerId === user.id
  })

  if (statusFilter.value !== 'all') {
    result = result.filter(product => product.status === parseInt(statusFilter.value, 10))
  }

  return result
})

const getStatusType = (status) => {
  return PRODUCT_STATUS_TYPE[status] || 'default'
}

const getStatusText = (status) => {
  return PRODUCT_STATUS_TEXT[status] || '未知'
}

const handleCancel = async (product) => {
  try {
    const statusText = product.status === 0 ? '撤销审核' : '下架商品'
    await ElMessageBox.confirm(`确定要${statusText}吗？`, '提示', {
      type: 'warning'
    })
    await request.put(`/product/audit/${product.id}/3`)
    ElMessage.success('操作成功')
    getList()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: var(--color-text-primary);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.data-table {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.price-text {
  color: #f56c6c;
  font-weight: bold;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.reject-reason {
  color: #f56c6c;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.reject-icon {
  font-size: 14px;
}

.no-reason {
  color: #c0c4cc;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  background: var(--color-bg-card);
  border-radius: 8px;
}

.empty-illustration {
  margin-bottom: 20px;
}

.empty-icon {
  font-size: 64px;
  display: block;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  color: var(--color-text-primary);
  margin-bottom: 10px;
}

.empty-desc {
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.header-left h1 {
  font-size: 24px;
  color: var(--color-text-primary);
  margin: 0;
}

.header-tip {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 5px 0 0 0;
}

.filter-section {
  margin-bottom: 20px;
}

.filter-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.active-tab {
  font-weight: 600;
}

.tab-count {
  display: inline-block;
  padding: 0 6px;
  margin-left: 4px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 10px;
}

.product-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-icon {
  font-size: 20px;
}

.product-title {
  font-weight: 500;
  color: var(--color-text-primary);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .filter-tabs {
    gap: 8px;
  }

  .empty-actions {
    flex-direction: column;
  }
}
</style>

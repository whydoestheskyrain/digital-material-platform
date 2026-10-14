<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>📦 商品审核管理</h1>
          <p class="header-desc">审核卖家发布的商品，管理商品上下架</p>
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
            <el-icon><CircleClose /></el-icon> 已拒绝 <span class="tab-count" v-if="statusCounts.rejected > 0">{{ statusCounts.rejected }}</span>
          </el-button>
          <el-button
            :type="statusFilter === '3' ? 'info' : 'default'"
            :class="{ 'active-tab': statusFilter === '3' }"
            @click="statusFilter = '3'"
          >
            <el-icon><Bottom /></el-icon> 已下架
          </el-button>
        </div>
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品标题..."
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
      </div>

      <div class="table-card">
        <el-table
          :data="filteredProducts"
          border
          stripe
          class="data-table"
          v-loading="loading"
          :header-cell-style="{ background: '#f8f9fc', color: '#475467', fontWeight: '600' }"
        >
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="title" label="商品信息" min-width="200">
            <template #default="scope">
              <div class="product-info">
                <span class="product-title">{{ scope.row.title }}</span>
                <span class="product-id">ID: {{ scope.row.id }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="gameServer" label="区服" width="120" align="center" />
          <el-table-column prop="price" label="价格" width="100" align="center">
            <template #default="scope">
              <span class="price-text">¥{{ scope.row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sellerId" label="卖家" width="100" align="center">
            <template #default="scope">
              <div class="seller-info">
                <el-icon><User /></el-icon> {{ scope.row.sellerId }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120" align="center">
            <template #default="scope">
              <el-tag
                v-if="scope.row.status === 0"
                type="warning"
                effect="dark"
                size="small"
              ><el-icon><Timer /></el-icon> 待审核</el-tag>
              <el-tag
                v-else-if="scope.row.status === 1"
                type="success"
                effect="dark"
                size="small"
              ><el-icon><CircleCheck /></el-icon> 已上架</el-tag>
              <el-tag
                v-else-if="scope.row.status === 2"
                type="danger"
                effect="dark"
                size="small"
              ><el-icon><CircleClose /></el-icon> 已拒绝</el-tag>
              <el-tag
                v-else-if="scope.row.status === 3"
                type="info"
                effect="dark"
                size="small"
              ><el-icon><Bottom /></el-icon> 已下架</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="accountUsername" label="账号信息" width="120" align="center">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="showAccount(scope.row)">
                <el-icon><View /></el-icon> 查看
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center">
            <template #default="scope">
              <div class="action-buttons">
                <el-button
                  size="small"
                  type="success"
                  v-if="scope.row.status === 0"
                  @click="approve(scope.row)"
                  :loading="scope.row.approving"
                >
                  <el-icon><Check /></el-icon> 通过
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  v-if="scope.row.status === 0"
                  @click="reject(scope.row)"
                >
                  <el-icon><Close /></el-icon> 拒绝
                </el-button>
                <el-button
                  size="small"
                  type="info"
                  plain
                  v-if="scope.row.status === 1"
                  @click="offline(scope.row)"
                >
                  <el-icon><Bottom /></el-icon> 下架
                </el-button>
                <el-button
                  size="small"
                  type="success"
                  v-if="scope.row.status === 3"
                  @click="approve(scope.row)"
                >
                  <el-icon><Top /></el-icon> 上架
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  @click="del(scope.row)"
                >
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="page"
            :page-sizes="[5, 10, 20, 50]"
            :page-size="size"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            background
          />
        </div>
      </div>

      <!-- 账号信息弹窗 -->
      <el-dialog
        v-model="accountDialogVisible"
        width="450px"
        class="account-dialog"
      >
        <template #header>
          <span class="dialog-title"><el-icon><Lock /></el-icon> 账号信息（仅管理员可见）</span>
        </template>
        <div class="account-info" v-if="currentProduct">
          <div class="info-item">
            <span class="label"><el-icon><Message /></el-icon> 游戏账号：</span>
            <span class="value">{{ currentProduct.accountUsername }}</span>
          </div>
          <div class="info-item">
            <span class="label"><el-icon><Key /></el-icon> 游戏密码：</span>
            <span class="value">{{ currentProduct.accountPassword }}</span>
          </div>
          <div class="info-item">
            <span class="label"><el-icon><Iphone /></el-icon> 绑定信息：</span>
            <span class="value">{{ currentProduct.bindInfo || '无' }}</span>
          </div>
        </div>
        <template #footer>
          <el-button @click="accountDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 拒绝原因弹窗 -->
      <el-dialog
        v-model="rejectDialogVisible"
        width="450px"
      >
        <template #header>
          <span class="dialog-title"><el-icon><CircleClose /></el-icon> 拒绝原因</span>
        </template>
        <div class="reject-form">
          <p class="reject-tip">请输入拒绝该商品的原因（选填），卖家会收到通知</p>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因..."
            maxlength="200"
            show-word-limit
          />
        </div>
        <template #footer>
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'


const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const statusFilter = ref('0')

const accountDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentProduct = ref(null)
const rejectReason = ref('')

const statusCounts = computed(() => {
  const counts = { all: 0, pending: 0, approved: 0, rejected: 0, offline: 0 }
  list.value.forEach(item => {
    counts.all++
    if (item.status === 0) counts.pending++
    else if (item.status === 1) counts.approved++
    else if (item.status === 2) counts.rejected++
    else if (item.status === 3) counts.offline++
  })
  return counts
})

const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/product/page', {
      params: { page: page.value, size: size.value }
    })
    list.value = res.data || []
    total.value = res.total || list.value.length
  } catch (err) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

const filteredProducts = computed(() => {
  let result = list.value
  if (statusFilter.value !== 'all') {
    result = result.filter(item => item.status === parseInt(statusFilter.value))
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(item =>
      item.title.toLowerCase().includes(keyword)
    )
  }
  return result
})

const handleSearch = () => {
  page.value = 1
  getList()
}

const handleSizeChange = (newSize) => {
  size.value = newSize
  getList()
}

const handleCurrentChange = (newPage) => {
  page.value = newPage
  getList()
}

const showAccount = (row) => {
  currentProduct.value = row
  accountDialogVisible.value = true
}

const approve = async (row) => {
  try {
    row.approving = true
    await request.put(`/product/audit/${row.id}/1`)
    ElMessage.success('审核通过，商品已上架')
    getList()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    row.approving = false
  }
}

const reject = (row) => {
  currentProduct.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  try {
    await request.put(`/product/audit/${currentProduct.value.id}/2`, {
      rejectReason: rejectReason.value
    })
    ElMessage.success('已拒绝该商品')
    rejectDialogVisible.value = false
    getList()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const offline = async (row) => {
  try {
    await request.put(`/product/audit/${row.id}/3`)
    ElMessage.success('商品已下架')
    getList()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const del = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除商品「${row.title}」吗？此操作不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    await request.delete('/product/delete/' + row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(getList)
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

.header-actions {
  display: flex;
  gap: 10px;
}

.header-actions .el-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
  background: var(--color-bg-card);
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  flex-wrap: wrap;
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

.search-bar {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 280px;
}

.table-card {
  background: var(--color-bg-card);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  padding: 20px;
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.data-table {
  border-radius: 8px;
}

.data-table :deep(.el-table__header th) {
  background: rgba(255,255,255,0.05) !important;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.product-id {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.seller-info {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-regular);
}

.dialog-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.price-text {
  color: #f56c6c;
  font-weight: bold;
  font-size: 15px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.account-info {
  padding: 10px 0;
}

.info-item {
  display: flex;
  padding: 14px 0;
  border-bottom: 1px solid #eee;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: 130px;
  color: var(--color-text-regular);
  font-weight: 500;
}

.info-item .label .el-icon {
  font-size: 15px;
  color: var(--color-primary);
}

.info-item .value {
  flex: 1;
  font-weight: 500;
  color: var(--color-text-primary);
  word-break: break-all;
}

.reject-form {
  padding: 10px 0;
}

.reject-tip {
  font-size: 13px;
  color: var(--color-text-secondary);
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
  }

  .search-bar {
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .pagination-wrapper {
    justify-content: center;
  }
}
</style>

<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="page-header">
        <h1>交易记录管理</h1>
        <div class="header-actions">
          <el-button type="primary" @click="toAdd">新增交易记录</el-button>
        </div>
      </div>

      <el-table 
        :data="list" 
        border 
        stripe 
        class="data-table"
        :empty-text="list.length === 0 ? '暂无交易记录' : ''"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单编号" width="150" />
        <el-table-column prop="buyerId" label="买家ID" width="100" />
        <el-table-column prop="sellerId" label="卖家ID" width="100" />
        <el-table-column prop="money" label="交易金额" width="120">
          <template #default="scope">
            <span :class="['amount-text', scope.row.type === 1 ? 'income' : 'refund']">
              {{ scope.row.type === 1 ? '+' : '-' }}¥{{ scope.row.money }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="交易类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.type === 1 ? 'success' : 'warning'">
              {{ scope.row.type === 1 ? '支付' : '退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="toEdit(scope.row.id)">编辑</el-button>
            <el-button size="small" type="danger" @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="page"
          :page-sizes="[5, 10, 20]"
          :page-size="size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const getList = async () => {
  const res = await request.get('/transaction/page', {
    params: { page: page.value, size: size.value }
  })
  list.value = res.data || []
  total.value = res.total || list.value.length
}

const toAdd = () => router.push('/transaction/add')
const toEdit = (id) => router.push('/transaction/edit/' + id)

const del = async (id) => {
  try {
    await request.get('/transaction/delete/' + id)
    ElMessage.success('删除成功')
    getList()
  } catch (err) {
    ElMessage.error('删除失败')
  }
}

const handleSizeChange = (newSize) => {
  size.value = newSize
  getList()
}

const handleCurrentChange = (newPage) => {
  page.value = newPage
  getList()
}

onMounted(getList)
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
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.data-table {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.amount-text {
  font-weight: bold;
}

.amount-text.income {
  color: #67c23a;
}

.amount-text.refund {
  color: #f56c6c;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
}
</style>

<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="page-header">
        <h1>用户管理</h1>
        <div class="header-actions">
          <el-button type="primary" @click="toAdd">新增用户</el-button>
          <el-button @click="$router.push('/admin/home')">返回控制台</el-button>
        </div>
      </div>

      <div class="search-bar">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索用户名或手机号..." 
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>

      <el-table 
        :data="filteredUsers" 
        border 
        stripe 
        class="data-table"
        :empty-text="filteredUsers.length === 0 ? '暂无用户' : ''"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.role?.toUpperCase() === 'ADMIN' ? 'danger' : 'success'">
              {{ scope.row.role?.toUpperCase() === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="toEdit(scope.row.id)">编辑</el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="del(scope.row.id)"
              :disabled="scope.row.role?.toUpperCase() === 'ADMIN'"
            >删除</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const getList = async () => {
  const res = await request.get('/user/page', {
    params: { page: page.value, size: size.value }
  })
  list.value = res.data || []
  total.value = res.total || list.value.length
}

const filteredUsers = computed(() => {
  if (!searchKeyword.value) return list.value
  const keyword = searchKeyword.value.toLowerCase()
  return list.value.filter(item => 
    item.username.toLowerCase().includes(keyword) ||
    item.phone?.toLowerCase().includes(keyword) ||
    item.nickname?.toLowerCase().includes(keyword)
  )
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

const toAdd = () => router.push('/user/add')
const toEdit = (id) => router.push('/user/edit/' + id)

const del = async (id) => {
  try {
    await request.get('/user/delete/' + id)
    ElMessage.success('删除成功')
    getList()
  } catch (err) {
    ElMessage.error('删除失败')
  }
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
  color: var(--color-text-primary);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  max-width: 400px;
}

.search-input {
  flex: 1;
}

.data-table {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
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
  
  .search-bar {
    max-width: 100%;
  }
}
</style>

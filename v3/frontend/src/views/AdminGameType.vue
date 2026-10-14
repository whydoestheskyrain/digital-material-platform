<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="page-header">
        <h1>游戏类型管理</h1>
        <div class="header-actions">
          <el-button type="primary" @click="openAddDialog">添加游戏类型</el-button>
        </div>
      </div>

      <el-table
        :data="gameTypes"
        border
        stripe
        class="data-table"
        v-loading="loading"
        :header-cell-style="{ background: '#f8f9fc', color: '#475467', fontWeight: '600' }"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="typeName" label="游戏类型名称" min-width="180">
          <template #default="scope">
            <div class="type-name-cell">
              <span class="type-icon">{{ scope.row.icon || '🎮' }}</span>
              <span class="type-name">{{ scope.row.typeName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" align="center">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.sortOrder || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              active-color="#67c23a"
              inactive-color="#f56c6c"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="primary" plain @click="openEditDialog(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(scope.row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="gameTypes.length === 0" class="empty-state">
        <div class="empty-icon">🎮</div>
        <p>暂无游戏类型</p>
        <el-button type="primary" @click="openAddDialog">添加类型</el-button>
      </div>

      <!-- 添加/编辑弹窗 -->
      <el-dialog :title="isEdit ? '编辑游戏类型' : '添加游戏类型'" v-model="dialogVisible" width="500px" class="game-type-dialog" destroy-on-close>
        <el-form :model="form" class="dialog-form" label-position="top">
          <el-form-item label="游戏类型名称" required>
            <el-input v-model="form.typeName" placeholder="请输入游戏类型名称" clearable>
              <template #prefix><span class="input-prefix-icon">{{ form.icon }}</span></template>
            </el-input>
          </el-form-item>
          <el-form-item label="图标选择">
            <div class="icon-picker">
              <span
                v-for="icon in iconOptions"
                :key="icon"
                :class="['icon-item', { active: form.icon === icon }]"
                @click="form.icon = icon"
              >{{ icon }}</span>
            </div>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" placeholder="数字越小越靠前" />
          </el-form-item>
          <el-form-item label="状态">
            <div class="status-switch">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-color="#67c23a" inactive-color="#f56c6c" />
              <span :class="['status-text', form.status === 1 ? 'active' : 'inactive']">
                {{ form.status === 1 ? '启用中' : '已禁用' }}
              </span>
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="save">确认保存</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const gameTypes = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  typeName: '',
  icon: '',
  sortOrder: 0,
  status: 1
})

const iconOptions = [
  '🎮', '🎯', '🎲', '🎪', '🎭',
  '⚽', '🏀', '🏈', '⚾', '🎾',
  '🎱', '🏓', '🏸', '🎳', '⛳',
  '🚀', '🛸', '🌟', '🌙', '🔥',
  '💎', '👑', '⚡', '💪', '⭐'
]

const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/gameType/list')
    gameTypes.value = res.data || []
  } catch (err) {
    ElMessage.error('获取游戏类型失败')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = {
    typeName: '',
    icon: '',
    sortOrder: 0,
    status: 1
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    typeName: row.typeName,
    icon: row.icon,
    sortOrder: row.sortOrder || 0,
    status: row.status
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.typeName.trim()) {
    ElMessage.warning('请输入游戏类型名称')
    return
  }

  try {
    if (isEdit.value) {
      await request.put('/gameType/update', form.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/gameType/add', form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    getList()
  } catch (err) {
    ElMessage.error(isEdit.value ? '修改失败' : '添加失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该游戏类型吗？删除后不可恢复！', '危险操作', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/gameType/delete/${id}`)
    ElMessage.success('删除成功')
    getList()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleStatusChange = async (row) => {
  try {
    await request.put('/gameType/update', {
      id: row.id,
      typeName: row.typeName,
      icon: row.icon,
      sortOrder: row.sortOrder,
      status: row.status
    })
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch (err) {
    ElMessage.error('状态更新失败')
    getList()
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

.data-table {
  background: var(--color-bg-card);
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  border: 1px solid var(--color-border);
}

.icon-text {
  font-size: 20px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  background: var(--color-bg-card);
  border-radius: 8px;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.empty-state p {
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}

.dialog-form {
  padding: 15px 0;
}

.form-item {
  margin-bottom: 20px;
}

.form-item .el-form-item__label {
  font-weight: 500;
  color: var(--color-text-primary);
}

.form-input {
  width: 100%;
  height: 40px;
  border-radius: 8px;
}

.icon-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

.icon-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  background: var(--color-bg-card);
  border: 2px solid transparent;
}

.icon-item:hover {
  background: rgba(56, 189, 248, 0.12);
  border-color: #38bdf8;
  transform: scale(1.1);
}

.icon-item.active {
  background: rgba(56, 189, 248, 0.12);
  border-color: #38bdf8;
  box-shadow: 0 0 8px rgba(56, 189, 248, 0.3);
}

.status-switch {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-text {
  font-size: 14px;
  font-weight: 500;
}

.status-text.active {
  color: #67c23a;
}

.status-text.inactive {
  color: #f56c6c;
}

.type-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-icon {
  font-size: 24px;
}

.type-name {
  font-weight: 500;
  color: var(--color-text-primary);
}

.input-prefix-icon {
  font-size: 18px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .icon-picker {
    max-height: 150px;
    overflow-y: auto;
  }
}

.status-text.inactive {
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 15px 20px;
  border-top: 1px solid #eee;
}

.btn-cancel {
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 14px;
}

.btn-save {
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 14px;
  background: var(--color-primary-gradient);
  border: none;
}

.btn-save:hover {
  background: var(--color-primary-gradient-hover);
}

.game-type-dialog .el-dialog__header {
  background: var(--color-primary-gradient);
  padding: 16px 20px;
  border-radius: 12px 12px 0 0;
}

.game-type-dialog .el-dialog__title {
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.game-type-dialog .el-dialog__close {
  color: white;
}

.game-type-dialog .el-dialog__body {
  padding: 20px;
}

.game-type-dialog .el-dialog__footer {
  padding: 0;
  margin-top: 0;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .icon-picker {
    gap: 8px;
  }
  
  .icon-item {
    width: 32px;
    height: 32px;
    font-size: 18px;
  }
}
</style>

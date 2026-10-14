<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="admin-header">
        <div class="header-left">
          <h1>🎮 桌宠皮肤管理</h1>
          <p class="welcome-text">管理AI桌宠的皮肤素材</p>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="showAddDialog = true">
            <span>+</span> 添加皮肤
          </el-button>
          <el-button @click="$router.push('/admin/home')">
            <span>←</span> 返回控制台
          </el-button>
        </div>
      </div>

      <div class="table-container">
        <el-table :data="skinList" border size="small">
          <el-table-column label="ID" prop="id" width="60" />
          <el-table-column label="皮肤名称" prop="skinName" width="120" />
          <el-table-column label="预览" width="100">
            <template #default="scope">
              <img :src="scope.row.mainImg" class="preview-img" />
            </template>
          </el-table-column>
          <el-table-column label="待机动画" prop="idleAni" min-width="150" />
          <el-table-column label="点击动画" prop="clickAni" min-width="150" />
          <el-table-column label="默认" width="80">
            <template #default="scope">
              <el-tag v-if="scope.row.isDefault === 1" type="success">是</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.status === 1 ? '启用' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="160">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="scope">
              <el-button size="small" @click="setDefault(scope.row.id)" v-if="scope.row.isDefault !== 1">
                设为默认
              </el-button>
              <el-button type="primary" size="small" @click="editSkin(scope.row)">编辑</el-button>
              <el-button 
                type="warning" 
                size="small" 
                @click="toggleStatus(scope.row)"
              >
                {{ scope.row.status === 1 ? '下架' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="deleteSkin(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-dialog v-model="showAddDialog" :title="editingSkin ? '编辑皮肤' : '添加皮肤'" width="500px">
        <el-form :model="form" label-width="100px">
          <el-form-item label="皮肤名称" required>
            <el-input v-model="form.skinName" placeholder="输入皮肤名称" />
          </el-form-item>
          <el-form-item label="待机动画(WebP)" required>
            <div class="upload-row">
              <el-input v-model="form.idleAni" placeholder="/images/pet/xxx-idle.webp" />
              <el-button type="primary" @click="uploadFile('idle')">上传</el-button>
            </div>
          </el-form-item>
          <el-form-item label="点击动画(WebP)">
            <div class="upload-row">
              <el-input v-model="form.clickAni" placeholder="/images/pet/xxx-click.webp" />
              <el-button type="primary" @click="uploadFile('click')">上传</el-button>
            </div>
          </el-form-item>
          <el-form-item label="预览图" required>
            <div class="upload-row">
              <el-input v-model="form.mainImg" placeholder="/images/pet/xxx-preview.png" />
              <el-button type="primary" @click="uploadFile('preview')">上传</el-button>
            </div>
          </el-form-item>
          <el-form-item label="设为默认">
            <el-switch v-model="form.isDefault" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" active-value="1" inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="saveSkin">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const skinList = ref([])
const showAddDialog = ref(false)
const editingSkin = ref(null)
const form = ref({
  skinName: '',
  idleAni: '',
  clickAni: '',
  mainImg: '',
  isDefault: '0',
  status: '1'
})

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const loadSkins = async () => {
  try {
    const res = await request.get('/pet/skin/list')
    if (res.code === 200) {
      skinList.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载皮肤列表失败')
  }
}

const saveSkin = async () => {
  if (!form.value.skinName || !form.value.idleAni || !form.value.mainImg) {
    ElMessage.warning('请填写完整信息')
    return
  }

  const data = {
    skinName: form.value.skinName,
    idleAni: form.value.idleAni,
    clickAni: form.value.clickAni,
    mainImg: form.value.mainImg,
    isDefault: parseInt(form.value.isDefault),
    status: parseInt(form.value.status)
  }

  if (editingSkin.value) {
    data.id = editingSkin.value.id
    try {
      await request.put('/pet/skin/update', data)
      ElMessage.success('更新成功')
    } catch (e) {
      ElMessage.error('更新失败')
    }
  } else {
    try {
      await request.post('/pet/skin/add', data)
      ElMessage.success('添加成功')
    } catch (e) {
      ElMessage.error('添加失败')
    }
  }

  showAddDialog.value = false
  editingSkin.value = null
  resetForm()
  loadSkins()
}

const editSkin = (skin) => {
  editingSkin.value = skin
  form.value = {
    skinName: skin.skinName,
    idleAni: skin.idleAni,
    clickAni: skin.clickAni || '',
    mainImg: skin.mainImg,
    isDefault: skin.isDefault?.toString() || '0',
    status: skin.status?.toString() || '1'
  }
  showAddDialog.value = true
}

const setDefault = async (id) => {
  try {
    await request.post(`/pet/skin/setDefault/${id}`)
    ElMessage.success('设置成功')
    loadSkins()
  } catch (e) {
    ElMessage.error('设置失败')
  }
}

const toggleStatus = async (skin) => {
  const newStatus = skin.status === 1 ? 0 : 1
  try {
    await request.put('/pet/skin/update', {
      id: skin.id,
      skinName: skin.skinName,
      idleAni: skin.idleAni,
      clickAni: skin.clickAni,
      mainImg: skin.mainImg,
      status: newStatus
    })
    ElMessage.success(newStatus === 1 ? '已启用' : '已下架')
    loadSkins()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteSkin = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该皮肤吗？', '提示', {
      type: 'danger'
    })
    await request.delete(`/pet/skin/delete/${id}`)
    ElMessage.success('删除成功')
    loadSkins()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const resetForm = () => {
  form.value = {
    skinName: '',
    idleAni: '',
    clickAni: '',
    mainImg: '',
    isDefault: '0',
    status: '1'
  }
}

const uploadFile = async (type) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = type === 'preview' ? 'image/png,image/jpeg' : 'image/webp'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    const formData = new FormData()
    formData.append('file', file)

    try {
      const res = await request.post('/api/upload/pet', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (res.code === 200) {
        if (type === 'idle') form.value.idleAni = res.data
        else if (type === 'click') form.value.clickAni = res.data
        else if (type === 'preview') form.value.mainImg = res.data
        ElMessage.success('上传成功')
      }
    } catch (e) {
      ElMessage.error('上传失败')
    }
  }
  input.click()
}

onMounted(() => {
  loadSkins()
})
</script>

<style scoped>
.page-wrapper {
  min-height: 100vh;
  padding: 20px 0;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 20px 30px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.header-left h1 {
  font-size: 22px;
  color: #333;
  margin-bottom: 4px;
}

.welcome-text {
  font-size: 14px;
  color: #999;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  overflow: hidden;
}

.preview-img {
  width: 50px;
  height: 50px;
  object-fit: contain;
  border-radius: 4px;
}

.upload-row {
  display: flex;
  gap: 8px;
}

.upload-row .el-input {
  flex: 1;
}
</style>
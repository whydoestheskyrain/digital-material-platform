<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="admin-header">
        <div class="header-left">
          <h1>📝 反馈管理</h1>
          <p class="welcome-text">查看和处理用户提交的反馈</p>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="goBack">
            <span>←</span> 返回控制台
          </el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-info">
            <div class="stat-value">{{ feedbacks.length }}</div>
            <div class="stat-label">总反馈数</div>
          </div>
        </div>
        <div class="stat-card stat-card--highlight">
          <div class="stat-info">
            <div class="stat-value">{{ pendingCount }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </div>
        <div class="stat-card stat-card--success">
          <div class="stat-info">
            <div class="stat-value">{{ repliedCount }}</div>
            <div class="stat-label">已回复</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-info">
            <div class="stat-value">{{ closedCount }}</div>
            <div class="stat-label">已关闭</div>
          </div>
        </div>
      </div>

      <!-- 筛选 -->
      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="全部状态" size="small" style="width:120px">
          <el-option label="全部" :value="-1" />
          <el-option label="待处理" :value="0" />
          <el-option label="已回复" :value="1" />
          <el-option label="已关闭" :value="2" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索反馈标题" size="small" style="width:200px" @keyup.enter="loadFeedbacks" />
        <el-button type="primary" size="small" @click="loadFeedbacks">搜索</el-button>
      </div>

      <!-- 反馈列表 -->
      <div class="table-container">
        <el-table :data="filteredFeedbacks" border size="small">
          <el-table-column label="ID" prop="id" width="60" />
          <el-table-column label="用户ID" prop="userId" width="80" />
          <el-table-column label="标题" prop="title" min-width="150" />
          <el-table-column label="内容" prop="content" min-width="200">
            <template #default="scope">
              <div class="content-preview">{{ scope.row.content }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" width="100">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="160">
            <template #default="scope">
              {{ formatTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button 
                type="primary" 
                size="small" 
                @click="showReplyDialog(scope.row)"
                v-if="scope.row.status === 0 || scope.row.status === 1"
              >回复</el-button>
              <el-button 
                type="warning" 
                size="small" 
                @click="closeFeedback(scope.row.id)"
                v-if="scope.row.status !== 2"
              >关闭</el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="deleteFeedback(scope.row.id)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 回复弹窗 -->
      <el-dialog v-model="showReply" title="回复反馈" width="500px">
        <div v-if="currentFeedback">
          <div class="feedback-detail">
            <div class="detail-title">📌 {{ currentFeedback.title }}</div>
            <div class="detail-content">{{ currentFeedback.content }}</div>
            <div v-if="currentFeedback.reply" class="detail-reply">
              <div class="reply-label">已有回复：</div>
              <div class="reply-text">{{ currentFeedback.reply }}</div>
            </div>
          </div>
          <el-form-item label="回复内容">
            <textarea v-model="replyContent" rows="4" placeholder="输入回复内容" class="custom-textarea" />
          </el-form-item>
        </div>
        <template #footer>
          <el-button @click="showReply = false">取消</el-button>
          <el-button type="primary" @click="submitReply" :loading="replying">提交回复</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const feedbacks = ref([])
const statusFilter = ref(-1)
const keyword = ref('')
const showReply = ref(false)
const currentFeedback = ref(null)
const replyContent = ref('')
const replying = ref(false)

const pendingCount = computed(() => feedbacks.value.filter(f => f.status === 0).length)
const repliedCount = computed(() => feedbacks.value.filter(f => f.status === 1).length)
const closedCount = computed(() => feedbacks.value.filter(f => f.status === 2).length)

const filteredFeedbacks = computed(() => {
  let result = feedbacks.value
  if (statusFilter.value !== -1) {
    result = result.filter(f => f.status === statusFilter.value)
  }
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    result = result.filter(f => f.title.toLowerCase().includes(kw))
  }
  return result
})

const getStatusText = (status) => {
  if (status === 0) return '待处理'
  if (status === 1) return '已回复'
  if (status === 2) return '已关闭'
  return '未知'
}

const getStatusTagType = (status) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return ''
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const goBack = () => {
  router.push('/admin/home')
}

const loadFeedbacks = async () => {
  try {
    const res = await request.get('/feedback/admin/list')
    if (res.code === 200) {
      feedbacks.value = res.data || []
    }
  } catch (e) {
    console.error('加载反馈失败:', e)
    ElMessage.error('加载反馈失败')
  }
}

const showReplyDialog = (feedback) => {
  currentFeedback.value = feedback
  replyContent.value = feedback.reply || ''
  showReply.value = true
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    const res = await request.post(`/feedback/reply/${currentFeedback.value.id}`, null, {
      params: { reply: replyContent.value }
    })
    if (res.code === 200) {
      ElMessage.success('回复成功')
      showReply.value = false
      loadFeedbacks()
    } else {
      ElMessage.error(res.msg || '回复失败')
    }
  } catch (e) {
    ElMessage.error('回复失败')
  } finally {
    replying.value = false
  }
}

const closeFeedback = async (id) => {
  try {
    await ElMessageBox.confirm('确定要关闭该反馈吗？', '提示', {
      type: 'warning'
    })
    await request.put(`/feedback/close/${id}`)
    ElMessage.success('已关闭')
    loadFeedbacks()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const deleteFeedback = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该反馈吗？此操作不可恢复。', '提示', {
      type: 'danger'
    })
    await request.delete(`/feedback/delete/${id}`)
    ElMessage.success('已删除')
    loadFeedbacks()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadFeedbacks()
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  overflow: hidden;
}

.content-preview {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.feedback-detail {
  margin-bottom: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.detail-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 12px;
}

.detail-reply {
  padding: 12px;
  background: #e3f2fd;
  border-radius: 6px;
}

.reply-label {
  font-size: 12px;
  color: #1976d2;
  margin-bottom: 4px;
}

.reply-text {
  font-size: 13px;
  color: #333;
}

.custom-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
}

.custom-textarea:focus {
  outline: none;
  border-color: #409eff;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  text-align: center;
}

.stat-card--highlight {
  background: linear-gradient(135deg, #fff9f0, #fff5eb);
}

.stat-card--highlight .stat-value {
  color: #ff6b00;
}

.stat-card--success {
  background: linear-gradient(135deg, #f0fff4, #f6ffed);
}

.stat-card--success .stat-value {
  color: #52c41a;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}
</style>

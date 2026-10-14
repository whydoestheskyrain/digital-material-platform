<template>
  <div class="notification-container">
    <div class="notification-header">
      <div class="header-left" @click="goBack">
        <el-icon class="back-icon"><ArrowLeft /></el-icon>
        <span>返回</span>
      </div>
      <div class="header-title">消息中心</div>
      <div class="header-right">
        <el-button type="text" @click="markAllRead" v-if="activeTab === 'notifications' && unreadTotal > 0">
          全部已读
        </el-button>
      </div>
    </div>

    <div class="notification-tabs">
      <div
        class="tab-item"
        :class="{ active: activeTab === 'notifications' }"
        @click="switchTab('notifications')"
      >
        <span>消息通知</span>
        <span v-if="unreadTotal > 0" class="badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'feedback' }"
        @click="switchTab('feedback')"
      >
        <span>我的反馈</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'broadcast' }"
        v-if="isAdmin"
        @click="switchTab('broadcast')"
      >
        <span>管理员广播</span>
      </div>
    </div>

    <!-- 消息通知：会话列表 + 会话详情 -->
    <div v-show="activeTab === 'notifications'" class="notification-content">
      <div class="message-pane" :class="{ 'detail-open': currentThread }">
        <!-- 会话列表 -->
        <div class="thread-pane">
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>
          <div v-else-if="threads.length === 0" class="empty-state">
            <p>暂无消息</p>
            <p class="empty-hint">下单、发货、退款等交易进度都会在这里通知你</p>
          </div>
          <div v-else class="thread-list">
            <div
              v-for="item in threads"
              :key="item.threadKey"
              class="thread-item"
              :class="{ active: currentThread && currentThread.threadKey === item.threadKey }"
              @click="openThread(item)"
            >
              <div class="thread-icon">
                <el-icon><component :is="typeIcon(item.type)" /></el-icon>
              </div>
              <div class="thread-body">
                <div class="thread-title-line">
                  <span class="thread-title">{{ item.title }}</span>
                  <span class="thread-time">{{ formatTime(item.lastTime) }}</span>
                </div>
                <div class="thread-last">{{ item.lastContent }}</div>
              </div>
              <span v-if="item.unreadCount > 0" class="thread-badge">
                {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
              </span>
            </div>
          </div>
        </div>

        <!-- 会话详情（时间轴） -->
        <div class="detail-pane">
          <template v-if="currentThread">
            <div class="detail-header">
              <el-icon class="detail-back" @click="closeDetail"><ArrowLeft /></el-icon>
              <span class="detail-icon">
                <el-icon><component :is="typeIcon(currentThread.type)" /></el-icon>
              </span>
              <div class="detail-title-wrap">
                <div class="detail-title">{{ currentThread.title }}</div>
                <div class="detail-sub">共 {{ currentThread.totalCount }} 条消息</div>
              </div>
              <span
                v-if="currentThread.type === 'order' && currentThread.orderId"
                class="detail-order-link"
                @click="goOrderDetail(currentThread.orderId)"
              >
                查看订单详情 →
              </span>
            </div>
            <div v-if="detailLoading" class="loading-state">
              <div class="loading-spinner"></div>
              <p>加载中...</p>
            </div>
            <div v-else-if="threadMessages.length === 0" class="empty-state">
              <p>暂无消息</p>
            </div>
            <div v-else class="timeline">
              <div v-for="msg in threadMessages" :key="msg.id" class="timeline-item">
                <div class="timeline-dot" :class="{ unread: msg.status === 0 }"></div>
                <div
                  class="timeline-card"
                  :class="{ unread: msg.status === 0, clickable: currentThread.type === 'order' && msg.orderId }"
                  @click="currentThread.type === 'order' && msg.orderId && goOrderDetail(msg.orderId)"
                >
                  <div class="timeline-card-title">{{ msg.title }}</div>
                  <div class="timeline-card-content">{{ msg.content }}</div>
                  <div class="timeline-card-time">
                    {{ formatFullTime(msg.createTime) }}
                    <span v-if="currentThread.type === 'order' && msg.orderId" class="card-order-link">查看订单 →</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="detail-placeholder">
            <div class="placeholder-icon">💬</div>
            <p>选择左侧会话查看消息详情</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 反馈列表 -->
    <div v-show="activeTab === 'feedback'" class="notification-content">
      <div class="feedback-header">
        <el-button type="primary" @click="showFeedbackForm = true">提交反馈</el-button>
      </div>
      <div v-if="feedbackLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="feedbacks.length === 0" class="empty-state">
        <p>暂无反馈</p>
        <p class="empty-hint">提交问题反馈后，管理员的处理结果会显示在这里</p>
      </div>
      <div v-else class="feedback-list">
        <div v-for="item in feedbacks" :key="item.id" class="feedback-item">
          <div class="feedback-header">
            <div class="feedback-title">{{ item.title }}</div>
            <span class="status-tag" :class="getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </span>
          </div>
          <div class="feedback-content">{{ item.content }}</div>
          <div v-if="item.reply" class="feedback-reply">
            <div class="reply-label">管理员回复：</div>
            <div class="reply-content">{{ item.reply }}</div>
          </div>
          <div class="feedback-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
    </div>

    <!-- 管理员广播 -->
    <div v-show="activeTab === 'broadcast'" class="notification-content">
      <div class="broadcast-form">
        <h3 class="form-title">📢 发送广播</h3>
        <el-form :model="broadcastForm" label-width="80px">
          <el-form-item label="标题">
            <el-input v-model="broadcastForm.title" placeholder="输入广播标题" />
          </el-form-item>
          <el-form-item label="内容">
            <textarea v-model="broadcastForm.content" rows="6" placeholder="输入广播内容" class="custom-textarea" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="sendBroadcast" :loading="broadcasting">
              发送广播
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 提交反馈弹窗 -->
    <el-dialog v-model="showFeedbackForm" title="提交反馈" width="500px">
      <el-form :model="feedbackForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="feedbackForm.title" placeholder="输入反馈标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <textarea v-model="feedbackForm.content" rows="6" placeholder="详细描述您的问题或建议" class="custom-textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFeedbackForm = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback" :loading="submitting">
          提交反馈
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getCurrentUser } from '@/utils/accountManager'

const router = useRouter()
const currentUser = getCurrentUser()
const isAdmin = currentUser?.role?.toUpperCase() === 'ADMIN'

const activeTab = ref('notifications')

// 会话相关
const threads = ref([])
const loading = ref(false)
const currentThread = ref(null)
const threadMessages = ref([])
const detailLoading = ref(false)

// 反馈相关
const feedbacks = ref([])
const feedbackLoading = ref(false)
const showFeedbackForm = ref(false)
const feedbackForm = ref({ title: '', content: '' })
const submitting = ref(false)

// 广播相关
const broadcastForm = ref({ title: '', content: '' })
const broadcasting = ref(false)

const unreadTotal = computed(() =>
  threads.value.reduce((sum, t) => sum + (t.unreadCount || 0), 0)
)

const typeIcon = (type) => {
  const map = {
    order: 'ShoppingBag',
    feedback: 'ChatDotRound',
    broadcast: 'Promotion',
    system: 'Message'
  }
  return map[type] || 'Message'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const formatFullTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { hour12: false })
}

const getStatusText = (status) => {
  if (status === 0) return '待处理'
  if (status === 1) return '已回复'
  if (status === 2) return '已关闭'
  return '未知'
}

const getStatusClass = (status) => {
  if (status === 0) return 'pending'
  if (status === 1) return 'replied'
  if (status === 2) return 'closed'
  return ''
}

const goBack = () => {
  router.back()
}

// ====================== 会话 ======================

const loadThreads = async () => {
  if (!currentUser) return
  loading.value = true
  try {
    const res = await request.get(`/notification/threads/${currentUser.id}`)
    if (res.code === 200) {
      threads.value = res.data || []
    }
  } catch (e) {
    console.error('加载会话列表失败:', e)
  } finally {
    loading.value = false
  }
}

const openThread = async (thread) => {
  currentThread.value = thread
  threadMessages.value = []
  detailLoading.value = true

  // 1) 先标记整组已读（必须在拉详情之前完成，否则时间轴仍显示未读样式）
  try {
    await request.put(`/notification/readThread/${currentUser.id}/${thread.threadKey}`)
    // 本地同步：会话角标与顶部 tab 角标立即清零
    thread.unreadCount = 0
    // 通知首页铃铛等其他页面即时刷新未读数
    window.dispatchEvent(new CustomEvent('notification:read', { detail: { userId: currentUser.id } }))
  } catch (e) {
    console.error('标记会话已读失败:', e)
  }

  // 2) 再拉取消息详情，此时拿到的 status 已是已读；已读失败也不影响查看消息
  try {
    const msgRes = await request.get(`/notification/thread/${currentUser.id}/${thread.threadKey}`)
    if (msgRes.code === 200) {
      threadMessages.value = msgRes.data || []
      // 时间轴按时间正序，打开后定位到最新一条（底部）
      await nextTick()
      const lastItem = document.querySelector('.timeline-item:last-child')
      if (lastItem) lastItem.scrollIntoView({ behavior: 'auto', block: 'end' })
    }
  } catch (e) {
    console.error('加载会话详情失败:', e)
  } finally {
    detailLoading.value = false
  }
}

const closeDetail = () => {
  currentThread.value = null
  threadMessages.value = []
}

const goOrderDetail = (orderId) => {
  router.push(`/order/detail/${orderId}`)
}

const markAllRead = async () => {
  if (!currentUser) return
  try {
    await request.put(`/notification/readAll/${currentUser.id}`)
    threads.value.forEach(t => { t.unreadCount = 0 })
    window.dispatchEvent(new CustomEvent('notification:read', { detail: { userId: currentUser.id } }))
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    console.error('全部已读失败:', e)
  }
}

// ====================== 反馈 ======================

const loadFeedbacks = async () => {
  if (!currentUser) return
  feedbackLoading.value = true
  try {
    const res = await request.get(`/feedback/list/${currentUser.id}`)
    if (res.code === 200) {
      feedbacks.value = res.data || []
    }
  } catch (e) {
    console.error('加载反馈失败:', e)
  } finally {
    feedbackLoading.value = false
  }
}

const submitFeedback = async () => {
  if (!feedbackForm.value.title || !feedbackForm.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    const res = await request.post('/feedback/submit', {
      userId: currentUser.id,
      title: feedbackForm.value.title,
      content: feedbackForm.value.content
    })
    if (res.code === 200) {
      ElMessage.success('反馈提交成功')
      showFeedbackForm.value = false
      feedbackForm.value = { title: '', content: '' }
      loadFeedbacks()
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// ====================== 广播 ======================

const sendBroadcast = async () => {
  if (!broadcastForm.value.title || !broadcastForm.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  broadcasting.value = true
  try {
    const res = await request.post('/admin/stats/broadcast', null, {
      params: {
        title: broadcastForm.value.title,
        content: broadcastForm.value.content
      }
    })
    if (res.success || res.code === 200) {
      ElMessage.success('广播发送成功')
      broadcastForm.value = { title: '', content: '' }
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    broadcasting.value = false
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'feedback' && feedbacks.value.length === 0) {
    loadFeedbacks()
  }
  if (tab !== 'notifications') {
    closeDetail()
  }
}

onMounted(() => {
  loadThreads()
})
</script>

<style scoped>
.notification-container {
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(124,58,237,0.12) 0%, rgba(34,211,238,0.06) 100%);
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--color-primary-gradient);
  color: white;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
}

.back-icon {
  font-size: 18px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.notification-tabs {
  display: flex;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 24px;
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text-regular);
  position: relative;
  transition: all 0.3s;
}

.tab-item.active {
  color: var(--color-primary);
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-primary);
}

.badge {
  background: #ff6b6b;
  color: white;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.notification-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
  text-align: center;
}

.empty-state p {
  margin: 0;
}

.empty-hint {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 6px !important;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== 会话列表 + 详情双栏 ========== */
.message-pane {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.thread-pane {
  width: 340px;
  flex-shrink: 0;
}

.thread-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.thread-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: var(--color-bg-card);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid var(--color-border);
}

.thread-item:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.thread-item.active {
  border-color: var(--color-primary);
  background: rgba(124, 58, 237, 0.18);
}

.thread-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

.thread-icon .el-icon {
  font-size: 19px;
  color: var(--color-primary);
}

.thread-body {
  flex: 1;
  min-width: 0;
}

.thread-title-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.thread-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.thread-time {
  font-size: 11px;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.thread-last {
  font-size: 12px;
  color: var(--color-text-regular);
  line-height: 1.5;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.thread-badge {
  background: #ff6b6b;
  color: white;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 18px;
  height: 18px;
  line-height: 16px;
  text-align: center;
  flex-shrink: 0;
  align-self: center;
}

/* ========== 会话详情 ========== */
.detail-pane {
  flex: 1;
  min-width: 0;
  background: var(--color-bg-card);
  border-radius: 12px;
  min-height: 480px;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.detail-back {
  display: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--color-text-regular);
  padding: 0 4px;
}

.detail-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

.detail-icon .el-icon {
  font-size: 19px;
  color: var(--color-primary);
}

.detail-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-sub {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.detail-title-wrap {
  flex: 1;
  min-width: 0;
}

.detail-order-link {
  flex-shrink: 0;
  font-size: 13px;
  color: #38bdf8;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 14px;
  border: 1px solid #d9ecff;
  background: #ecf5ff;
  transition: all 0.2s;
}

.detail-order-link:hover {
  background: #38bdf8;
  color: #fff;
}

.detail-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  gap: 12px;
}

.placeholder-icon {
  font-size: 48px;
}

/* ========== 时间轴 ========== */
.timeline {
  padding: 20px;
}

.timeline-item {
  position: relative;
  padding-left: 26px;
  padding-bottom: 16px;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 16px;
  bottom: 0;
  width: 2px;
  background: var(--color-border);
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 6px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-border);
  border: 2px solid var(--color-bg-card);
  box-shadow: 0 0 0 1px var(--color-border);
}

.timeline-dot.unread {
  background: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.timeline-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 12px 14px;
}

.timeline-card.unread {
  background: rgba(124, 58, 237, 0.18);
  border-color: var(--color-primary);
}

.timeline-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.timeline-card-content {
  font-size: 13px;
  color: var(--color-text-regular);
  line-height: 1.6;
  word-break: break-all;
}

.timeline-card-time {
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-top: 8px;
}

.timeline-card.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.timeline-card.clickable:hover {
  border-color: #38bdf8;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.card-order-link {
  margin-left: 8px;
  color: #38bdf8;
  font-size: 12px;
}

/* ========== 反馈列表 ========== */
.feedback-header {
  margin-bottom: 16px;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-item {
  padding: 16px;
  background: var(--color-bg-card);
  border-radius: 12px;
}

.feedback-item .feedback-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  border: none;
  padding: 0;
}

.feedback-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}

.status-tag.pending {
  background: rgba(251, 191, 36, 0.12);
  color: #fbbf24;
}

.status-tag.replied {
  background: rgba(56, 189, 248, 0.12);
  color: #38bdf8;
}

.status-tag.closed {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text-secondary);
}

.feedback-content {
  font-size: 13px;
  color: var(--color-text-regular);
  line-height: 1.5;
  margin-bottom: 8px;
}

.feedback-reply {
  background: rgba(255, 255, 255, 0.05);
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.reply-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.reply-content {
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.5;
}

.feedback-time {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* ========== 广播表单 ========== */
.broadcast-form {
  background: var(--color-bg-card);
  padding: 24px;
  border-radius: 12px;
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.custom-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
  transition: border-color 0.3s;
}

.custom-textarea:focus {
  outline: none;
  border-color: #38bdf8;
}

.custom-textarea::placeholder {
  color: #c0c4cc;
}

/* ========== 平板/手机（≤992）：会话列表与详情上下堆叠、互斥切换 ========== */
@media (max-width: 992px) {
  .notification-content {
    padding: 14px;
  }

  .message-pane {
    flex-direction: column;
  }

  .thread-pane {
    width: 100%;
  }

  .detail-pane {
    display: none;
  }

  .message-pane.detail-open .thread-pane {
    display: none;
  }

  .message-pane.detail-open .detail-pane {
    display: flex;
  }

  .detail-back {
    display: inline-flex;
  }
}
</style>

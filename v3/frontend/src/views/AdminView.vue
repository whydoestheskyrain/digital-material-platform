<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="admin-header">
        <div class="header-left">
          <h1>👋 管理员控制台</h1>
          <p class="welcome-text">欢迎回来，查看平台运营数据</p>
        </div>
        <div class="header-right">
          <el-button type="primary" plain @click="goHome">
            <el-icon><HomeFilled /></el-icon> 返回首页
          </el-button>
          <el-button type="danger" @click="logout">
            <el-icon><SwitchButton /></el-icon> 退出登录
          </el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card" @click="$router.push('/admin/user')">
          <div class="stat-icon users-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <span v-if="loading">--</span>
              <span v-else>{{ userCount }}</span>
            </div>
            <div class="stat-label">用户总数</div>
          </div>
          <div class="stat-trend up">
            <el-icon><TrendCharts /></el-icon>
          </div>
        </div>

        <div class="stat-card" @click="$router.push('/admin/product')">
          <div class="stat-icon product-icon">
            <el-icon><Goods /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <span v-if="loading">--</span>
              <span v-else>{{ productCount }}</span>
            </div>
            <div class="stat-label">商品总数</div>
          </div>
          <div class="stat-trend">
            <el-icon><DataLine /></el-icon>
          </div>
        </div>

        <div class="stat-card stat-card--highlight" @click="$router.push('/admin/product')">
          <div class="stat-icon audit-icon">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <span v-if="loading">--</span>
              <span v-else>{{ pendingCount }}</span>
            </div>
            <div class="stat-label">待审核商品</div>
          </div>
          <div class="stat-badge" v-if="pendingCount > 0">
            {{ pendingCount > 99 ? '99+' : pendingCount }}
          </div>
        </div>

        <div class="stat-card" @click="$router.push('/admin/order')">
          <div class="stat-icon order-icon">
            <el-icon><ShoppingCart /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <span v-if="loading">--</span>
              <span v-else>{{ orderCount }}</span>
            </div>
            <div class="stat-label">订单总数</div>
          </div>
          <div class="stat-trend">
            <el-icon><List /></el-icon>
          </div>
        </div>

        <div class="stat-card stat-card--success">
          <div class="stat-icon revenue-icon">
            <el-icon><Money /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <span v-if="loading">--</span>
              <span v-else>¥{{ totalRevenue.toFixed(2) }}</span>
            </div>
            <div class="stat-label">总收入</div>
          </div>
          <div class="stat-trend up">
            <el-icon><TrendCharts /></el-icon>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-grid">
        <div class="chart-card">
          <h3 class="chart-title">📊 订单状态分布</h3>
          <div ref="orderStatusChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <h3 class="chart-title">📈 近7日交易额</h3>
          <div ref="revenueChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <h3 class="chart-title">🎮 商品分类分布</h3>
          <div ref="productChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <h3 class="chart-title">📱 用户增长</h3>
          <div ref="userChart" class="chart-container"></div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="quick-actions">
        <h3 class="section-title">快捷操作</h3>
        <div class="action-grid">
          <div class="action-item" @click="$router.push('/admin/product')">
            <div class="action-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="action-text">
              <span class="action-title">审核商品</span>
              <span class="action-desc">审核待处理的商品</span>
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="action-item" @click="$router.push('/admin/order')">
            <div class="action-icon"><el-icon><Van /></el-icon></div>
            <div class="action-text">
              <span class="action-title">订单发货</span>
              <span class="action-desc">处理待发货订单</span>
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="action-item" @click="$router.push('/admin/gameType')">
            <div class="action-icon"><el-icon><Setting /></el-icon></div>
            <div class="action-text">
              <span class="action-title">游戏分类</span>
              <span class="action-desc">管理游戏类型</span>
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="action-item" @click="$router.push('/admin/user')">
            <div class="action-icon"><el-icon><User /></el-icon></div>
            <div class="action-text">
              <span class="action-title">用户管理</span>
              <span class="action-desc">查看所有用户</span>
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 管理菜单 -->
      <div class="admin-nav">
        <h3 class="section-title">管理菜单</h3>
        <div class="nav-grid">
          <div class="nav-item" @click="$router.push('/admin/user')">
            <el-icon class="nav-icon"><User /></el-icon>
            <span class="nav-text">用户管理</span>
            <span class="nav-count" v-if="userCount > 0">{{ userCount }}</span>
          </div>
          <div class="nav-item" @click="$router.push('/admin/gameType')">
            <el-icon class="nav-icon"><Grid /></el-icon>
            <span class="nav-text">游戏类型</span>
            <span class="nav-count" v-if="gameTypeCount > 0">{{ gameTypeCount }}</span>
          </div>
          <div class="nav-item" @click="$router.push('/admin/product')">
            <el-icon class="nav-icon"><Goods /></el-icon>
            <span class="nav-text">商品管理</span>
            <span class="nav-count nav-count--warning" v-if="pendingCount > 0">{{ pendingCount }}</span>
          </div>
          <div class="nav-item" @click="$router.push('/admin/order')">
            <el-icon class="nav-icon"><ShoppingCart /></el-icon>
            <span class="nav-text">订单管理</span>
            <span class="nav-count" v-if="orderCount > 0">{{ orderCount }}</span>
          </div>
          <div class="nav-item" @click="$router.push('/admin/feedback')">
            <el-icon class="nav-icon"><ChatDotRound /></el-icon>
            <span class="nav-text">反馈管理</span>
            <span class="nav-count nav-count--warning" v-if="pendingFeedbackCount > 0">{{ pendingFeedbackCount > 99 ? '99+' : pendingFeedbackCount }}</span>
          </div>
          <div class="nav-item" @click="$router.push('/admin/petSkin')">
            <el-icon class="nav-icon"><MagicStick /></el-icon>
            <span class="nav-text">桌宠皮肤</span>
          </div>
          <div class="nav-item" @click="goHome">
            <el-icon class="nav-icon"><HomeFilled /></el-icon>
            <span class="nav-text">返回首页</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getCurrentUser, removeAccount } from '@/utils/accountManager'
import * as echarts from 'echarts'

const router = useRouter()
const userCount = ref(0)
const productCount = ref(0)
const orderCount = ref(0)
const gameTypeCount = ref(0)
const pendingCount = ref(0)
const totalRevenue = ref(0)
const pendingFeedbackCount = ref(0)
const loading = ref(true)

const orderStatusChart = ref(null)
const revenueChart = ref(null)
const productChart = ref(null)
const userChart = ref(null)

let chartInstances = []

const goHome = () => {
  router.push('/')
}

const logout = () => {
  const user = getCurrentUser()
  if (user) {
    removeAccount('user_' + user.id)
  }
  ElMessage.success('退出成功')
  router.push('/login')
}

const initCharts = async () => {
  await nextTick()
  console.log('图表DOM:', orderStatusChart.value, revenueChart.value, productChart.value, userChart.value)

  try {
    const orderByStatus = await request.get('/admin/stats/orderByStatus')
    console.log('订单状态数据:', orderByStatus)
    initOrderStatusChart(orderByStatus)
  } catch (e) {
    console.error('加载订单状态数据失败', e)
  }

  try {
    const revenueByDay = await request.get('/admin/stats/revenueByDay')
    console.log('交易额数据:', revenueByDay)
    initRevenueChart(revenueByDay)
  } catch (e) {
    console.error('加载交易额数据失败', e)
  }

  try {
    const productByGame = await request.get('/admin/stats/productByGame')
    console.log('商品分类数据:', productByGame)
    initProductChart(productByGame)
  } catch (e) {
    console.error('加载商品分类数据失败', e)
  }

  try {
    const activeUsers = await request.get('/admin/stats/activeUsers')
    console.log('用户数据:', activeUsers)
    initUserChart(activeUsers)
  } catch (e) {
    console.error('加载用户数据失败', e)
  }
}

const initOrderStatusChart = (data) => {
  if (!orderStatusChart.value) return
  const chart = echarts.init(orderStatusChart.value)
  chartInstances.push(chart)

  chart.setOption({
    color: ['#E6A23C', '#409EFF', '#67C23A', '#909399', '#F56C6C'],
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: 0,
      itemWidth: 12,
      itemHeight: 12
    },
    series: [{
      name: '订单状态',
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '40%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}\n{c}'
      },
      data: data.labels.map((label, index) => ({
        name: label,
        value: data.data[index]
      }))
    }]
  })
}

const initRevenueChart = (data) => {
  if (!revenueChart.value) return
  const chart = echarts.init(revenueChart.value)
  chartInstances.push(chart)

  chart.setOption({
    color: ['#9d5cff'],
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>交易额: ¥{c}'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.labels,
      axisLine: { lineStyle: { color: '#d0d5dd' } },
      axisLabel: { color: '#6b7280', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#d0d5dd' } },
      axisLabel: { color: '#6b7280', fontSize: 12, formatter: '¥{value}' }
    },
    series: [{
      name: '交易额',
      type: 'bar',
      barWidth: '50%',
      data: data.data,
      itemStyle: {
        borderRadius: [6, 6, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: '#22d3ee'
        }
      }
    }]
  })
}

const initProductChart = (data) => {
  if (!productChart.value) return
  const chart = echarts.init(productChart.value)
  chartInstances.push(chart)

  chart.setOption({
    color: ['#9d5cff', '#f472b6', '#38bdf8', '#4ade80', '#fbbf24'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.labels,
      axisLine: { lineStyle: { color: '#d0d5dd' } },
      axisLabel: { color: '#6b7280', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#d0d5dd' } },
      axisLabel: { color: '#6b7280', fontSize: 12 }
    },
    series: [{
      name: '商品数量',
      type: 'bar',
      barWidth: '50%',
      data: data.data,
      itemStyle: {
        borderRadius: [6, 6, 0, 0]
      }
    }]
  })
}

const initUserChart = (data) => {
  if (!userChart.value) return
  const chart = echarts.init(userChart.value)
  chartInstances.push(chart)

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 人'
    },
    radar: {
      indicator: [
        { name: '今日新增', max: Math.max(data.today * 2, 10) },
        { name: '本周新增', max: Math.max(data.week * 2, 50) },
        { name: '本月新增', max: Math.max(data.month * 2, 200) }
      ],
      shape: 'circle',
      axisName: { color: '#6b7280', fontSize: 12 }
    },
    series: [{
      name: '用户增长',
      type: 'radar',
      data: [{
        value: [data.today, data.week, data.month],
        name: '用户数据',
        areaStyle: { color: 'rgba(157, 92, 255, 0.25)' },
        lineStyle: { color: '#9d5cff', width: 2 },
        itemStyle: { color: '#9d5cff' }
      }]
    }]
  })
}

const handleResize = () => {
  chartInstances.forEach(chart => chart.resize())
}

onMounted(() => {
  const user = getCurrentUser()
  if (!user || user.role?.toUpperCase() !== 'ADMIN') {
    ElMessage.warning('无管理员权限！')
    router.push('/login')
    return
  }

  Promise.all([
    request.get('/user/list').catch(() => ({ data: [] })),
    request.get('/product/list').catch(() => ({ data: [] })),
    request.get('/order/listAll').catch(() => ({ data: [] })),
    request.get('/gameType/list').catch(() => ({ data: [] })),
    request.get('/admin/stats/overview').catch(() => ({})),
    request.get('/feedback/admin/list').catch(() => ({ data: [] }))
  ]).then(([users, products, orders, gameTypes, overview, feedbacks]) => {
    userCount.value = users.data?.length || 0
    productCount.value = products.data?.length || 0
    orderCount.value = orders.data?.length || 0
    gameTypeCount.value = gameTypes.data?.length || 0
    pendingCount.value = products.data?.filter(p => p.status === 0).length || 0
    totalRevenue.value = overview?.totalRevenue || 0
    pendingFeedbackCount.value = feedbacks.data?.filter(f => f.status === 0).length || 0
    loading.value = false

    initCharts()
  })

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.forEach(chart => chart.dispose())
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
  margin-bottom: 30px;
  background: var(--color-bg-card);
  padding: 24px 30px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border);
}

.header-left h1 {
  font-size: 24px;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.welcome-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.header-right {
  display: flex;
  gap: 12px;
}

.header-right .el-button {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: var(--color-bg-card);
  padding: 24px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--color-border);
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(124, 58, 237, 0.2);
}

.stat-card--highlight {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.12), rgba(249, 115, 22, 0.06));
  border-color: rgba(251, 191, 36, 0.3);
}

.stat-card--highlight .stat-icon {
  background: var(--gradient-orange);
}

.stat-card--highlight .stat-value {
  color: #b45309;
}

.stat-card--success {
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.12), rgba(34, 197, 94, 0.06));
  border-color: rgba(74, 222, 128, 0.3);
}

.stat-card--success .stat-icon {
  background: var(--gradient-green);
}

.stat-card--success .stat-value {
  color: var(--color-success);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}

.users-icon {
  background: var(--color-primary-gradient);
}

.product-icon {
  background: var(--gradient-pink);
}

.order-icon {
  background: var(--gradient-blue);
}

.audit-icon {
  background: var(--gradient-orange);
}

.revenue-icon {
  background: var(--gradient-green);
}

/* 统计卡彩色图标容器内 EP 图标统一白色 */
.stat-icon .el-icon {
  color: #fff;
  font-size: 26px;
}

.stat-trend .el-icon {
  font-size: 20px;
  color: #b6bcc7;
}

.stat-trend.up .el-icon {
  color: var(--color-success);
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.stat-trend {
  font-size: 20px;
  opacity: 0.6;
}

.stat-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: #f97316;
  color: white;
  font-size: 12px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 24px;
  text-align: center;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.chart-card {
  background: var(--color-bg-card);
  padding: 24px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border);
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 20px;
}

.chart-container {
  height: 280px;
}

.quick-actions {
  background: var(--color-bg-card);
  padding: 24px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  margin-bottom: 30px;
  border: 1px solid var(--color-border);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 20px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: #f8f9fc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.action-item:hover {
  background: #f3f2ff;
  border-color: #d9d7ff;
  color: var(--color-text-primary);
  transform: translateX(3px);
}

.action-item:hover .action-icon {
  background: var(--color-primary-gradient);
}

.action-item:hover .action-icon .el-icon {
  color: #fff;
}

.action-item:hover .action-desc {
  color: var(--color-text-secondary);
}

.action-item:hover .action-arrow {
  color: var(--color-primary);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.action-icon .el-icon {
  font-size: 22px;
  color: var(--color-primary);
}

.action-arrow {
  font-size: 16px;
  color: #c0c4cc;
  transition: all 0.3s ease;
}

.action-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
}

.action-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  transition: color 0.3s ease;
}

.admin-nav {
  background: var(--color-bg-card);
  padding: 24px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border);
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  background: #f8f9fc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  border: 1px solid transparent;
}

.nav-item:hover {
  background: #f3f2ff;
  border-color: #d9d7ff;
  color: var(--color-text-primary);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.1);
}

.nav-item:hover .nav-text {
  color: var(--color-text-primary);
}

.nav-item:hover .nav-icon {
  color: var(--color-primary);
}

.nav-icon {
  font-size: 30px;
  margin-bottom: 10px;
  color: var(--color-primary);
  transition: color 0.3s ease;
}

.nav-text {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
  transition: color 0.3s ease;
}

.nav-count {
  position: absolute;
  top: 10px;
  right: 10px;
  background: var(--color-primary);
  color: white;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 7px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.nav-count--warning {
  background: #f97316;
}

/* 平板竖屏 / 小笔记本：统计卡两列，操作区两列 */
@media (max-width: 992px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-right {
    width: 100%;
    justify-content: flex-end;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-container {
    height: 220px;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }

  .nav-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

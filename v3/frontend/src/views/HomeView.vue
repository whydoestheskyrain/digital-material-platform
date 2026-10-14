<template>
  <div class="yp-home">
    <!-- 顶部导航栏 -->
    <header class="yp-header">
      <div class="container">
        <div class="header-content">
          <div class="logo-section" @click="$router.push('/')">
            <span class="logo-icon"></span>
            <span class="logo-text">虚拟交易平台</span>
          </div>

          <!-- 搜索框 -->
          <div class="search-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称、区服..."
              class="search-input"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon class="search-icon"><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
          </div>

        <!-- 用户操作按钮 -->
<div class="header-btns">
  <template v-if="!isLogin">
    <el-button class="navbar-button" @click="$router.push('/login')">登录</el-button>
    <el-button class="navbar-button navbar-button--primary" @click="$router.push('/register')">注册</el-button>
  </template>
  <template v-else>
    <!-- 原有四个按钮 -->
    <el-button class="navbar-button" @click="$router.push('/product/add')">
      <el-icon class="btn-icon"><Promotion /></el-icon> 发布
    </el-button>
    <el-button class="navbar-button" @click="$router.push('/product/list')">
      <el-icon class="btn-icon"><Goods /></el-icon> 我的
    </el-button>
    <div class="navbar-badge-wrap" @click="$router.push('/order/my')">
      <el-button class="navbar-button">
        <el-icon class="btn-icon"><ShoppingCart /></el-icon> 订单
      </el-button>
      <span v-if="orderUnreadCount > 0" class="order-badge">{{ orderUnreadCount > 99 ? '99+' : orderUnreadCount }}</span>
    </div>

    <!-- 消息通知（仅登录后显示，未读数口径与消息中心一致） -->
    <div
      class="notification-btn"
      :class="{ 'has-unread': unreadCount > 0 }"
      title="消息中心"
      @click="$router.push('/user/notifications')"
    >
      <svg class="bell-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M13.73 21a2 2 0 0 1-3.46 0" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </div>

    <!-- 多账号切换下拉菜单 -->
    <el-dropdown trigger="hover" @command="handleAccountCommand">
      <div class="avatar-wrapper">
        <el-avatar :size="40" :src="user.avatar || '/images/default-avatar.jpg'" />
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <!-- 当前账号信息 -->
            <div class="user-info-tip">
              <div><strong>{{ user.nickname || user.username }}</strong></div>
              <div style="font-size:12px;color:var(--color-text-secondary)">{{ user.phone || '未绑定' }}</div>
            </div>
            <el-dropdown-item @click="$router.push('/user/center')">个人中心</el-dropdown-item>

          <!-- 已登录账号列表 -->
          <el-dropdown-item
            v-for="(account, id) in accounts"
            :key="id"
            :command="{ type: 'switch', id }"
            :class="{ active: id === currentAccountId }"
          >
            <el-avatar :size="24" :src="account.user.avatar || '/images/default-avatar.jpg'" />
            <span style="margin-left:8px">{{ account.user.nickname || account.user.username }}</span>
            <el-tag v-if="id === currentAccountId" size="small" type="success" style="margin-left:8px">当前</el-tag>
          </el-dropdown-item>

          <el-dropdown-item divided :command="{ type: 'add' }">
            <el-icon style="margin-right:6px;vertical-align:-2px"><Plus /></el-icon> 添加账号
          </el-dropdown-item>
          <el-dropdown-item :command="{ type: 'logout' }" style="color:#f56c6c">
            <el-icon style="margin-right:6px;vertical-align:-2px"><SwitchButton /></el-icon> 退出当前账号
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </template>
</div>
        </div> <!-- 闭合 .header-content -->
      </div> <!-- 闭合 .container -->
    </header>

    <!-- Banner轮播 -->
    <section class="banner-section">
      <div class="container">
        <div class="banner-carousel">
          <div
            v-for="(banner, index) in banners"
            :key="index"
            :class="['banner-item', { active: bannerIndex === index }]"
          >
            <div class="banner-content">
              <h2>{{ banner.title }}</h2>
              <p>{{ banner.subtitle }}</p>
              <el-button v-if="banner.action" type="primary" plain @click="$router.push(banner.action)">
                {{ banner.actionText }}
              </el-button>
            </div>
            <div class="banner-bg" :style="getBannerStyle(banner)"></div>
          </div>
          <div class="banner-dots">
            <span
              v-for="(_, index) in banners"
              :key="index"
              :class="['dot', { active: bannerIndex === index }]"
              @click="handleDotClick(index)"
            ></span>
          </div>
        </div>
      </div>
    </section>

    <!-- 游戏类型分类导航 -->
    <section class="category-section">
      <div class="container">
        <div class="section-header">
          <h3 class="section-title">游戏类型</h3>
        </div>
        <div class="category-list">
          <div
            :class="['category-item', { active: activeCategory === 'all' }]"
            @click="activeCategory = 'all'"
          >
            <span class="cat-icon">🎯</span>
            <span class="cat-name">全部</span>
          </div>
          <div
            v-for="type in gameTypes"
            :key="type.id"
            :class="['category-item', { active: activeCategory === type.id }]"
            @click="activeCategory = type.id"
          >
            <span class="cat-icon">{{ type.icon || '🎮' }}</span>
            <span class="cat-name">{{ type.typeName }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 商品展示区 -->
    <section class="goods-section">
      <div class="container">
        <div class="section-header">
          <h3 class="section-title">
            <span v-if="activeCategory === 'all'">全部商品</span>
            <span v-else>{{ gameTypes.find(t => t.id === activeCategory)?.typeName || '商品' }}</span>
          </h3>
          <span class="section-count">共 {{ showProducts.length }} 件商品</span>
        </div>

        <div v-if="loading" class="loading-container">
          <div class="skeleton-grid">
            <div v-for="i in 8" :key="i" class="skeleton-card">
              <div class="skeleton-image"></div>
              <div class="skeleton-content">
                <div class="skeleton-title"></div>
                <div class="skeleton-tags"></div>
                <div class="skeleton-desc"></div>
                <div class="skeleton-footer"></div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="showProducts.length > 0" class="goods-grid">
          <div
            v-for="item in showProducts"
            :key="item.id"
            class="goods-card"
            :class="{ 'sold-out': item.status === PRODUCT_STATUS.SOLD_OUT }"
            @click="goToDetail(item)"
          >
            <div class="card-image">
              <img
                :src="item.imageUrl || '/images/game-default.png'"
                :alt="item.title"
                class="img-responsive"
                loading="lazy"
              />
              <div v-if="item.status === PRODUCT_STATUS.SOLD_OUT" class="sold-overlay">
                <span class="sold-text">已售出</span>
              </div>
              <div class="card-badges">
                <span v-if="item.isHot" class="badge badge-hot">🔥 热门</span>
              </div>
            </div>
            <div class="card-info">
              <h3 class="card-title">{{ item.title }}</h3>
              <div class="card-meta">
              <span class="meta-item">
                  <el-icon class="meta-icon"><Monitor /></el-icon>
                  {{ item.gameServer || '全区服' }}
                </span>
                <span class="meta-item">
                  <el-icon class="meta-icon"><DataLine /></el-icon>
                  {{ item.roleLevel || 0 }}级
                </span>
              </div>
              <div class="card-tags">
                <el-tag size="small" effect="plain" type="primary">{{ item.gameTypeName || item.gameType || '未分类' }}</el-tag>
                <el-tag v-if="item.status === PRODUCT_STATUS.PENDING" type="warning" size="small" effect="plain">待审核</el-tag>
                <el-tag v-else-if="item.status === PRODUCT_STATUS.REJECTED" type="danger" size="small" effect="plain">已拒绝</el-tag>
                <el-tag v-else-if="item.status === PRODUCT_STATUS.SOLD_OUT" type="info" size="small" effect="plain">已售出</el-tag>
              </div>
              <p class="card-desc">{{ item.description || '暂无描述' }}</p>
              <div class="card-footer">
                <div class="price-info">
                  <span class="price">¥{{ item.price }}</span>
                  <span v-if="item.originalPrice && item.originalPrice > item.price" class="original-price">
                    ¥{{ item.originalPrice }}
                  </span>
                </div>
                <el-button
                  v-if="item.status === PRODUCT_STATUS.AVAILABLE"
                  type="primary"
                  size="default"
                  class="buy-btn"
                  :loading="buyingId === item.id"
                  @click.stop="handleBuy(item)"
                >
                  立即购买
                </el-button>
                <span v-else class="status-label" :class="{ disabled: item.status === PRODUCT_STATUS.SOLD_OUT }">
                  {{ item.status === PRODUCT_STATUS.SOLD_OUT ? '已售出' : '不可购买' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-illustration">
            <span class="empty-icon">{{ searchKeyword ? '🔍' : '📦' }}</span>
          </div>
          <h4 class="empty-title">{{ searchKeyword ? '未找到相关商品' : '暂无商品' }}</h4>
          <p class="empty-desc">
            {{ searchKeyword ? '换个关键词试试吧' : '敬请期待更多商品上架' }}
          </p>
          <div class="empty-actions">
            <el-button v-if="searchKeyword" @click="searchKeyword = ''">清除搜索</el-button>
            <el-button type="primary" @click="$router.push('/product/add')" v-if="isLogin">发布商品</el-button>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAccounts, getCurrentAccount, removeAccount, switchAccount, migrateFromLegacy } from '@/utils/accountManager'
import { useOrderUnread } from '@/composables/useOrderUnread'

const router = useRouter()

// 首页订单按钮角标（与订单列表页共享状态）
const { unreadCount: orderUnreadCount, refresh: refreshOrderUnread } = useOrderUnread()

const PRODUCT_STATUS = {
  PENDING: 0,
  AVAILABLE: 1,
  REJECTED: 2,
  SOLD_OUT: 3
}

const goods = ref([])
const gameTypes = ref([])
const isLogin = ref(false)
const user = ref({})
const accounts = ref({})
const currentAccountId = ref('')
const searchKeyword = ref('')
const bannerIndex = ref(0)
const activeCategory = ref('all')
const loading = ref(false)
const buyingId = ref(null)
const unreadCount = ref(0)
// 未读数轮询定时器（统一刷新节奏，页面卸载时清理）
let unreadTimer = null
// 未读数轮询间隔：60秒
const UNREAD_POLL_INTERVAL = 60000

// 获取未读消息数（静默刷新：失败时不清零、不打扰用户，保留上次数字）
const getUnreadCount = async () => {
  if (!user.value?.id) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await request.get(`/notification/count/${user.value.id}`)
    if (res.code === 200) {
      unreadCount.value = res.data || 0
    }
  } catch (e) {
    console.error('获取未读消息失败:', e)
  }
}

// 页面重新可见（从订单页/消息中心返回、切回浏览器标签页）时刷新角标
const handleVisibilityRefresh = () => {
  if (document.visibilityState === 'visible') {
    getUnreadCount()
    refreshOrderUnread()
  }
}

// 消息中心标记已读后即时刷新铃铛角标（同一用户才响应）
const handleNotificationRead = (e) => {
  if (!e?.detail?.userId || e.detail.userId === user.value?.id) {
    getUnreadCount()
  }
}

// 订单列表标记已查看后即时刷新订单按钮角标（同一用户才响应）
const handleOrderUnreadChange = (e) => {
  if (!e?.detail?.userId || e.detail.userId === user.value?.id) {
    refreshOrderUnread()
  }
}

// 未读数统一刷新：消息铃铛 + 订单按钮
const refreshAllUnread = () => {
  getUnreadCount()
  refreshOrderUnread()
}

// 刷新登录状态
const refreshLoginState = async () => {
  const current = getCurrentAccount()
  if (current && current.user) {
    user.value = current.user
    isLogin.value = true
    accounts.value = getAccounts()
    currentAccountId.value = localStorage.getItem('currentAccount') || ''
    await getUnreadCount()
    refreshOrderUnread()
  } else {
    user.value = {}
    isLogin.value = false
    accounts.value = {}
    currentAccountId.value = ''
    unreadCount.value = 0
  }
}

const banners = [
  {
    title: '安全交易，放心购买',
    subtitle: '专业游戏账号交易平台',
    image: '/images/banner1.jpg',
    gradient: 'linear-gradient(135deg, rgba(124,58,237,0.9) 0%, rgba(79,70,229,0.85) 100%)',
  },
  {
    title: '海量优质账号',
    subtitle: '覆盖热门游戏',
    image: '/images/banner2.jpg',
    gradient: 'linear-gradient(135deg, rgba(124,58,237,0.85) 0%, rgba(34,211,238,0.85) 100%)',
  },
  {
    title: '快速发货',
    subtitle: '秒级到账体验',
    image: '/images/banner4.jpg',
    gradient: 'linear-gradient(135deg, rgba(14,165,233,0.9) 0%, rgba(34,211,238,0.85) 100%)',
  },
  {
    title: '安全有保障',
    subtitle: '专业客服全程服务',
    image: '/images/banner3.jpg',
    gradient: 'linear-gradient(135deg, rgba(79,70,229,0.9) 0%, rgba(14,165,233,0.85) 100%)',
  }
]

let bannerTimer = null

const getBannerStyle = (banner) => {
  if (banner.image) {
    return {
      background: `linear-gradient(0deg, rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0.4)), url(${banner.image})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return { background: banner.gradient }
}

const resetBannerTimer = () => {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = setInterval(() => {
      bannerIndex.value = (bannerIndex.value + 1) % banners.length
    }, 4000)
  }
}

const handleDotClick = (index) => {
  bannerIndex.value = index
  resetBannerTimer()
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    const goodsSection = document.querySelector('.goods-section')
    if (goodsSection) {
      goodsSection.scrollIntoView({ behavior: 'smooth' })
    }
  }
}

const loadGameTypes = async () => {
  try {
    const res = await request.get('/gameType/enabled')
    gameTypes.value = res.data || []
  } catch (e) {
    console.error('加载游戏类型失败', e)
  }
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await request.get('/product/listByStatus/1')
    goods.value = (res.data || []).map(item => ({
      ...item,
      gameTypeId: item.gameTypeId || item.gameType
    }))
  } catch (e) {
    console.error(e)
    ElMessage.error('商品加载失败')
  } finally {
    loading.value = false
  }
}

const showProducts = computed(() => {
  let arr = [...goods.value]
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (keyword) {
    arr = arr.filter(item =>
      item.title.toLowerCase().includes(keyword) ||
      (item.gameServer && item.gameServer.toLowerCase().includes(keyword))
    )
  }
  if (activeCategory.value !== 'all') {
    arr = arr.filter(item => String(item.gameTypeId) === String(activeCategory.value))
  }
  return arr
})

const goToDetail = (item) => {
  router.push(`/product/detail/${item.id}`)
}

const handleBuy = async (item) => {
  if (buyingId.value) return
  if (!isLogin.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (item.status !== PRODUCT_STATUS.AVAILABLE) {
    ElMessage.warning('该商品暂时无法购买')
    return
  }
  if (user.value.id === item.sellerId) {
    ElMessage.warning('不能购买自己发布的商品')
    return
  }
  buyingId.value = item.id
  try {
    const res = await request.post('/order/add', {
      productId: item.id
    })
    if (res && res.code === 200) {
      ElMessage.success('下单成功！')
      router.push('/order/my')
    } else {
      ElMessage.error(res?.msg || '下单失败')
    }
  } catch (e) {
    console.error('购买失败详情:', e)
    const errorMsg = e.response?.data?.msg || e.message || '购买失败，请检查后端服务'
    ElMessage.error(errorMsg)
  } finally {
    buyingId.value = null
  }
}

const handleAccountCommand = (command) => {
  if (command.type === 'switch') {
    switchAccount(command.id)
    window.location.reload()
  } else if (command.type === 'add') {
    router.push('/login')
  } else if (command.type === 'logout') {
    handleLogout()
  }
}

const handleLogout = () => {
  const current = getCurrentAccount()
  if (current && current.user) {
    removeAccount('user_' + current.user.id)
  }
  refreshLoginState()
  searchKeyword.value = ''
  activeCategory.value = 'all'
  ElMessage.success('退出成功')
}

onMounted(() => {
  // 首次使用：迁移旧数据
  migrateFromLegacy()
  refreshLoginState()
  loadGameTypes()
  loadProducts()
  bannerTimer = setInterval(() => {
    bannerIndex.value = (bannerIndex.value + 1) % banners.length
  }, 4000)

  unreadTimer = setInterval(refreshAllUnread, UNREAD_POLL_INTERVAL)
  document.addEventListener('visibilitychange', handleVisibilityRefresh)
  window.addEventListener('focus', refreshAllUnread)
  window.addEventListener('notification:read', handleNotificationRead)
  window.addEventListener('order:unread-change', handleOrderUnreadChange)
})

onUnmounted(() => {
  if (bannerTimer) clearInterval(bannerTimer)
  if (unreadTimer) clearInterval(unreadTimer)
  document.removeEventListener('visibilitychange', handleVisibilityRefresh)
  window.removeEventListener('focus', refreshAllUnread)
  window.removeEventListener('notification:read', handleNotificationRead)
  window.removeEventListener('order:unread-change', handleOrderUnreadChange)
})
</script>

<style scoped>
.avatar-wrapper {
  cursor: pointer;
  margin-left: 12px;
  display: flex;
  align-items: center;
}

.user-info-tip {
  padding: 8px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
  line-height: 1.5;
  min-width: 180px;
}

.user-info-tip div {
  margin: 4px 0;
}

/* 以下为原有样式，保持完整，此处省略重复部分以节省篇幅，请保留您原有的全部样式 */
/* 注意：实际替换时请将下方占位符替换为原 <style> 中所有样式代码 */


/* 原有样式基本保持不变，仅微调骨架屏动画 */
.yp-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 2px 12px rgba(16, 24, 40, 0.04);
  backdrop-filter: blur(14px);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.logo-section:hover {
  transform: translateY(-1px);
}

.logo-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: var(--color-primary-gradient);
  box-shadow: 0 5px 12px rgba(99, 91, 255, 0.24);
  position: relative;
}

.logo-icon::after {
  content: 'V';
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 17px;
  font-weight: 800;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--color-text-primary);
}

.search-section {
  display: flex;
  flex: 1;
  max-width: 500px;
  margin: 0 40px;
}

.search-input {
  flex: 1;
  border-radius: 6px 0 0 6px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 6px 0 0 6px;
  background: #f8f9fc;
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.search-btn {
  border-radius: 0 6px 6px 0;
  background: var(--color-primary-gradient);
  border: 0;
  color: #fff;
}

.search-btn:hover {
  background: var(--color-primary-gradient-hover);
}

.search-icon {
  font-size: 16px;
}

.header-btns {
  display: flex;
  gap: 12px;
  align-items: center;
}

.navbar-button {
  background: transparent;
  border: 1px solid transparent;
  color: var(--color-text-regular);
  border-radius: 6px;
  transition: all 0.3s ease;
}

.navbar-button:hover {
  background: #f3f2ff;
  border-color: #e8e7ff;
  transform: none;
}

.navbar-button--primary {
  background: var(--color-primary-gradient);
  color: #fff;
  border-color: transparent;
}

.navbar-button--primary:hover {
  background: var(--color-primary-gradient-hover);
}

.btn-icon {
  margin-right: 4px;
  font-size: 15px;
}

.notification-btn {
  background: transparent;
  border: none;
  color: var(--color-text-regular);
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  width: 44px;
  height: 44px;
  position: relative;
  flex-shrink: 0;
}

.notification-btn:hover {
  background: #f3f2ff;
  color: var(--color-primary);
  transform: none;
}

.bell-icon {
  width: 22px;
  height: 22px;
  color: currentColor;
  transition: all 0.3s ease;
}

.notification-btn:hover .bell-icon {
  transform: none;
  filter: none;
}

/* 有未读时铃铛轻微摇动一次（不循环，避免打扰） */
.notification-btn.has-unread .bell-icon {
  animation: bell-shake 0.5s ease 1;
}

@keyframes bell-shake {
  0%, 100% { transform: rotate(0); }
  20% { transform: rotate(12deg); }
  40% { transform: rotate(-10deg); }
  60% { transform: rotate(6deg); }
  80% { transform: rotate(-4deg); }
}

/* 未读数字角标：统一红色圆点规格，无未读时整块不渲染 */
.notification-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #ff4d4f;
  color: white;
  font-size: 10px;
  font-weight: 600;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 8px;
  padding: 0 4px;
  text-align: center;
  box-sizing: border-box;
  border: 1.5px solid #fff;
  pointer-events: none;
}

/* 首页订单按钮角标包裹层 */
.navbar-badge-wrap {
  position: relative;
  display: inline-flex;
  cursor: pointer;
}

.order-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ff4d4f;
  color: white;
  font-size: 10px;
  font-weight: 600;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 8px;
  padding: 0 4px;
  text-align: center;
  box-sizing: border-box;
  border: 1.5px solid #fff;
  pointer-events: none;
  z-index: 2;
}

.banner-section {
  margin-top: 60px;
  padding: 40px 0;
}

.banner-carousel {
  position: relative;
  height: 500px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
}

.banner-item {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0;
  transition: opacity 0.5s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-item.active {
  opacity: 1;
}

.banner-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: white;
}

.banner-content h2 {
  font-size: 36px;
  margin-bottom: 10px;
  text-shadow: 0 2px 10px rgba(0,0,0,0.3);
}

.banner-content p {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 20px;
}

.banner-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-size: cover !important;
  background-position: center !important;
  background-repeat: no-repeat;
}

.banner-dots {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 3;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
  transition: all 0.3s ease;
}

.dot.active {
  background: white;
  transform: scale(1.2);
}

.category-section {
  padding: 25px 0;
  background: #fff;
  border-bottom: 1px solid var(--color-border);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.section-count {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.category-list {
  display: flex;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 12px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f8f9fc;
  border: 1px solid var(--color-border);
}

.category-item:hover {
  background: #f3f2ff;
  border-color: #d9d7ff;
  transform: none;
}

.category-item.active {
  background: var(--color-primary-gradient);
  color: white;
  box-shadow: 0 4px 15px rgba(124, 58, 237, 0.3);
}

.cat-icon {
  font-size: 20px;
}

.cat-name {
  font-size: 14px;
  font-weight: 500;
}

.goods-section {
  padding: 40px 0;
  background: #f1f1f1;
  min-height: 400px;
}

.loading-container {
  padding: 20px 0;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: var(--color-bg-card);
  border-radius: 12px;
  overflow: hidden;
}

.skeleton-image {
  height: 180px;
  background: linear-gradient(90deg, #f2f4f7 25%, #e4e7ec 50%, #f2f4f7 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
}

.skeleton-content {
  padding: 20px;
}

.skeleton-title {
  height: 20px;
  width: 70%;
  background: #eaecf0;
  border-radius: 4px;
  margin-bottom: 12px;
  animation: skeleton-loading 1.5s infinite;
}

.skeleton-tags {
  height: 16px;
  width: 50%;
  background: #eaecf0;
  border-radius: 4px;
  margin-bottom: 12px;
  animation: skeleton-loading 1.5s infinite;
}

.skeleton-desc {
  height: 14px;
  width: 90%;
  background: #eaecf0;
  border-radius: 4px;
  margin-bottom: 12px;
  animation: skeleton-loading 1.5s infinite;
}

.skeleton-footer {
  height: 24px;
  width: 40%;
  background: #eaecf0;
  border-radius: 4px;
  animation: skeleton-loading 1.5s infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.goods-card {
  background: var(--color-bg-card);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-card);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid var(--color-border);
}

.goods-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(124, 58, 237, 0.3);
}

.goods-card.sold-out {
  opacity: 0.75;
}

.card-image {
  position: relative;
  height: 230px;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.goods-card:hover .card-image img {
  transform: scale(1.08);
}

.sold-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sold-text {
  font-size: 24px;
  font-weight: bold;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.card-badges {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
}

.badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.badge-hot {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  color: white;
}

.card-info {
  padding: 18px 18px 16px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: var(--color-text-primary);
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-regular);
}

.meta-icon {
  font-size: 12px;
}

.card-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.tag {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text-regular);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.card-desc {
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 15px;
  min-height: 39px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}

.original-price {
  font-size: 14px;
  color: var(--color-text-secondary);
  text-decoration: line-through;
}

.buy-btn {
  background: var(--color-primary-gradient);
  border: none;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);
}

.buy-btn:hover {
  background: var(--color-primary-gradient-hover);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(124, 58, 237, 0.4);
}

.status-label {
  padding: 6px 16px;
  background: var(--color-fill-subtle);
  color: var(--color-text-secondary);
  border-radius: 4px;
  font-size: 13px;
}

.status-label.disabled {
  background: #f2f4f7;
  color: var(--color-text-secondary);
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: var(--color-bg-card);
  border-radius: 12px;
}

.empty-illustration {
  margin-bottom: 24px;
}

.empty-icon {
  font-size: 72px;
  opacity: 0.8;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.empty-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}



/* 小笔记本 / 平板横屏（≤1200）：商品网格固定 3 列，避免 auto-fill 在中下列数错乱 */
@media (max-width: 1200px) {
  .goods-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 平板竖屏（≤992）：商品网格 2 列 */
@media (max-width: 992px) {
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .banner-carousel {
    height: 240px;
  }
}

@media (max-width: 768px) {
  /* 手机端 header 折成三行后高度变大，fixed + 固定 margin 会压住轮播图；
     改为 sticky 正常占据文档流，轮播区无需再猜高度避让，滚动时仍可吸顶 */
  .yp-header {
    position: sticky;
  }

  .banner-section {
    margin-top: 0;
    padding: 14px 0 28px;
  }

  .header-content {
    flex-wrap: wrap;
    gap: 10px;
  }

  .search-section {
    order: 3;
    width: 100%;
    margin: 0;
    max-width: none;
  }

  .header-btns {
    gap: 6px;
  }

  .header-btns .el-button {
    font-size: 12px;
    padding: 6px 10px;
  }

  .banner-carousel {
    height: auto;
    aspect-ratio: 16 / 9;
  }

  /* 小屏：cover 填满容器避免留白，居中裁剪保证横幅观感完整 */
  .banner-bg {
    background-size: cover !important;
    background-position: center !important;
    background-color: #1a1a2e;
  }

  .banner-content h2 {
    font-size: 24px;
  }

  .banner-content p {
    font-size: 14px;
  }

  .category-list {
    gap: 8px;
  }

  .category-item {
    padding: 8px 14px;
  }

  .cat-icon {
    font-size: 18px;
  }

  .cat-name {
    font-size: 13px;
  }

  .goods-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 15px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
}
</style>

<template>
  <div class="detail-container">
    <!-- 顶部导航 -->
    <div class="detail-header">
      <div class="header-left" @click="goBack">
        <el-icon class="back-icon"><ArrowLeft /></el-icon>
        <span>返回</span>
      </div>
      <div class="header-title">商品详情</div>
      <div class="header-right"></div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="!product" class="empty-state">
      <p>商品不存在或已下架</p>
      <el-button type="primary" @click="goBack">返回首页</el-button>
    </div>

    <div v-else class="detail-content">
      <!-- 商品主图区域 -->
      <div class="product-hero">
        <div class="product-image-wrapper">
          <img :src="product.imageUrl || '/images/game-default.png'" class="product-main-image" />
        </div>
        <div class="product-basic-info">
          <h1 class="product-title">{{ product.title }}</h1>
          <div class="product-tags">
            <span class="tag game-type">{{ product.gameType }}</span>
            <span class="tag server">{{ product.gameServer || '默认区服' }}</span>
            <span class="tag level">等级 {{ product.roleLevel || 0 }}</span>
          </div>
          <div class="product-price-row">
            <span class="price-label">售价</span>
            <span class="price-value">¥{{ product.price?.toFixed(2) }}</span>
          </div>
          <div class="product-meta">
            <span>发布时间：{{ formatTime(product.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 商品描述 -->
      <div class="info-section">
        <h3 class="section-title">
          <el-icon class="section-icon"><EditPen /></el-icon>
          商品描述
        </h3>
        <div class="section-content">
          <p v-if="product.description" class="desc-text">{{ product.description }}</p>
          <p v-else class="desc-empty">暂无商品描述</p>
        </div>
      </div>

      <!-- 商品详情 -->
      <div class="info-section" v-if="product.detailDesc || detailImageList.length">
        <h3 class="section-title">
          <el-icon class="section-icon"><Document /></el-icon>
          商品详情
        </h3>
        <div class="section-content">
          <!-- 商品详情是用户输入的纯文本。Vue 的文本插值会自动转义 HTML，避免存储型 XSS。 -->
          <div v-if="product.detailDesc" class="detail-desc">{{ product.detailDesc }}</div>
          <div v-if="detailImageList.length" class="detail-images">
            <div v-for="(img, index) in detailImageList" :key="index" class="detail-image-item">
              <img :src="img" @click="previewImage(img)" />
            </div>
          </div>
        </div>
      </div>

      <!-- 账号信息（仅管理员和卖家可见） -->
      <div class="info-section" v-if="isAdmin || isSeller">
        <h3 class="section-title">
          <el-icon class="section-icon"><Lock /></el-icon>
          账号信息
          <span class="section-tag">仅管理员/卖家可见</span>
        </h3>
        <div class="section-content account-info">
          <div class="info-row">
            <span class="info-label">游戏账号：</span>
            <span class="info-value">{{ product.gameAccount || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">游戏密码：</span>
            <span class="info-value">{{ product.gamePassword || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">绑定信息：</span>
            <span class="info-value">{{ product.bindInfo || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 卖家信息 -->
      <div class="info-section">
        <h3 class="section-title">
          <el-icon class="section-icon"><User /></el-icon>
          卖家信息
        </h3>
        <div class="section-content seller-info">
          <div class="seller-avatar">{{ sellerName?.charAt(0) || '?' }}</div>
          <div class="seller-detail">
            <div class="seller-name">{{ sellerName || '未知卖家' }}</div>
            <div class="seller-id">ID: {{ product.sellerId }}</div>
          </div>
        </div>
      </div>

      <!-- 底部购买栏 -->
      <div class="buy-bar">
        <div class="buy-bar-price">
          <span class="price-label">合计：</span>
          <span class="price-value">¥{{ product.price?.toFixed(2) }}</span>
        </div>
        <el-button
          type="primary"
          class="buy-btn"
          :disabled="!canBuy"
          @click="handleBuy"
        >
          <el-icon v-if="canBuy"><ShoppingCart /></el-icon>
          {{ buyButtonText }}
        </el-button>
      </div>
    </div>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" :show-close="true" class="preview-dialog">
      <img :src="previewUrl" class="preview-image" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getCurrentUser } from '@/utils/accountManager'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const sellerName = ref('')
const loading = ref(true)
const previewVisible = ref(false)
const previewUrl = ref('')

const currentUser = getCurrentUser()

const isAdmin = computed(() => currentUser?.role?.toUpperCase() === 'ADMIN')
const isSeller = computed(() => currentUser?.id === product.value?.sellerId)

const detailImageList = computed(() => {
  if (!product.value?.detailImages) return []
  return product.value.detailImages.split(',').filter(img => img.trim())
})

const canBuy = computed(() => {
  if (!product.value) return false
  if (product.value.status !== 1) return false
  if (currentUser?.id === product.value.sellerId) return false
  return true
})

const buyButtonText = computed(() => {
  if (!product.value) return '立即购买'
  if (product.value.status !== 1) return '已下架'
  if (currentUser?.id === product.value.sellerId) return '自己的商品'
  return '立即购买'
})

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const previewImage = (url) => {
  previewUrl.value = url
  previewVisible.value = true
}

const goBack = () => {
  router.back()
}

const loadProduct = async () => {
  const id = route.params.id
  if (!id) {
    ElMessage.error('商品ID不存在')
    router.push('/')
    return
  }

  try {
    loading.value = true
    const res = await request.get(`/product/detail/${id}`)
    if (res.code === 200 && res.data) {
      product.value = res.data

      // 公开详情永远不含游戏凭据。卖家本人或管理员需要查看时，才请求受权限保护的完整资料接口。
      if (currentUser && (currentUser.id === res.data.sellerId || isAdmin.value)) {
        const privateRes = await request.get(`/product/get/${id}`)
        if (privateRes.code === 200 && privateRes.data) {
          product.value = privateRes.data
        }
      }

      // 获取卖家信息
      if (res.data.sellerId) {
        try {
          const userRes = await request.get(`/user/get/${res.data.sellerId}`)
          if (userRes.code === 200) {
            sellerName.value = userRes.data?.username || userRes.data?.nickname || `用户${res.data.sellerId}`
          }
        } catch (e) {
          sellerName.value = `用户${res.data.sellerId}`
        }
      }
    } else {
      ElMessage.error('商品不存在')
    }
  } catch (err) {
    console.error('加载商品失败:', err)
    ElMessage.error('加载商品失败')
  } finally {
    loading.value = false
  }
}

const handleBuy = async () => {
  if (!currentUser) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认购买「${product.value.title}」吗？\n价格：¥${product.value.price?.toFixed(2)}`,
      '确认购买',
      {
        confirmButtonText: '确认购买',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await request.post('/order/add', {
      productId: product.value.id
    })

    if (res.code === 200) {
      ElMessage.success('下单成功！')
      router.push('/order/list')
    } else {
      ElMessage.error(res.msg || '下单失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('购买失败:', err)
      ElMessage.error(err.response?.data?.msg || '购买失败，请检查余额是否充足')
    }
  }
}

onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.detail-container {
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(124,58,237,0.12) 0%, rgba(34,211,238,0.06) 100%);
  padding-bottom: 80px;
}

.detail-header {
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

.header-right {
  width: 50px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  color: var(--color-text-secondary);
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

.detail-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

/* 商品主图区域 */
.product-hero {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  margin-bottom: 16px;
}

.product-image-wrapper {
  width: 100%;
  height: 300px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05);
}

.product-main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-basic-info {
  padding: 20px;
}

.product-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.tag.game-type {
  background: rgba(255, 255, 255, 0.05);
  color: #38bdf8;
}

.tag.server {
  background: rgba(255, 255, 255, 0.05);
  color: #7b1fa2;
}

.tag.level {
  background: rgba(255, 255, 255, 0.05);
  color: #388e3c;
}

.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.price-label {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.price-value {
  font-size: 28px;
  font-weight: 700;
  color: #ff6b6b;
}

.product-meta {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 信息区块 */
.info-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.section-icon {
  font-size: 18px;
  color: var(--color-primary);
  margin-right: 2px;
}

.section-tag {
  margin-left: auto;
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.05);
  color: #fbbf24;
  border-radius: 10px;
  font-weight: normal;
}

.section-content {
  color: var(--color-text-regular);
  line-height: 1.8;
}

.desc-text {
  margin: 0;
  white-space: pre-wrap;
}

.desc-empty {
  color: var(--color-text-secondary);
  text-align: center;
  padding: 20px;
}

.detail-desc {
  margin-bottom: 16px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.detail-images {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.detail-image-item {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s;
}

.detail-image-item:hover {
  transform: scale(1.02);
}

.detail-image-item img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

/* 账号信息 */
.account-info .info-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px dashed var(--color-border);
}

.account-info .info-row:last-child {
  border-bottom: none;
}

.info-label {
  width: 100px;
  color: var(--color-text-regular);
  font-weight: 500;
}

.info-value {
  color: var(--color-text-primary);
  font-family: monospace;
}

/* 卖家信息 */
.seller-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.seller-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary-gradient);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
}

.seller-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.seller-id {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

/* 底部购买栏 */
.buy-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
  padding: 12px 20px;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 100;
}

.buy-bar-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.buy-bar-price .price-label {
  font-size: 14px;
  color: var(--color-text-regular);
}

.buy-bar-price .price-value {
  font-size: 24px;
  font-weight: 700;
  color: #ff6b6b;
}

.buy-btn {
  padding: 12px 40px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  border: none;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.18);
}

.buy-btn .el-icon {
  margin-right: 4px;
  vertical-align: -2px;
}

.buy-btn:disabled {
  background: #ccc;
}

/* 图片预览 */
.preview-image {
  width: 100%;
  max-height: 80vh;
  object-fit: contain;
}

@media (max-width: 768px) {
  .detail-images {
    grid-template-columns: 1fr;
  }

  .product-image-wrapper {
    height: 220px;
  }

  .detail-content {
    padding: 12px;
  }

  .detail-main {
    flex-direction: column;
  }

  .detail-main .product-image-wrapper,
  .detail-main .detail-info {
    width: 100%;
  }

  .account-info .info-row {
    flex-direction: column;
    gap: 2px;
  }

  .info-label {
    width: auto;
    font-size: 13px;
  }

  .seller-info {
    flex-wrap: wrap;
    gap: 10px;
  }

  .action-bar {
    position: sticky;
    bottom: 0;
    background: var(--color-bg-card);
    border-top: 1px solid var(--color-border);
    padding: 10px 12px;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
    z-index: 10;
  }

  .action-bar .el-button {
    flex: 1;
  }
}

@media (max-width: 480px) {
  .product-title {
    font-size: 18px;
  }

  .product-price {
    font-size: 24px;
  }

  .detail-image-item img {
    height: 140px;
  }
}
</style>

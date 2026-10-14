<template>
  <div class="order-detail">
    <!-- 头部 -->
    <div class="detail-header">
      <div class="header-left" @click="goBack">
        <el-icon class="back-icon"><ArrowLeft /></el-icon>
        <span>返回</span>
      </div>
      <div class="header-title">订单详情</div>
      <div class="header-right"></div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="!order" class="empty-state">
      <div class="empty-icon">📦</div>
      <p>订单不存在或无权查看</p>
    </div>

    <div v-else class="detail-body">
      <!-- 状态横幅：实色高对比，不使用浅淡渐变 -->
      <div :class="['status-banner', bannerClass]">
        <span class="status-text">{{ order.statusDesc }}</span>
        <span class="role-tag">
          <el-icon><component :is="isSellerView ? 'ShoppingBag' : 'ShoppingCart'" /></el-icon>
          {{ isSellerView ? '我卖出的' : '我买入的' }}
        </span>
      </div>

      <!-- 风险提示：担保交易核心安全感区块 -->
      <div class="risk-tip-card">
        <div class="risk-title">
          <el-icon><Warning /></el-icon> 交易安全提示
        </div>
        <ul>
          <li>本平台采用<strong>担保交易</strong>：买家付款后资金由平台托管，确认收货后才会打给卖家。</li>
          <li>账号信息仅在<strong>卖家发货后</strong>对买家可见，请勿在平台外与卖家私下交易。</li>
          <li>如遇卖家要求加 QQ/微信私聊、绕开平台付款，请<strong>立即拒绝并举报</strong>，谨防诈骗。</li>
          <li>交易过程中如遇纠纷，可在订单中申请平台介入处理。</li>
        </ul>
      </div>

      <!-- 商品卡片 -->
      <div class="section-card">
        <div class="section-title">商品信息</div>
        <div class="product-card" @click="goToProduct">
          <img :src="order.imageUrl || '/images/game-default.png'" :alt="order.productName" class="product-img" />
          <div class="product-info">
            <h3 class="product-name">{{ order.productName || '商品已删除' }}</h3>
            <p class="product-meta">游戏类型：{{ order.gameType || '未分类' }}</p>
            <p class="product-meta">区服：{{ order.gameServer || '全区服' }}</p>
            <p class="product-price"><span class="amount-num">¥{{ formatMoney(order.price) }}</span></p>
          </div>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="section-card">
        <div class="section-title">订单信息</div>
        <div class="info-grid">
          <div class="info-row">
            <span class="info-label">订单号</span>
            <span class="info-value">{{ order.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">下单时间</span>
            <span class="info-value">{{ formatTime(order.createTime) }}</span>
          </div>
          <div class="info-row" v-if="order.payTime">
            <span class="info-label">支付时间</span>
            <span class="info-value">{{ formatTime(order.payTime) }}</span>
          </div>
          <div class="info-row" v-if="order.paymentMethod">
            <span class="info-label">支付方式</span>
            <span class="info-value">{{ order.paymentMethod === 'alipay' ? '支付宝' : '余额支付' }}</span>
          </div>
          <div class="info-row" v-if="order.tradeNo">
            <span class="info-label">支付宝交易号</span>
            <span class="info-value">{{ order.tradeNo }}</span>
          </div>
          <div class="info-row total-row">
            <span class="info-label">实付金额</span>
            <span class="info-value total-amount amount-num">¥{{ formatMoney(order.price) }}</span>
          </div>
        </div>
      </div>

      <!-- 账号信息（已发货后可见） -->
      <div class="section-card" v-if="order.status >= 3 && order.accountInfo">
        <div class="section-title">
          <el-icon class="title-icon"><Lock /></el-icon> 账号信息
        </div>
        <pre class="account-data">{{ order.accountInfo }}</pre>
      </div>

      <!-- 操作区 -->
      <div class="action-bar">
        <template v-if="!isSellerView">
          <el-button v-if="order.status === 1" type="primary" @click="openPay">
            <el-icon><CreditCard /></el-icon> 去支付
          </el-button>
          <el-button v-if="order.status === 1" type="danger" plain @click="handleCancelOrder">
            <el-icon><Close /></el-icon> 取消订单
          </el-button>
          <el-button v-if="order.status === 3" type="success" @click="handleConfirm">
            <el-icon><CircleCheck /></el-icon> 确认收货
          </el-button>
        </template>
        <template v-else>
          <el-button v-if="order.status === 2" type="success" @click="openDeliver">
            <el-icon><Van /></el-icon> 发货
          </el-button>
        </template>
      </div>
    </div>

    <!-- 支付方式选择弹窗 -->
    <el-dialog
      title="选择支付方式"
      v-model="showPayModal"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="pay-methods">
        <div
          :class="['pay-method', { selected: selectedPayMethod === 'balance' }]"
          @click="selectedPayMethod = 'balance'"
        >
          <div class="method-icon"><el-icon><Wallet /></el-icon></div>
          <div class="method-info">
            <h4>余额支付</h4>
            <p>当前余额：<span class="amount-num">¥{{ formatMoney(userBalance) }}</span></p>
          </div>
          <div :class="['method-check', { active: selectedPayMethod === 'balance' }]">
            <el-icon><Check /></el-icon>
          </div>
        </div>
        <div
          :class="['pay-method', { selected: selectedPayMethod === 'alipay' }]"
          @click="selectedPayMethod = 'alipay'"
        >
          <div class="method-icon method-icon--alipay"><el-icon><Money /></el-icon></div>
          <div class="method-info">
            <h4>支付宝支付</h4>
            <p>跳转到支付宝沙箱支付</p>
          </div>
          <div :class="['method-check', { active: selectedPayMethod === 'alipay' }]">
            <el-icon><Check /></el-icon>
          </div>
        </div>
      </div>
      <div class="pay-amount-info">
        <span class="label">订单金额：</span>
        <span class="amount amount-num">¥{{ formatMoney(order?.price) }}</span>
      </div>
      <template #footer>
        <el-button @click="showPayModal = false">取消</el-button>
        <el-button type="primary" @click="confirmPayMethod" :disabled="!selectedPayMethod">
          确认支付
        </el-button>
      </template>
    </el-dialog>

    <!-- 卖家发货弹窗 -->
    <el-dialog
      v-model="showDeliverModal"
      width="450px"
      :close-on-click-modal="false"
    >
      <template #header>
        <span class="dialog-title"><el-icon><Van /></el-icon> 填写账号信息并发货</span>
      </template>
      <div class="deliver-form">
        <p class="deliver-order-no">订单号：{{ order?.orderNo }}</p>
        <p class="deliver-tip">请填写游戏账号 / 密码 / 区服等交付信息，发货后买家即可在订单中查看：</p>
        <el-input
          v-model="deliverAccountInfo"
          type="textarea"
          :rows="4"
          placeholder="例如：账号 xxx，密码 xxx，区服 xxx"
        />
      </div>
      <template #footer>
        <el-button @click="showDeliverModal = false">取消</el-button>
        <el-button type="success" :loading="delivering" @click="confirmDeliver">
          <el-icon><Van /></el-icon> 确认发货
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { getCurrentUser } from '@/utils/accountManager';
import { useOrderUnread } from '@/composables/useOrderUnread';
import { formatMoney } from '@/utils/constants';

const { markViewed } = useOrderUnread();

const route = useRoute();
const router = useRouter();
const currentUser = getCurrentUser();

const order = ref(null);
const loading = ref(false);

// 支付弹窗
const showPayModal = ref(false);
const selectedPayMethod = ref('');
const userBalance = ref(0);

// 发货弹窗
const showDeliverModal = ref(false);
const deliverAccountInfo = ref('');
const delivering = ref(false);

// 当前用户在订单中的角色：买家视角 / 卖家视角
const isSellerView = computed(() =>
  order.value && currentUser && order.value.sellerId === currentUser.id && order.value.buyerId !== currentUser.id
);

// 状态横幅：按订单状态映射实色高对比底色（1待支付/2待发货/3待收货/4已完成/5已取消）
const bannerClass = computed(() => {
  const map = {
    1: 'status-banner--warning',
    2: 'status-banner--primary',
    3: 'status-banner--success',
    4: 'status-banner--info',
    5: 'status-banner--danger'
  };
  return map[order.value?.status] || 'status-banner--primary';
});

const formatTime = (time) => {
  if (!time) return '';
  return new Date(time).toLocaleString('zh-CN', { hour12: false });
};

const loadDetail = async () => {
  loading.value = true;
  try {
    const res = await request.get(`/order/detail/${route.params.id}`);
    if (res.code === 200) {
      order.value = res.data;
      // 进入详情页即视为已查看（无论从订单列表、消息中心哪个入口进来）
      markViewed(res.data);
    } else {
      order.value = null;
      ElMessage.error(res.msg || '订单不存在或无权查看');
    }
  } catch (err) {
    console.error('获取订单详情失败:', err);
    order.value = null;
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.back();
};

const goToProduct = () => {
  if (order.value?.productId) {
    router.push(`/product/detail/${order.value.productId}`);
  }
};

// ====================== 买家：支付 ======================
const getUserBalance = async () => {
  try {
    const res = await request.get('/user/balance');
    if (res.code === 200) {
      userBalance.value = res.data.balance || 0;
    }
  } catch (err) {
    console.error('获取余额失败:', err);
  }
};

const openPay = () => {
  selectedPayMethod.value = '';
  getUserBalance();
  showPayModal.value = true;
};

const confirmPayMethod = async () => {
  if (!selectedPayMethod.value) {
    ElMessage.warning('请选择支付方式');
    return;
  }
  try {
    showPayModal.value = false;
    if (selectedPayMethod.value === 'balance') {
      await ElMessageBox.confirm(
        '确认使用余额支付该订单？\n\n订单号：' + order.value.orderNo + '\n金额：¥' + order.value.price,
        '💳 余额支付确认',
        { type: 'warning', confirmButtonText: '确认支付', cancelButtonText: '取消' }
      );
      const res = await request.put(`/order/pay/${order.value.id}`);
      if (res.code === 200) {
        ElMessage.success('支付成功！等待卖家发货');
        loadDetail();
      } else {
        ElMessage.error(res.msg || '支付失败');
      }
    } else if (selectedPayMethod.value === 'alipay') {
      window.location.href = `/alipay/pay?orderId=${order.value.id}`;
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('支付失败:', err);
      ElMessage.error('支付失败，请稍后重试');
    }
  }
};

// ====================== 买家：取消订单 ======================
const handleCancelOrder = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要取消该订单吗？取消后商品将重新上架。',
      '取消订单',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    );
    const res = await request.post(`/order/cancel/${order.value.id}`);
    if (res.code === 200) {
      ElMessage.success('订单已取消');
      loadDetail();
    } else {
      ElMessage.error(res.msg || '取消失败');
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('取消失败，请稍后重试');
  }
};

// ====================== 买家：确认收货 ======================
const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm(
      '请确认您已收到游戏账号且账号信息正确！',
      '📦 确认收货',
      { type: 'warning' }
    );
    const res = await request.post(`/order/confirm/${order.value.id}`);
    if (res.code === 200) {
      ElMessage.success('交易完成');
      loadDetail();
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败');
  }
};

// ====================== 卖家：发货 ======================
const openDeliver = () => {
  deliverAccountInfo.value = '';
  showDeliverModal.value = true;
};

const confirmDeliver = async () => {
  if (!deliverAccountInfo.value.trim()) {
    ElMessage.warning('请填写账号交付信息');
    return;
  }
  delivering.value = true;
  try {
    const res = await request.post(`/order/deliver/${order.value.id}`, {
      accountInfo: deliverAccountInfo.value
    });
    if (res.code === 200) {
      ElMessage.success('发货成功，等待买家确认收货');
      showDeliverModal.value = false;
      loadDetail();
    } else {
      ElMessage.error(res.msg || '发货失败');
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '发货失败');
  } finally {
    delivering.value = false;
  }
};

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.order-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #38bdf8;
  font-size: 15px;
}

.header-left .back-icon {
  font-size: 18px;
}

.header-left:hover {
  opacity: 0.8;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  width: 60px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--color-text-secondary);
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: #38bdf8;
  border-radius: 50%;
  margin: 0 auto 12px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.status-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-radius: var(--radius-card);
  color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
}

/* 状态横幅统一实色：危险红 / 警告橙 / 成功绿 / 蓝 / 灰，白字加粗保证对比度 */
.status-banner--warning { background: #e0860a; }
.status-banner--primary { background: #2b7de9; }
.status-banner--success { background: #2f9e44; }
.status-banner--info    { background: #6b7280; }
.status-banner--danger  { background: #dd3a3d; }

.status-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.18);
}

.role-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  padding: 5px 12px;
  border-radius: 20px;
}

.dialog-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.section-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-card);
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 6px;
}

.title-icon {
  color: var(--color-primary);
  font-size: 17px;
}

.product-card {
  display: flex;
  gap: 15px;
  cursor: pointer;
}

.product-img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.product-meta {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

.product-price {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
  margin: 0;
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.info-label {
  color: var(--color-text-secondary);
}

.info-value {
  color: var(--color-text-primary);
}

.total-row {
  padding-top: 12px;
  border-top: 1px dashed var(--color-border);
}

.total-amount {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}

.account-data {
  background: rgba(255, 255, 255, 0.05);
  padding: 15px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-all;
  border: 1px solid var(--color-border);
  margin: 0;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 0 30px;
}

.pay-methods {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 20px;
}

.pay-method {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 2px solid var(--color-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.pay-method:hover {
  border-color: #38bdf8;
}

.pay-method.selected {
  border-color: #38bdf8;
  background: rgba(255, 255, 255, 0.05);
}

.method-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: #fff;
  background: var(--color-primary-gradient);
  flex-shrink: 0;
}

.method-icon--alipay {
  background: linear-gradient(135deg, #4facfe 0%, #2b7de9 100%);
}

.method-info h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: var(--color-text-primary);
}

.method-info p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.method-check {
  margin-left: auto;
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: transparent;
}

.method-check .el-icon {
  font-size: 15px;
}

.method-check.active {
  background: #38bdf8;
  border-color: #38bdf8;
  color: white;
}

.pay-amount-info {
  display: flex;
  justify-content: center;
  align-items: baseline;
  gap: 5px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin-bottom: 10px;
}

.pay-amount-info .label {
  font-size: 14px;
  color: var(--color-text-regular);
}

.pay-amount-info .amount {
  font-size: 24px;
  font-weight: 600;
  color: #f56c6c;
}

.deliver-form .deliver-order-no {
  font-weight: 600;
  margin-bottom: 10px;
}

.deliver-form .deliver-tip {
  color: var(--color-warning);
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 10px;
  line-height: 1.5;
}
</style>

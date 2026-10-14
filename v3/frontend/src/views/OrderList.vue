<template>
  <div class="order-list">
    <!-- 买家 / 卖家 视图切换 -->
    <div class="view-switch">
      <div
        :class="['view-item', { active: viewMode === 'buyer' }]"
        @click="switchView('buyer')"
      >
        <el-icon><ShoppingCart /></el-icon> 我买到的
      </div>
      <div
        :class="['view-item', { active: viewMode === 'seller' }]"
        @click="switchView('seller')"
      >
        <el-icon><ShoppingBag /></el-icon> 我卖出的
      </div>
    </div>

    <!-- 头部标签页 -->
    <div class="tabs">
      <div
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-item', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        <el-icon class="tab-icon"><component :is="tab.icon" /></el-icon>
        {{ tab.label }}
        <span class="tab-count" v-if="tab.count > 0">{{ tab.count }}</span>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-container">
      <div
        v-for="order in orderList"
        :key="order.id"
        class="order-card clickable"
        @click="goOrderDetail(order)"
      >
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span :class="['order-status', getStatusClass(order.status)]">
            {{ getStatusText(order.status) }}
          </span>
        </div>
        
        <div class="order-items">
          <div class="item-card" @click.stop="goToProduct(order)">
            <img :src="order.imageUrl || '/images/game-default.png'" :alt="order.productName" class="item-img" />
            <div class="item-info">
              <h3 class="item-name">{{ order.productName }}</h3>
              <p class="item-desc">游戏类型：{{ order.gameType }}</p>
              <p class="item-price amount-num">¥{{ formatMoney(order.price) }}</p>
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-total">
            <span class="total-label">实付金额：</span>
            <span class="total-amount amount-num">¥{{ formatMoney(order.price) }}</span>
          </div>
          <div class="order-actions">
            <!-- ===== 买家视角 ===== -->
            <template v-if="viewMode === 'buyer'">
              <template v-if="order.status === 1">
                <el-button
                  type="primary"
                  class="pay-btn"
                  @click.stop="handlePay(order)"
                >
                  <el-icon><CreditCard /></el-icon> 支付
                </el-button>
                <el-button
                  type="danger"
                  plain
                  @click.stop="handleCancelOrder(order)"
                >
                  <el-icon><Close /></el-icon> 取消订单
                </el-button>
              </template>
              <template v-else-if="order.status === 2">
                <span class="status-text"><el-icon><Clock /></el-icon> 等待卖家发货</span>
              </template>
              <template v-else-if="order.status === 3">
                <el-button
                  type="info"
                  class="info-btn"
                  @click.stop="viewAccountInfo(order)"
                >
                  <el-icon><View /></el-icon> 查看账号信息
                </el-button>
                <el-button
                  type="success"
                  class="confirm-btn"
                  @click.stop="handleConfirm(order)"
                >
                  <el-icon><CircleCheck /></el-icon> 确认收货
                </el-button>
              </template>
              <template v-else>
                <span class="status-text">{{ getStatusText(order.status) }}</span>
              </template>
            </template>
            <!-- ===== 卖家视角 ===== -->
            <template v-else>
              <!-- 待支付：买家还没付款，对卖家是“待收款” -->
              <span v-if="order.status === 1" class="status-text"><el-icon><Wallet /></el-icon> 待收款（等待买家付款）</span>
              <el-button
                v-else-if="order.status === 2"
                type="success"
                class="pay-btn"
                @click.stop="openDeliver(order)"
              >
                <el-icon><Van /></el-icon> 发货
              </el-button>
              <span v-else-if="order.status === 3" class="status-text"><el-icon><Van /></el-icon> 已发货，等待买家确认收货</span>
              <span v-else-if="order.status === 4" class="status-text"><el-icon><CircleCheck /></el-icon> 已完成，款项已到账</span>
              <span v-else class="status-text">{{ getStatusText(order.status) }}</span>
            </template>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="orderList.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p>暂无订单</p>
        <el-button type="primary" class="empty-action" @click="router.push('/')">
          <el-icon><ShoppingCart /></el-icon> 去逛逛商品
        </el-button>
      </div>
    </div>

    <!-- 账号信息弹窗 -->
    <el-dialog
      v-model="showAccountModal"
      width="400px"
      :close-on-click-modal="false"
    >
      <template #header>
        <span class="dialog-title"><el-icon><Lock /></el-icon> 账号信息</span>
      </template>
      <div v-if="accountInfo" class="account-info">
        <div class="info-section">
          <label>订单号</label>
          <span>{{ currentAccountOrder?.orderNo }}</span>
        </div>
        <div class="info-section">
          <label>商品名称</label>
          <span>{{ currentAccountOrder?.productName }}</span>
        </div>
        <div class="info-section">
          <label>账号信息</label>
          <pre class="account-data">{{ accountInfo }}</pre>
        </div>
      </div>
      <div v-else class="no-info">
        <p>暂无账号信息</p>
      </div>

      <template #footer>
        <el-button @click="showAccountModal = false">关闭</el-button>
      </template>
    </el-dialog>

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
        <span class="amount amount-num">¥{{ formatMoney(currentOrder?.price) }}</span>
      </div>

      <template #footer>
        <el-button @click="showPayModal = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmPayMethod"
          :disabled="!selectedPayMethod"
        >
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
      <div v-if="currentOrder" class="deliver-form">
        <p class="deliver-order-no">订单号：{{ currentOrder.orderNo }}</p>
        <p class="deliver-tip">
          请填写游戏账号 / 密码 / 区服等交付信息，发货后买家即可在订单中查看：
        </p>
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
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { useRouter } from 'vue-router';
import { getCurrentUser } from '@/utils/accountManager';
import { useOrderUnread } from '@/composables/useOrderUnread';
import { formatMoney } from '@/utils/constants';

const router = useRouter();
const activeTab = ref('all');
// buyer=我买到的（买家视角） seller=我卖出的（卖家视角，可发货）
const viewMode = ref('buyer');
const orderList = ref([]);
// 两种视角各自的全量订单（用于 tab 计数与前端筛选）
const buyerAllOrders = ref([]);
const sellerAllOrders = ref([]);
const showPayModal = ref(false);
const selectedPayMethod = ref('');
const currentOrder = ref(null);
const userBalance = ref(0);
const showAccountModal = ref(false);
const currentAccountOrder = ref(null);
const accountInfo = ref('');
// 卖家发货弹窗
const showDeliverModal = ref(false);
const deliverAccountInfo = ref('');
const delivering = ref(false);

// 订单未读角标（与首页订单按钮共享状态）
const {
  markViewed, loadViewed, setOrders, statusUnread, totalUnreadForMode
} = useOrderUnread();

// 当前视角的全量数据源
const allSource = computed(() =>
  viewMode.value === 'seller' ? sellerAllOrders.value : buyerAllOrders.value
);

const tabs = computed(() => [
  { label: '全部', value: 'all', icon: 'List', count: totalUnreadForMode(viewMode.value) },
  { label: '待支付', value: 'unpaid', icon: 'Wallet', count: statusUnread(1, viewMode.value) },
  { label: '已支付', value: 'paid', icon: 'ShoppingBag', count: statusUnread(2, viewMode.value) },
  { label: '已发货', value: 'delivered', icon: 'Van', count: statusUnread(3, viewMode.value) },
  { label: '已完成', value: 'completed', icon: 'CircleCheck', count: statusUnread(4, viewMode.value) }
]);

const getStatusText = (status) => {
  const statusMap = {
    1: '待支付',
    2: '已支付',
    3: '已发货',
    4: '已完成',
    5: '已取消'
  };
  return statusMap[status] || '未知';
};

const getStatusClass = (status) => {
  const map = {
    1: 'unpaid',
    2: 'paid',
    3: 'delivered',
    4: 'completed',
    5: 'cancelled'
  };
  return map[status] || '';
};

const getStatusValue = (tab) => {
  const map = { unpaid:1, paid:2, delivered:3, completed:4 };
  return map[tab];
};

const getList = async () => {
  try {
    const user = getCurrentUser();
    if (!user || !user.id) {
      buyerAllOrders.value = [];
      sellerAllOrders.value = [];
      orderList.value = [];
      return;
    }
    // 切换账号后同步加载该账号的已查看记录
    loadViewed();
    if (viewMode.value === 'seller') {
      // 卖家视角：查询我卖出的全部订单；前端再按 sellerId 兜底过滤，防止串单
      const res = await request.get(`/order/seller/${user.id}/all`);
      const list = res.code === 200 ? (res.data || []) : [];
      sellerAllOrders.value = list.filter(o => o.sellerId === user.id);
    } else {
      // 买家视角：查询我买到的全部订单；前端再按 buyerId 兜底过滤，防止串单
      const res = await request.get('/order/listAll', { params: { buyerId: user.id } });
      const list = res.code === 200 ? (res.data || []) : [];
      buyerAllOrders.value = list.filter(o => o.buyerId === user.id);
    }
    // 同步给共享角标状态
    setOrders(viewMode.value, allSource.value);
    applyFilter();
  } catch (err) {
    console.error('获取订单列表失败:', err);
  }
};

// 按当前 tab 对全量数据做前端筛选
const applyFilter = () => {
  const status = getStatusValue(activeTab.value);
  orderList.value = status
    ? allSource.value.filter(o => o.status === status)
    : allSource.value;
};

// 切换买家/卖家视角
const switchView = (mode) => {
  if (viewMode.value === mode) return;
  viewMode.value = mode;
  activeTab.value = 'all';
  getList();
};

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

const goToProduct = (order) => {
  // 点击商品区同样视为已查看该订单，角标消除
  markViewed(order);
  router.push(`/product/detail/${order.productId}`);
};

// 点击订单卡片 → 标记该订单在当前状态下已查看（角标消除）→ 订单详情页
const goOrderDetail = (order) => {
  markViewed(order);
  router.push(`/order/detail/${order.id}`);
};

const handlePay = (order) => {
  markViewed(order);
  currentOrder.value = order;
  selectedPayMethod.value = '';
  getUserBalance();
  showPayModal.value = true;
};

const confirmPayMethod = async () => {
  if (!selectedPayMethod.value) {
    ElMessage.warning('请选择支付方式');
    return;
  }
  if (!currentOrder.value) {
    showPayModal.value = false;
    return;
  }
  
  try {
    showPayModal.value = false;
    
    if (selectedPayMethod.value === 'balance') {
      await ElMessageBox.confirm(
        '确认使用余额支付该订单？\n\n订单号：' + currentOrder.value.orderNo + '\n金额：¥' + currentOrder.value.price, 
        '💳 余额支付确认', 
        {
          type: 'warning',
          confirmButtonText: '确认支付',
          cancelButtonText: '取消'
        }
      );
      
      const res = await request.put(`/order/pay/${currentOrder.value.id}`);
      if (res.code === 200) {
        ElMessage.success('支付成功！等待卖家发货');
        // 买家自己支付的，新状态不再提醒买家自己（卖家侧仍会收到待发货角标）
        markViewed({ id: currentOrder.value.id, status: 2 });
        getList();
        getUserBalance();
      } else {
        ElMessage.error(res.msg || '支付失败');
      }
    } 
    else if (selectedPayMethod.value === 'alipay') {
      window.location.href = `/alipay/pay?orderId=${currentOrder.value.id}`;
    }
  } catch (err) {
    console.error('支付失败:', err);
    if (err !== 'cancel') {
      ElMessage.error('支付失败，请稍后重试');
    }
  }
};

// 取消订单（买家，仅待支付状态展示入口）
const handleCancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消该订单吗？取消后商品将重新上架。',
      '取消订单',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    );

    const res = await request.post(`/order/cancel/${order.id}`);
    if (res.code === 200) {
      ElMessage.success('订单已取消');
      getList();
    } else {
      ElMessage.error(res.msg || '取消失败');
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('取消失败，请稍后重试');
  }
};

const viewAccountInfo = async (order) => {
  markViewed(order);
  currentAccountOrder.value = order;
  showAccountModal.value = true;
  
  try {
    const res = await request.get(`/order/${order.id}/account-info`);
    if (res.code === 200) {
      accountInfo.value = res.data.accountInfo || '暂无账号信息';
    } else {
      accountInfo.value = res.msg || '获取账号信息失败';
    }
  } catch (err) {
    console.error('获取账号信息失败:', err);
    accountInfo.value = '获取账号信息失败';
  }
};

const handleConfirm = async (order) => {
  try {
    await ElMessageBox.confirm(
      '请确认您已收到游戏账号且账号信息正确！',
      '📦 确认收货',
      { type: 'warning' }
    );
    
    const res = await request.post(`/order/confirm/${order.id}`);
    if (res.code === 200) {
      ElMessage.success('交易完成');
      // 买家自己确认的，已完成状态不再提醒买家（卖家侧会收到款项到账角标）
      markViewed({ id: order.id, status: 4 });
      getList();
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败');
  }
};

// 打开发货弹窗（卖家）
const openDeliver = (order) => {
  markViewed(order);
  currentOrder.value = order;
  deliverAccountInfo.value = '';
  showDeliverModal.value = true;
};

// 确认发货（卖家）
const confirmDeliver = async () => {
  if (!deliverAccountInfo.value.trim()) {
    ElMessage.warning('请填写账号交付信息');
    return;
  }
  delivering.value = true;
  try {
    const res = await request.post(`/order/deliver/${currentOrder.value.id}`, {
      accountInfo: deliverAccountInfo.value
    });
    if (res.code === 200) {
      ElMessage.success('发货成功，等待买家确认收货');
      showDeliverModal.value = false;
      // 卖家自己发的货，已发货状态不再提醒卖家（买家侧会收到待收货角标）
      markViewed({ id: currentOrder.value.id, status: 3 });
      getList();
    } else {
      ElMessage.error(res.msg || '发货失败');
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '发货失败');
  } finally {
    delivering.value = false;
  }
};

// tab 切换时只做前端筛选，无需重新请求
watch(activeTab, () => {
  applyFilter();
});

onMounted(() => {
  getList();
  getUserBalance();

  window.addEventListener('focus', () => {
    getList();
    getUserBalance();
  });
});
</script>

<style scoped>
.order-list {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.view-switch {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.view-item {
  padding: 8px 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-regular);
  transition: all 0.3s;
}

.view-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.view-item.active {
  background: var(--color-primary-gradient);
  color: #fff;
}

.tabs {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--color-border);
}

.deliver-form .deliver-order-no {
  font-weight: 600;
  margin-bottom: 10px;
}

.deliver-form .deliver-tip {
  color: #e6a23c;
  font-size: 13px;
  margin-bottom: 10px;
  line-height: 1.5;
}

.tab-item {
  padding: 10px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-icon {
  font-size: 15px;
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.tab-item.active {
  background: var(--color-primary-gradient);
  color: white;
}

.tab-count {
  background: #f56c6c;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 5px;
}

.tab-item.active .tab-count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.order-container {
  display: grid;
  gap: 20px;
}

.order-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.order-card.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.order-card.clickable:hover {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.18);
  transform: translateY(-2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid var(--color-border);
}

.order-no {
  font-size: 14px;
  color: var(--color-text-regular);
}

.order-status {
  font-size: 14px;
  font-weight: 700;
  padding: 5px 13px;
  border-radius: 20px;
  border: 1px solid transparent;
  letter-spacing: 0.3px;
}

/* 浅彩底 + 同色系深色文字 + 描边，保证对比度 */
.order-status.unpaid {
  background: var(--color-warning-bg);
  color: #a86400;
  border-color: #f3d19e;
}

.order-status.paid {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-link);
  border-color: var(--color-border);
}

.order-status.delivered {
  background: var(--color-success-bg);
  color: #3d8a1f;
  border-color: #b3e19d;
}

.order-status.completed {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text-regular);
  border-color: var(--color-border);
}

.order-items {
  padding: 15px 20px;
}

.item-card {
  display: flex;
  gap: 15px;
  cursor: pointer;
}

.item-img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.item-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.item-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.item-price {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-top: 1px solid var(--color-border);
}

.order-total {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.total-label {
  font-size: 14px;
  color: var(--color-text-regular);
}

.total-amount {
  font-size: 20px;
  font-weight: 600;
  color: #f56c6c;
}

.order-actions {
  display: flex;
  gap: 10px;
}

.pay-btn, .confirm-btn {
  padding: 8px 20px;
  border-radius: 6px;
}

.status-text {
  color: var(--color-text-secondary);
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.status-text .el-icon {
  font-size: 15px;
  color: var(--color-primary);
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--color-text-secondary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.empty-action {
  margin-top: 16px;
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
  border-radius: 10px;
  margin-right: 15px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-violet);
  color: #fff;
  font-size: 22px;
}

.method-icon--alipay {
  background: var(--gradient-blue);
}

.dialog-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.dialog-title .el-icon {
  color: var(--color-primary);
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
  font-size: 15px;
  color: transparent;
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

.account-info {
  padding: 10px;
}

.info-section {
  display: flex;
  margin-bottom: 15px;
}

.info-section label {
  width: 100px;
  font-weight: 600;
  color: var(--color-text-regular);
  flex-shrink: 0;
}

.info-section span {
  flex: 1;
  color: var(--color-text-primary);
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
}

.no-info {
  text-align: center;
  padding: 40px;
  color: var(--color-text-secondary);
}

.info-btn {
  padding: 8px 16px;
  border-radius: 6px;
}

/* ===== 响应式适配 ===== */
@media (max-width: 768px) {
  .order-list {
    padding: 12px;
  }

  .view-switch {
    gap: 8px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 4px;
  }

  .view-item {
    padding: 6px 16px;
    font-size: 13px;
    white-space: nowrap;
  }

  .item-card {
    gap: 10px;
    padding: 12px;
  }

  .item-img {
    width: 72px;
    height: 72px;
    flex-shrink: 0;
  }

  .item-name {
    font-size: 14px;
  }

  .order-card-footer {
    flex-wrap: wrap;
    gap: 8px;
  }

  .info-section {
    flex-direction: column;
    gap: 4px;
  }

  .info-section label {
    width: auto;
    font-size: 13px;
  }

  .action-buttons {
    width: 100%;
    justify-content: flex-end;
  }

  .action-buttons .el-button {
    font-size: 12px;
    padding: 6px 12px;
  }
}

@media (max-width: 480px) {
  .item-img {
    width: 60px;
    height: 60px;
  }

  .order-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
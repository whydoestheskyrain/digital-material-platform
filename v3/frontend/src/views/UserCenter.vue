<template>
  <div class="mine-container">
    <!-- 平板/手机：汉堡按钮展开侧边菜单 -->
    <button class="menu-toggle" @click="menuOpen = true" aria-label="打开菜单">
      <el-icon><Menu /></el-icon>
      <span>个人中心菜单</span>
    </button>
    <div v-if="menuOpen" class="mine-mask" @click="menuOpen = false"></div>

    <!-- 左侧侧边栏菜单 -->
    <div class="mine-left" :class="{ open: menuOpen }">
      <div class="user-info">
        <!-- 头像上传 -->
        <el-upload
          class="avatar-upload"
          :http-request="customUploadAvatar"
          :show-file-list="false"
        >
          <img v-if="user.avatar" :src="user.avatar" class="avatar-img" />
          <img v-else src="/images/default-avatar.jpg" class="avatar-img" />
        </el-upload>

        <div class="name">{{ user.nickname || user.username }}</div>

        <!-- 余额信息 -->
        <div class="balance-info">
          <div class="balance-main">可用余额：<span class="amount-num">¥{{ formatMoney(balance) }}</span></div>
          <div class="balance-frozen">
            冻结余额：<span class="amount-num">¥{{ formatMoney(frozenBalance) }}</span>
            <span class="frozen-tip">（交易中托管资金，交易完成自动解冻）</span>
          </div>
          <el-button type="primary" size="small" @click="rechargeDialogVisible = true">
            <el-icon><Plus /></el-icon> 充值
          </el-button>
        </div>
      </div>

      <div
        class="menu-item"
        :class="{active: menuKey === 'info'}"
        @click="selectMenu('info')"
      >
        <el-icon class="menu-icon"><User /></el-icon> 基本资料
      </div>
      <div
        class="menu-item"
        :class="{active: menuKey === 'buyOrder'}"
        @click="selectMenu('buyOrder')"
      >
        <el-icon class="menu-icon"><ShoppingCart /></el-icon> 我买到的订单
      </div>
      <div
        class="menu-item"
        :class="{active: menuKey === 'sellOrder'}"
        @click="selectMenu('sellOrder')"
      >
        <el-icon class="menu-icon"><ShoppingBag /></el-icon> 我卖出的订单
      </div>
      <div
        class="menu-item"
        :class="{active: menuKey === 'bill'}"
        @click="selectMenu('bill')"
      >
        <el-icon class="menu-icon"><Wallet /></el-icon> 我的账单
      </div>
      <div
        class="menu-item"
        :class="{active: menuKey === 'editNick'}"
        @click="selectMenu('editNick')"
      >
        <el-icon class="menu-icon"><EditPen /></el-icon> 修改昵称
      </div>
      <div
        class="menu-item"
        :class="{active: menuKey === 'editPwd'}"
        @click="selectMenu('editPwd')"
      >
        <el-icon class="menu-icon"><Lock /></el-icon> 修改密码
      </div>
      <!-- 新增：修改手机号侧边菜单，放到左侧正确位置 -->
      <div
        class="menu-item"
        :class="{active: menuKey === 'editPhone'}"
        @click="selectMenu('editPhone')"
      >
        <el-icon class="menu-icon"><Iphone /></el-icon> 修改手机号
      </div>
      <!-- 管理员专属菜单 -->
      <div
        v-if="user.role?.toUpperCase() === 'ADMIN'"
        class="menu-item"
        @click="menuOpen = false; goToAdmin()"
      >
        <el-icon class="menu-icon"><Setting /></el-icon> 管理控制台
      </div>
      <div class="menu-item logout" @click="handleLogout">
        <el-icon class="menu-icon"><SwitchButton /></el-icon> 退出登录
      </div>
    </div>

    <!-- 右侧内容区域 -->
    <div class="mine-right">
      <!-- 基本资料 -->
      <div v-if="menuKey === 'info'" class="content-card">
        <h3>基本资料</h3>
        <div class="info-row"><span>用户ID</span>{{ user.id }}</div>
        <div class="info-row"><span>用户名</span>{{ user.username }}</div>
        <div class="info-row"><span>昵称</span>{{ user.nickname }}</div>
        <div class="info-row"><span>手机号</span>{{ desensitizePhone(user.phone) }}</div>
        <div class="info-row">
          <span>减少界面动效</span>
          <el-switch
            v-model="reduceMotion"
            active-text="开启（无障碍）"
            inline-prompt
            @change="toggleReduceMotion"
          />
        </div>
      </div>

      <!-- 买家订单 -->
      <div v-if="menuKey === 'buyOrder'" class="content-card">
        <h3>我买到的订单</h3>
        <div v-loading="loadingBuyerOrders" element-loading-text="加载订单中...">
          <div v-if="!loadingBuyerOrders && buyerList.length === 0" class="empty-tip">
            <div class="empty-icon">📦</div>
            <span>暂无买到的订单</span>
            <el-button type="primary" size="small" @click="router.push('/')">
              <el-icon><ShoppingCart /></el-icon> 去逛逛商品
            </el-button>
          </div>
          <el-table v-else :data="buyerList" border size="small" style="width:100%">
            <el-table-column label="订单ID" prop="id" />
            <el-table-column label="商品名称">
              <template #default="scope">
                {{ scope.row.productName || scope.row.product?.title || '未命名商品' }}
              </template>
            </el-table-column>
            <el-table-column label="价格" prop="price" />
            <el-table-column label="状态">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260">
  <template #default="scope">
    <!-- 待支付订单显示支付按钮 -->
    <el-button
      v-if="scope.row.status === ORDER_STATUS.PENDING"
      type="success"
      size="small"
      @click="payOrder(scope.row.id)"
    >立即支付</el-button>
    <!-- 待支付订单显示取消按钮（可选） -->
    <el-button
      v-if="scope.row.status === ORDER_STATUS.PENDING"
      type="warning"
      size="small"
      @click="cancelOrder(scope.row.id)"
    >取消订单</el-button>
    <!-- 已发货订单显示确认收货按钮 -->
    <el-button
      v-if="scope.row.status === ORDER_STATUS.SHIPPED"
      type="primary"
      size="small"
      @click="confirmReceive(scope.row.id)"
    >确认收货</el-button>
  </template>
</el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 卖家订单 -->
      <div v-if="menuKey === 'sellOrder'" class="content-card">
        <h3>我卖出的订单</h3>
        <div v-loading="loadingSellerOrders" element-loading-text="加载订单中...">
          <div v-if="!loadingSellerOrders && sellerList.length === 0" class="empty-tip">
            <div class="empty-icon">📦</div>
            <span>暂无卖出的订单</span>
            <el-button type="primary" size="small" @click="router.push('/product/add')">
              <el-icon><Promotion /></el-icon> 去发布商品
            </el-button>
          </div>
          <el-table v-else :data="sellerList" border size="small" style="width:100%">
            <el-table-column label="订单ID" prop="id" />
            <el-table-column label="商品名称">
              <template #default="scope">
                {{ scope.row.productName || scope.row.product?.title || '未命名商品' }}
              </template>
            </el-table-column>
            <el-table-column label="价格" prop="price" />
            <el-table-column label="状态">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <!-- 待发货（status=2）时卖家可发货 -->
                <el-button
                  v-if="scope.row.status === ORDER_STATUS.PAID"
                  type="success"
                  size="small"
                  @click="openDeliver(scope.row)"
                >
                  <el-icon><Van /></el-icon> 发货
                </el-button>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 我的账单 -->
      <div v-if="menuKey === 'bill'" class="content-card">
        <h3><el-icon class="h3-icon"><Wallet /></el-icon> 我的账单</h3>

        <!-- 资金概览 -->
        <div class="bill-summary">
          <div class="bill-card bill-card--blue">
            <div class="bill-card-label">可用余额</div>
            <div class="bill-card-value amount-num">¥{{ formatMoney(balance) }}</div>
          </div>
          <div class="bill-card bill-card--orange">
            <div class="bill-card-label">冻结余额</div>
            <div class="bill-card-value amount-num">¥{{ formatMoney(frozenBalance) }}</div>
          </div>
          <div class="bill-card bill-card--green">
            <div class="bill-card-label">累计充值</div>
            <div class="bill-card-value amount-num">¥{{ formatMoney(billStats.totalRecharge) }}</div>
          </div>
          <div class="bill-card bill-card--purple">
            <div class="bill-card-label">卖出收入</div>
            <div class="bill-card-value amount-num">¥{{ formatMoney(billStats.totalIncome) }}</div>
          </div>
        </div>

        <el-tabs v-model="billTab" class="bill-tabs">
          <!-- 资金流水 -->
          <el-tab-pane label="资金流水" name="logs">
            <el-table :data="billLogs" border size="small" v-loading="loadingBill" style="width:100%">
              <el-table-column label="时间" prop="createTime" width="160" />
              <el-table-column label="类型" width="100">
                <template #default="scope">
                  <el-tag :type="logTypeMap[scope.row.type]?.tag || 'info'" size="small">
                    {{ logTypeMap[scope.row.type]?.text || scope.row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="备注" prop="remark" min-width="200" show-overflow-tooltip />
              <el-table-column label="金额" width="120" align="right">
                <template #default="scope">
                  <span class="amount-num" :class="Number(scope.row.amount) >= 0 ? 'money-in' : 'money-out'">
                    {{ Number(scope.row.amount) >= 0 ? '+' : '' }}{{ formatMoney(scope.row.amount) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="变动前余额" width="120" align="right">
                <template #default="scope"><span class="amount-num">{{ formatMoney(scope.row.balanceBefore) }}</span></template>
              </el-table-column>
              <el-table-column label="变动后余额" width="120" align="right">
                <template #default="scope"><span class="amount-num">{{ formatMoney(scope.row.balanceAfter) }}</span></template>
              </el-table-column>
              <template #empty>暂无资金流水</template>
            </el-table>
          </el-tab-pane>

          <!-- 充值记录 -->
          <el-tab-pane label="充值记录" name="recharge">
            <el-table :data="billRecharges" border size="small" v-loading="loadingBill" style="width:100%">
              <el-table-column label="充值单号" prop="orderNo" min-width="220" show-overflow-tooltip />
              <el-table-column label="金额" width="120" align="right">
                <template #default="scope">
                  <span class="money-in amount-num">+{{ formatMoney(scope.row.amount) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : scope.row.status === 2 ? 'info' : 'warning'" size="small">
                    {{ scope.row.status === 1 ? '充值成功' : scope.row.status === 2 ? '已取消' : '待支付' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="支付宝交易号" prop="tradeNo" min-width="180" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.tradeNo || '-' }}</template>
              </el-table-column>
              <el-table-column label="创建时间" prop="createTime" width="160" />
              <el-table-column label="支付时间" prop="payTime" width="160">
                <template #default="scope">{{ scope.row.payTime || '-' }}</template>
              </el-table-column>
              <template #empty>暂无充值记录</template>
            </el-table>
          </el-tab-pane>

          <!-- 交易订单 -->
          <el-tab-pane label="交易订单" name="orders">
            <el-table :data="billOrders" border size="small" v-loading="loadingBill" style="width:100%">
              <el-table-column label="角色" width="80">
                <template #default="scope">
                  <el-tag :type="scope.row.tradeRole === '买入' ? 'warning' : 'success'" size="small">
                    {{ scope.row.tradeRole }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="订单号" prop="orderNo" min-width="200" show-overflow-tooltip />
              <el-table-column label="金额" width="120" align="right">
                <template #default="scope">¥{{ Number(scope.row.price).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusTagType(scope.row.status)" size="small">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="支付方式" width="100">
                <template #default="scope">
                  {{ scope.row.paymentMethod === 'alipay' ? '支付宝' : scope.row.paymentMethod === 'balance' ? '余额' : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="下单时间" prop="createTime" width="160" />
              <template #empty>暂无交易订单</template>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 修改昵称 -->
      <div v-if="menuKey === 'editNick'" class="content-card">
        <h3>修改昵称</h3>
        <el-input v-model="editNickname" placeholder="请输入新昵称" class="responsive-input" />
        <el-button type="primary" @click="updateNickname">保存修改</el-button>
      </div>

      <!-- 修改密码 -->
      <div v-if="menuKey === 'editPwd'" class="content-card">
        <h3>修改密码</h3>
        <el-input v-model="oldPassword" placeholder="旧密码" type="password" class="responsive-input" />
        <el-input v-model="newPassword" placeholder="新密码" type="password" class="responsive-input" />
        <el-input v-model="confirmPassword" placeholder="确认新密码" type="password" class="responsive-input" />
        <el-button type="primary" @click="updatePassword">确认修改</el-button>
      </div>

      <!-- 修改手机号面板 -->
      <div v-if="menuKey === 'editPhone'" class="content-card">
        <h3>修改绑定手机号</h3>
        <el-input 
          v-model="editPhone" 
          placeholder="请输入新的11位手机号" 
          class="responsive-input"
          maxlength="11"
        />
        <el-button type="primary" @click="submitUpdatePhone">保存修改</el-button>
      </div>
    </div>

    <!-- 充值弹窗 -->
    <el-dialog title="充值" v-model="rechargeDialogVisible" width="90%" :style="{ maxWidth: '360px' }">
      <el-input 
        v-model="rechargeAmount" 
        type="number" 
        placeholder="请输入充值金额"
        :precision="2"
        :min="0.01"
        step="0.01"
      />
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <!-- 卖家发货弹窗 -->
    <el-dialog
      v-model="deliverDialogVisible"
      width="450px"
      :close-on-click-modal="false"
    >
      <template #header>
        <span class="dialog-title"><el-icon><Van /></el-icon> 填写账号信息并发货</span>
      </template>
      <div v-if="currentDeliverOrder" class="deliver-form">
        <p class="deliver-order-no">订单号：{{ currentDeliverOrder.orderNo }}</p>
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
        <el-button @click="deliverDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="delivering" @click="confirmDeliver">
          <el-icon><Van /></el-icon> 确认发货
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getCurrentToken, getCurrentUser, removeAccount, updateCurrentUser } from '@/utils/accountManager'
import { formatMoney } from '@/utils/constants'
// 订单状态常量【和后端完全对齐】
const ORDER_STATUS = {
  PENDING: 1,    // 待支付
  PAID: 2,       // 待发货
  SHIPPED: 3,    // 待收货
  COMPLETED: 4,  // 已完成
  CANCELLED: 5   // 已取消
}

const statusMap = {
  [ORDER_STATUS.PENDING]: '待支付',
  [ORDER_STATUS.PAID]: '待发货',
  [ORDER_STATUS.SHIPPED]: '待收货',
  [ORDER_STATUS.COMPLETED]: '已完成',
  [ORDER_STATUS.CANCELLED]: '已取消'
}
const statusTypeMap = {
  [ORDER_STATUS.PENDING]: 'warning',
  [ORDER_STATUS.PAID]: 'primary',
  [ORDER_STATUS.SHIPPED]: 'success',
  [ORDER_STATUS.COMPLETED]: 'info',
  [ORDER_STATUS.CANCELLED]: 'danger'
}

const router = useRouter()
const menuKey = ref('info')
// 平板/手机下侧边菜单的展开状态（汉堡按钮触发）
const menuOpen = ref(false)
const selectMenu = (key) => {
  menuKey.value = key
  menuOpen.value = false
}
// 全局动效开关（无障碍）：与 localStorage + html.reduce-motion 类同步
const reduceMotion = ref(localStorage.getItem('ui_reduce_motion') === '1')
const toggleReduceMotion = (val) => {
  localStorage.setItem('ui_reduce_motion', val ? '1' : '0')
  document.documentElement.classList.toggle('reduce-motion', val)
}
const user = ref({})
const buyerList = ref([])
const sellerList = ref([])
const editNickname = ref('')
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loadingBuyerOrders = ref(false)
const loadingSellerOrders = ref(false)
// 卖家发货弹窗
const deliverDialogVisible = ref(false)
const currentDeliverOrder = ref(null)
const deliverAccountInfo = ref('')
const delivering = ref(false)
// 状态文本
const getStatusText = (s) => statusMap[s] || '未知'
const getStatusTagType = (s) => statusTypeMap[s] || ''

// ========== 我的账单 ==========
const billTab = ref('logs')
const billLogs = ref([])
const billRecharges = ref([])
const billOrders = ref([])
const loadingBill = ref(false)
const billStats = ref({ totalRecharge: 0, totalIncome: 0 })

// 流水类型展示映射（recharge充值/pay支出/income收入/refund退款/freeze冻结）
const logTypeMap = {
  recharge: { text: '充值', tag: 'success' },
  pay: { text: '购买支出', tag: 'danger' },
  income: { text: '卖出收入', tag: 'success' },
  refund: { text: '退款', tag: 'warning' },
  freeze: { text: '资金冻结', tag: 'info' }
}

const loadBill = async () => {
  if (!user.value?.id) return
  loadingBill.value = true
  try {
    const [logsRes, rechargeRes, buyerRes, sellerRes] = await Promise.all([
      request.get('/user/transactions'),
      request.get('/recharge/myRecords'),
      request.get('/order/listAll', { params: { buyerId: user.value.id } }),
      request.get(`/order/seller/${user.value.id}/all`)
    ])
    billLogs.value = logsRes.code === 200 ? (logsRes.data || []) : []
    billRecharges.value = rechargeRes.code === 200 ? (rechargeRes.data || []) : []

    // 买卖订单合并并标记角色
    const buys = (buyerRes.code === 200 ? buyerRes.data || [] : []).map(o => ({ ...o, tradeRole: '买入' }))
    const sells = (sellerRes.code === 200 ? sellerRes.data || [] : []).map(o => ({ ...o, tradeRole: '卖出' }))
    billOrders.value = [...buys, ...sells].sort((a, b) =>
      new Date(b.createTime || 0) - new Date(a.createTime || 0)
    )

    // 汇总：累计充值（仅成功单）、卖出收入
    billStats.value.totalRecharge = billRecharges.value
      .filter(r => r.status === 1)
      .reduce((sum, r) => sum + Number(r.amount || 0), 0)
    billStats.value.totalIncome = billLogs.value
      .filter(l => l.type === 'income')
      .reduce((sum, l) => sum + Number(l.amount || 0), 0)
  } catch (e) {
    console.error('加载账单失败', e)
    ElMessage.error('加载账单失败')
  } finally {
    loadingBill.value = false
  }
}

// 修改手机号弹窗变量
const editPhone = ref('')
const editPhoneDialogVisible = ref(false)
// 打开修改手机号弹窗，回填当前手机号
// 提交修改手机号
const submitUpdatePhone = async () => {
    const phone = editPhone.value.trim()
    // 前端格式校验
    const phoneReg = /^1[3-9]\d{9}$/
    if (!phoneReg.test(phone)) {
        ElMessage.warning('请输入合法11位手机号')
        return
    }
    try {
        const res = await request.put('/user/update/phone', { phone })
        if (res.code === 200) {
            ElMessage.success('手机号修改成功')
            // 更新本地用户缓存
            user.value.phone = phone
            updateCurrentUser(user.value)
            editPhoneDialogVisible.value = false
        } else {
            ElMessage.error(res.msg)
        }
    } catch (err) {
        ElMessage.error(err.response?.data?.msg || '修改失败')
    }
}



// 支付订单
const payOrder = async (orderId) => {
  try {
    const res = await request.put(`/order/pay/${orderId}`)
    if (res.code === 200) {
      ElMessage.success('支付成功，等待卖家发货')
      // 刷新订单列表和余额
      await refreshOrdersByMenu()
      await getBalance()
    } else {
      ElMessage.error(res.msg || '支付失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '支付失败，请稍后重试')
  }
}
// 手机号脱敏（增强健壮性）
const desensitizePhone = (phone) => {
  if (!phone || phone.length !== 11) return phone || '未绑定'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// ========== 余额相关 ==========
const balance = ref(0)
const frozenBalance = ref(0)
const rechargeAmount = ref(null)
const rechargeDialogVisible = ref(false)

// 获取余额
const getBalance = async () => {
  try {
    const res = await request.get('/user/balance')
    if (res.code === 200) {
      balance.value = res.data.balance || 0
      frozenBalance.value = res.data.frozenBalance ?? res.data.frozen ?? 0
    }
  } catch (err) {
    console.error('获取余额失败', err)
  }
}

// 充值（增强校验）
const handleRecharge = async () => {
  let amount = parseFloat(rechargeAmount.value)
  if (isNaN(amount) || amount < 0.01) {
    ElMessage.warning('充值金额至少为0.01元')
    return
  }
  // 保留两位小数
  amount = Math.round(amount * 100) / 100
  try {
    const res = await request.post('/recharge/create', { amount })
    if (res.code === 200) {
      // 后端返回的已经是完整的表单HTML，直接写入页面即可自动提交
      document.write(res.data)
      document.close()
      rechargeDialogVisible.value = false
      rechargeAmount.value = null
    } else {
      ElMessage.error(res.msg || '充值失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '充值失败，请稍后重试')
  }
}

// 加载买家订单
const loadBuyerOrders = async () => {
  if (!user.value?.id) {
    console.log('用户id为空')
    return
  }
  loadingBuyerOrders.value = true
  try {
    // 正确后端地址：listAll 传 buyerId 参数
    const res = await request.get(`/order/listAll`, {
      params: { buyerId: user.value.id }
    })
    console.log('买家订单返回', res)
    buyerList.value = res.code === 200 ? res.data : []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载买家订单失败')
  } finally {
    loadingBuyerOrders.value = false
  }
}

// 加载卖家订单
const loadSellerOrders = async () => {
  if (!user.value?.id) {
    console.log('用户id为空')
    return
  }
  loadingSellerOrders.value = true
  try {
    // 正确后端地址 /seller/{id}/all
    const res = await request.get(`/order/seller/${user.value.id}/all`)
    console.log('卖家订单返回', res)
    sellerList.value = res.code === 200 ? res.data : []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载卖家订单失败')
  } finally {
    loadingSellerOrders.value = false
  }
}

// 打开发货弹窗
const openDeliver = (order) => {
  currentDeliverOrder.value = order
  deliverAccountInfo.value = ''
  deliverDialogVisible.value = true
}

// 确认发货
const confirmDeliver = async () => {
  if (!deliverAccountInfo.value.trim()) {
    ElMessage.warning('请填写账号交付信息')
    return
  }
  delivering.value = true
  try {
    const res = await request.post(`/order/deliver/${currentDeliverOrder.value.id}`, {
      accountInfo: deliverAccountInfo.value
    })
    if (res.code === 200) {
      ElMessage.success('发货成功，等待买家确认收货')
      deliverDialogVisible.value = false
      loadSellerOrders()
    } else {
      ElMessage.error(res.msg || '发货失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '发货失败')
  } finally {
    delivering.value = false
  }
}

// 根据当前菜单刷新订单
const refreshOrdersByMenu = () => {
  if (menuKey.value === 'buyOrder') {
    loadBuyerOrders()
  } else if (menuKey.value === 'sellOrder') {
    loadSellerOrders()
  }
}

// 头像上传（添加文件类型和大小校验）
const customUploadAvatar = async (options) => {
  const file = options.file
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB！')
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/user/upload/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200) {
      user.value.avatar = res.data
      updateCurrentUser(user.value)
      ElMessage.success('头像更新成功')
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (err) {
    console.error('上传失败', err)
    ElMessage.error('上传失败，请稍后重试')
  }
}

// 修改昵称
const updateNickname = async () => {
  if (!editNickname.value.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  try {
    await request.put('/user/update/nickname', {
      id: user.value.id,
      nickname: editNickname.value
    })
    user.value.nickname = editNickname.value
    updateCurrentUser(user.value)
    ElMessage.success('昵称修改成功')
  } catch {
    ElMessage.error('修改失败')
  }
}

// 修改密码
const updatePassword = async () => {
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    ElMessage.warning('请填写完整')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.error('两次密码不一致')
    return
  }
  try {
    await request.put('/user/update/password', {
      id: user.value.id,
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })
    ElMessage.success('密码修改成功，请重新登录')
    // 清除当前账号并跳转登录
    const current = getCurrentUser()
    if (current) {
      removeAccount('user_' + current.id)
    }
    delete request.defaults.headers.common['Authorization']
    router.push('/login')
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '旧密码错误或修改失败')
  }
}

// 取消订单（添加二次确认）
const cancelOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.post(`/order/cancel/${orderId}`)
    ElMessage.success('已取消')
    refreshOrdersByMenu() // 刷新当前菜单订单
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 确认收货（添加二次确认）
const confirmReceive = async (orderId) => {
  try {
    await ElMessageBox.confirm('请确认已收到商品，确认后将完成交易。', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'info'
    })
    await request.put(`/order/confirm/${orderId}`)
    ElMessage.success('收货成功')
    refreshOrdersByMenu()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('确认收货失败')
    }
  }
}

// 跳转到管理控制台
const goToAdmin = () => {
  router.push('/admin/home')
}

// 退出登录
const handleLogout = () => {
  const current = getCurrentUser()
  if (current) {
    removeAccount('user_' + current.id)
  }
  delete request.defaults.headers.common['Authorization']
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 监听菜单切换，自动刷新对应订单/账单
watch(menuKey, (newKey) => {
  if (newKey === 'buyOrder') {
    loadBuyerOrders()
  } else if (newKey === 'sellOrder') {
    loadSellerOrders()
  } else if (newKey === 'bill') {
    loadBill()
    getBalance()
  }
})

// 生命周期
onMounted(async () => {
  const token = getCurrentToken()
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const res = await request.get('/user/info')
    if (res.code === 200 && res.data?.id) {
      user.value = res.data
      editNickname.value = user.value.nickname
      // 并发加载余额和初始订单
      await Promise.allSettled([getBalance(), refreshOrdersByMenu()])
    } else {
      ElMessage.error('获取用户信息失败，请重新登录')
      router.push('/login')
    }
  } catch (err) {
    ElMessage.error('获取用户信息失败')
    router.push('/login')
  }
})
</script>

<style scoped>
.balance-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
  font-size: 14px;
}
.balance-main {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}
.balance-frozen {
  font-size: 13px;
  color: #409EFF;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.frozen-tip {
  font-size: 11px;
  color: var(--color-text-secondary);
  font-weight: 400;
}
.mine-container {
  display: flex;
  max-width: 1200px;
  margin: 40px auto;
  gap: 20px;
  padding: 0 20px;
}

/* 汉堡按钮：仅 ≤992px 显示 */
.menu-toggle {
  display: none;
  align-items: center;
  gap: 8px;
  position: fixed;
  top: 14px;
  left: 14px;
  z-index: 200;
  padding: 8px 14px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary-gradient);
  border: none;
  border-radius: var(--radius-btn);
  box-shadow: var(--shadow-card-hover);
  cursor: pointer;
}

.mine-mask {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 150;
}

.mine-left {
  width: 220px;
  flex-shrink: 0;
  align-self: flex-start;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.user-info {
  padding: 30px 20px;
  text-align: center;
  border-bottom: 1px solid var(--color-border);
}
.avatar-img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  margin: 0 auto 10px;
  display: block;
}
.avatar-upload {
  cursor: pointer;
}
.name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 10px;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-left: 4px solid transparent;
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text-regular);
  transition: all 0.2s;
}
.menu-item .menu-icon {
  font-size: 17px;
  flex-shrink: 0;
}
.menu-item.active {
  background: var(--color-primary-gradient);
  color: #fff;
  font-weight: 600;
  border-left-color: rgba(255, 255, 255, 0.7);
}
.menu-item:hover:not(.active) {
  background: rgba(255, 255, 255, 0.05);
}
.menu-item.logout {
  color: var(--color-danger);
}
.menu-item.logout.active {
  color: #fff;
}
.logout {
  margin-top: 20px;
}

.mine-right {
  flex: 1;
  min-width: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 30px;
}

.h3-icon {
  vertical-align: -2px;
  margin-right: 4px;
  font-size: 19px;
  color: var(--color-primary);
}

.dialog-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}
.content-card h3 {
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}
.info-row {
  font-size: 15px;
  margin: 12px 0;
}
.info-row span {
  display: inline-block;
  width: 80px;
  color: var(--color-text-regular);
}
.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  text-align: center;
  padding: 60px 0;
  color: var(--color-text-secondary);
}
.empty-icon {
  font-size: 50px;
  margin-bottom: 6px;
}
.text-muted {
  color: #bbb;
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

/* ========== 我的账单 ========== */
.bill-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}
.bill-card {
  border-radius: 10px;
  padding: 18px 16px;
  color: #fff;
}
.bill-card--blue { background: var(--gradient-blue); }
.bill-card--orange { background: var(--gradient-orange); }
.bill-card--green { background: var(--gradient-green); }
.bill-card--purple { background: var(--gradient-violet); }
.bill-card-label {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}
.bill-card-value {
  font-size: 22px;
  font-weight: 700;
}
.bill-tabs {
  margin-top: 4px;
}
.money-in {
  color: #1ea97c;
  font-weight: 600;
}
.money-out {
  color: #f04d5a;
  font-weight: 600;
}
/* ========== 响应式：平板（≤992）左侧菜单折叠为汉堡抽屉 ========== */
@media (max-width: 992px) {
  .mine-container {
    margin: 64px auto 30px;
    flex-direction: column;
  }

  .menu-toggle {
    display: inline-flex;
  }

  .mine-mask {
    display: block;
  }

  .mine-left {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 260px;
    z-index: 160;
    border-radius: 0;
    overflow-y: auto;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: var(--shadow-dialog);
  }

  .mine-left.open {
    transform: translateX(0);
  }

  .mine-right {
    width: 100%;
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .mine-container {
    padding: 0 12px;
  }

  .bill-summary {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .bill-card {
    padding: 14px 12px;
  }

  .bill-card-value {
    font-size: 18px;
  }
}

/* 响应式输入框：桌面固定 300px，移动端占满宽度 */
.responsive-input {
  width: 100%;
  max-width: 300px;
  margin: 12px 0;
}
@media (max-width: 480px) {
  .responsive-input {
    max-width: 100%;
  }
}
</style>
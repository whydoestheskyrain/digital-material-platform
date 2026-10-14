<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="page-header">
        <h1>用户管理</h1>
        <div class="header-actions">
          <el-button @click="$router.push('/admin/home')">返回控制台</el-button>
        </div>
      </div>

      <el-table
        :data="list"
        border
        stripe
        class="data-table"
        :empty-text="list.length === 0 ? '暂无用户' : ''"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="余额" width="110" align="right">
          <template #default="scope">¥{{ Number(scope.row.balance || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="冻结余额" width="110" align="right">
          <template #default="scope">¥{{ Number(scope.row.frozenBalance || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.role?.toUpperCase() === 'ADMIN' ? 'danger' : 'success'">
              {{ scope.row.role?.toUpperCase() === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              @click="openEvidence(scope.row)"
              :disabled="scope.row.role?.toUpperCase() === 'ADMIN'"
            ><el-icon><Search /></el-icon> 账单取证</el-button>
            <el-button
              size="small"
              type="danger"
              @click="del(scope.row.id)"
              :disabled="scope.row.role?.toUpperCase() === 'ADMIN'"
            ><el-icon><Delete /></el-icon> 删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 账单取证弹窗 -->
    <el-dialog
      v-model="evidenceVisible"
      :title="`资金取证 - ${evidenceUser.nickname || evidenceUser.username}（ID: ${evidenceUser.id}）`"
      width="86%"
      top="5vh"
      destroy-on-close
    >
      <!-- 用户当前资金状态 -->
      <div class="evidence-summary">
        <div class="evidence-item">
          <span class="evidence-label">当前可用余额：</span>
          <span class="evidence-value">¥{{ Number(evidenceUser.balance || 0).toFixed(2) }}</span>
        </div>
        <div class="evidence-item">
          <span class="evidence-label">当前冻结余额：</span>
          <span class="evidence-value">¥{{ Number(evidenceUser.frozenBalance || 0).toFixed(2) }}</span>
        </div>
        <div class="evidence-tip">
          取证依据：每笔资金变动均记录变动前/变动后余额，可逐笔核对；流水时间以服务器为准。
        </div>
      </div>

      <el-tabs v-model="evidenceTab">
        <!-- 资金流水 -->
        <el-tab-pane :label="`资金流水 (${evidenceLogs.length})`" name="logs">
          <el-table :data="evidenceLogs" border size="small" v-loading="evidenceLoading" max-height="520">
            <el-table-column label="流水ID" prop="id" width="90" />
            <el-table-column label="时间" prop="createTime" width="160" />
            <el-table-column label="类型" width="100">
              <template #default="scope">
                <el-tag :type="logTypeMap[scope.row.type]?.tag || 'info'" size="small">
                  {{ logTypeMap[scope.row.type]?.text || scope.row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="关联订单ID" prop="orderId" width="100">
              <template #default="scope">{{ scope.row.orderId ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="200" show-overflow-tooltip />
            <el-table-column label="金额" width="120" align="right">
              <template #default="scope">
                <span :class="Number(scope.row.amount) >= 0 ? 'money-in' : 'money-out'">
                  {{ Number(scope.row.amount) >= 0 ? '+' : '' }}{{ Number(scope.row.amount).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="变动前余额" width="110" align="right">
              <template #default="scope">{{ Number(scope.row.balanceBefore).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="变动后余额" width="110" align="right">
              <template #default="scope">{{ Number(scope.row.balanceAfter).toFixed(2) }}</template>
            </el-table-column>
            <template #empty>该用户暂无资金流水</template>
          </el-table>
        </el-tab-pane>

        <!-- 充值记录 -->
        <el-tab-pane :label="`充值记录 (${evidenceRecharges.length})`" name="recharge">
          <el-table :data="evidenceRecharges" border size="small" v-loading="evidenceLoading" max-height="520">
            <el-table-column label="充值单号" prop="orderNo" min-width="220" show-overflow-tooltip />
            <el-table-column label="金额" width="120" align="right">
              <template #default="scope">
                <span class="money-in">+{{ Number(scope.row.amount).toFixed(2) }}</span>
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
            <template #empty>该用户暂无充值记录</template>
          </el-table>
        </el-tab-pane>

        <!-- 交易订单 -->
        <el-tab-pane :label="`交易订单 (${evidenceOrders.length})`" name="orders">
          <el-table :data="evidenceOrders" border size="small" v-loading="evidenceLoading" max-height="520">
            <el-table-column label="角色" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.tradeRole === '买入' ? 'warning' : 'success'" size="small">
                  {{ scope.row.tradeRole }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="订单号" prop="orderNo" min-width="200" show-overflow-tooltip />
            <el-table-column label="买家ID" prop="buyerId" width="80" />
            <el-table-column label="卖家ID" prop="sellerId" width="80" />
            <el-table-column label="金额" width="110" align="right">
              <template #default="scope">¥{{ Number(scope.row.price).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-tag :type="orderStatusType(scope.row.status)" size="small">
                  {{ orderStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="支付方式" width="90">
              <template #default="scope">
                {{ scope.row.paymentMethod === 'alipay' ? '支付宝' : scope.row.paymentMethod === 'balance' ? '余额' : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="第三方交易号" prop="tradeNo" min-width="160" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.tradeNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="下单时间" prop="createTime" width="160" />
            <template #empty>该用户暂无交易订单</template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getCurrentUser } from '@/utils/accountManager'

const router = useRouter()
const list = ref([])

// 账单取证
const evidenceVisible = ref(false)
const evidenceTab = ref('logs')
const evidenceLoading = ref(false)
const evidenceUser = ref({})
const evidenceLogs = ref([])
const evidenceRecharges = ref([])
const evidenceOrders = ref([])

const logTypeMap = {
  recharge: { text: '充值', tag: 'success' },
  pay: { text: '购买支出', tag: 'danger' },
  income: { text: '卖出收入', tag: 'success' },
  refund: { text: '退款', tag: 'warning' },
  freeze: { text: '资金冻结', tag: 'info' }
}

const orderStatusMap = {
  1: { text: '待支付', tag: 'warning' },
  2: { text: '待发货', tag: 'primary' },
  3: { text: '待收货', tag: 'success' },
  4: { text: '已完成', tag: 'info' },
  5: { text: '已取消', tag: 'danger' }
}
const orderStatusText = (s) => orderStatusMap[s]?.text || '未知'
const orderStatusType = (s) => orderStatusMap[s]?.tag || ''

onMounted(() => {
  const user = getCurrentUser()
  if (!user || user.role?.toUpperCase() !== 'ADMIN') {
    ElMessage.warning('无管理员权限！')
    router.push('/login')
    return
  }

  loadUsers()
})

const loadUsers = () => {
  request.get('/user/list').then(res => {
    list.value = res.data || []
  }).catch(() => {
    ElMessage.error('获取用户列表失败')
  })
}

const openEvidence = async (row) => {
  evidenceUser.value = row
  evidenceTab.value = 'logs'
  evidenceLogs.value = []
  evidenceRecharges.value = []
  evidenceOrders.value = []
  evidenceVisible.value = true
  evidenceLoading.value = true
  try {
    const [logsRes, rechargeRes, ordersRes] = await Promise.all([
      request.get(`/admin/stats/user/${row.id}/logs`),
      request.get(`/admin/stats/user/${row.id}/recharge`),
      request.get(`/admin/stats/user/${row.id}/orders`)
    ])
    evidenceLogs.value = logsRes.code === 200 ? (logsRes.data || []) : []
    evidenceRecharges.value = rechargeRes.code === 200 ? (rechargeRes.data || []) : []
    evidenceOrders.value = ordersRes.code === 200 ? (ordersRes.data || []) : []
  } catch (e) {
    console.error('加载取证数据失败', e)
    ElMessage.error('加载取证数据失败')
  } finally {
    evidenceLoading.value = false
  }
}

const del = async (id) => {
  try {
    await request.get('/user/delete/' + id)
    ElMessage.success('删除成功')
    list.value = list.value.filter(i => i.id !== id)
  } catch (err) {
    ElMessage.error('删除失败')
  }
}
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

.evidence-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 14px 18px;
  margin-bottom: 14px;
}
.evidence-item {
  font-size: 14px;
}
.evidence-label {
  color: var(--color-text-regular);
}
.evidence-value {
  font-size: 17px;
  font-weight: 700;
  color: #2b7de9;
}
.evidence-tip {
  width: 100%;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.money-in {
  color: #1ea97c;
  font-weight: 600;
}
.money-out {
  color: #f04d5a;
  font-weight: 600;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
}
</style>

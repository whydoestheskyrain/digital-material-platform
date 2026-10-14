import { ref, computed } from 'vue'
import request from '@/utils/request'
import { getCurrentUser } from '@/utils/accountManager'

/**
 * 订单未读角标 —— 跨页面（首页订单按钮 / 订单列表）共享的模块级单例
 *
 * 已查看记录按用户存在 localStorage，key 为 `订单ID_状态`：
 * 订单状态变化（已支付→已发货等）会生成新 key，从而再次产生角标提醒对方；
 * 谁操作/查看谁的账号消除自己的角标，不影响交易对方。
 */

const storageKey = () => `order_viewed_${getCurrentUser()?.id || 'anon'}`

// 当前账号已查看的 `订单ID_状态` 集合
const viewedSet = ref(new Set())
// 买家视角 / 卖家视角的全量订单缓存
const buyerOrders = ref([])
const sellerOrders = ref([])

const orderViewKey = (o) => `${o.id}_${o.status}`
const isViewed = (o) => viewedSet.value.has(orderViewKey(o))

// 合并买家+卖家订单（同一订单去重，自买自卖时只算一次）
const allOrders = computed(() => {
  const map = new Map()
  buyerOrders.value.forEach(o => map.set(o.id, o))
  sellerOrders.value.forEach(o => map.set(o.id, o))
  return [...map.values()]
})

// 首页订单按钮角标总数（已取消订单 status=5 不提醒）
const unreadCount = computed(() =>
  allOrders.value.filter(o => o.status !== 5 && !isViewed(o)).length
)

const loadViewed = () => {
  try {
    viewedSet.value = new Set(JSON.parse(localStorage.getItem(storageKey()) || '[]'))
  } catch (e) {
    viewedSet.value = new Set()
  }
}

const persistViewed = () => {
  localStorage.setItem(storageKey(), JSON.stringify([...viewedSet.value]))
}

/** 标记单个订单在其当前状态下已查看，角标立即消除，并通知其他页面同步 */
const markViewed = (order) => {
  if (!order || order.id == null || order.status == null) return
  const key = orderViewKey(order)
  if (viewedSet.value.has(key)) return
  viewedSet.value = new Set(viewedSet.value).add(key)
  persistViewed()
  window.dispatchEvent(new CustomEvent('order:unread-change', {
    detail: { userId: getCurrentUser()?.id }
  }))
}

/** 拉取当前用户买家+卖家两侧订单（首页角标用，静默失败） */
const refresh = async () => {
  const user = getCurrentUser()
  loadViewed()
  if (!user?.id) {
    buyerOrders.value = []
    sellerOrders.value = []
    return
  }
  try {
    const [buyerRes, sellerRes] = await Promise.all([
      request.get('/order/listAll', { params: { buyerId: user.id } }),
      request.get(`/order/seller/${user.id}/all`)
    ])
    buyerOrders.value = buyerRes.code === 200
      ? (buyerRes.data || []).filter(o => o.buyerId === user.id) : []
    sellerOrders.value = sellerRes.code === 200
      ? (sellerRes.data || []).filter(o => o.sellerId === user.id) : []
  } catch (e) {
    console.error('获取订单未读数失败:', e)
  }
}

/** 订单列表页把自己拉到的数据同步进缓存，避免首页重复请求 */
const setOrders = (mode, list) => {
  if (mode === 'seller') {
    sellerOrders.value = list
  } else {
    buyerOrders.value = list
  }
}

/** 指定视角下某状态的未查看订单数（订单列表标签角标） */
const statusUnread = (status, mode) => {
  const source = mode === 'seller' ? sellerOrders.value : buyerOrders.value
  return source.filter(o => o.status === status && !isViewed(o)).length
}

/** 指定视角的全部未查看订单数（“全部”标签角标；已取消订单不提醒） */
const totalUnreadForMode = (mode) => {
  const source = mode === 'seller' ? sellerOrders.value : buyerOrders.value
  return source.filter(o => o.status !== 5 && !isViewed(o)).length
}

export function useOrderUnread() {
  return {
    viewedSet,
    unreadCount,
    isViewed,
    markViewed,
    refresh,
    loadViewed,
    setOrders,
    statusUnread,
    totalUnreadForMode
  }
}

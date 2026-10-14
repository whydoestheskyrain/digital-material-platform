// 订单状态常量
export const ORDER_STATUS = {
  UNPAID: 'UNPAID',
  PAID: 'PAID',
  DELIVERED: 'DELIVERED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
}

// 商品状态常量
export const PRODUCT_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
  OFFLINE: 3
}

// 订单状态文本映射
export const ORDER_STATUS_TEXT = {
  UNPAID: '待支付',
  PAID: '待发货',
  DELIVERED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

// 订单状态样式映射
export const ORDER_STATUS_TYPE = {
  UNPAID: 'warning',
  PAID: 'primary',
  DELIVERED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger'
}

// 商品状态文本映射
export const PRODUCT_STATUS_TEXT = {
  0: '待审核',
  1: '已上架',
  2: '审核拒绝',
  3: '已下架'
}

// 商品状态样式映射
export const PRODUCT_STATUS_TYPE = {
  0: 'warning',
  1: 'success',
  2: 'danger',
  3: 'info'
}

/**
 * 金额千位分隔符格式化（保留两位小数）
 * @param {number|string} value
 * @returns {string} 例：12,345.00
 */
export const formatMoney = (value) => {
  const num = Number(value)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}
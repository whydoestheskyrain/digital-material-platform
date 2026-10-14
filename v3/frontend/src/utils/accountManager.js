/**
 * 多账号管理工具
 * 支持在同一浏览器登录多个账号并切换
 */

const ACCOUNTS_KEY = 'accounts'
const CURRENT_KEY = 'currentAccount'

// 获取所有已登录账号
export function getAccounts() {
  const data = localStorage.getItem(ACCOUNTS_KEY)
  return data ? JSON.parse(data) : {}
}

// 获取当前活跃账号
export function getCurrentAccount() {
  const accounts = getAccounts()
  const currentId = localStorage.getItem(CURRENT_KEY)
  return currentId ? accounts[currentId] : null
}

// 获取当前账号的 token
export function getCurrentToken() {
  const account = getCurrentAccount()
  return account ? account.token : null
}

// 获取当前账号的用户信息
export function getCurrentUser() {
  const account = getCurrentAccount()
  return account ? account.user : null
}

// 添加/切换账号
export function setAccount(accountId, token, user) {
  const accounts = getAccounts()
  accounts[accountId] = {
    token,
    user,
    loginTime: Date.now()
  }
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts))
  localStorage.setItem(CURRENT_KEY, accountId)
}

// 切换当前账号
export function switchAccount(accountId) {
  const accounts = getAccounts()
  if (accounts[accountId]) {
    localStorage.setItem(CURRENT_KEY, accountId)
    return true
  }
  return false
}

// 移除某个账号
export function removeAccount(accountId) {
  const accounts = getAccounts()
  delete accounts[accountId]
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts))

  const current = localStorage.getItem(CURRENT_KEY)
  if (current === accountId) {
    const ids = Object.keys(accounts)
    localStorage.setItem(CURRENT_KEY, ids.length > 0 ? ids[0] : '')
  }
}

// 更新当前账号的用户信息
export function updateCurrentUser(user) {
  const accounts = getAccounts()
  const currentId = localStorage.getItem(CURRENT_KEY)
  if (currentId && accounts[currentId]) {
    accounts[currentId].user = user
    localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts))
  }
}

// 清空所有账号
export function clearAllAccounts() {
  localStorage.removeItem(ACCOUNTS_KEY)
  localStorage.removeItem(CURRENT_KEY)
}

// 兼容旧版：从旧格式迁移数据（首次使用）
export function migrateFromLegacy() {
  const oldToken = localStorage.getItem('token')
  const oldUser = localStorage.getItem('user')
  if (oldToken && oldUser) {
    try {
      const user = JSON.parse(oldUser)
      const accountId = 'user_' + user.id
      setAccount(accountId, oldToken, user)
      // 清理旧数据
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      return true
    } catch (e) {
      console.error('迁移旧账号数据失败', e)
    }
  }
  return false
}

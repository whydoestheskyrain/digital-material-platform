import axios from 'axios'
import { getCurrentToken, removeAccount, getCurrentAccount } from './accountManager'

const service = axios.create({
  // 生产环境由 Nginx 同源反向代理，开发环境由 vue.config.js 代理，均无需写死域名
  baseURL: '',
  timeout: 30000  // AI对话需要更长时间响应
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = getCurrentToken()
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器（修复版）
service.interceptors.response.use(
  res => res.data,
  error => {
    console.error('请求错误：', error)

    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        // 多账号：只退出当前账号，不影响其他账号
        const current = getCurrentAccount()
        const accountId = current ? 'user_' + current.user.id : ''
        removeAccount(accountId)
        alert('当前账号登录已过期，请重新登录')
        window.location.href = '/login'
      } else if (status === 403) {
        alert('没有权限访问该资源')
      } else if (status === 404) {
        alert('请求的资源不存在')
      } else if (status >= 500) {
        alert('服务器内部错误，请稍后重试')
      } else {
        const message = error.response.data?.message || error.response.data?.msg || '请求失败'
        alert(message)
      }
    } else if (error.request) {
      alert('请求失败，请检查网络连接或后端服务是否已启动')
    } else {
      alert('请求配置错误：' + error.message)
    }

    return Promise.reject(error)
  }
)

export default service

import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getCurrentToken, removeAccount, getCurrentAccount } from './accountManager'

const service = axios.create({
  // 生产环境由 Nginx 同源反向代理，开发环境由 vue.config.js 代理，均无需写死域名
  baseURL: '',
  timeout: 30000  // AI对话需要更长时间响应
})

// 防止 401 重复跳转（多接口并发返回 401 时只提示一次）
let isRedirecting401 = false

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

// 响应拦截器
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
        // 防重复跳转：只弹一次提示、只跳一次
        if (!isRedirecting401) {
          isRedirecting401 = true
          ElMessage.warning('登录已过期，请重新登录')
          router.push('/login').finally(() => {
            // 跳完后重置标记（下一次 401 还能正常处理）
            setTimeout(() => { isRedirecting401 = false }, 1500)
          })
        }
      } else if (status === 403) {
        ElMessage.error('没有权限访问该资源')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else {
        const message = error.response.data?.message || error.response.data?.msg || '请求失败'
        ElMessage.error(message)
      }
    } else if (error.request) {
      ElMessage.error('网络连接异常，请检查网络或后端服务')
    } else {
      ElMessage.error('请求配置错误：' + error.message)
    }

    return Promise.reject(error)
  }
)

export default service

import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('iot_token')
  const expiresAt = Number(localStorage.getItem('iot_token_expires_at') || 0)
  if (expiresAt && Date.now() > expiresAt) {
    localStorage.removeItem('iot_token')
    localStorage.removeItem('iot_token_expires_at')
    localStorage.removeItem('iot_user')
    if (location.pathname !== '/login') location.href = '/login'
    return config
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  try {
    const user = JSON.parse(localStorage.getItem('iot_user') || '{}')
    if (user.roleName || user.role) config.headers['X-User-Role'] = user.roleName || user.role
    if (user.username) config.headers['X-User-Name'] = user.username
  } catch {}
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0 || body.code === 200) return body.data ?? body
      return Promise.reject(new Error(body.message || '平台服务返回异常'))
    }
    return body
  },
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('iot_token')
      localStorage.removeItem('iot_token_expires_at')
      localStorage.removeItem('iot_user')
      if (location.pathname !== '/login') location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const quietError = (error, fallbackText = '平台服务暂不可用，已展示演示数据') => {
  if (import.meta.env.DEV) console.warn(error)
  ElMessage.warning(fallbackText)
}

export default http

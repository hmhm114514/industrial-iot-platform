import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('iot_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
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

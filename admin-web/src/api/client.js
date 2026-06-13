import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const client = axios.create({ baseURL: '/api', timeout: 15000 })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers['satoken'] = token
  return config
})

client.interceptors.response.use(
  (res) => {
    const body = res.data
    // 文件流等非标准包装直接放行
    if (body === null || typeof body !== 'object' || !('code' in body)) return body
    if (body.code === 0) return body.data
    if (body.code === 401) {
      localStorage.removeItem('admin_token')
      router.push('/login')
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message))
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || '网络异常')
    return Promise.reject(err)
  }
)

export default client

import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useAppStore } from '../stores/app'

const client = axios.create({ baseURL: '/api', timeout: 15000 })

client.interceptors.request.use((config) => {
  const app = useAppStore()
  if (app.token) config.headers['satoken'] = app.token
  config.headers['Accept-Language'] = app.locale
  // 商品/结算类接口统一带上语言与币种
  config.params = { locale: app.locale, currency: app.currency, ...(config.params || {}) }
  return config
})

client.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body === null || typeof body !== 'object' || !('code' in body)) return body
    if (body.code === 0) return body.data
    if (body.code === 401) {
      useAppStore().logout()
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    ElMessage.error(body.message || 'Request failed')
    return Promise.reject(Object.assign(new Error(body.message), { code: body.code }))
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || 'Network error')
    return Promise.reject(err)
  }
)

export default client

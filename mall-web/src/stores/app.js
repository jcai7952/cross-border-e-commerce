import { defineStore } from 'pinia'
import { setLocale } from '../i18n'

/** 全局偏好：语言 / 币种 / 登录态。 */
export const useAppStore = defineStore('app', {
  state: () => ({
    locale: localStorage.getItem('locale') || (navigator.language.startsWith('zh') ? 'zh-CN' : 'en-US'),
    currency: localStorage.getItem('currency') || 'USD',
    currencies: [],
    token: localStorage.getItem('user_token') || '',
    user: JSON.parse(localStorage.getItem('user_info') || 'null'),
    cartCount: 0
  }),
  getters: {
    loggedIn: (s) => !!s.token
  },
  actions: {
    switchLocale(locale) {
      this.locale = locale
      setLocale(locale)
    },
    switchCurrency(code) {
      this.currency = code
      localStorage.setItem('currency', code)
    },
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('user_token', token)
      localStorage.setItem('user_info', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      this.cartCount = 0
      localStorage.removeItem('user_token')
      localStorage.removeItem('user_info')
    }
  }
})

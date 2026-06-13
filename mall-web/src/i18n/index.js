import { createI18n } from 'vue-i18n'
import zh from './zh-CN'
import en from './en-US'

const saved = localStorage.getItem('locale') || (navigator.language.startsWith('zh') ? 'zh-CN' : 'en-US')

const i18n = createI18n({
  legacy: false,
  locale: saved,
  fallbackLocale: 'en-US',
  messages: { 'zh-CN': zh, 'en-US': en }
})

export function setLocale(locale) {
  i18n.global.locale.value = locale
  localStorage.setItem('locale', locale)
  document.documentElement.lang = locale
}

export default i18n

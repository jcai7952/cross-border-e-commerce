<template>
  <div class="container result-page">
    <div class="result-card">
      <!-- 处理中 / 轮询中 -->
      <template v-if="state === 'loading' || state === 'processing'">
        <div class="icon-circle pending">…</div>
        <h1 class="result-title">{{ $t('pay.processing') }}</h1>
      </template>

      <!-- 成功 -->
      <template v-else-if="state === 'success'">
        <div class="icon-circle success">✓</div>
        <h1 class="result-title">{{ $t('pay.success') }}</h1>
      </template>

      <!-- 失败 -->
      <template v-else-if="state === 'failed'">
        <div class="icon-circle failed">✕</div>
        <h1 class="result-title">{{ $t('pay.failed') }}</h1>
      </template>

      <!-- 已取消 -->
      <template v-else-if="state === 'cancel'">
        <div class="icon-circle pending">✕</div>
        <h1 class="result-title">{{ txt.canceled }}</h1>
      </template>

      <div class="result-btns">
        <button
          v-if="(state === 'failed' || state === 'cancel') && orderNo"
          class="btn-dark"
          @click="$router.push(`/pay/${orderNo}`)"
        >
          {{ txt.retry }}
        </button>
        <button v-if="state !== 'loading'" class="btn-light" @click="$router.push('/orders')">
          {{ $t('pay.backToOrders') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const route = useRoute()
const app = useAppStore()

const payNo = route.query.payNo
const state = ref('loading') // loading | success | failed | processing | cancel
const orderNo = ref(route.query.orderNo || '')
let stopped = false

const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? { canceled: '支付已取消', retry: '重新支付' }
    : { canceled: 'Payment Canceled', retry: 'Retry Payment' }
)

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function pollPay() {
  // 回跳后先主动同步渠道状态兜底
  try { await client.post(`/pay/${payNo}/sync`) } catch { /* ignore */ }
  for (let i = 0; i < 5; i++) {
    if (stopped) return
    try {
      const p = await client.get(`/pay/${payNo}`)
      orderNo.value = p.orderNo || orderNo.value
      if (p.status === 'SUCCESS') { state.value = 'success'; return }
      if (p.status === 'FAILED' || p.status === 'CLOSED') { state.value = 'failed'; return }
    } catch {
      state.value = 'processing'
      return
    }
    state.value = 'processing'
    await sleep(2000)
  }
}

async function byOrder() {
  try {
    const o = await client.get(`/order/${orderNo.value}`)
    if (['PAID', 'SHIPPED', 'FINISHED'].includes(o.status)) state.value = 'success'
    else if (o.status === 'CLOSED') state.value = 'failed'
    else state.value = 'processing'
  } catch {
    state.value = 'processing'
  }
}

onMounted(async () => {
  // 支付成功后同步购物车角标
  if (app.loggedIn) {
    client.get('/cart/count').then((r) => { app.cartCount = r?.count || 0 }).catch(() => {})
  }
  if (route.query.cancel === '1') {
    state.value = 'cancel'
    return
  }
  if (payNo) {
    await pollPay()
  } else if (orderNo.value) {
    await byOrder()
  } else {
    state.value = 'processing'
  }
})

onUnmounted(() => { stopped = true })
</script>

<style scoped>
.result-page { padding-top: 64px; padding-bottom: 80px; display: flex; justify-content: center; min-height: 50vh; }
.result-card {
  width: 520px; max-width: 100%; border: 1px solid var(--line); background: #fff;
  padding: 52px 44px; text-align: center;
}
.icon-circle {
  width: 84px; height: 84px; border-radius: 50%; margin: 0 auto 22px;
  display: flex; align-items: center; justify-content: center;
  font-size: 40px; font-weight: 700; color: #fff;
}
.icon-circle.success { background: var(--brand); }
.icon-circle.failed { background: var(--brand-accent); }
.icon-circle.pending { background: #bbb; }
.result-title { font-size: 20px; font-weight: 800; letter-spacing: 1px; }
.result-btns { margin-top: 32px; display: flex; flex-direction: column; gap: 12px; }
.btn-dark {
  background: var(--brand); color: #fff; border: 1px solid var(--brand);
  padding: 13px 0; font-size: 14px; font-weight: 700; letter-spacing: 2px;
  cursor: pointer; border-radius: 0; text-transform: uppercase;
}
.btn-dark:hover { opacity: 0.85; }
.btn-light {
  background: #fff; color: var(--brand); border: 1px solid var(--brand);
  padding: 12px 0; font-size: 13px; font-weight: 700; letter-spacing: 1px;
  cursor: pointer; border-radius: 0;
}
.btn-light:hover { background: var(--brand); color: #fff; }
</style>

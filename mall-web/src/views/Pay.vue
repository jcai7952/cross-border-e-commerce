<template>
  <div class="container pay-page" v-loading="loading">
    <template v-if="order">
      <div class="pay-card">
        <h1 class="pay-title">{{ $t('pay.title') }}</h1>
        <div class="pay-no muted">{{ order.orderNo }}</div>

        <template v-if="closed">
          <el-alert type="info" :closable="false" show-icon :title="$t('order.closed')" class="closed-alert" />
          <button class="btn-light" @click="$router.push('/orders')">{{ $t('pay.backToOrders') }}</button>
        </template>

        <template v-else>
          <div class="amount-label">{{ $t('pay.amount') }}</div>
          <div class="amount price">{{ money(order.total) }}</div>

          <div class="deadline" :class="{ expired }">
            {{ $t('pay.deadline') }}
            <b class="cd">{{ mmss }}</b>
          </div>

          <div class="choose">{{ $t('pay.choose') }}</div>
          <div class="channels">
            <div
              v-for="c in channels"
              :key="c.code"
              class="channel"
              :class="{ active: channel === c.code }"
              @click="channel = c.code"
            >
              <span class="ch-icon">{{ c.icon }}</span>
              <span class="ch-name">{{ $t(c.label) }}</span>
            </div>
          </div>

          <button class="btn-dark" :disabled="expired || paying" @click="goPay">
            {{ paying ? $t('common.loading') : $t('pay.goPay') }}
          </button>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const orderNo = route.params.orderNo
const loading = ref(true)
const order = ref(null)
const channel = ref('SIMULATOR')
const paying = ref(false)
const remain = ref(0)
const closed = ref(false)
let timer = null

const channels = [
  { code: 'SIMULATOR', icon: '🎮', label: 'pay.simulator' },
  { code: 'STRIPE', icon: '💳', label: 'pay.stripe' },
  { code: 'PAYPAL', icon: '🅿️', label: 'pay.paypal' }
]

const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? { stripeKey: '该渠道需配置 Stripe 公钥（本期未集成）' }
    : { stripeKey: 'This channel requires a Stripe publishable key (not integrated yet)' }
)

const money = (m) => (m?.display ? m.display.symbol + m.display.text : '')
const expired = computed(() => remain.value <= 0)
const mmss = computed(() => {
  const s = Math.max(0, remain.value)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})

function startCountdown() {
  timer = setInterval(() => {
    if (remain.value > 0) remain.value -= 1
    else if (timer) { clearInterval(timer); timer = null }
  }, 1000)
}

async function goPay() {
  paying.value = true
  try {
    const r = await client.post('/pay/create', { orderNo, channel: channel.value })
    if (r.payloadType === 'REDIRECT' && r.payload?.redirectUrl) {
      location.href = r.payload.redirectUrl
    } else if (r.payloadType === 'CLIENT_SECRET') {
      ElMessage.info(txt.value.stripeKey)
    }
  } catch (e) {
    // 40002 渠道未启用：拦截器已弹出后端文案
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  try {
    const o = await client.get(`/order/${orderNo}`)
    order.value = o
    if (o.status === 'WAIT_PAY') {
      remain.value = o.countdownSeconds ?? Math.max(0, Math.floor((new Date(o.payDeadline).getTime() - Date.now()) / 1000))
      startCountdown()
    } else if (o.status === 'CLOSED') {
      closed.value = true
    } else {
      // 已支付（PAID/SHIPPED/FINISHED）→ 结果页
      router.replace({ path: '/pay/result', query: { orderNo } })
    }
  } finally {
    loading.value = false
  }
})

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.pay-page { padding-top: 48px; padding-bottom: 60px; min-height: 50vh; display: flex; justify-content: center; }
.pay-card {
  width: 560px; max-width: 100%; border: 1px solid var(--line); background: #fff;
  padding: 40px 44px; text-align: center;
}
.pay-title { font-size: 20px; font-weight: 800; letter-spacing: 2px; text-transform: uppercase; }
.pay-no { margin-top: 6px; }
.muted { color: var(--muted); font-size: 12px; }

.amount-label { margin-top: 26px; font-size: 13px; color: #555; }
.amount { font-size: 40px; margin-top: 6px; }

.deadline { margin-top: 12px; font-size: 13px; color: #555; }
.deadline .cd { color: var(--price); font-size: 16px; font-variant-numeric: tabular-nums; margin-left: 6px; }
.deadline.expired .cd { color: var(--muted); }

.choose { margin: 28px 0 12px; font-size: 13px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; }
.channels { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.channel {
  border: 1px solid var(--line); padding: 16px 8px; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 8px; transition: all 0.15s;
}
.channel:hover { border-color: #999; }
.channel.active { border: 2px solid var(--brand); padding: 15px 7px; }
.ch-icon { font-size: 24px; }
.ch-name { font-size: 12px; color: #333; }

.btn-dark {
  width: 100%; margin-top: 26px; background: var(--brand); color: #fff; border: 1px solid var(--brand);
  padding: 13px 0; font-size: 14px; font-weight: 700; letter-spacing: 2px;
  cursor: pointer; border-radius: 0; transition: opacity 0.2s; text-transform: uppercase;
}
.btn-dark:hover:not(:disabled) { opacity: 0.85; }
.btn-dark:disabled { background: #ccc; border-color: #ccc; cursor: not-allowed; }
.btn-light {
  width: 100%; margin-top: 20px; background: #fff; color: var(--brand); border: 1px solid var(--brand);
  padding: 12px 0; font-size: 13px; font-weight: 700; letter-spacing: 1px; cursor: pointer; border-radius: 0;
}
.btn-light:hover { background: var(--brand); color: #fff; }
.closed-alert { margin-top: 24px; }
</style>

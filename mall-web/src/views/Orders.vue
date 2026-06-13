<template>
  <div class="container orders-page">
    <h1 class="page-title">{{ $t('order.title') }}</h1>

    <el-tabs v-model="tab" @tab-change="onTab">
      <el-tab-pane v-for="t in TABS" :key="t.value" :label="$t(t.label)" :name="t.value" />
    </el-tabs>

    <div v-loading="loading">
      <template v-if="list.length">
        <div v-for="o in list" :key="o.orderNo" class="order-card">
          <div class="oc-head">
            <div class="oc-left">
              <span class="oc-time muted">{{ o.createTime?.replace('T', ' ') }}</span>
              <span class="oc-no">{{ o.orderNo }}</span>
            </div>
            <div class="oc-right">
              <span v-if="o.status === 'WAIT_PAY' && o.countdownSeconds > 0" class="oc-cd">
                {{ $t('order.countdown', { t: mmss(o.countdownSeconds) }) }}
              </span>
              <el-tag :type="statusTag(o.status)" effect="plain">{{ statusLabel(o.status) }}</el-tag>
            </div>
          </div>

          <div class="oc-items" @click="$router.push(`/order/${o.orderNo}`)">
            <div v-for="(it, idx) in o.items" :key="idx" class="oc-item">
              <img class="oc-img" :src="it.image" :alt="it.productName" />
              <div class="oc-info">
                <div class="oc-name">{{ it.productName }}</div>
                <div class="oc-sku muted">{{ it.skuText }}</div>
              </div>
              <div class="oc-price muted">${{ (it.priceCents / 100).toFixed(2) }} × {{ it.quantity }}</div>
            </div>
          </div>

          <div class="oc-foot">
            <div class="oc-total">
              {{ $t('checkout.total') }}
              <span class="price oc-total-num">{{ fmt(o.totalDisplay) }}</span>
            </div>
            <div class="oc-btns">
              <button v-if="o.status === 'WAIT_PAY'" class="btn-dark sm" @click="$router.push(`/pay/${o.orderNo}`)">
                {{ $t('order.pay') }}
              </button>
              <button v-if="o.status === 'WAIT_PAY'" class="btn-light sm" @click="onCancel(o)">
                {{ $t('order.cancel') }}
              </button>
              <button v-if="o.status === 'SHIPPED'" class="btn-dark sm" @click="onConfirm(o)">
                {{ $t('order.confirm') }}
              </button>
              <button v-if="o.status === 'SHIPPED'" class="btn-light sm" @click="$router.push(`/order/${o.orderNo}`)">
                {{ $t('order.track') }}
              </button>
              <button v-if="o.status === 'FINISHED'" class="btn-light sm" @click="$router.push(`/order/${o.orderNo}`)">
                {{ $t('order.review') }}
              </button>
              <button class="btn-light sm" @click="$router.push(`/order/${o.orderNo}`)">
                {{ $t('order.detail') }}
              </button>
            </div>
          </div>
        </div>

        <div class="pager">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="10"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="load"
          />
        </div>
      </template>

      <el-empty v-else-if="!loading" :description="$t('common.empty')" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const TABS = [
  { value: 'all', label: 'order.all' },
  { value: 'WAIT_PAY', label: 'order.waitPay' },
  { value: 'PAID', label: 'order.paid' },
  { value: 'SHIPPED', label: 'order.shipped' },
  { value: 'FINISHED', label: 'order.finished' },
  { value: 'CLOSED', label: 'order.closed' }
]

const app = useAppStore()
const tab = ref('all')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
let timer = null

const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? { confirmCancel: '确认取消该订单？', confirmReceipt: '确认已收到货？', confirm: '确认', cancel: '取消' }
    : { confirmCancel: 'Cancel this order?', confirmReceipt: 'Confirm you have received the package?', confirm: 'Confirm', cancel: 'Cancel' }
)

const fmt = (p) => (p ? p.symbol + p.text : '')
const mmss = (s) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

const STATUS_KEY = { WAIT_PAY: 'order.waitPay', PAID: 'order.paid', SHIPPED: 'order.shipped', FINISHED: 'order.finished', CLOSED: 'order.closed' }
const STATUS_TAG = { WAIT_PAY: 'warning', PAID: 'primary', SHIPPED: 'success', FINISHED: 'success', CLOSED: 'info' }
const { t } = useI18n()
const statusLabel = (s) => (STATUS_KEY[s] ? t(STATUS_KEY[s]) : s)
const statusTag = (s) => STATUS_TAG[s] || 'info'

async function load() {
  loading.value = true
  try {
    const r = await client.get('/order/page', {
      params: {
        status: tab.value === 'all' ? undefined : tab.value,
        pageNum: pageNum.value,
        pageSize: 10
      }
    })
    list.value = r?.list || []
    total.value = r?.total || 0
  } finally {
    loading.value = false
  }
}

function onTab() {
  pageNum.value = 1
  load()
}

async function onCancel(o) {
  await ElMessageBox.confirm(txt.value.confirmCancel, undefined, {
    confirmButtonText: txt.value.confirm,
    cancelButtonText: txt.value.cancel,
    type: 'warning'
  })
  await client.post(`/order/${o.orderNo}/cancel`)
  await load()
}

async function onConfirm(o) {
  await ElMessageBox.confirm(txt.value.confirmReceipt, undefined, {
    confirmButtonText: txt.value.confirm,
    cancelButtonText: txt.value.cancel,
    type: 'warning'
  })
  await client.post(`/order/${o.orderNo}/confirm`)
  await load()
}

onMounted(() => {
  load()
  // 待支付倒计时：本地每秒递减，归零刷新列表
  timer = setInterval(() => {
    let hitZero = false
    for (const o of list.value) {
      if (o.status === 'WAIT_PAY' && o.countdownSeconds > 0) {
        o.countdownSeconds -= 1
        if (o.countdownSeconds <= 0) hitZero = true
      }
    }
    if (hitZero) load()
  }, 1000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.orders-page { padding-top: 32px; padding-bottom: 48px; min-height: 50vh; }
.page-title { font-size: 22px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 18px; }
.muted { color: var(--muted); font-size: 12px; }

.order-card { border: 1px solid var(--line); margin-bottom: 16px; background: #fff; }
.oc-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; background: #fafafa; border-bottom: 1px solid var(--line);
}
.oc-left { display: flex; align-items: center; gap: 14px; }
.oc-no { font-size: 13px; font-weight: 600; }
.oc-right { display: flex; align-items: center; gap: 12px; }
.oc-cd { font-size: 12px; color: var(--price); font-variant-numeric: tabular-nums; }

.oc-items { cursor: pointer; }
.oc-item {
  display: grid; grid-template-columns: 64px 1fr 160px; gap: 14px; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid var(--line);
}
.oc-img { width: 64px; height: 84px; object-fit: cover; background: #f7f7f7; }
.oc-name { font-size: 13px; line-height: 1.5; margin-bottom: 4px; }
.oc-price { text-align: right; font-size: 13px; }

.oc-foot { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; }
.oc-total { font-size: 13px; color: #444; }
.oc-total-num { font-size: 20px; margin-left: 6px; }
.oc-btns { display: flex; gap: 10px; }

.btn-dark {
  background: var(--brand); color: #fff; border: 1px solid var(--brand);
  font-weight: 700; letter-spacing: 1px; cursor: pointer; border-radius: 0;
}
.btn-dark:hover { opacity: 0.85; }
.btn-light { background: #fff; color: var(--brand); border: 1px solid var(--brand); font-weight: 600; cursor: pointer; border-radius: 0; }
.btn-light:hover { background: var(--brand); color: #fff; }
.sm { padding: 8px 18px; font-size: 12px; }

.pager { display: flex; justify-content: center; margin-top: 24px; }
</style>

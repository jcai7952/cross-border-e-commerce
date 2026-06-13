<template>
  <div class="container od-page" v-loading="loading">
    <template v-if="order">
      <div class="od-head">
        <h1 class="page-title">{{ $t('order.detail') }}</h1>
        <div class="od-btns">
          <button v-if="order.status === 'WAIT_PAY'" class="btn-dark sm" @click="$router.push(`/pay/${order.orderNo}`)">
            {{ $t('order.pay') }}
          </button>
          <button v-if="order.status === 'WAIT_PAY'" class="btn-light sm" @click="onCancel">
            {{ $t('order.cancel') }}
          </button>
          <button v-if="order.status === 'SHIPPED'" class="btn-dark sm" @click="onConfirm">
            {{ $t('order.confirm') }}
          </button>
        </div>
      </div>
      <div class="od-no muted">
        {{ order.orderNo }} · {{ order.createTime?.replace('T', ' ') }}
        <span v-if="order.status === 'WAIT_PAY' && countdown > 0" class="od-cd">
          {{ $t('order.countdown', { t: mmss(countdown) }) }}
        </span>
      </div>

      <!-- 进度 / 关闭信息条 -->
      <el-alert
        v-if="order.status === 'CLOSED'"
        type="info"
        :closable="false"
        show-icon
        class="closed-bar"
        :title="`${$t('order.closed')}${order.closedAt ? ' · ' + order.closedAt.replace('T', ' ') : ''}`"
      />
      <el-steps v-else :active="stepActive" align-center class="od-steps" finish-status="success">
        <el-step :title="$t('order.waitPay')" />
        <el-step :title="$t('order.paid')" />
        <el-step :title="$t('order.shipped')" />
        <el-step :title="$t('order.finished')" />
      </el-steps>

      <div class="od-grid">
        <div class="od-main">
          <!-- 收件 / 实名快照 -->
          <section class="card">
            <div class="card-head"><h2>{{ $t('checkout.address') }}</h2></div>
            <div class="recv">
              <b>{{ order.receiver?.receiverName }}</b>
              <span class="muted">{{ order.receiver?.phone }}</span>
            </div>
            <div class="recv-line">{{ receiverText }}</div>
            <template v-if="order.identity">
              <div class="divider"></div>
              <div class="recv">
                <span class="muted">{{ $t('checkout.identity') }}</span>
                <b>{{ order.identity.realName }}</b>
                <span class="muted">{{ order.identity.idCardMask }}</span>
              </div>
            </template>
            <div v-if="order.remark" class="recv-line muted">{{ order.remark }}</div>
          </section>

          <!-- 商品明细 -->
          <section class="card">
            <div class="card-head"><h2>{{ $t('checkout.goods') }}</h2></div>
            <div v-for="(it, idx) in order.items" :key="idx" class="line-row">
              <img class="line-img" :src="it.image" :alt="it.productName" />
              <div>
                <div class="line-name">{{ it.productName }}</div>
                <div class="muted">{{ it.skuText }}</div>
              </div>
              <div class="line-price muted">${{ (it.priceCents / 100).toFixed(2) }} × {{ it.quantity }}</div>
              <div class="line-total price">${{ (it.totalCents / 100).toFixed(2) }}</div>
              <button
                v-if="order.status === 'FINISHED'"
                class="btn-light xs"
                @click="openReview(it)"
              >
                {{ $t('order.review') }}
              </button>
              <span v-else></span>
            </div>
          </section>

          <!-- 物流轨迹 -->
          <section v-if="track" class="card">
            <div class="card-head">
              <h2>{{ $t('logistics.title') }}</h2>
              <span class="muted">
                {{ $t('logistics.carrier') }}: {{ track.carrier }} · {{ $t('logistics.shipmentNo') }}: {{ track.shipmentNo }}
              </span>
            </div>
            <el-timeline class="tl">
              <el-timeline-item
                v-for="(n, i) in track.tracks"
                :key="i"
                :timestamp="n.trackTime?.replace('T', ' ')"
                :type="i === 0 ? 'primary' : ''"
                :hollow="i !== 0"
              >
                <b>{{ zh ? n.nodeZh : n.nodeEn }}</b>
                <div class="muted">
                  {{ n.location }}<template v-if="n.remark"> · {{ n.remark }}</template>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>

          <!-- 状态时间线 -->
          <section class="card">
            <div class="card-head"><h2>{{ txt.logs }}</h2></div>
            <el-timeline class="tl">
              <el-timeline-item
                v-for="(l, i) in logsDesc"
                :key="i"
                :timestamp="l.createTime?.replace('T', ' ')"
                :type="i === 0 ? 'primary' : ''"
                :hollow="i !== 0"
              >
                <b>{{ statusLabel(l.toStatus) }}</b>
                <span class="muted log-meta">{{ l.operator }}<template v-if="l.remark"> · {{ l.remark }}</template></span>
              </el-timeline-item>
            </el-timeline>
          </section>
        </div>

        <!-- 金额汇总 -->
        <aside class="od-aside">
          <div class="card">
            <div class="card-head"><h2>{{ $t('checkout.total') }}</h2></div>
            <div class="sum-row"><span>{{ $t('checkout.goods') }}</span><span>{{ money(order.goods) }}</span></div>
            <div class="sum-row"><span>{{ $t('checkout.discount') }}</span>
              <span :class="{ price: order.discount?.usdCents > 0 }">{{ order.discount?.usdCents > 0 ? '-' + money(order.discount) : money(order.discount) }}</span>
            </div>
            <div class="sum-row"><span>{{ $t('checkout.shipping') }}</span><span>{{ money(order.shipping) }}</span></div>
            <div class="sum-row"><span>{{ $t('checkout.tax') }}</span><span>{{ money(order.tax) }}</span></div>
            <div class="sum-total">
              <span>{{ $t('checkout.total') }}</span>
              <span class="price total-num">{{ money(order.total) }}</span>
            </div>
            <div class="sum-row pay-row">
              <span>{{ $t('checkout.payCurrency') }}</span>
              <span>{{ order.payCurrency }} {{ payAmountText }}（{{ txt.rate }} {{ Number(order.exchangeRate) }}）</span>
            </div>
          </div>
        </aside>
      </div>

      <!-- 评价弹窗 -->
      <el-dialog v-model="reviewDialog" :title="$t('order.review')" width="520px">
        <div class="rv-product muted">{{ reviewItem?.productName }}</div>
        <el-rate v-model="reviewForm.rating" />
        <el-input
          v-model="reviewForm.content"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          class="rv-content"
          :placeholder="txt.reviewPlaceholder"
        />
        <el-upload
          v-model:file-list="reviewFiles"
          list-type="picture-card"
          :limit="6"
          accept="image/*"
          :http-request="doUpload"
        >
          <span class="up-plus">+</span>
        </el-upload>
        <template #footer>
          <el-button @click="reviewDialog = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" color="#111111" :loading="reviewSaving" @click="submitReview">
            {{ $t('common.submit') }}
          </el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const { t } = useI18n()

const orderNo = route.params.orderNo
const loading = ref(true)
const order = ref(null)
const track = ref(null)
const countdown = ref(0)
let timer = null

const reviewDialog = ref(false)
const reviewItem = ref(null)
const reviewSaving = ref(false)
const reviewFiles = ref([])
const reviewForm = ref({ rating: 5, content: '' })

const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? {
        logs: '订单动态', rate: '汇率', reviewPlaceholder: '分享你的使用体验…',
        reviewOk: '评价已提交，审核通过后展示', confirmCancel: '确认取消该订单？',
        confirmReceipt: '确认已收到货？', confirm: '确认', cancel: '取消'
      }
    : {
        logs: 'Order Timeline', rate: 'rate', reviewPlaceholder: 'Share your experience…',
        reviewOk: 'Review submitted, visible after approval', confirmCancel: 'Cancel this order?',
        confirmReceipt: 'Confirm you have received the package?', confirm: 'Confirm', cancel: 'Cancel'
      }
)

const money = (m) => (m?.display ? m.display.symbol + m.display.text : '')
const mmss = (s) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

const STATUS_KEY = {
  WAIT_PAY: 'order.waitPay', PAID: 'order.paid', SHIPPED: 'order.shipped',
  FINISHED: 'order.finished', CLOSED: 'order.closed'
}
const statusLabel = (s) => (STATUS_KEY[s] ? t(STATUS_KEY[s]) : s)

const STEP_ACTIVE = { WAIT_PAY: 1, PAID: 2, SHIPPED: 3, FINISHED: 4 }
const stepActive = computed(() => STEP_ACTIVE[order.value?.status] ?? 0)

const receiverText = computed(() => {
  const a = order.value?.receiver
  if (!a) return ''
  return [a.addressLine1, a.addressLine2, a.city, a.state, a.countryCode, a.postcode].filter(Boolean).join(', ')
})

/** 支付币金额（按币种 decimalDigits 格式化，找不到按 2 位） */
const payAmountText = computed(() => {
  const o = order.value
  if (!o || o.payAmountCents == null) return ''
  const c = app.currencies.find((x) => x.code === o.payCurrency)
  const digits = c?.decimalDigits ?? 2
  return (c?.symbol || '') + (o.payAmountCents / 10 ** digits).toFixed(digits)
})

const logsDesc = computed(() => [...(order.value?.logs || [])].reverse())

async function load() {
  loading.value = true
  try {
    order.value = await client.get(`/order/${orderNo}`)
    if (order.value.status === 'WAIT_PAY') {
      countdown.value = order.value.countdownSeconds
        ?? Math.max(0, Math.floor((new Date(order.value.payDeadline).getTime() - Date.now()) / 1000))
      if (!timer) {
        timer = setInterval(() => {
          if (countdown.value > 0) countdown.value -= 1
        }, 1000)
      }
    }
    if (['SHIPPED', 'FINISHED'].includes(order.value.status)) {
      try { track.value = await client.get(`/logistics/track/${orderNo}`) } catch { /* 未出单则忽略 */ }
    }
  } finally {
    loading.value = false
  }
}

async function onCancel() {
  await ElMessageBox.confirm(txt.value.confirmCancel, undefined, {
    confirmButtonText: txt.value.confirm, cancelButtonText: txt.value.cancel, type: 'warning'
  })
  await client.post(`/order/${orderNo}/cancel`)
  await load()
}

async function onConfirm() {
  await ElMessageBox.confirm(txt.value.confirmReceipt, undefined, {
    confirmButtonText: txt.value.confirm, cancelButtonText: txt.value.cancel, type: 'warning'
  })
  await client.post(`/order/${orderNo}/confirm`)
  await load()
}

function openReview(it) {
  reviewItem.value = it
  reviewForm.value = { rating: 5, content: '' }
  reviewFiles.value = []
  reviewDialog.value = true
}

/** el-upload 自定义上传：POST /review/upload FormData(file) → {key,url} */
async function doUpload(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  try {
    const r = await client.post('/review/upload', fd)
    opt.onSuccess(r)
  } catch (e) {
    opt.onError(e)
  }
}

async function submitReview() {
  reviewSaving.value = true
  try {
    await client.post('/review', {
      orderNo,
      productId: reviewItem.value.productId,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content || null,
      images: reviewFiles.value.map((f) => f.response?.key).filter(Boolean)
    })
    ElMessage.success(txt.value.reviewOk)
    reviewDialog.value = false
  } catch (e) {
    // “已评价”等错误：拦截器已弹出后端文案
  } finally {
    reviewSaving.value = false
  }
}

onMounted(load)
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.od-page { padding-top: 32px; padding-bottom: 48px; min-height: 50vh; }
.page-title { font-size: 22px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; }
.muted { color: var(--muted); font-size: 12px; }

.od-head { display: flex; align-items: center; justify-content: space-between; }
.od-no { margin: 8px 0 18px; font-size: 13px; }
.od-cd { color: var(--price); margin-left: 12px; font-variant-numeric: tabular-nums; }
.od-btns { display: flex; gap: 10px; }

.closed-bar { margin-bottom: 20px; }
.od-steps { margin: 8px 0 26px; }

.od-grid { display: grid; grid-template-columns: 1fr 340px; gap: 20px; align-items: start; }
.od-aside { position: sticky; top: 16px; }

.card { border: 1px solid var(--line); padding: 18px; margin-bottom: 16px; background: #fff; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; gap: 12px; flex-wrap: wrap; }
.card-head h2 { font-size: 15px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; }

.recv { display: flex; align-items: center; gap: 10px; font-size: 14px; margin-bottom: 6px; flex-wrap: wrap; }
.recv-line { font-size: 13px; color: #555; line-height: 1.6; }
.divider { border-top: 1px dashed var(--line); margin: 12px 0; }

.line-row {
  display: grid; grid-template-columns: 56px 1fr 130px 90px 80px;
  gap: 12px; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--line);
}
.line-row:last-child { border-bottom: 0; }
.line-img { width: 56px; height: 74px; object-fit: cover; background: #f7f7f7; }
.line-name { font-size: 13px; line-height: 1.5; margin-bottom: 4px; }
.line-price { text-align: right; }
.line-total { text-align: right; font-size: 14px; }

.tl { padding-left: 4px; }
.log-meta { margin-left: 8px; }

.sum-row { display: flex; justify-content: space-between; font-size: 13px; color: #444; padding: 6px 0; gap: 10px; }
.sum-total {
  display: flex; justify-content: space-between; align-items: baseline;
  border-top: 1px solid var(--brand); margin-top: 10px; padding-top: 12px;
  font-size: 14px; font-weight: 700;
}
.total-num { font-size: 24px; }
.pay-row { border-top: 1px dashed var(--line); margin-top: 10px; padding-top: 10px; }

.btn-dark {
  background: var(--brand); color: #fff; border: 1px solid var(--brand);
  font-weight: 700; letter-spacing: 1px; cursor: pointer; border-radius: 0;
}
.btn-dark:hover { opacity: 0.85; }
.btn-light { background: #fff; color: var(--brand); border: 1px solid var(--brand); font-weight: 600; cursor: pointer; border-radius: 0; }
.btn-light:hover { background: var(--brand); color: #fff; }
.sm { padding: 8px 18px; font-size: 12px; }
.xs { padding: 5px 12px; font-size: 12px; }

.rv-product { margin-bottom: 10px; font-size: 13px; }
.rv-content { margin: 14px 0; }
.up-plus { font-size: 24px; color: #999; }

@media (max-width: 900px) {
  .od-grid { grid-template-columns: 1fr; }
}
</style>

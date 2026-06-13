<template>
  <div class="container co-page">
    <h1 class="page-title">{{ $t('checkout.title') }}</h1>

    <div class="co-grid">
      <div class="co-main">
        <!-- 收货地址 -->
        <section class="card">
          <div class="card-head">
            <h2>{{ $t('checkout.address') }}</h2>
            <el-button text type="primary" @click="openAddrDialog">+ {{ $t('checkout.addAddress') }}</el-button>
          </div>
          <div v-if="addresses.length" class="addr-grid">
            <div
              v-for="a in addresses"
              :key="a.id"
              class="pick-card"
              :class="{ active: a.id === addressId }"
              @click="selectAddress(a.id)"
            >
              <div class="pick-title">
                <b>{{ a.receiverName }}</b>
                <span class="muted">{{ a.phone }}</span>
                <el-tag v-if="a.isDefault" size="small" effect="dark" color="#111" style="border: 0">{{ txt.default }}</el-tag>
              </div>
              <div class="pick-sub">{{ addrText(a) }}</div>
            </div>
          </div>
          <div v-else class="muted empty-tip">{{ txt.noAddress }}</div>
        </section>

        <!-- 清关实名（目的国要求时） -->
        <section
          v-if="preview && preview.identityRequired"
          ref="identityCardRef"
          class="card"
          :class="{ 'card-warn': identityHighlight }"
        >
          <div class="card-head">
            <h2>{{ $t('checkout.identity') }}</h2>
            <el-button text type="primary" @click="identityDialog = true">+ {{ $t('checkout.addIdentity') }}</el-button>
          </div>
          <el-alert
            v-if="!identityId"
            :title="$t('checkout.identityTip')"
            type="warning"
            :closable="false"
            show-icon
            class="identity-alert"
          />
          <div v-if="identities.length" class="addr-grid">
            <div
              v-for="i in identities"
              :key="i.id"
              class="pick-card"
              :class="{ active: i.id === identityId }"
              @click="identityId = i.id"
            >
              <div class="pick-title">
                <b>{{ i.realName }}</b>
                <el-tag v-if="i.isDefault" size="small" effect="dark" color="#111" style="border: 0">{{ txt.default }}</el-tag>
              </div>
              <div class="pick-sub">{{ i.idCardMask }}</div>
            </div>
          </div>
        </section>

        <!-- 商品明细 -->
        <section class="card" v-loading="previewLoading">
          <div class="card-head"><h2>{{ $t('checkout.goods') }}</h2></div>
          <div v-for="line in preview?.items || []" :key="line.skuId" class="line-row">
            <img class="line-img" :src="line.image" :alt="line.name" />
            <div class="line-info">
              <div class="line-name">{{ line.name }}</div>
              <div class="line-sku muted">{{ line.skuText }}</div>
            </div>
            <div class="line-price">
              <span class="price">{{ fmt(line.unitPrice) }}</span>
              <span class="muted"> × {{ line.quantity }}</span>
            </div>
            <div class="line-total price">{{ fmt(line.lineTotal) }}</div>
          </div>
        </section>

        <!-- 优惠券 -->
        <section class="card">
          <div class="card-head"><h2>{{ $t('checkout.coupon') }}</h2></div>
          <el-select v-model="userCouponId" style="width: 100%" @change="doPreview">
            <el-option :label="$t('checkout.noCoupon')" :value="0" />
            <el-option
              v-for="c in preview?.availableCoupons || []"
              :key="c.userCouponId"
              :label="`${c.title}（-$${(c.discountCents / 100).toFixed(2)}）`"
              :value="c.userCouponId"
            />
          </el-select>
        </section>
      </div>

      <!-- 汇总 -->
      <aside class="co-aside">
        <div class="card" v-loading="previewLoading">
          <div class="card-head"><h2>{{ $t('checkout.total') }}</h2></div>
          <template v-if="preview">
            <div class="sum-row"><span>{{ $t('checkout.goods') }}</span><span>{{ money(preview.goods) }}</span></div>
            <div class="sum-row"><span>{{ $t('checkout.discount') }}</span>
              <span :class="{ price: preview.discount.usdCents > 0 }">{{ preview.discount.usdCents > 0 ? '-' + money(preview.discount) : money(preview.discount) }}</span>
            </div>
            <div class="sum-row"><span>{{ $t('checkout.shipping') }}</span><span>{{ money(preview.shipping) }}</span></div>
            <div class="sum-row">
              <span>{{ $t('checkout.tax') }}<span v-if="preview.taxNote" class="tax-note muted">（{{ preview.taxNote }}）</span></span>
              <span>{{ money(preview.tax) }}</span>
            </div>
            <div class="sum-row sum-pay"><span>{{ $t('checkout.payCurrency') }}</span><span>{{ app.currency }}</span></div>
            <div class="sum-total">
              <span>{{ $t('checkout.total') }}</span>
              <span class="price total-num">{{ money(preview.total) }}</span>
            </div>
            <div class="est muted">{{ $t('checkout.estDelivery', { min: preview.estDaysMin, max: preview.estDaysMax }) }}</div>
          </template>

          <el-input
            v-model="remark"
            class="remark"
            :placeholder="txt.remark"
            maxlength="255"
            type="textarea"
            :rows="2"
          />
          <button class="btn-dark" :disabled="!canSubmit || submitting" @click="submit">
            {{ submitting ? $t('common.loading') : $t('checkout.placeOrder') }}
          </button>
        </div>
      </aside>
    </div>

    <!-- 新增地址 -->
    <el-dialog v-model="addrDialog" :title="$t('checkout.addAddress')" width="560px">
      <el-form ref="addrFormRef" :model="addrForm" :rules="addrRules" label-position="top">
        <div class="form-2col">
          <el-form-item :label="txt.receiverName" prop="receiverName">
            <el-input v-model="addrForm.receiverName" />
          </el-form-item>
          <el-form-item :label="txt.phone" prop="phone">
            <el-input v-model="addrForm.phone" />
          </el-form-item>
          <el-form-item :label="txt.country" prop="countryCode">
            <el-select v-model="addrForm.countryCode" style="width: 100%">
              <el-option v-for="c in COUNTRIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item :label="txt.state" prop="state">
            <el-input v-model="addrForm.state" />
          </el-form-item>
          <el-form-item :label="txt.city" prop="city">
            <el-input v-model="addrForm.city" />
          </el-form-item>
          <el-form-item :label="txt.postcode" prop="postcode">
            <el-input v-model="addrForm.postcode" />
          </el-form-item>
        </div>
        <el-form-item :label="txt.addressLine1" prop="addressLine1">
          <el-input v-model="addrForm.addressLine1" />
        </el-form-item>
        <el-form-item :label="txt.addressLine2">
          <el-input v-model="addrForm.addressLine2" />
        </el-form-item>
        <el-checkbox v-model="addrForm.isDefault">{{ txt.setDefault }}</el-checkbox>
      </el-form>
      <template #footer>
        <el-button @click="addrDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" color="#111111" :loading="addrSaving" @click="saveAddress">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 添加实名 -->
    <el-dialog v-model="identityDialog" :title="$t('checkout.addIdentity')" width="420px">
      <el-form ref="idFormRef" :model="idForm" :rules="idRules" label-position="top">
        <el-form-item :label="txt.realName" prop="realName">
          <el-input v-model="idForm.realName" />
        </el-form-item>
        <el-form-item :label="txt.idCardNo" prop="idCardNo">
          <el-input v-model="idForm.idCardNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="identityDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" color="#111111" :loading="idSaving" @click="saveIdentity">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const COUNTRIES = ['CN', 'US', 'CA', 'GB', 'DE', 'FR', 'IT', 'ES', 'JP', 'AU', 'SG']

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const fromCart = route.query.fromCart === '1'
const directItems = !fromCart && route.query.skuId
  ? [{ skuId: Number(route.query.skuId), quantity: Number(route.query.quantity) || 1 }]
  : null

const addresses = ref([])
const addressId = ref(null)
const identities = ref([])
const identityId = ref(null)
const preview = ref(null)
const userCouponId = ref(0)
const remark = ref('')
const previewLoading = ref(false)
const submitting = ref(false)
const identityHighlight = ref(false)
const identityCardRef = ref()

const addrDialog = ref(false)
const addrSaving = ref(false)
const addrFormRef = ref()
const addrForm = ref({
  receiverName: '', phone: '', countryCode: 'CN', state: '', city: '',
  addressLine1: '', addressLine2: '', postcode: '', isDefault: false
})

const identityDialog = ref(false)
const idSaving = ref(false)
const idFormRef = ref()
const idForm = ref({ realName: '', idCardNo: '' })

const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? {
        default: '默认', noAddress: '暂无收货地址，请先新增', remark: '订单备注（选填）',
        receiverName: '收件人', phone: '电话', country: '国家/地区', state: '省/州',
        city: '城市', postcode: '邮编', addressLine1: '详细地址', addressLine2: '详细地址 2（选填）',
        setDefault: '设为默认', realName: '真实姓名', idCardNo: '身份证号', required: '必填',
        needIdentity: '请先选择或添加清关实名信息'
      }
    : {
        default: 'Default', noAddress: 'No address yet, please add one', remark: 'Order note (optional)',
        receiverName: 'Receiver', phone: 'Phone', country: 'Country/Region', state: 'State/Province',
        city: 'City', postcode: 'Postcode', addressLine1: 'Address line 1', addressLine2: 'Address line 2 (optional)',
        setDefault: 'Set as default', realName: 'Full name', idCardNo: 'ID card number', required: 'Required',
        needIdentity: 'Please select or add customs ID info first'
      }
)

const req = computed(() => ({ required: true, message: txt.value.required, trigger: 'blur' }))
const addrRules = computed(() => ({
  receiverName: [req.value], phone: [req.value], countryCode: [req.value],
  state: [req.value], city: [req.value], addressLine1: [req.value], postcode: [req.value]
}))
const idRules = computed(() => ({ realName: [req.value], idCardNo: [req.value] }))

const fmt = (p) => (p ? p.symbol + p.text : '')
const money = (m) => (m?.display ? m.display.symbol + m.display.text : '')
const addrText = (a) =>
  [a.addressLine1, a.addressLine2, a.city, a.state, a.countryCode, a.postcode].filter(Boolean).join(', ')

const canSubmit = computed(() => !!preview.value && !!addressId.value)

function itemsPayload() {
  return fromCart ? { fromCart: true } : { items: directItems }
}

async function doPreview() {
  if (!addressId.value || (!fromCart && !directItems)) return
  previewLoading.value = true
  try {
    preview.value = await client.post('/checkout/preview', {
      addressId: addressId.value,
      userCouponId: userCouponId.value || null,
      currency: app.currency,
      locale: app.locale,
      ...itemsPayload()
    })
  } catch (e) {
    // 券不可用等场景：去掉券重试一次
    if (userCouponId.value) {
      userCouponId.value = 0
      await doPreview()
    }
  } finally {
    previewLoading.value = false
  }
}

function selectAddress(id) {
  if (addressId.value === id) return
  addressId.value = id
  doPreview()
}

function openAddrDialog() {
  addrForm.value = {
    receiverName: '', phone: '', countryCode: 'CN', state: '', city: '',
    addressLine1: '', addressLine2: '', postcode: '', isDefault: false
  }
  addrDialog.value = true
}

async function saveAddress() {
  await addrFormRef.value.validate()
  addrSaving.value = true
  try {
    const r = await client.post('/address', addrForm.value)
    addrDialog.value = false
    addresses.value = (await client.get('/address/list')) || []
    addressId.value = r?.id || addresses.value[addresses.value.length - 1]?.id
    await doPreview()
  } finally {
    addrSaving.value = false
  }
}

async function saveIdentity() {
  await idFormRef.value.validate()
  idSaving.value = true
  try {
    const r = await client.post('/identity', idForm.value)
    identityDialog.value = false
    idForm.value = { realName: '', idCardNo: '' }
    identities.value = (await client.get('/identity/list')) || []
    identityId.value = r?.id || identities.value[identities.value.length - 1]?.id
    identityHighlight.value = false
  } finally {
    idSaving.value = false
  }
}

async function submit() {
  if (preview.value?.identityRequired && !identityId.value) {
    identityHighlight.value = true
    identityCardRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    ElMessage.warning(txt.value.needIdentity)
    return
  }
  submitting.value = true
  try {
    const r = await client.post('/order/create', {
      addressId: addressId.value,
      userCouponId: userCouponId.value || null,
      identityId: preview.value?.identityRequired ? identityId.value : null,
      payCurrency: app.currency,
      locale: app.locale,
      remark: remark.value || null,
      ...itemsPayload()
    })
    if (fromCart) {
      try {
        const c = await client.get('/cart/count')
        app.cartCount = c?.count || 0
      } catch { /* ignore */ }
    }
    router.push(`/pay/${r.orderNo}`)
  } catch (e) {
    if (e?.code === 30005) {
      identityHighlight.value = true
      identityCardRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!fromCart && !directItems) {
    router.replace('/cart')
    return
  }
  const [addrs, ids] = await Promise.all([
    client.get('/address/list').catch(() => []),
    client.get('/identity/list').catch(() => [])
  ])
  addresses.value = addrs || []
  identities.value = ids || []
  const defAddr = addresses.value.find((a) => a.isDefault) || addresses.value[0]
  addressId.value = defAddr?.id || null
  const defId = identities.value.find((i) => i.isDefault) || identities.value[0]
  identityId.value = defId?.id || null
  if (addressId.value) await doPreview()
})
</script>

<style scoped>
.co-page { padding-top: 32px; padding-bottom: 48px; }
.page-title { font-size: 22px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 22px; }
.muted { color: var(--muted); font-size: 12px; }

.co-grid { display: grid; grid-template-columns: 1fr 360px; gap: 20px; align-items: start; }
.co-aside { position: sticky; top: 16px; }

.card { border: 1px solid var(--line); padding: 18px; margin-bottom: 16px; background: #fff; }
.card-warn { border-color: var(--brand-accent); box-shadow: 0 0 0 2px rgba(250, 99, 56, 0.18); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.card-head h2 { font-size: 15px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; }
.empty-tip { padding: 14px 0; font-size: 13px; }

.addr-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.pick-card { border: 1px solid var(--line); padding: 12px 14px; cursor: pointer; transition: all 0.15s; }
.pick-card:hover { border-color: #999; }
.pick-card.active { border: 2px solid var(--brand); padding: 11px 13px; }
.pick-title { display: flex; align-items: center; gap: 8px; font-size: 14px; margin-bottom: 6px; flex-wrap: wrap; }
.pick-sub { font-size: 12px; color: #666; line-height: 1.5; }
.identity-alert { margin-bottom: 12px; }

.line-row {
  display: grid; grid-template-columns: 64px 1fr 150px 90px;
  gap: 14px; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--line);
}
.line-row:last-child { border-bottom: 0; }
.line-img { width: 64px; height: 84px; object-fit: cover; background: #f7f7f7; }
.line-name { font-size: 13px; line-height: 1.5; margin-bottom: 4px; }
.line-price { text-align: right; font-size: 13px; }
.line-total { text-align: right; font-size: 14px; }

.sum-row { display: flex; justify-content: space-between; font-size: 13px; color: #444; padding: 6px 0; }
.sum-pay { border-top: 1px dashed var(--line); margin-top: 4px; padding-top: 10px; }
.tax-note { font-size: 11px; }
.sum-total {
  display: flex; justify-content: space-between; align-items: baseline;
  border-top: 1px solid var(--brand); margin-top: 10px; padding-top: 12px;
  font-size: 14px; font-weight: 700;
}
.total-num { font-size: 26px; }
.est { margin-top: 8px; font-size: 12px; }
.remark { margin: 14px 0; }

.btn-dark {
  width: 100%; background: var(--brand); color: #fff; border: 1px solid var(--brand);
  padding: 13px 0; font-size: 14px; font-weight: 700; letter-spacing: 2px;
  cursor: pointer; border-radius: 0; transition: opacity 0.2s; text-transform: uppercase;
}
.btn-dark:hover:not(:disabled) { opacity: 0.85; }
.btn-dark:disabled { background: #ccc; border-color: #ccc; cursor: not-allowed; }

.form-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }

@media (max-width: 900px) {
  .co-grid { grid-template-columns: 1fr; }
  .addr-grid { grid-template-columns: 1fr; }
}
</style>

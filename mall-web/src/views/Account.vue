<template>
  <div class="container acc-page">
    <h1 class="page-title">{{ $t('nav.account') }}</h1>

    <div class="acc-grid">
      <!-- 左侧菜单 -->
      <aside class="acc-menu">
        <div
          v-for="m in MENUS"
          :key="m"
          class="menu-item"
          :class="{ active: section === m }"
          @click="section = m"
        >
          {{ $t(`account.${m}`) }}
        </div>
      </aside>

      <!-- 右侧内容 -->
      <div class="acc-content">
        <!-- 个人资料 -->
        <section v-if="section === 'profile'" class="card">
          <div class="card-head"><h2>{{ $t('account.profile') }}</h2></div>
          <el-form label-position="top" class="profile-form" @submit.prevent>
            <el-form-item :label="$t('account.email')">
              <el-input :model-value="profile?.email" disabled />
            </el-form-item>
            <el-form-item :label="$t('account.nickname')">
              <el-input v-model="profileForm.nickname" maxlength="30" />
            </el-form-item>
            <el-form-item :label="$t('account.language')">
              <el-select v-model="profileForm.locale" style="width: 100%">
                <el-option label="中文" value="zh-CN" />
                <el-option label="English" value="en-US" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('account.currency')">
              <el-select v-model="profileForm.currency" style="width: 100%">
                <el-option
                  v-for="c in currencies"
                  :key="c.code"
                  :label="`${c.code} ${c.symbol} ${zh ? c.nameZh : c.nameEn}`"
                  :value="c.code"
                />
              </el-select>
            </el-form-item>
            <button class="btn-dark" :disabled="profileSaving" @click="saveProfile">{{ $t('common.save') }}</button>
          </el-form>

          <el-collapse class="pwd-collapse">
            <el-collapse-item :title="$t('account.password')">
              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" @submit.prevent>
                <el-form-item :label="txt.oldPassword" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="current-password" />
                </el-form-item>
                <el-form-item :label="$t('auth.newPassword')" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="new-password" />
                </el-form-item>
                <button class="btn-dark" :disabled="pwdSaving" @click="savePassword">{{ $t('common.submit') }}</button>
              </el-form>
            </el-collapse-item>
          </el-collapse>
        </section>

        <!-- 收货地址 -->
        <section v-else-if="section === 'address'" class="card">
          <div class="card-head">
            <h2>{{ $t('account.address') }}</h2>
            <el-button text type="primary" @click="openAddrDialog(null)">+ {{ $t('checkout.addAddress') }}</el-button>
          </div>
          <div v-if="addresses.length" class="item-grid">
            <div v-for="a in addresses" :key="a.id" class="item-card">
              <div class="item-title">
                <b>{{ a.receiverName }}</b>
                <span class="muted">{{ a.phone }}</span>
                <el-tag v-if="a.isDefault" size="small" effect="dark" color="#111" style="border: 0">{{ txt.default }}</el-tag>
              </div>
              <div class="item-sub">{{ addrText(a) }}</div>
              <div class="item-ops">
                <el-button text size="small" @click="openAddrDialog(a)">{{ $t('common.edit') }}</el-button>
                <el-button text size="small" @click="deleteAddress(a)">{{ $t('common.delete') }}</el-button>
                <el-button v-if="!a.isDefault" text size="small" @click="setDefaultAddress(a)">{{ txt.setDefault }}</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else :description="$t('common.empty')" :image-size="70" />
        </section>

        <!-- 实名信息 -->
        <section v-else-if="section === 'identity'" class="card">
          <div class="card-head">
            <h2>{{ $t('account.identity') }}</h2>
            <el-button text type="primary" @click="identityDialog = true">+ {{ $t('checkout.addIdentity') }}</el-button>
          </div>
          <el-alert :title="$t('checkout.identityTip')" type="info" :closable="false" show-icon class="id-tip" />
          <div v-if="identities.length" class="item-grid">
            <div v-for="i in identities" :key="i.id" class="item-card">
              <div class="item-title">
                <b>{{ i.realName }}</b>
                <el-tag v-if="i.isDefault" size="small" effect="dark" color="#111" style="border: 0">{{ txt.default }}</el-tag>
              </div>
              <div class="item-sub">{{ i.idCardMask }}</div>
              <div class="item-ops">
                <el-button text size="small" @click="deleteIdentity(i)">{{ $t('common.delete') }}</el-button>
                <el-button v-if="!i.isDefault" text size="small" @click="setDefaultIdentity(i)">{{ txt.setDefault }}</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else :description="$t('common.empty')" :image-size="70" />
        </section>

        <!-- 我的优惠券 -->
        <section v-else-if="section === 'coupons'" class="card">
          <div class="card-head"><h2>{{ $t('account.coupons') }}</h2></div>
          <el-tabs v-model="couponTab" @tab-change="loadMine">
            <el-tab-pane :label="txt.unused" name="0" />
            <el-tab-pane :label="txt.used" name="1" />
            <el-tab-pane :label="txt.expired" name="2" />
          </el-tabs>
          <div v-if="mine.length" class="coupon-grid" v-loading="couponLoading">
            <div v-for="c in mine" :key="c.id" class="coupon-card" :class="{ dim: couponTab !== '0' }">
              <div class="cp-value">{{ couponValue(c) }}</div>
              <div class="cp-info">
                <div class="cp-title">{{ c.title }}</div>
                <div class="cp-cond muted">{{ couponCond(c) }}</div>
                <div class="cp-date muted">{{ (c.validTo || '').slice(0, 10) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else v-loading="couponLoading" :description="$t('common.empty')" :image-size="70" />

          <!-- 领券中心 -->
          <div class="card-head claim-head"><h2>{{ txt.claimCenter }}</h2></div>
          <div v-if="available.length" class="coupon-grid">
            <div v-for="c in available" :key="c.id" class="coupon-card">
              <div class="cp-value">{{ couponValue(c) }}</div>
              <div class="cp-info">
                <div class="cp-title">{{ c.title }}</div>
                <div class="cp-cond muted">{{ couponCond(c) }}</div>
                <div class="cp-date muted">{{ (c.validTo || '').slice(0, 10) }}</div>
              </div>
              <button class="cp-claim" :disabled="!c.canClaim" @click="claim(c)">
                {{ c.canClaim ? txt.claim : txt.claimed }}
              </button>
            </div>
          </div>
          <el-empty v-else :description="$t('common.empty')" :image-size="70" />
        </section>
      </div>
    </div>

    <!-- 地址新增/编辑 -->
    <el-dialog v-model="addrDialog" :title="editingAddr ? $t('common.edit') : $t('checkout.addAddress')" width="560px">
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
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const MENUS = ['profile', 'address', 'identity', 'coupons']
const COUNTRIES = ['CN', 'US', 'CA', 'GB', 'DE', 'FR', 'IT', 'ES', 'JP', 'AU', 'SG']

const router = useRouter()
const app = useAppStore()

const section = ref('profile')
const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? {
        default: '默认', setDefault: '设为默认', oldPassword: '旧密码', required: '必填',
        receiverName: '收件人', phone: '电话', country: '国家/地区', state: '省/州',
        city: '城市', postcode: '邮编', addressLine1: '详细地址', addressLine2: '详细地址 2（选填）',
        realName: '真实姓名', idCardNo: '身份证号', saved: '已保存', pwdOk: '密码已修改，请重新登录',
        unused: '未使用', used: '已使用', expired: '已过期', claimCenter: '领券中心',
        claim: '立即领取', claimed: '不可领取', claimOk: '领取成功',
        confirmDelete: '确认删除？', confirm: '确认', cancel: '取消', over: '满'
      }
    : {
        default: 'Default', setDefault: 'Set default', oldPassword: 'Current password', required: 'Required',
        receiverName: 'Receiver', phone: 'Phone', country: 'Country/Region', state: 'State/Province',
        city: 'City', postcode: 'Postcode', addressLine1: 'Address line 1', addressLine2: 'Address line 2 (optional)',
        realName: 'Full name', idCardNo: 'ID card number', saved: 'Saved', pwdOk: 'Password updated, please sign in again',
        unused: 'Available', used: 'Used', expired: 'Expired', claimCenter: 'Coupon Center',
        claim: 'Claim', claimed: 'Unavailable', claimOk: 'Claimed',
        confirmDelete: 'Delete this item?', confirm: 'Confirm', cancel: 'Cancel', over: 'Over'
      }
)

const req = computed(() => ({ required: true, message: txt.value.required, trigger: 'blur' }))

/* ---------- profile ---------- */
const profile = ref(null)
const currencies = ref([])
const profileForm = ref({ nickname: '', locale: app.locale, currency: app.currency })
const profileSaving = ref(false)

const pwdFormRef = ref()
const pwdForm = ref({ oldPassword: '', newPassword: '' })
const pwdSaving = ref(false)
const pwdRules = computed(() => ({
  oldPassword: [req.value],
  newPassword: [req.value, { min: 6, max: 32, message: txt.value.required, trigger: 'blur' }]
}))

async function loadProfile() {
  profile.value = await client.get('/user/profile')
  profileForm.value = {
    nickname: profile.value?.nickname || '',
    locale: profile.value?.locale || app.locale,
    currency: profile.value?.currency || app.currency
  }
}

async function saveProfile() {
  profileSaving.value = true
  try {
    const r = await client.put('/user/profile', profileForm.value)
    app.setLogin(app.token, r || profile.value)
    ElMessage.success(txt.value.saved)
    const localeChanged = profileForm.value.locale !== app.locale
    const currencyChanged = profileForm.value.currency !== app.currency
    if (localeChanged) app.switchLocale(profileForm.value.locale)
    if (currencyChanged) app.switchCurrency(profileForm.value.currency)
    if (localeChanged || currencyChanged) location.reload()
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  await pwdFormRef.value.validate()
  pwdSaving.value = true
  try {
    await client.put('/user/password', pwdForm.value)
    ElMessage.success(txt.value.pwdOk)
    try { await client.post('/auth/logout') } catch { /* ignore */ }
    app.logout()
    router.push('/login')
  } finally {
    pwdSaving.value = false
  }
}

/* ---------- address ---------- */
const addresses = ref([])
const addrDialog = ref(false)
const addrSaving = ref(false)
const addrFormRef = ref()
const editingAddr = ref(null)
const addrForm = ref({})
const addrRules = computed(() => ({
  receiverName: [req.value], phone: [req.value], countryCode: [req.value],
  state: [req.value], city: [req.value], addressLine1: [req.value], postcode: [req.value]
}))

const addrText = (a) =>
  [a.addressLine1, a.addressLine2, a.city, a.state, a.countryCode, a.postcode].filter(Boolean).join(', ')

async function loadAddresses() {
  addresses.value = (await client.get('/address/list')) || []
}

function openAddrDialog(a) {
  editingAddr.value = a
  addrForm.value = a
    ? {
        receiverName: a.receiverName, phone: a.phone, countryCode: a.countryCode,
        state: a.state, city: a.city, addressLine1: a.addressLine1,
        addressLine2: a.addressLine2 || '', postcode: a.postcode, isDefault: !!a.isDefault
      }
    : {
        receiverName: '', phone: '', countryCode: 'CN', state: '', city: '',
        addressLine1: '', addressLine2: '', postcode: '', isDefault: false
      }
  addrDialog.value = true
}

async function saveAddress() {
  await addrFormRef.value.validate()
  addrSaving.value = true
  try {
    if (editingAddr.value) await client.put(`/address/${editingAddr.value.id}`, addrForm.value)
    else await client.post('/address', addrForm.value)
    addrDialog.value = false
    await loadAddresses()
  } finally {
    addrSaving.value = false
  }
}

async function deleteAddress(a) {
  await ElMessageBox.confirm(txt.value.confirmDelete, undefined, {
    confirmButtonText: txt.value.confirm, cancelButtonText: txt.value.cancel, type: 'warning'
  })
  await client.delete(`/address/${a.id}`)
  await loadAddresses()
}

async function setDefaultAddress(a) {
  await client.put(`/address/${a.id}/default`)
  await loadAddresses()
}

/* ---------- identity ---------- */
const identities = ref([])
const identityDialog = ref(false)
const idSaving = ref(false)
const idFormRef = ref()
const idForm = ref({ realName: '', idCardNo: '' })
const idRules = computed(() => ({ realName: [req.value], idCardNo: [req.value] }))

async function loadIdentities() {
  identities.value = (await client.get('/identity/list')) || []
}

async function saveIdentity() {
  await idFormRef.value.validate()
  idSaving.value = true
  try {
    await client.post('/identity', idForm.value)
    identityDialog.value = false
    idForm.value = { realName: '', idCardNo: '' }
    await loadIdentities()
  } finally {
    idSaving.value = false
  }
}

async function deleteIdentity(i) {
  await ElMessageBox.confirm(txt.value.confirmDelete, undefined, {
    confirmButtonText: txt.value.confirm, cancelButtonText: txt.value.cancel, type: 'warning'
  })
  await client.delete(`/identity/${i.id}`)
  await loadIdentities()
}

async function setDefaultIdentity(i) {
  await client.put(`/identity/${i.id}/default`)
  await loadIdentities()
}

/* ---------- coupons ---------- */
const couponTab = ref('0')
const mine = ref([])
const available = ref([])
const couponLoading = ref(false)

const couponValue = (c) => (c.type === 'PERCENT' ? `${c.value}% OFF` : `$${(c.value / 100).toFixed(2)}`)
const couponCond = (c) =>
  c.minAmountCents > 0 ? `${txt.value.over} $${(c.minAmountCents / 100).toFixed(2)}` : ''

async function loadMine() {
  couponLoading.value = true
  try {
    mine.value = (await client.get('/coupon/mine', { params: { status: couponTab.value } })) || []
  } finally {
    couponLoading.value = false
  }
}

async function loadAvailable() {
  available.value = (await client.get('/coupon/available')) || []
}

async function claim(c) {
  await client.post(`/coupon/${c.id}/claim`)
  ElMessage.success(txt.value.claimOk)
  couponTab.value = '0'
  await Promise.all([loadMine(), loadAvailable()])
}

/* ---------- 切换分区按需加载 ---------- */
watch(section, (s) => {
  if (s === 'address' && !addresses.value.length) loadAddresses()
  if (s === 'identity' && !identities.value.length) loadIdentities()
  if (s === 'coupons') { loadMine(); loadAvailable() }
})

onMounted(async () => {
  loadProfile()
  currencies.value = app.currencies.length
    ? app.currencies
    : (await client.get('/currency/list').catch(() => [])) || []
})
</script>

<style scoped>
.acc-page { padding-top: 32px; padding-bottom: 48px; min-height: 60vh; }
.page-title { font-size: 22px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 22px; }
.muted { color: var(--muted); font-size: 12px; }

.acc-grid { display: grid; grid-template-columns: 220px 1fr; gap: 20px; align-items: start; }

.acc-menu { border: 1px solid var(--line); }
.menu-item {
  padding: 14px 18px; font-size: 14px; cursor: pointer; border-bottom: 1px solid var(--line);
  transition: all 0.15s;
}
.menu-item:last-child { border-bottom: 0; }
.menu-item:hover { background: #fafafa; }
.menu-item.active { background: var(--brand); color: #fff; font-weight: 700; }

.card { border: 1px solid var(--line); padding: 22px; background: #fff; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.card-head h2 { font-size: 15px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; }

.profile-form { max-width: 420px; }
.pwd-collapse { margin-top: 26px; max-width: 420px; }

.item-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.item-card { border: 1px solid var(--line); padding: 14px 16px; }
.item-title { display: flex; align-items: center; gap: 8px; font-size: 14px; margin-bottom: 6px; flex-wrap: wrap; }
.item-sub { font-size: 12px; color: #666; line-height: 1.5; }
.item-ops { margin-top: 10px; display: flex; }
.id-tip { margin-bottom: 14px; }

.coupon-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 8px; }
.coupon-card {
  display: flex; align-items: center; gap: 14px;
  border: 1px dashed var(--brand-accent); padding: 14px 16px; background: #fff7f4;
}
.coupon-card.dim { border-color: #ccc; background: #fafafa; filter: grayscale(1); opacity: 0.7; }
.cp-value { font-size: 18px; font-weight: 800; color: var(--price); min-width: 84px; }
.cp-info { flex: 1; min-width: 0; }
.cp-title { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
.cp-claim {
  background: var(--brand); color: #fff; border: 1px solid var(--brand);
  padding: 8px 16px; font-size: 12px; font-weight: 700; cursor: pointer; border-radius: 0;
}
.cp-claim:hover:not(:disabled) { opacity: 0.85; }
.cp-claim:disabled { background: #ccc; border-color: #ccc; cursor: not-allowed; }
.claim-head { margin-top: 28px; }

.btn-dark {
  background: var(--brand); color: #fff; border: 1px solid var(--brand);
  padding: 11px 36px; font-size: 13px; font-weight: 700; letter-spacing: 1px;
  cursor: pointer; border-radius: 0;
}
.btn-dark:hover:not(:disabled) { opacity: 0.85; }
.btn-dark:disabled { background: #ccc; border-color: #ccc; cursor: not-allowed; }

.form-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }

@media (max-width: 900px) {
  .acc-grid { grid-template-columns: 1fr; }
  .item-grid, .coupon-grid { grid-template-columns: 1fr; }
}
</style>

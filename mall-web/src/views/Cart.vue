<template>
  <div class="container cart-page" v-loading="loading">
    <h1 class="page-title">{{ $t('cart.title') }}</h1>

    <template v-if="data && data.items.length">
      <div class="cart-head">
        <el-checkbox :model-value="allChecked" :indeterminate="indeterminate" @change="onCheckAll">
          {{ $t('cart.selectAll') }}
        </el-checkbox>
        <el-button text :disabled="!data.checkedCount" @click="onDeleteChecked">
          {{ $t('cart.deleteChecked') }}
        </el-button>
      </div>

      <div class="cart-list">
        <div v-for="it in data.items" :key="it.id" class="cart-row" :class="{ 'row-invalid': it.invalid }">
          <el-checkbox
            :model-value="it.checked && !it.invalid"
            :disabled="it.invalid"
            @change="(v) => onCheck(it, v)"
          />
          <img class="row-img" :src="it.image" :alt="it.name || ''" />
          <div class="row-info">
            <div class="row-name">{{ it.name }}</div>
            <div class="row-sku">{{ it.skuText }}</div>
            <el-tag v-if="it.invalid" size="small" type="info">{{ $t('cart.invalid') }}</el-tag>
          </div>
          <div class="row-price">
            <span class="price">{{ fmt(it.price) }}</span>
            <span v-if="it.discountPercent && it.originalPrice" class="price-origin">{{ fmt(it.originalPrice) }}</span>
          </div>
          <el-input-number
            :model-value="it.quantity"
            :min="1"
            :max="Math.max(1, Math.min(it.stock || 1, 99))"
            :disabled="it.invalid"
            size="small"
            @change="(v) => onQty(it, v)"
          />
          <div class="row-total price">{{ lineTotal(it) }}</div>
          <el-button text class="row-del" :title="$t('common.delete')" @click="onDelete(it)">✕</el-button>
        </div>
      </div>

      <!-- 结算条 -->
      <div class="cart-bar">
        <span class="bar-count">{{ selectedText }}</span>
        <div class="bar-right">
          <span class="bar-label">{{ $t('cart.subtotal') }}</span>
          <span class="bar-total price">{{ fmt(data.subtotal) }}</span>
          <button class="btn-dark" :disabled="!data.checkedCount" @click="goCheckout">
            {{ $t('cart.checkout') }}
          </button>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" :description="$t('cart.empty')">
      <button class="btn-dark" @click="$router.push('/')">{{ $t('nav.home') }}</button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const router = useRouter()
const app = useAppStore()

const loading = ref(false)
const data = ref(null)

const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? { confirmDelete: '确认删除该商品？', confirmDeleteChecked: '确认删除选中的商品？' }
    : { confirmDelete: 'Remove this item?', confirmDeleteChecked: 'Remove all selected items?' }
)

const fmt = (p) => (p ? p.symbol + p.text : '')

/** 行小计 = 实时单价 × 数量（小数位跟随单价 text） */
function lineTotal(it) {
  if (!it.price) return ''
  const digits = (it.price.text.split('.')[1] || '').length
  const v = (it.price.amountMinor * it.quantity) / 10 ** digits
  return it.price.symbol + v.toFixed(digits)
}

const validItems = computed(() => (data.value?.items || []).filter((i) => !i.invalid))
const allChecked = computed(
  () => validItems.value.length > 0 && validItems.value.every((i) => i.checked)
)
const indeterminate = computed(
  () => !allChecked.value && validItems.value.some((i) => i.checked)
)
const selectedText = computed(() =>
  zh.value ? `已选 ${data.value?.checkedCount || 0} 件` : `${data.value?.checkedCount || 0} selected`
)

async function load() {
  loading.value = true
  try {
    data.value = await client.get('/cart/list')
    await syncCount()
  } finally {
    loading.value = false
  }
}

async function syncCount() {
  try {
    const r = await client.get('/cart/count')
    app.cartCount = r?.count || 0
  } catch { /* ignore */ }
}

async function onCheck(it, checked) {
  await client.put(`/cart/${it.id}`, { checked })
  await load()
}

async function onQty(it, quantity) {
  if (!quantity || quantity === it.quantity) return
  await client.put(`/cart/${it.id}`, { quantity })
  await load()
}

async function onCheckAll(checked) {
  await client.put('/cart/check-all', { checked })
  await load()
}

async function onDelete(it) {
  await ElMessageBox.confirm(txt.value.confirmDelete, undefined, {
    confirmButtonText: zh.value ? '确认' : 'Confirm',
    cancelButtonText: zh.value ? '取消' : 'Cancel',
    type: 'warning'
  })
  await client.delete(`/cart/${it.id}`)
  await load()
}

async function onDeleteChecked() {
  await ElMessageBox.confirm(txt.value.confirmDeleteChecked, undefined, {
    confirmButtonText: zh.value ? '确认' : 'Confirm',
    cancelButtonText: zh.value ? '取消' : 'Cancel',
    type: 'warning'
  })
  await client.delete('/cart/checked')
  await load()
}

function goCheckout() {
  router.push('/checkout?fromCart=1')
}

onMounted(load)
</script>

<style scoped>
.cart-page { padding-top: 32px; padding-bottom: 40px; min-height: 50vh; }
.page-title { font-size: 22px; font-weight: 800; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 22px; }

.cart-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; border: 1px solid var(--line); border-bottom: 0; background: #fafafa;
}

.cart-list { border: 1px solid var(--line); }
.cart-row {
  display: grid; grid-template-columns: 24px 90px 1fr 140px 130px 110px 36px;
  gap: 16px; align-items: center; padding: 18px 16px; border-bottom: 1px solid var(--line);
}
.cart-row:last-child { border-bottom: 0; }
.row-img { width: 90px; height: 120px; object-fit: cover; background: #f7f7f7; }
.row-name { font-size: 14px; line-height: 1.5; margin-bottom: 6px; }
.row-sku { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.row-price { display: flex; flex-direction: column; gap: 2px; }
.row-price .price { font-size: 15px; }
.row-total { font-size: 15px; text-align: right; }
.row-del { color: var(--muted); font-size: 14px; }
.row-del:hover { color: var(--brand); }
.row-invalid .row-img { opacity: 0.4; }
.row-invalid .row-name, .row-invalid .row-price { color: var(--muted); }

/* sticky 结算条 */
.cart-bar {
  position: sticky; bottom: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border: 1px solid var(--line); border-top: 2px solid var(--brand);
  padding: 14px 16px; margin-top: 16px;
}
.bar-count { font-size: 13px; color: #555; }
.bar-right { display: flex; align-items: center; gap: 14px; }
.bar-label { font-size: 13px; color: #555; }
.bar-total { font-size: 24px; }

.btn-dark {
  background: var(--brand); color: #fff; border: 1px solid var(--brand);
  padding: 12px 42px; font-size: 14px; font-weight: 700; letter-spacing: 2px;
  cursor: pointer; border-radius: 0; transition: opacity 0.2s; text-transform: uppercase;
}
.btn-dark:hover:not(:disabled) { opacity: 0.85; }
.btn-dark:disabled { background: #ccc; border-color: #ccc; cursor: not-allowed; }
</style>

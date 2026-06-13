<template>
  <div class="container pd" v-if="prod">
    <div class="pd-top">
      <!-- 左：图廊 -->
      <div class="pd-gallery">
        <div class="pd-thumbs">
          <img
            v-for="(img, i) in prod.images"
            :key="i"
            :src="img"
            :class="{ active: img === mainImg }"
            @click="mainImg = img"
            :alt="prod.name"
          />
        </div>
        <div class="pd-main"><img :src="mainImg" :alt="prod.name" /></div>
      </div>

      <!-- 右：信息 -->
      <div class="pd-info">
        <h1 class="pd-name">{{ prod.name }}</h1>
        <p class="pd-subtitle" v-if="prod.subtitle">{{ prod.subtitle }}</p>

        <div class="pd-rating">
          <el-rate :model-value="prod.ratingAvg" disabled allow-half />
          <span class="pd-rating-num">{{ prod.ratingAvg }}</span>
          <span class="pd-muted">({{ prod.ratingCount }} {{ $t('product.reviews') }})</span>
          <span class="pd-muted">· {{ $t('product.sales', { n: prod.salesCount }) }}</span>
        </div>

        <div class="pd-pricebox">
          <span class="price pd-price">{{ fmt(curPrice) }}</span>
          <span v-if="hasFlash" class="price-origin pd-origin">{{ fmt(priceSku.price) }}</span>
          <span v-if="hasFlash && prod.flash" class="flash-tag">{{ $t('product.flashLabel') }} -{{ prod.flash.discountPercent }}%</span>
        </div>

        <div class="pd-trade">
          <span class="trade-badge">{{ prod.tradeMode === 'BONDED' ? $t('product.bonded') : $t('product.direct') }}</span>
        </div>

        <!-- 颜色 -->
        <div class="pd-row">
          <div class="pd-label">{{ $t('product.color') }}: <b>{{ colorLabel(selColor) }}</b></div>
          <div class="pd-colors">
            <button
              v-for="c in colors"
              :key="c.color"
              class="color-item"
              :class="{ active: selColor === c.color }"
              :title="colorLabel(c.color)"
              @click="pickColor(c)"
            >
              <img v-if="c.image" :src="c.image" :alt="colorLabel(c.color)" />
              <span v-else>{{ colorLabel(c.color) }}</span>
            </button>
          </div>
        </div>

        <!-- 尺码 -->
        <div class="pd-row">
          <div class="pd-label">
            {{ $t('product.size') }}: <b>{{ selSize }}</b>
            <a v-if="sizeRows.length" class="size-link" @click="sizeDlg = true">{{ $t('product.sizeChart') }}</a>
          </div>
          <div class="pd-sizes">
            <button
              v-for="s in sizesForColor"
              :key="s.size"
              class="size-item"
              :class="{ active: selSize === s.size, disabled: s.stock === 0 }"
              :disabled="s.stock === 0"
              @click="selSize = s.size"
            >
              {{ s.size }}
            </button>
          </div>
        </div>

        <!-- 数量 -->
        <div class="pd-row pd-qty">
          <div class="pd-label">{{ $t('product.qty') }}</div>
          <el-input-number v-model="qty" :min="1" :max="maxQty" />
          <span class="pd-muted" v-if="curSku">{{ $t('product.stock') }}: {{ curSku.stock }}</span>
        </div>

        <!-- 操作 -->
        <div class="pd-actions">
          <button class="btn-cart" @click="addCart">{{ $t('product.addToCart') }}</button>
          <button class="btn-buy" @click="buyNow">{{ $t('product.buyNow') }}</button>
          <button
            class="btn-fav"
            :class="{ on: prod.favorite }"
            :title="prod.favorite ? $t('product.unfavorite') : $t('product.favorite')"
            @click="toggleFav"
          >
            <svg viewBox="0 0 24 24" width="22" height="22" :fill="prod.favorite ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 尺码表弹窗 -->
    <el-dialog v-model="sizeDlg" :title="$t('product.sizeChart')" width="560px">
      <el-table :data="sizeRows" border size="small">
        <el-table-column v-for="col in sizeCols" :key="col" :prop="col" :label="col" />
      </el-table>
    </el-dialog>

    <!-- 详情 / 评论 -->
    <el-tabs v-model="tab" class="pd-tabs">
      <el-tab-pane :label="$t('product.detail')" name="detail">
        <div class="pd-detail" v-html="prod.detail"></div>
      </el-tab-pane>
      <el-tab-pane :label="`${$t('product.reviews')} (${prod.ratingCount})`" name="reviews">
        <div v-if="reviews.length" class="rv-list">
          <div v-for="r in reviews" :key="r.id" class="rv-item">
            <div class="rv-head">
              <span class="rv-nick">{{ r.userNickname }}</span>
              <el-rate :model-value="r.rating" disabled size="small" />
              <span class="rv-time">{{ (r.createTime || '').replace('T', ' ') }}</span>
            </div>
            <p class="rv-content">{{ r.content }}</p>
            <div v-if="r.images && r.images.length" class="rv-imgs">
              <el-image
                v-for="(img, i) in r.images"
                :key="i"
                :src="img"
                :preview-src-list="r.images"
                :initial-index="i"
                fit="cover"
                class="rv-img"
              />
            </div>
            <div v-if="r.skuText" class="rv-sku">{{ r.skuText }}</div>
          </div>
          <div class="pager" v-if="rvTotal > rvPageSize">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="rvTotal"
              :page-size="rvPageSize"
              :current-page="rvPageNum"
              @current-change="loadReviews"
            />
          </div>
        </div>
        <el-empty v-else :description="$t('common.empty')" />
      </el-tab-pane>
    </el-tabs>

    <!-- 推荐 -->
    <section v-if="recommends.length" class="pd-rec">
      <h2 class="rec-title">YOU MAY ALSO LIKE</h2>
      <div class="p-grid">
        <router-link v-for="p in recommends" :key="p.id" :to="`/product/${p.id}`" class="p-card">
          <div class="p-img">
            <img :src="p.mainImage" :alt="p.name" loading="lazy" />
            <span v-if="p.discountPercent" class="flash-tag p-off">-{{ p.discountPercent }}%</span>
          </div>
          <div class="p-name">{{ p.name }}</div>
          <div class="p-price">
            <span class="price">{{ fmt(p.flashPrice || p.price) }}</span>
            <span v-if="p.flashPrice" class="price-origin">{{ fmt(p.price) }}</span>
          </div>
          <div class="p-meta">
            <span class="p-star">★ {{ p.ratingAvg }}</span>
            <span>{{ $t('product.sales', { n: p.salesCount }) }}</span>
          </div>
        </router-link>
      </div>
    </section>
  </div>
  <div v-else class="container pd-loading" v-loading="true"></div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const prod = ref(null)
const mainImg = ref('')
const selColor = ref('')
const selSize = ref('')
const qty = ref(1)
const tab = ref('detail')
const sizeDlg = ref(false)

const reviews = ref([])
const rvTotal = ref(0)
const rvPageNum = ref(1)
const rvPageSize = 10
const recommends = ref([])

const fmt = (p) => (p ? p.symbol + p.text : '')
const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? { selectSku: '请选择颜色和尺码', added: '已加入购物车' }
    : { selectSku: 'Please select color and size', added: 'Added to cart' }
)

/** skus 按 color 去重 → 颜色块 */
const colors = computed(() => {
  const seen = new Map()
  for (const s of prod.value?.skus || []) {
    if (!seen.has(s.color)) seen.set(s.color, { color: s.color, colorZh: s.colorZh, image: s.image })
  }
  return [...seen.values()]
})
const colorLabel = (color) => {
  const c = colors.value.find((x) => x.color === color)
  return zh.value ? c?.colorZh || color : color
}

/** 当前颜色下的尺码 */
const sizesForColor = computed(() =>
  (prod.value?.skus || []).filter((s) => s.color === selColor.value).map((s) => ({ size: s.size, stock: s.stock }))
)

/** 颜色+尺码都选定后的 SKU */
const curSku = computed(() =>
  (prod.value?.skus || []).find((s) => s.color === selColor.value && s.size === selSize.value) || null
)
/** 价格展示用 SKU：选定 SKU > 当前颜色首个 > 首个 */
const priceSku = computed(() => {
  const skus = prod.value?.skus || []
  return curSku.value || skus.find((s) => s.color === selColor.value) || skus[0] || null
})
const hasFlash = computed(() => !!priceSku.value?.flashPrice)
const curPrice = computed(() => priceSku.value?.flashPrice || priceSku.value?.price)
const maxQty = computed(() => (curSku.value ? Math.max(1, Math.min(curSku.value.stock, 99)) : 99))

/** sizeChart 兼容 JSON 字符串 / 已解析数组 */
const sizeRows = computed(() => {
  let sc = prod.value?.sizeChart
  if (!sc) return []
  if (typeof sc === 'string') {
    try { sc = JSON.parse(sc) } catch { return [] }
  }
  return Array.isArray(sc) ? sc : []
})
const sizeCols = computed(() => (sizeRows.value.length ? Object.keys(sizeRows.value[0]) : []))

function pickColor(c) {
  selColor.value = c.color
  if (c.image) mainImg.value = c.image
  // 当前颜色下原尺码缺货/不存在则重置
  const s = sizesForColor.value.find((x) => x.size === selSize.value)
  if (!s || s.stock === 0) selSize.value = ''
}

watch(curSku, (s) => { if (s && qty.value > maxQty.value) qty.value = maxQty.value })

function requireLogin() {
  if (!app.token) {
    router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
    return false
  }
  return true
}
function ensureSku() {
  if (!curSku.value) {
    ElMessage.warning(txt.value.selectSku)
    return false
  }
  return true
}

async function addCart() {
  if (!requireLogin() || !ensureSku()) return
  await client.post('/cart', { skuId: curSku.value.id, quantity: qty.value })
  try {
    const r = await client.get('/cart/count')
    app.cartCount = r?.count || 0
  } catch { /* ignore */ }
  ElMessage.success(txt.value.added)
}
function buyNow() {
  if (!requireLogin() || !ensureSku()) return
  router.push(`/checkout?skuId=${curSku.value.id}&quantity=${qty.value}`)
}
async function toggleFav() {
  if (!requireLogin()) return
  const r = await client.post(`/favorite/${prod.value.id}`)
  prod.value.favorite = !!r?.favorite
}

async function loadReviews(p = 1) {
  rvPageNum.value = p
  try {
    const r = await client.get('/review/list', { params: { productId: route.params.id, pageNum: p, pageSize: rvPageSize } })
    reviews.value = r?.list || []
    rvTotal.value = r?.total || 0
  } catch {
    reviews.value = []
    rvTotal.value = 0
  }
}

async function load() {
  prod.value = null
  tab.value = 'detail'
  selColor.value = ''
  selSize.value = ''
  qty.value = 1
  try {
    const r = await client.get(`/product/${route.params.id}`)
    prod.value = r
    mainImg.value = r?.images?.[0] || ''
    if (colors.value.length) pickColor(colors.value[0])
  } catch { /* ignore */ }
  loadReviews(1)
  client.get('/product/recommend', { params: { productId: route.params.id, limit: 8 } })
    .then((r) => { recommends.value = r || [] }).catch(() => { recommends.value = [] })
}

watch(
  () => route.params.id,
  (id) => { if (id && route.path.startsWith('/product/')) load() }
)
onMounted(load)
</script>

<style scoped>
.pd { padding-top: 28px; }
.pd-loading { min-height: 50vh; }

.pd-top { display: grid; grid-template-columns: 1fr 1fr; gap: 48px; }

/* 图廊 */
.pd-gallery { display: flex; gap: 12px; }
.pd-thumbs { display: flex; flex-direction: column; gap: 10px; width: 64px; flex-shrink: 0; }
.pd-thumbs img {
  width: 64px; height: 84px; object-fit: cover; cursor: pointer;
  border: 2px solid transparent; background: #f7f7f7;
}
.pd-thumbs img.active { border-color: var(--brand); }
.pd-main { flex: 1; aspect-ratio: 3 / 4; background: #f7f7f7; overflow: hidden; }
.pd-main img { width: 100%; height: 100%; object-fit: cover; }

/* 信息区 */
.pd-name { font-size: 22px; font-weight: 700; line-height: 1.4; }
.pd-subtitle { font-size: 13px; color: var(--muted); margin-top: 6px; }
.pd-rating { display: flex; align-items: center; gap: 8px; margin-top: 12px; font-size: 13px; }
.pd-rating-num { font-weight: 700; color: #f7ba2a; }
.pd-muted { color: var(--muted); font-size: 12px; }

.pd-pricebox { display: flex; align-items: baseline; gap: 10px; margin-top: 18px; }
.pd-price { font-size: 32px; }
.pd-origin { font-size: 15px; }

.pd-trade { margin-top: 12px; }
.trade-badge {
  display: inline-block; font-size: 12px; font-weight: 600;
  border: 1px solid var(--brand-accent); color: var(--brand-accent);
  padding: 2px 10px; border-radius: 2px;
}

.pd-row { margin-top: 22px; }
.pd-label { font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.pd-label b { font-weight: 700; }
.size-link { margin-left: 14px; font-size: 12px; color: var(--brand-accent); text-decoration: underline; cursor: pointer; }

.pd-colors { display: flex; gap: 10px; flex-wrap: wrap; }
.color-item {
  min-width: 52px; height: 68px; border: 2px solid var(--line); background: #fff;
  cursor: pointer; padding: 0; overflow: hidden; font-size: 12px;
}
.color-item img { width: 52px; height: 64px; object-fit: cover; }
.color-item span { padding: 0 10px; }
.color-item.active { border-color: var(--brand); }

.pd-sizes { display: flex; gap: 10px; flex-wrap: wrap; }
.size-item {
  min-width: 52px; height: 36px; padding: 0 14px;
  border: 1px solid #ccc; border-radius: 18px; background: #fff;
  font-size: 13px; cursor: pointer; transition: all 0.15s;
}
.size-item:hover:not(.disabled) { border-color: var(--brand); }
.size-item.active { border-color: var(--brand); background: var(--brand); color: #fff; }
.size-item.disabled { color: #ccc; text-decoration: line-through; cursor: not-allowed; background: #fafafa; }

.pd-qty { display: flex; align-items: center; gap: 14px; }
.pd-qty .pd-label { margin-bottom: 0; }

.pd-actions { display: flex; gap: 12px; margin-top: 28px; }
.btn-cart, .btn-buy {
  height: 48px; padding: 0 38px; font-size: 14px; font-weight: 800;
  letter-spacing: 1px; cursor: pointer; transition: all 0.2s;
}
.btn-cart { background: var(--brand); color: #fff; border: 1px solid var(--brand); }
.btn-cart:hover { opacity: 0.85; }
.btn-buy { background: var(--brand-accent); color: #fff; border: 1px solid var(--brand-accent); }
.btn-buy:hover { opacity: 0.85; }
.btn-fav {
  width: 48px; height: 48px; border: 1px solid #ccc; background: #fff;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: #666; transition: all 0.2s;
}
.btn-fav:hover, .btn-fav.on { color: var(--brand-accent); border-color: var(--brand-accent); }

/* tabs */
.pd-tabs { margin-top: 48px; }
.pd-detail { font-size: 14px; line-height: 1.8; padding: 12px 4px; }
.pd-detail :deep(img) { max-width: 100%; }

/* 评论 */
.rv-item { border-bottom: 1px solid var(--line); padding: 16px 4px; }
.rv-head { display: flex; align-items: center; gap: 12px; }
.rv-nick { font-size: 13px; font-weight: 700; }
.rv-time { font-size: 12px; color: var(--muted); margin-left: auto; }
.rv-content { font-size: 13px; line-height: 1.7; margin-top: 8px; }
.rv-imgs { display: flex; gap: 8px; margin-top: 10px; }
.rv-img { width: 70px; height: 70px; }
.rv-sku { font-size: 12px; color: var(--muted); margin-top: 8px; }
.pager { display: flex; justify-content: center; margin-top: 24px; }

/* 推荐 */
.pd-rec { margin-top: 56px; }
.rec-title { text-align: center; font-size: 22px; font-weight: 800; letter-spacing: 2px; margin-bottom: 28px; }

/* 商品卡片（与首页一致） */
.p-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 28px 18px; }
.p-card { display: block; transition: all 0.2s; padding-bottom: 10px; }
.p-card:hover { box-shadow: 0 10px 28px rgba(0, 0, 0, 0.12); transform: translateY(-3px); }
.p-img { position: relative; aspect-ratio: 3 / 4; background: #f7f7f7; overflow: hidden; }
.p-img img { width: 100%; height: 100%; object-fit: cover; }
.p-off { position: absolute; top: 8px; left: 8px; }
.p-name {
  font-size: 13px; line-height: 1.5; margin: 10px 8px 6px; height: 39px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.p-price { margin: 0 8px; display: flex; align-items: baseline; gap: 8px; }
.p-price .price { font-size: 16px; }
.p-meta { margin: 6px 8px 0; display: flex; justify-content: space-between; font-size: 12px; color: var(--muted); }
.p-star { color: #f7ba2a; font-weight: 600; }
</style>

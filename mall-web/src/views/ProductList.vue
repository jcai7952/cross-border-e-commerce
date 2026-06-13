<template>
  <div class="container plist">
    <!-- 面包屑 / 搜索标题 -->
    <div class="plist-head">
      <el-breadcrumb v-if="!isSearch" separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">{{ $t('nav.home') }}</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(c, i) in catPath" :key="c.id" :to="i < catPath.length - 1 ? { path: `/category/${c.id}` } : undefined">
          {{ c.name }}
        </el-breadcrumb-item>
        <el-breadcrumb-item v-if="!catPath.length">{{ $t('common.all') }}</el-breadcrumb-item>
      </el-breadcrumb>
      <h2 v-else class="search-title">{{ txt.search }}: <em>{{ $route.query.keyword }}</em></h2>
    </div>

    <!-- 排序 tab -->
    <div class="sort-bar">
      <button
        v-for="s in sorts"
        :key="s.key"
        class="sort-item"
        :class="{ active: sort === s.key }"
        @click="onSort(s.key)"
      >
        {{ s.label }}
      </button>
      <span class="sort-total" v-if="total">{{ total }} {{ txt.items }}</span>
    </div>

    <!-- 商品网格 -->
    <div v-if="list.length" class="p-grid" v-loading="loading">
      <router-link v-for="p in list" :key="p.id" :to="`/product/${p.id}`" class="p-card">
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
    <el-empty v-else-if="!loading" :description="$t('common.empty')" />

    <!-- 分页 -->
    <div class="pager" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const route = useRoute()
const app = useAppStore()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const sort = ref('new')
const loading = ref(false)
const cats = ref([])

const fmt = (p) => (p ? p.symbol + p.text : '')

const zh = computed(() => app.locale === 'zh-CN')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  zh.value
    ? { search: '搜索', items: '件商品', sortAll: '综合', sortSales: '销量', sortAsc: '价格 ↑', sortDesc: '价格 ↓' }
    : { search: 'Search', items: 'items', sortAll: 'Recommend', sortSales: 'Best Selling', sortAsc: 'Price ↑', sortDesc: 'Price ↓' }
)
const sorts = computed(() => [
  { key: 'new', label: txt.value.sortAll },
  { key: 'sales', label: txt.value.sortSales },
  { key: 'price_asc', label: txt.value.sortAsc },
  { key: 'price_desc', label: txt.value.sortDesc }
])

const isSearch = computed(() => route.path === '/search')

/** 在类目树中找 id 的路径（一级/二级），用于面包屑 */
const catPath = computed(() => {
  const id = Number(route.params.id)
  if (!id) return []
  for (const c of cats.value) {
    if (c.id === id) return [c]
    for (const ch of c.children || []) {
      if (ch.id === id) return [c, ch]
    }
  }
  return []
})

async function load() {
  loading.value = true
  try {
    const params = { sort: sort.value, pageNum: pageNum.value, pageSize }
    if (route.params.id) params.categoryId = route.params.id
    if (route.query.keyword) params.keyword = route.query.keyword
    const r = await client.get('/product/page', { params })
    list.value = r?.list || []
    total.value = r?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSort(key) {
  if (sort.value === key) return
  sort.value = key
  pageNum.value = 1
  load()
}
function onPage(p) {
  pageNum.value = p
  load()
  window.scrollTo({ top: 0 })
}

watch(
  () => [route.params.id, route.query.keyword],
  () => {
    if (route.path !== '/search' && !route.path.startsWith('/category')) return
    pageNum.value = 1
    sort.value = 'new'
    load()
  }
)

onMounted(() => {
  client.get('/category/tree').then((r) => { cats.value = r || [] }).catch(() => {})
  load()
})
</script>

<style scoped>
.plist { padding-top: 24px; }
.plist-head { margin-bottom: 18px; }
.search-title { font-size: 20px; font-weight: 800; letter-spacing: 1px; }
.search-title em { font-style: normal; color: var(--brand-accent); }

.sort-bar {
  display: flex; align-items: center; gap: 8px;
  border: 1px solid var(--line); background: #fafafa;
  padding: 8px 12px; margin-bottom: 24px;
}
.sort-item {
  border: 0; background: transparent; cursor: pointer;
  font-size: 13px; font-weight: 600; color: #555;
  padding: 6px 16px; transition: all 0.15s;
}
.sort-item:hover { color: var(--brand); }
.sort-item.active { background: var(--brand); color: #fff; }
.sort-total { margin-left: auto; font-size: 12px; color: var(--muted); }

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

.pager { display: flex; justify-content: center; margin-top: 36px; }
</style>

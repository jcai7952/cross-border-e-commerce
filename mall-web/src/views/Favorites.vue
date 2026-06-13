<template>
  <div class="container fav">
    <h2 class="fav-title">{{ $t('nav.favorites') }}</h2>

    <div v-if="list.length" class="p-grid" v-loading="loading">
      <router-link v-for="p in list" :key="p.id" :to="`/product/${p.id}`" class="p-card">
        <div class="p-img">
          <img :src="p.mainImage" :alt="p.name" loading="lazy" />
          <span v-if="p.discountPercent" class="flash-tag p-off">-{{ p.discountPercent }}%</span>
          <button class="p-remove" :title="$t('common.delete')" @click.prevent.stop="remove(p)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
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

    <el-empty v-else-if="!loading" :description="$t('common.empty')">
      <el-button type="primary" color="#111111" @click="$router.push('/')">{{ txt.goShopping }}</el-button>
    </el-empty>

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
import { ref, computed, onMounted } from 'vue'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const app = useAppStore()
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const loading = ref(false)

const fmt = (p) => (p ? p.symbol + p.text : '')
/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  app.locale === 'zh-CN' ? { goShopping: '去逛逛' } : { goShopping: 'Go Shopping' }
)

async function load() {
  loading.value = true
  try {
    const r = await client.get('/favorite/list', { params: { pageNum: pageNum.value, pageSize } })
    list.value = r?.list || []
    total.value = r?.total || 0
    // 删除最后一页最后一项后回退页码
    if (!list.value.length && pageNum.value > 1) {
      pageNum.value -= 1
      return load()
    }
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function remove(p) {
  await client.post(`/favorite/${p.id}`)
  load()
}

function onPage(p) {
  pageNum.value = p
  load()
  window.scrollTo({ top: 0 })
}

onMounted(load)
</script>

<style scoped>
.fav { padding-top: 28px; }
.fav-title {
  font-size: 22px; font-weight: 800; letter-spacing: 2px;
  text-transform: uppercase; margin-bottom: 24px;
}

/* 商品卡片（与首页一致） */
.p-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 28px 18px; }
.p-card { display: block; transition: all 0.2s; padding-bottom: 10px; }
.p-card:hover { box-shadow: 0 10px 28px rgba(0, 0, 0, 0.12); transform: translateY(-3px); }
.p-img { position: relative; aspect-ratio: 3 / 4; background: #f7f7f7; overflow: hidden; }
.p-img img { width: 100%; height: 100%; object-fit: cover; }
.p-off { position: absolute; top: 8px; left: 8px; }
.p-remove {
  position: absolute; top: 8px; right: 8px; width: 28px; height: 28px;
  border: 0; border-radius: 50%; background: rgba(255, 255, 255, 0.92); color: #333;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15); transition: all 0.15s;
}
.p-remove:hover { background: var(--brand); color: #fff; }
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

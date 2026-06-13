<template>
  <div>
    <!-- hero banner -->
    <section class="hero">
      <div class="hero-kicker">KINNSHOP · 2026</div>
      <h1 class="hero-title">NEW SEASON</h1>
      <div class="hero-sub">UP TO 30% OFF</div>
      <router-link class="hero-btn" to="/category">{{ $t('home.viewAll') }}</router-link>
    </section>

    <!-- 限时闪购 -->
    <section v-if="flash" class="container section">
      <div class="flash-head">
        <h2 class="section-title flash-title">{{ flash.title }}</h2>
        <span class="flash-cd">
          <span class="flash-tag">{{ $t('product.flashLabel') }}</span>
          {{ $t('home.endsIn') }} <b class="flash-time">{{ cd }}</b>
        </span>
      </div>
      <div class="flash-strip">
        <router-link v-for="p in flash.items" :key="p.id" :to="`/product/${p.id}`" class="p-card flash-card">
          <div class="p-img">
            <img :src="p.mainImage" :alt="p.name" loading="lazy" />
            <span v-if="p.discountPercent" class="flash-tag p-off">-{{ p.discountPercent }}%</span>
          </div>
          <div class="p-name">{{ p.name }}</div>
          <div class="p-price">
            <span class="price">{{ fmt(p.flashPrice || p.price) }}</span>
            <span v-if="p.flashPrice" class="price-origin">{{ fmt(p.price) }}</span>
          </div>
        </router-link>
      </div>
    </section>

    <!-- 类目宫格 -->
    <section v-if="cats.length" class="container section">
      <h2 class="section-title">{{ $t('nav.category') }}</h2>
      <div class="cat-grid">
        <router-link v-for="c in cats" :key="c.id" :to="`/category/${c.id}`" class="cat-card">
          <span class="cat-avatar">{{ c.name.slice(0, 1) }}</span>
          <span class="cat-name">{{ c.name }}</span>
        </router-link>
      </div>
    </section>

    <!-- Best Sellers -->
    <section class="container section">
      <h2 class="section-title">{{ $t('home.bestSellers') }}</h2>
      <div class="p-grid">
        <router-link v-for="p in best" :key="p.id" :to="`/product/${p.id}`" class="p-card">
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

    <!-- New Arrivals -->
    <section class="container section">
      <h2 class="section-title">{{ $t('home.newArrivals') }}</h2>
      <div class="p-grid">
        <router-link v-for="p in news" :key="p.id" :to="`/product/${p.id}`" class="p-card">
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
      <div class="more-row">
        <router-link to="/category" class="more-link">{{ $t('home.viewAll') }} →</router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import client from '../api/client'

const flash = ref(null)
const cd = ref('--:--:--')
const cats = ref([])
const best = ref([])
const news = ref([])
let timer = null

const fmt = (p) => (p ? p.symbol + p.text : '')

function tick() {
  if (!flash.value?.endTime) return
  const diff = Math.max(0, new Date(flash.value.endTime).getTime() - Date.now())
  const s = Math.floor(diff / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  cd.value = [h, m, sec].map((v) => String(v).padStart(2, '0')).join(':')
  if (diff <= 0 && timer) { clearInterval(timer); timer = null }
}

onMounted(() => {
  client.get('/flash-sale/current').then((r) => {
    if (r && r.items && r.items.length) {
      flash.value = r
      tick()
      timer = setInterval(tick, 1000)
    }
  }).catch(() => {})
  client.get('/category/tree').then((r) => { cats.value = (r || []).slice(0, 7) }).catch(() => {})
  client.get('/product/best-sellers', { params: { limit: 10 } }).then((r) => { best.value = r || [] }).catch(() => {})
  client.get('/product/page', { params: { sort: 'new', pageNum: 1, pageSize: 8 } })
    .then((r) => { news.value = r?.list || [] }).catch(() => {})
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
/* hero */
.hero { background: var(--brand); color: #fff; text-align: center; padding: 80px 20px 88px; }
.hero-kicker { font-size: 13px; letter-spacing: 4px; color: #bbb; margin-bottom: 18px; }
.hero-title { font-size: 60px; font-weight: 900; letter-spacing: 8px; line-height: 1.1; }
.hero-sub { font-size: 20px; letter-spacing: 6px; margin: 16px 0 32px; color: #eee; }
.hero-btn {
  display: inline-block; background: #fff; color: var(--brand);
  padding: 13px 44px; font-size: 14px; font-weight: 800; letter-spacing: 2px;
  border: 1px solid #fff; transition: all 0.2s;
}
.hero-btn:hover { background: transparent; color: #fff; }

.section { margin-top: 56px; }
.section-title {
  text-align: center; font-size: 24px; font-weight: 800; letter-spacing: 2px;
  text-transform: uppercase; margin-bottom: 28px;
}

/* 闪购 */
.flash-head { display: flex; align-items: baseline; justify-content: center; gap: 18px; margin-bottom: 24px; }
.flash-title { margin-bottom: 0; }
.flash-cd { font-size: 13px; color: var(--muted); display: flex; align-items: center; gap: 8px; }
.flash-time { color: var(--price); font-size: 15px; font-variant-numeric: tabular-nums; }
.flash-strip { display: flex; gap: 16px; overflow-x: auto; padding-bottom: 8px; }
.flash-card { width: 210px; flex-shrink: 0; }

/* 类目宫格 */
.cat-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 16px; }
.cat-card {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  background: #fafafa; border-radius: 12px; padding: 24px 8px;
  transition: all 0.2s; border: 1px solid transparent;
}
.cat-card:hover { box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1); transform: translateY(-2px); border-color: var(--line); background: #fff; }
.cat-avatar {
  width: 56px; height: 56px; border-radius: 50%; background: var(--brand); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 800;
}
.cat-name { font-size: 13px; font-weight: 600; }

/* 商品卡片 */
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

.more-row { text-align: center; margin-top: 28px; }
.more-link {
  display: inline-block; border: 1px solid var(--brand); padding: 10px 36px;
  font-size: 13px; font-weight: 700; letter-spacing: 1px; transition: all 0.2s;
}
.more-link:hover { background: var(--brand); color: #fff; }
</style>

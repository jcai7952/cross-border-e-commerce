<template>
  <div class="mall">
    <!-- 顶部通告条 -->
    <div class="topbar">
      <div class="container topbar-inner">
        <span class="topbar-promo">FREE SHIPPING OVER $49</span>
        <div class="topbar-right">
          <el-dropdown trigger="click" @command="onLocale">
            <span class="topbar-link">{{ app.locale === 'zh-CN' ? '中文' : 'English' }} ▾</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh-CN">中文</el-dropdown-item>
                <el-dropdown-item command="en-US">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="onCurrency">
            <span class="topbar-link">{{ currencyLabel }} ▾</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="c in app.currencies" :key="c.code" :command="c.code">
                  {{ c.code }} {{ c.symbol }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <!-- 主 header -->
    <header class="header">
      <div class="container header-inner">
        <router-link to="/" class="logo">KINNSHOP</router-link>

        <div class="search-box">
          <input
            v-model="keyword"
            :placeholder="$t('nav.searchPlaceholder')"
            @keyup.enter="onSearch"
          />
          <button class="search-btn" @click="onSearch" :aria-label="$t('nav.searchPlaceholder')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </button>
        </div>

        <div class="header-icons">
          <router-link to="/favorites" class="icon-item" :title="$t('nav.favorites')">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
            <span class="icon-label">{{ $t('nav.favorites') }}</span>
          </router-link>

          <router-link to="/cart" class="icon-item" :title="$t('nav.cart')">
            <el-badge :value="app.cartCount" :hidden="!app.cartCount" :max="99">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" /><line x1="3" y1="6" x2="21" y2="6" /><path d="M16 10a4 4 0 0 1-8 0" />
              </svg>
            </el-badge>
            <span class="icon-label">{{ $t('nav.cart') }}</span>
          </router-link>

          <el-dropdown v-if="app.loggedIn" trigger="click" @command="onUserCmd">
            <span class="icon-item">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
              </svg>
              <span class="icon-label">{{ app.user?.nickname || $t('nav.account') }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="orders">{{ $t('nav.orders') }}</el-dropdown-item>
                <el-dropdown-item command="account">{{ $t('nav.account') }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>{{ $t('nav.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <router-link v-else to="/login" class="icon-item" :title="$t('nav.login')">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
            </svg>
            <span class="icon-label">{{ $t('nav.login') }}</span>
          </router-link>
        </div>
      </div>
    </header>

    <!-- 类目导航 -->
    <nav class="catnav" v-if="cats.length">
      <div class="container">
        <ul class="catnav-list">
          <li v-for="c in cats" :key="c.id" class="catnav-item">
            <router-link :to="`/category/${c.id}`" class="catnav-link">{{ c.name }}</router-link>
            <div class="catnav-drop" v-if="c.children && c.children.length">
              <router-link v-for="ch in c.children" :key="ch.id" :to="`/category/${ch.id}`" class="catnav-sub">
                {{ ch.name }}
              </router-link>
            </div>
          </li>
        </ul>
      </div>
    </nav>

    <main class="mall-main"><router-view /></main>

    <!-- footer -->
    <footer class="footer">
      <div class="container footer-cols">
        <div class="footer-col">
          <h4>KINNSHOP</h4>
          <p>Cross-border fashion, beauty &amp; lifestyle.</p>
          <p>Quality picks shipped worldwide.</p>
        </div>
        <div class="footer-col">
          <h4>HELP &amp; SUPPORT</h4>
          <router-link to="/orders">{{ $t('nav.orders') }}</router-link>
          <router-link to="/favorites">{{ $t('nav.favorites') }}</router-link>
          <router-link to="/account">{{ $t('nav.account') }}</router-link>
        </div>
        <div class="footer-col">
          <h4>PAYMENT &amp; SHIPPING</h4>
          <p>Visa · MasterCard · PayPal</p>
          <p>{{ $t('product.bonded') }} / {{ $t('product.direct') }}</p>
        </div>
      </div>
      <div class="footer-bottom">© 2026 KinnShop</div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const app = useAppStore()
const router = useRouter()
const keyword = ref('')
const cats = ref([])

const currencyLabel = computed(() => {
  const c = app.currencies.find((x) => x.code === app.currency)
  return c ? `${c.code} ${c.symbol}` : app.currency
})

function onLocale(cmd) {
  if (cmd === app.locale) return
  app.switchLocale(cmd)
  location.reload()
}
function onCurrency(code) {
  if (code === app.currency) return
  app.switchCurrency(code)
  location.reload()
}
function onSearch() {
  const k = keyword.value.trim()
  if (k) router.push({ path: '/search', query: { keyword: k } })
}
async function onUserCmd(cmd) {
  if (cmd === 'orders') return router.push('/orders')
  if (cmd === 'account') return router.push('/account')
  if (cmd === 'logout') {
    try { await client.post('/auth/logout') } catch { /* ignore */ }
    app.logout()
    router.push('/')
  }
}

onMounted(async () => {
  try { app.currencies = (await client.get('/currency/list')) || [] } catch { /* ignore */ }
  try { cats.value = (await client.get('/category/tree')) || [] } catch { /* ignore */ }
  if (app.loggedIn) {
    try {
      const r = await client.get('/cart/count')
      app.cartCount = r?.count || 0
    } catch { /* ignore */ }
  }
})
</script>

<style scoped>
.mall { display: flex; flex-direction: column; min-height: 100vh; }

/* 顶部通告条 */
.topbar { background: var(--brand); color: #fff; font-size: 12px; }
.topbar-inner { display: flex; align-items: center; justify-content: space-between; height: 32px; }
.topbar-promo { letter-spacing: 1px; font-weight: 600; }
.topbar-right { display: flex; align-items: center; gap: 20px; }
.topbar-link { color: #fff; font-size: 12px; cursor: pointer; outline: none; }

/* 主 header */
.header { border-bottom: 1px solid var(--line); }
.header-inner { display: flex; align-items: center; gap: 32px; padding: 18px 0; }
.logo { font-size: 26px; font-weight: 900; letter-spacing: 3px; white-space: nowrap; }
.search-box {
  flex: 1; max-width: 480px; display: flex; align-items: center;
  border: 2px solid var(--brand); height: 38px;
}
.search-box input { flex: 1; border: 0; outline: none; padding: 0 12px; font-size: 13px; height: 100%; }
.search-btn {
  width: 44px; height: 100%; border: 0; background: var(--brand); color: #fff;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.header-icons { display: flex; align-items: center; gap: 26px; margin-left: auto; }
.icon-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  cursor: pointer; color: var(--brand); outline: none;
}
.icon-item:hover { color: var(--brand-accent); }
.icon-label { font-size: 11px; color: var(--muted); max-width: 72px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 类目导航 */
.catnav { border-bottom: 1px solid var(--line); }
.catnav-list { display: flex; gap: 34px; list-style: none; }
.catnav-item { position: relative; }
.catnav-link { display: block; padding: 12px 0; font-size: 14px; font-weight: 700; }
.catnav-item:hover .catnav-link { color: var(--brand-accent); }
.catnav-drop {
  display: none; position: absolute; top: 100%; left: 50%; transform: translateX(-50%);
  background: #fff; border: 1px solid var(--line); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  min-width: 150px; padding: 6px 0; z-index: 100;
}
.catnav-item:hover .catnav-drop { display: block; }
.catnav-sub { display: block; padding: 9px 18px; font-size: 13px; white-space: nowrap; }
.catnav-sub:hover { background: #f7f7f7; color: var(--brand-accent); }

.mall-main { flex: 1; }

/* footer */
.footer { background: #fafafa; border-top: 1px solid var(--line); margin-top: 60px; }
.footer-cols { display: grid; grid-template-columns: repeat(3, 1fr); gap: 40px; padding: 40px 20px; }
.footer-col h4 { font-size: 13px; font-weight: 800; letter-spacing: 1px; margin-bottom: 14px; }
.footer-col p, .footer-col a { display: block; font-size: 13px; color: var(--muted); line-height: 2; }
.footer-col a:hover { color: var(--brand); }
.footer-bottom {
  border-top: 1px solid var(--line); text-align: center;
  padding: 16px 0; font-size: 12px; color: var(--muted);
}
</style>

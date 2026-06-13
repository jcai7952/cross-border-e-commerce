<template>
  <el-container style="height: 100%">
    <el-aside width="220px" class="aside">
      <div class="logo">KinnShop<span>运营后台</span></div>
      <el-menu :default-active="$route.path" router background-color="#1f2937" text-color="#cbd5e1" active-text-color="#ffffff">
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-sub-menu index="goods">
          <template #title>商品</template>
          <el-menu-item index="/product">商品管理</el-menu-item>
          <el-menu-item index="/category">类目与税率</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="trade">
          <template #title>交易</template>
          <el-menu-item index="/order">订单管理</el-menu-item>
          <el-menu-item index="/payment">支付流水</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="marketing">
          <template #title>营销</template>
          <el-menu-item index="/coupon">优惠券</el-menu-item>
          <el-menu-item index="/flash-sale">限时闪购</el-menu-item>
          <el-menu-item index="/review">评论审核</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system">
          <template #title>配置</template>
          <el-menu-item index="/currency">币种与汇率</el-menu-item>
          <el-menu-item index="/shipping">运费模板</el-menu-item>
          <el-menu-item index="/user">用户管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ $route.meta.title || '' }}</span>
        <el-dropdown @command="onCommand">
          <span class="admin-name">{{ adminName }} ▾</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'

const router = useRouter()
const adminName = computed(() => {
  try { return JSON.parse(localStorage.getItem('admin_info') || '{}').nickname || 'admin' }
  catch { return 'admin' }
})

async function onCommand(cmd) {
  if (cmd === 'logout') {
    try { await client.post('/admin/auth/logout') } catch { /* token 失效也照常清理 */ }
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    router.push('/login')
  }
}
</script>

<style scoped>
.aside { background: #1f2937; }
.logo {
  height: 60px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 18px; letter-spacing: .5px;
}
.logo span { font-size: 12px; font-weight: 400; color: #94a3b8; margin-left: 6px; }
.el-menu { border-right: none; }
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.title { font-size: 16px; font-weight: 600; }
.admin-name { cursor: pointer; color: #374151; }
</style>

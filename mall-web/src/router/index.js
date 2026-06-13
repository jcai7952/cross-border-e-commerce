import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../layout/MallLayout.vue'),
    children: [
      { path: '', component: () => import('../views/Home.vue') },
      { path: 'category/:id?', component: () => import('../views/ProductList.vue') },
      { path: 'search', component: () => import('../views/ProductList.vue') },
      { path: 'product/:id', component: () => import('../views/ProductDetail.vue') },
      { path: 'cart', component: () => import('../views/Cart.vue'), meta: { auth: true } },
      { path: 'checkout', component: () => import('../views/Checkout.vue'), meta: { auth: true } },
      { path: 'pay/:orderNo', component: () => import('../views/Pay.vue'), meta: { auth: true } },
      { path: 'pay/result', component: () => import('../views/PayResult.vue') },
      { path: 'orders', component: () => import('../views/Orders.vue'), meta: { auth: true } },
      { path: 'order/:orderNo', component: () => import('../views/OrderDetail.vue'), meta: { auth: true } },
      { path: 'favorites', component: () => import('../views/Favorites.vue'), meta: { auth: true } },
      { path: 'account', component: () => import('../views/Account.vue'), meta: { auth: true } }
    ]
  },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('user_token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

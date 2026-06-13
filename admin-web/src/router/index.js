import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '仪表盘' } },
      { path: 'product', component: () => import('../views/product/ProductList.vue'), meta: { title: '商品管理' } },
      { path: 'product/edit/:id?', component: () => import('../views/product/ProductEdit.vue'), meta: { title: '商品编辑' } },
      { path: 'category', component: () => import('../views/product/CategoryManage.vue'), meta: { title: '类目与税率' } },
      { path: 'order', component: () => import('../views/order/OrderList.vue'), meta: { title: '订单管理' } },
      { path: 'order/:orderNo', component: () => import('../views/order/OrderDetail.vue'), meta: { title: '订单详情' } },
      { path: 'coupon', component: () => import('../views/marketing/CouponManage.vue'), meta: { title: '优惠券' } },
      { path: 'flash-sale', component: () => import('../views/marketing/FlashSaleManage.vue'), meta: { title: '限时闪购' } },
      { path: 'review', component: () => import('../views/marketing/ReviewAudit.vue'), meta: { title: '评论审核' } },
      { path: 'currency', component: () => import('../views/system/CurrencyManage.vue'), meta: { title: '币种与汇率' } },
      { path: 'shipping', component: () => import('../views/system/ShippingManage.vue'), meta: { title: '运费模板' } },
      { path: 'payment', component: () => import('../views/system/PaymentFlow.vue'), meta: { title: '支付流水' } },
      { path: 'user', component: () => import('../views/system/UserManage.vue'), meta: { title: '用户管理' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (!to.meta.public && !localStorage.getItem('admin_token')) {
    return '/login'
  }
})

export default router

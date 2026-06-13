export default {
  nav: {
    home: '首页', category: '分类', cart: '购物车', orders: '我的订单',
    favorites: '收藏夹', account: '个人中心', login: '登录', register: '注册', logout: '退出登录',
    searchPlaceholder: '搜索商品…'
  },
  common: {
    confirm: '确认', cancel: '取消', save: '保存', delete: '删除', edit: '编辑',
    loading: '加载中…', empty: '暂无数据', more: '查看更多', all: '全部',
    submit: '提交', back: '返回'
  },
  home: { flashSale: '限时闪购', endsIn: '距结束', bestSellers: '热销榜', newArrivals: '新品上架', viewAll: '查看全部' },
  product: {
    color: '颜色', size: '尺码', qty: '数量', stock: '库存', soldOut: '已售罄',
    addToCart: '加入购物车', buyNow: '立即购买', favorite: '收藏', unfavorite: '取消收藏',
    sizeChart: '尺码表', reviews: '评论', sales: '已售 {n} 件', detail: '商品详情',
    bonded: '保税仓发货', direct: '海外直邮', flashLabel: '闪购'
  },
  cart: {
    title: '购物车', selectAll: '全选', subtotal: '合计', checkout: '去结算',
    empty: '购物车还是空的', invalid: '已失效', deleteChecked: '删除选中'
  },
  checkout: {
    title: '确认订单', address: '收货地址', addAddress: '新增地址', identity: '清关实名信息',
    identityTip: '中国大陆订单海关申报需要订购人身份证信息', addIdentity: '添加实名信息',
    coupon: '优惠券', noCoupon: '不使用', goods: '商品金额', discount: '优惠',
    shipping: '运费', tax: '税费', total: '应付总额', placeOrder: '提交订单',
    estDelivery: '预计 {min}-{max} 天送达', payCurrency: '支付币种'
  },
  pay: {
    title: '收银台', amount: '支付金额', deadline: '支付剩余时间', choose: '选择支付方式',
    simulator: '模拟支付（演示）', stripe: '银行卡支付 (Stripe)', paypal: 'PayPal',
    goPay: '立即支付', success: '支付成功', failed: '支付失败', processing: '支付确认中…',
    backToOrders: '查看订单'
  },
  order: {
    title: '我的订单', all: '全部', waitPay: '待付款', paid: '待发货', shipped: '待收货',
    finished: '已完成', closed: '已关闭', pay: '去支付', cancel: '取消订单',
    confirm: '确认收货', track: '查看物流', review: '评价', detail: '订单详情',
    countdown: '剩余 {t} 自动关闭'
  },
  logistics: { title: '物流轨迹', carrier: '承运商', shipmentNo: '运单号' },
  account: {
    profile: '个人资料', address: '收货地址', identity: '实名信息', coupons: '我的优惠券',
    nickname: '昵称', email: '邮箱', language: '语言', currency: '币种', password: '修改密码'
  },
  auth: {
    login: '登录', register: '注册', email: '邮箱', password: '密码', code: '验证码',
    sendCode: '发送验证码', resend: '{s}s 后重发', nickname: '昵称（选填）',
    toRegister: '没有账号？去注册', toLogin: '已有账号？去登录', forgot: '忘记密码',
    reset: '重置密码', newPassword: '新密码'
  }
}

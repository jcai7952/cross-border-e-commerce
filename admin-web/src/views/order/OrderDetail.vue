<template>
  <div v-loading="loading">
    <template v-if="order">
      <!-- 头部 -->
      <div class="page-card head-card">
        <div class="head-left">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="STATUS_TAG[order.status] || 'info'" :effect="order.status === 'CLOSED' ? 'plain' : 'light'">
            {{ STATUS_TEXT[order.status] || order.status }}
          </el-tag>
          <el-tag size="small" :type="order.tradeMode === 'BONDED' ? 'success' : order.tradeMode === 'DIRECT' ? 'warning' : 'info'">
            {{ TRADE_TEXT[order.tradeMode] || order.tradeMode }}
          </el-tag>
        </div>
        <div>
          <el-button @click="$router.push('/order')">返回列表</el-button>
          <el-button v-if="order.status === 'PAID'" type="primary" @click="ship">发货</el-button>
        </div>
      </div>

      <!-- 金额卡 -->
      <div class="page-card section-card">
        <div class="section-title">金额信息</div>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="商品金额">{{ usd(order.goodsAmountCents) }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">-{{ usd(order.discountAmountCents) }}</el-descriptions-item>
          <el-descriptions-item label="运费">{{ usd(order.shippingAmountCents) }}</el-descriptions-item>
          <el-descriptions-item label="税费">{{ usd(order.taxAmountCents) }}</el-descriptions-item>
          <el-descriptions-item label="应付金额(USD)">
            <span class="total-amount">{{ usd(order.totalAmountCents) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付币种">{{ order.payCurrency || '-' }}</el-descriptions-item>
          <el-descriptions-item label="锁定汇率">{{ order.exchangeRate ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付币金额">
            <span class="total-amount">{{ payMoney(order.payCurrency, order.payAmountCents) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 收件信息 + 清关实名 -->
      <el-row :gutter="16">
        <el-col :span="identityVisible ? 14 : 24">
          <div class="page-card section-card">
            <div class="section-title">收件信息</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="收件人">{{ receiver.receiverName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="电话">{{ receiver.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="国家/地区">{{ receiver.countryCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="州/省 - 城市">{{ receiver.state || '-' }} / {{ receiver.city || '-' }}</el-descriptions-item>
              <el-descriptions-item label="地址" :span="2">
                {{ receiver.addressLine1 || '-' }}{{ receiver.addressLine2 ? ' ' + receiver.addressLine2 : '' }}
              </el-descriptions-item>
              <el-descriptions-item label="邮编">{{ receiver.postcode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="买家备注">{{ order.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
        <el-col v-if="identityVisible" :span="10">
          <div class="page-card section-card">
            <div class="section-title">清关实名</div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="真实姓名">{{ order.identity.realName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">{{ order.identity.idCardMask || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <!-- 商品明细 -->
      <div class="page-card section-card">
        <div class="section-title">商品明细</div>
        <el-table :data="order.items || []" stripe>
          <el-table-column label="图片" width="80">
            <template #default="{ row }">
              <el-image :src="row.image" fit="cover" style="width: 48px; height: 48px; border-radius: 4px" />
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品名称" min-width="200" />
          <el-table-column prop="skuText" label="规格" min-width="140" />
          <el-table-column label="单价" width="110">
            <template #default="{ row }">{{ usd(row.priceCents) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="110">
            <template #default="{ row }">{{ usd(row.totalCents) }}</template>
          </el-table-column>
          <template #empty><el-empty description="暂无商品明细" /></template>
        </el-table>
      </div>

      <el-row :gutter="16">
        <!-- 状态时间线 -->
        <el-col :span="shipmentVisible ? 12 : 24">
          <div class="page-card section-card">
            <div class="section-title">状态时间线</div>
            <el-timeline v-if="order.logs && order.logs.length" style="padding-left: 4px">
              <el-timeline-item
                v-for="(log, i) in order.logs"
                :key="i"
                :timestamp="fmtTime(log.createTime)"
                :type="i === order.logs.length - 1 ? 'primary' : ''"
                placement="top"
              >
                <div class="log-title">
                  {{ STATUS_TEXT[log.fromStatus] || log.fromStatus }} → {{ STATUS_TEXT[log.toStatus] || log.toStatus }}
                </div>
                <div class="log-meta">操作人：{{ log.operator || '-' }}<template v-if="log.remark">｜备注：{{ log.remark }}</template></div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无状态记录" />
          </div>
        </el-col>

        <!-- 物流卡 -->
        <el-col v-if="shipmentVisible" :span="12">
          <div class="page-card section-card">
            <div class="section-title">
              物流轨迹
              <span v-if="shipment" class="ship-no">运单号：{{ shipment.shipmentNo }}（{{ shipment.carrier }}）</span>
            </div>
            <el-timeline v-if="shipment && shipment.tracks && shipment.tracks.length" style="padding-left: 4px">
              <el-timeline-item
                v-for="(t, i) in shipment.tracks"
                :key="i"
                :timestamp="fmtTime(t.trackTime)"
                :type="i === 0 ? 'success' : ''"
                placement="top"
              >
                <div class="log-title">{{ t.nodeZh }}</div>
                <div class="log-meta">{{ t.location || '-' }}<template v-if="t.remark">｜{{ t.remark }}</template></div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无物流轨迹" />
          </div>
        </el-col>
      </el-row>
    </template>

    <div v-else-if="!loading" class="page-card">
      <el-empty description="订单不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../../api/client'

const STATUS_TEXT = {
  CREATE: '创建',
  WAIT_PAY: '待付款',
  PAID: '待发货',
  SHIPPED: '已发货',
  FINISHED: '已完成',
  CLOSED: '已关闭'
}
const STATUS_TAG = {
  WAIT_PAY: 'warning',
  PAID: 'primary',
  SHIPPED: 'info',
  FINISHED: 'success',
  CLOSED: 'info'
}
const TRADE_TEXT = { BONDED: '保税仓', DIRECT: '海外直邮', MIXED: '混合' }

const route = useRoute()
const orderNo = route.params.orderNo

const order = ref(null)
const shipment = ref(null)
const loading = ref(false)

const receiver = computed(() => order.value?.receiver || {})
const identityVisible = computed(() => !!order.value?.identity)
const shipmentVisible = computed(() => ['SHIPPED', 'FINISHED'].includes(order.value?.status))

function usd(cents) {
  if (cents == null) return '-'
  return '$' + (Number(cents) / 100).toFixed(2)
}

function payMoney(currency, minor) {
  if (minor == null) return '-'
  const digits = currency === 'JPY' ? 0 : 2
  return `${currency} ${(Number(minor) / Math.pow(10, digits)).toFixed(digits)}`
}

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function loadShipment() {
  try {
    const page = await client.get('/admin/logistics/shipment/page', { params: { orderNo, pageNum: 1, pageSize: 1 } })
    const first = page.list && page.list[0]
    if (first) {
      shipment.value = await client.get(`/admin/logistics/shipment/${first.shipmentNo}`)
    }
  } catch {
    // 物流信息加载失败不阻塞订单详情展示
  }
}

async function load() {
  loading.value = true
  try {
    order.value = await client.get(`/admin/order/${orderNo}`)
    if (shipmentVisible.value) await loadShipment()
  } finally {
    loading.value = false
  }
}

async function ship() {
  try {
    await ElMessageBox.confirm(`确认对订单 ${orderNo} 执行发货？`, '发货确认', {
      confirmButtonText: '确认发货',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const data = await client.post(`/admin/order/${orderNo}/ship`)
  ElMessage.success(`发货成功，运单号：${data?.shipmentNo || '-'}`)
  load()
}

onMounted(load)
</script>

<style scoped>
.head-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.head-left { display: flex; align-items: center; gap: 12px; }
.order-no { font-size: 16px; font-weight: 600; color: #303133; }
.section-card { margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.total-amount { color: #f56c6c; font-weight: 600; }
.log-title { font-size: 14px; color: #303133; font-weight: 500; }
.log-meta { font-size: 12px; color: #909399; margin-top: 4px; }
.ship-no { font-size: 12px; color: #909399; font-weight: 400; margin-left: 8px; }
</style>

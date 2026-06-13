<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 150px" @change="search">
        <el-option v-for="(text, key) in STATUS_TEXT" :key="key" :label="text" :value="key" />
      </el-select>
      <el-input
        v-model="query.orderNo"
        placeholder="订单号"
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="订单号" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/order/${row.orderNo}`)">{{ row.orderNo }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column label="商品摘要" min-width="220">
        <template #default="{ row }">{{ itemSummary(row.items) }}</template>
      </el-table-column>
      <el-table-column label="订单金额" width="110">
        <template #default="{ row }">{{ usd(row.totalAmountCents) }}</template>
      </el-table-column>
      <el-table-column label="支付币金额" width="130">
        <template #default="{ row }">{{ payMoney(row.payCurrency, row.payAmountCents) }}</template>
      </el-table-column>
      <el-table-column label="贸易模式" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.tradeMode === 'BONDED' ? 'success' : row.tradeMode === 'DIRECT' ? 'warning' : 'info'">
            {{ TRADE_TEXT[row.tradeMode] || row.tradeMode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="STATUS_TAG[row.status] || 'info'" :effect="row.status === 'CLOSED' ? 'plain' : 'light'">
            {{ STATUS_TEXT[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="160">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="$router.push(`/order/${row.orderNo}`)">详情</el-button>
          <el-button v-if="row.status === 'PAID'" type="success" link @click="ship(row)">发货</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无订单数据" /></template>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../../api/client'

const STATUS_TEXT = {
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

const query = reactive({ status: '', orderNo: '', pageNum: 1, pageSize: 20 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

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

function itemSummary(items) {
  if (!items || !items.length) return '-'
  const first = items[0]
  let text = `${first.productName} ×${first.quantity}`
  if (items.length > 1) {
    const totalQty = items.reduce((s, it) => s + (it.quantity || 0), 0)
    text += ` 等${totalQty}件`
  }
  return text
}

async function load() {
  loading.value = true
  try {
    const data = await client.get('/admin/order/page', {
      params: {
        status: query.status || undefined,
        orderNo: query.orderNo || undefined,
        pageNum: query.pageNum,
        pageSize: query.pageSize
      }
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.status = ''
  query.orderNo = ''
  search()
}

async function ship(row) {
  try {
    await ElMessageBox.confirm(`确认对订单 ${row.orderNo} 执行发货？`, '发货确认', {
      confirmButtonText: '确认发货',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const data = await client.post(`/admin/order/${row.orderNo}/ship`)
  ElMessage.success(`发货成功，运单号：${data?.shipmentNo || '-'}`)
  load()
}

onMounted(load)
</script>

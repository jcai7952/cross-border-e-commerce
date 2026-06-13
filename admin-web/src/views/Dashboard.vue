<template>
  <div>
    <!-- 统计卡 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">今日销售额</div>
          <div class="stat-value">{{ usd(todayStat.sales_cents) }}</div>
          <div class="stat-extra">单位 USD</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">今日订单</div>
          <div class="stat-value">{{ todayStat.orders ?? 0 }}</div>
          <div class="stat-extra">今日新增订单数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">累计销售额</div>
          <div class="stat-value">{{ usd(totalStat.sales_cents) }}</div>
          <div class="stat-extra">累计订单 {{ totalStat.orders ?? 0 }} 笔</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">
            用户总数
            <el-tag v-if="userStat.todayNew > 0" type="success" size="small" effect="light">
              今日 +{{ userStat.todayNew }}
            </el-tag>
          </div>
          <div class="stat-value">{{ userStat.totalUsers ?? 0 }}</div>
          <div class="stat-extra">注册用户</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16">
      <el-col :span="14">
        <div class="page-card chart-card">
          <div class="chart-title">近 7 天销售额与订单量</div>
          <div ref="trendRef" class="chart-box" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card chart-card">
          <div class="chart-title">订单状态分布</div>
          <div ref="statusRef" class="chart-box" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import client from '../api/client'

const STATUS_TEXT = {
  WAIT_PAY: '待付款',
  PAID: '待发货',
  SHIPPED: '已发货',
  FINISHED: '已完成',
  CLOSED: '已关闭'
}

const todayStat = ref({})
const totalStat = ref({})
const userStat = ref({})

const trendRef = ref(null)
const statusRef = ref(null)
let trendChart = null
let statusChart = null

function usd(cents) {
  if (cents == null) return '$0.00'
  return '$' + (Number(cents) / 100).toFixed(2)
}

function lastNDays(n) {
  const days = []
  const now = new Date()
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i)
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    days.push(`${d.getFullYear()}-${m}-${dd}`)
  }
  return days
}

function renderTrend(daily) {
  const days = lastNDays(7)
  const map = {}
  ;(daily || []).forEach((d) => { map[d.day] = d })
  const sales = days.map((d) => map[d] ? Number(map[d].sales_cents || 0) / 100 : 0)
  const orders = days.map((d) => map[d] ? Number(map[d].orders || 0) : 0)

  trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => v,
      formatter(params) {
        let html = params[0].axisValue
        params.forEach((p) => {
          const val = p.seriesName === '销售额' ? '$' + Number(p.value).toFixed(2) : p.value + ' 单'
          html += `<br/>${p.marker}${p.seriesName}：${val}`
        })
        return html
      }
    },
    legend: { data: ['销售额', '订单量'], top: 0 },
    grid: { left: 50, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: days.map((d) => d.slice(5)) },
    yAxis: [
      { type: 'value', name: '销售额($)', axisLabel: { formatter: '${value}' } },
      { type: 'value', name: '订单量', minInterval: 1 }
    ],
    series: [
      {
        name: '销售额',
        type: 'bar',
        barMaxWidth: 28,
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        data: sales
      },
      {
        name: '订单量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        itemStyle: { color: '#67c23a' },
        data: orders
      }
    ]
  })
}

function renderStatus(statusCounts) {
  const data = (statusCounts || []).map((s) => ({
    name: STATUS_TEXT[s.status] || s.status,
    value: Number(s.cnt || 0)
  }))
  statusChart = echarts.init(statusRef.value)
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 单 ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    color: ['#e6a23c', '#409eff', '#909399', '#67c23a', '#c0c4cc'],
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c} 单' },
        data: data.length ? data : [{ name: '暂无订单', value: 0 }]
      }
    ]
  })
}

function handleResize() {
  trendChart && trendChart.resize()
  statusChart && statusChart.resize()
}

async function load() {
  const [orderStats, userStats] = await Promise.all([
    client.get('/admin/order/stats', { params: { days: 7 } }),
    client.get('/admin/user/stats')
  ])
  todayStat.value = orderStats.today || {}
  totalStat.value = orderStats.total || {}
  userStat.value = userStats || {}
  renderTrend(orderStats.daily)
  renderStatus(orderStats.statusCounts)
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) { trendChart.dispose(); trendChart = null }
  if (statusChart) { statusChart.dispose(); statusChart = null }
})
</script>

<style scoped>
.stat-row { margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 10px 0 6px;
}
.stat-extra { font-size: 12px; color: #c0c4cc; }
.chart-card { min-height: 380px; }
.chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.chart-box { width: 100%; height: 320px; }
</style>

<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 支付单 -->
      <el-tab-pane label="支付单" name="pay">
        <div class="toolbar">
          <el-input
            v-model="payQuery.orderNo"
            placeholder="订单号"
            clearable
            style="width: 220px"
            @keyup.enter="searchPay"
            @clear="searchPay"
          />
          <el-select v-model="payQuery.channel" placeholder="支付渠道" clearable style="width: 150px" @change="searchPay">
            <el-option v-for="(text, key) in CHANNEL_TEXT" :key="key" :label="text" :value="key" />
          </el-select>
          <el-select v-model="payQuery.status" placeholder="支付状态" clearable style="width: 150px" @change="searchPay">
            <el-option v-for="(text, key) in PAY_STATUS_TEXT" :key="key" :label="text" :value="key" />
          </el-select>
          <el-button type="primary" @click="searchPay">查询</el-button>
          <el-button @click="resetPay">重置</el-button>
        </div>

        <el-table v-loading="payLoading" :data="payRows" stripe>
          <el-table-column prop="payNo" label="支付单号" min-width="200" />
          <el-table-column prop="orderNo" label="订单号" min-width="200" />
          <el-table-column label="渠道" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="CHANNEL_TAG[row.channel] || 'info'">{{ CHANNEL_TEXT[row.channel] || row.channel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">{{ money(row.currency, row.amountCents) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="PAY_STATUS_TAG[row.status] || 'info'">{{ PAY_STATUS_TEXT[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="channelTradeNo" label="渠道单号" min-width="210">
            <template #default="{ row }">{{ row.channelTradeNo || '-' }}</template>
          </el-table-column>
          <el-table-column label="支付时间" width="160">
            <template #default="{ row }">{{ fmtTime(row.paidAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'SUCCESS'" type="danger" link @click="openRefund(row)">退款</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无支付流水" /></template>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="payQuery.pageNum"
            :page-size="payQuery.pageSize"
            :total="payTotal"
            layout="total, prev, pager, next"
            background
            @current-change="loadPay"
          />
        </div>
      </el-tab-pane>

      <!-- 退款单 -->
      <el-tab-pane label="退款单" name="refund">
        <el-table v-loading="refundLoading" :data="refundRows" stripe>
          <el-table-column prop="refundNo" label="退款单号" min-width="200" />
          <el-table-column prop="payNo" label="支付单号" min-width="200" />
          <el-table-column prop="orderNo" label="订单号" min-width="200" />
          <el-table-column label="退款金额" width="120">
            <template #default="{ row }">{{ money(row.currency, row.amountCents) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="REFUND_STATUS_TAG[row.status] || 'info'">
                {{ REFUND_STATUS_TEXT[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因" min-width="140">
            <template #default="{ row }">{{ row.reason || '-' }}</template>
          </el-table-column>
          <el-table-column label="退款时间" width="160">
            <template #default="{ row }">{{ fmtTime(row.refundedAt || row.createTime) }}</template>
          </el-table-column>
          <template #empty><el-empty description="暂无退款单" /></template>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="refundQuery.pageNum"
            :page-size="refundQuery.pageSize"
            :total="refundTotal"
            layout="total, prev, pager, next"
            background
            @current-change="loadRefund"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 退款 dialog -->
    <el-dialog v-model="refundDialogVisible" title="发起退款" width="460px" destroy-on-close>
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="100px">
        <el-form-item label="支付单号">
          <span>{{ refundTarget?.payNo }}</span>
        </el-form-item>
        <el-form-item label="支付金额">
          <span>{{ refundTarget ? money(refundTarget.currency, refundTarget.amountCents) : '-' }}</span>
        </el-form-item>
        <el-form-item label="退款金额" prop="amount">
          <el-input-number
            v-model="refundForm.amount"
            :min="minRefund"
            :max="maxRefund"
            :precision="currencyDigits(refundTarget?.currency)"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-tip">{{ refundTarget?.currency }}，支持部分退款</span>
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input v-model="refundForm.reason" type="textarea" :rows="2" maxlength="255" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="refunding" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import client from '../../api/client'

const CHANNEL_TEXT = { SIMULATOR: '模拟支付', STRIPE: 'Stripe', PAYPAL: 'PayPal' }
const CHANNEL_TAG = { SIMULATOR: 'info', STRIPE: 'primary', PAYPAL: 'warning' }
const PAY_STATUS_TEXT = { CREATED: '已创建', PENDING: '支付中', SUCCESS: '成功', FAILED: '失败', CLOSED: '已关闭' }
const PAY_STATUS_TAG = { CREATED: 'info', PENDING: 'warning', SUCCESS: 'success', FAILED: 'danger', CLOSED: 'info' }
const REFUND_STATUS_TEXT = { CREATED: '已创建', PENDING: '处理中', SUCCESS: '成功', FAILED: '失败' }
const REFUND_STATUS_TAG = { CREATED: 'info', PENDING: 'warning', SUCCESS: 'success', FAILED: 'danger' }

const activeTab = ref('pay')

const payQuery = reactive({ orderNo: '', channel: '', status: '', pageNum: 1, pageSize: 20 })
const payRows = ref([])
const payTotal = ref(0)
const payLoading = ref(false)

const refundQuery = reactive({ pageNum: 1, pageSize: 20 })
const refundRows = ref([])
const refundTotal = ref(0)
const refundLoading = ref(false)
const refundLoaded = ref(false)

const refundDialogVisible = ref(false)
const refundFormRef = ref(null)
const refundTarget = ref(null)
const refundForm = reactive({ amount: 0, reason: '' })
const refunding = ref(false)
const refundRules = { amount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }] }

function currencyDigits(currency) {
  return currency === 'JPY' ? 0 : 2
}

function money(currency, minor) {
  if (minor == null) return '-'
  const digits = currencyDigits(currency)
  return `${currency} ${(Number(minor) / Math.pow(10, digits)).toFixed(digits)}`
}

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

const minRefund = computed(() => (currencyDigits(refundTarget.value?.currency) === 0 ? 1 : 0.01))
const maxRefund = computed(() => {
  if (!refundTarget.value) return 0
  const digits = currencyDigits(refundTarget.value.currency)
  return Number(refundTarget.value.amountCents) / Math.pow(10, digits)
})

async function loadPay() {
  payLoading.value = true
  try {
    const data = await client.get('/admin/pay/page', {
      params: {
        orderNo: payQuery.orderNo || undefined,
        channel: payQuery.channel || undefined,
        status: payQuery.status || undefined,
        pageNum: payQuery.pageNum,
        pageSize: payQuery.pageSize
      }
    })
    payRows.value = data.list || []
    payTotal.value = data.total || 0
  } finally {
    payLoading.value = false
  }
}

function searchPay() {
  payQuery.pageNum = 1
  loadPay()
}

function resetPay() {
  payQuery.orderNo = ''
  payQuery.channel = ''
  payQuery.status = ''
  searchPay()
}

async function loadRefund() {
  refundLoading.value = true
  try {
    const data = await client.get('/admin/pay/refund/page', {
      params: { pageNum: refundQuery.pageNum, pageSize: refundQuery.pageSize }
    })
    refundRows.value = data.list || []
    refundTotal.value = data.total || 0
    refundLoaded.value = true
  } finally {
    refundLoading.value = false
  }
}

function onTabChange(name) {
  if (name === 'refund' && !refundLoaded.value) loadRefund()
}

function openRefund(row) {
  refundTarget.value = row
  refundForm.amount = maxRefundOf(row)
  refundForm.reason = ''
  refundDialogVisible.value = true
}

function maxRefundOf(row) {
  const digits = currencyDigits(row.currency)
  return Number(row.amountCents) / Math.pow(10, digits)
}

async function submitRefund() {
  await refundFormRef.value.validate()
  const digits = currencyDigits(refundTarget.value.currency)
  const amountMinor = Math.round(refundForm.amount * Math.pow(10, digits))
  if (amountMinor < 1) {
    ElMessage.warning('退款金额必须大于 0')
    return
  }
  refunding.value = true
  try {
    await client.post(`/admin/pay/${refundTarget.value.payNo}/refund`, {
      amountMinor,
      reason: refundForm.reason || undefined
    })
    ElMessage.success('退款已提交')
    refundDialogVisible.value = false
    loadPay()
    refundLoaded.value = false
    if (activeTab.value === 'refund') loadRefund()
  } finally {
    refunding.value = false
  }
}

onMounted(loadPay)
</script>

<style scoped>
.form-tip { margin-left: 10px; font-size: 12px; color: #909399; }
</style>

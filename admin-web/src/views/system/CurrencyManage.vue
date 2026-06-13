<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <span class="card-title">汇率管理（基准 USD）</span>
        <div style="flex: 1"></div>
        <el-button type="primary" :loading="refreshing" @click="refresh">立即从 frankfurter 刷新</el-button>
      </div>
      <el-table :data="rates" stripe v-loading="loading">
        <template #empty><el-empty description="暂无汇率数据" /></template>
        <el-table-column label="货币对" width="160">
          <template #default="{ row }">{{ row.baseCurrency }} → {{ row.quoteCurrency }}</template>
        </el-table-column>
        <el-table-column label="汇率" width="160">
          <template #default="{ row }">{{ fmtRate(row.rate) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :type="row.source === 'MANUAL' ? 'warning' : 'success'" size="small">
              {{ row.source === 'MANUAL' ? '手工 MANUAL' : 'API' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ fmtTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="toolbar">
        <span class="card-title">启用币种（只读）</span>
      </div>
      <el-table :data="currencies" stripe v-loading="currencyLoading">
        <template #empty><el-empty description="暂无币种" /></template>
        <el-table-column prop="code" label="代码" width="100" />
        <el-table-column prop="symbol" label="符号" width="80" />
        <el-table-column prop="nameZh" label="中文名" min-width="140" />
        <el-table-column prop="nameEn" label="英文名" min-width="160" />
        <el-table-column prop="decimalDigits" label="小数位" width="90" />
      </el-table>
    </div>

    <el-dialog v-model="dialog.visible" :title="`手工覆盖汇率 - USD → ${dialog.quote}`" width="420px">
      <el-form label-width="90px">
        <el-form-item label="货币对">
          <span>USD → {{ dialog.quote }}</span>
        </el-form-item>
        <el-form-item label="汇率" required>
          <el-input-number v-model="dialog.rate" :min="0.000001" :precision="6" :step="0.01" :controls="false" style="width: 200px" />
        </el-form-item>
        <div class="dialog-tip">覆盖后来源将标记为 MANUAL，并清除汇率缓存</div>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="save">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import client from '../../api/client'

const loading = ref(false)
const refreshing = ref(false)
const currencyLoading = ref(false)
const rates = ref([])
const currencies = ref([])
const dialog = reactive({ visible: false, saving: false, quote: '', rate: null })

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const fmtRate = (r) => (r == null ? '-' : Number(r).toFixed(6).replace(/0+$/, '').replace(/\.$/, ''))

async function loadRates() {
  loading.value = true
  try {
    rates.value = (await client.get('/admin/currency/rates')) || []
  } finally {
    loading.value = false
  }
}

async function loadCurrencies() {
  currencyLoading.value = true
  try {
    currencies.value = (await client.get('/currency/list')) || []
  } finally {
    currencyLoading.value = false
  }
}

async function refresh() {
  refreshing.value = true
  try {
    // 接口直接返回刷新后的最新列表
    rates.value = (await client.post('/admin/currency/rates/refresh')) || []
    ElMessage.success('汇率已从 frankfurter 刷新')
  } finally {
    refreshing.value = false
  }
}

function openEdit(row) {
  dialog.quote = row.quoteCurrency
  dialog.rate = Number(row.rate)
  dialog.visible = true
}

async function save() {
  if (!dialog.rate || dialog.rate <= 0) return ElMessage.warning('汇率必须大于 0')
  dialog.saving = true
  try {
    // 入参 {quote, rate}，接口返回覆盖后的最新列表
    rates.value = (await client.put('/admin/currency/rates', { quote: dialog.quote, rate: dialog.rate })) || []
    ElMessage.success('汇率覆盖成功')
    dialog.visible = false
  } finally {
    dialog.saving = false
  }
}

onMounted(() => {
  loadRates()
  loadCurrencies()
})
</script>

<style scoped>
.card-title { font-size: 15px; font-weight: 600; }
.dialog-tip { font-size: 12px; color: #909399; padding-left: 90px; }
</style>

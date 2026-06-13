<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增优惠券</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'FIXED' ? 'success' : 'warning'">
            {{ row.type === 'FIXED' ? '满减' : '折扣' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="面额" width="110">
        <template #default="{ row }">
          <span v-if="row.type === 'FIXED'">{{ usd(row.value) }}</span>
          <span v-else>{{ row.value }}% off</span>
        </template>
      </el-table-column>
      <el-table-column label="使用门槛" width="110">
        <template #default="{ row }">
          {{ Number(row.minAmountCents) > 0 ? '满 ' + usd(row.minAmountCents) : '无门槛' }}
        </template>
      </el-table-column>
      <el-table-column label="已领/总量" width="110">
        <template #default="{ row }">
          {{ row.receivedCount }} / {{ Number(row.totalCount) === 0 ? '不限' : row.totalCount }}
        </template>
      </el-table-column>
      <el-table-column prop="perUserLimit" label="限领" width="70" />
      <el-table-column label="有效期" min-width="230">
        <template #default="{ row }">{{ fmtTime(row.validFrom) }} ~ {{ fmtTime(row.validTo) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="row._switching"
            @change="(val) => toggleStatus(row, val)"
          />
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无优惠券" /></template>
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

    <!-- 新增优惠券 -->
    <el-dialog v-model="dialogVisible" title="新增优惠券" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="64" placeholder="如：新人专享 $5 OFF" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="FIXED">满减（固定金额）</el-radio>
            <el-radio value="PERCENT">折扣（百分比）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.type === 'FIXED' ? '减免金额' : '折扣比例'" prop="value">
          <el-input-number
            v-if="form.type === 'FIXED'"
            v-model="form.value"
            :min="0.01"
            :precision="2"
            :step="1"
            controls-position="right"
            style="width: 200px"
          />
          <el-input-number
            v-else
            v-model="form.value"
            :min="1"
            :max="99"
            :precision="0"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-tip">{{ form.type === 'FIXED' ? '美元（USD）' : '% off，1~99' }}</span>
        </el-form-item>
        <el-form-item label="使用门槛" prop="minAmount">
          <el-input-number
            v-model="form.minAmount"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-tip">美元（USD），0 为无门槛</span>
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="0" :precision="0" controls-position="right" style="width: 200px" />
          <span class="form-tip">0 为不限量</span>
        </el-form-item>
        <el-form-item label="每人限领" prop="perUserLimit">
          <el-input-number v-model="form.perUserLimit" :min="1" :precision="0" controls-position="right" style="width: 200px" />
          <span class="form-tip">张</span>
        </el-form-item>
        <el-form-item label="有效期" prop="validRange">
          <el-date-picker
            v-model="form.validRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import client from '../../api/client'

const query = reactive({ pageNum: 1, pageSize: 20 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  title: '',
  type: 'FIXED',
  value: 5,
  minAmount: 0,
  totalCount: 0,
  perUserLimit: 1,
  validRange: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  value: [{ required: true, message: '请输入面额', trigger: 'blur' }],
  validRange: [
    {
      required: true,
      validator: (rule, val, cb) => {
        if (!val || val.length !== 2) cb(new Error('请选择有效期'))
        else cb()
      },
      trigger: 'change'
    }
  ]
}

function usd(cents) {
  if (cents == null) return '-'
  return '$' + (Number(cents) / 100).toFixed(2)
}

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const data = await client.get('/admin/coupon/page', {
      params: { pageNum: query.pageNum, pageSize: query.pageSize }
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row, val) {
  row._switching = true
  try {
    await client.put(`/admin/coupon/${row.id}/status`, { status: val ? 1 : 0 })
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已启用' : '已停用')
  } finally {
    row._switching = false
  }
}

function openCreate() {
  form.title = ''
  form.type = 'FIXED'
  form.value = 5
  form.minAmount = 0
  form.totalCount = 0
  form.perUserLimit = 1
  form.validRange = []
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  // 后端 LocalDateTime 需 ISO 格式（YYYY-MM-DDTHH:mm:ss）
  const toIso = (s) => String(s).replace(' ', 'T')
  const body = {
    title: form.title,
    type: form.type,
    value: form.type === 'FIXED' ? Math.round(form.value * 100) : Math.round(form.value),
    minAmountCents: Math.round(form.minAmount * 100),
    totalCount: form.totalCount,
    perUserLimit: form.perUserLimit,
    validFrom: toIso(form.validRange[0]),
    validTo: toIso(form.validRange[1])
  }
  saving.value = true
  try {
    await client.post('/admin/coupon', body)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    query.pageNum = 1
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.form-tip { margin-left: 10px; font-size: 12px; color: #909399; }
</style>

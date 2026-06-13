<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增活动</el-button>
      <span class="tip">折扣力度 30 表示 off 30%（即七折）；限量 0 为不限量</span>
    </div>

    <el-table :data="rows" stripe v-loading="loading" row-key="id">
      <template #empty><el-empty description="暂无闪购活动" /></template>
      <el-table-column type="expand">
        <template #default="{ row }">
          <el-table :data="row.items" size="small" class="sub-table">
            <el-table-column prop="productId" label="商品 ID" width="90" />
            <el-table-column prop="productNameEn" label="商品名（英文）" min-width="240" />
            <el-table-column label="折扣" width="110">
              <template #default="{ row: item }">-{{ item.discountPercent }}%</template>
            </el-table-column>
            <el-table-column label="限量" width="100">
              <template #default="{ row: item }">{{ item.quota > 0 ? item.quota : '不限' }}</template>
            </el-table-column>
            <el-table-column prop="sold" label="已售" width="100" />
          </el-table>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="活动标题" min-width="200" />
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="商品数" width="80">
        <template #default="{ row }">{{ (row.items || []).length }}</template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="640px">
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="活动标题" required>
          <el-input v-model="dialog.form.title" maxlength="64" placeholder="如：夏日闪购 Summer Flash Sale" />
        </el-form-item>
        <el-form-item label="起止时间" required>
          <el-date-picker
            v-model="dialog.form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD[T]HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动商品" required>
          <div class="item-rows">
            <div v-for="(item, i) in dialog.form.items" :key="i" class="item-row">
              <el-input-number v-model="item.productId" :min="1" :precision="0" :controls="false" placeholder="商品 ID" style="width: 120px" />
              <el-input-number v-model="item.discountPercent" :min="1" :max="90" :precision="0" :controls="false" placeholder="折扣 1-90" style="width: 110px" />
              <span class="unit">% off</span>
              <el-input-number v-model="item.quota" :min="0" :precision="0" :controls="false" placeholder="限量(0不限)" style="width: 110px" />
              <el-button link type="danger" @click="dialog.form.items.splice(i, 1)">删除</el-button>
            </div>
            <el-button size="small" @click="addItem">＋ 添加商品</el-button>
          </div>
        </el-form-item>
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
const rows = ref([])
const dialog = reactive({
  visible: false,
  title: '',
  saving: false,
  editId: null,
  form: { title: '', timeRange: [], items: [] }
})

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

async function load() {
  loading.value = true
  try {
    rows.value = (await client.get('/admin/flash-sale/list')) || []
  } finally {
    loading.value = false
  }
}

function addItem() {
  dialog.form.items.push({ productId: null, discountPercent: 30, quota: 0 })
}

function openCreate() {
  dialog.editId = null
  dialog.title = '新增闪购活动'
  dialog.form = { title: '', timeRange: [], items: [{ productId: null, discountPercent: 30, quota: 0 }] }
  dialog.visible = true
}

function openEdit(row) {
  dialog.editId = row.id
  dialog.title = `编辑活动 - ${row.title}`
  dialog.form = {
    title: row.title,
    timeRange: [row.startTime, row.endTime],
    items: (row.items || []).map((it) => ({
      productId: it.productId,
      discountPercent: it.discountPercent,
      quota: it.quota ?? 0
    }))
  }
  dialog.visible = true
}

async function save() {
  const f = dialog.form
  if (!f.title.trim()) return ElMessage.warning('请填写活动标题')
  if (!f.timeRange || f.timeRange.length !== 2) return ElMessage.warning('请选择起止时间')
  if (!f.items.length) return ElMessage.warning('请至少添加一个活动商品')
  for (const it of f.items) {
    if (!it.productId) return ElMessage.warning('商品 ID 不能为空')
    if (!it.discountPercent || it.discountPercent < 1 || it.discountPercent > 90) return ElMessage.warning('折扣需在 1-90 之间')
  }
  // 后端 LocalDateTime 仅接受 ISO 格式（yyyy-MM-dd'T'HH:mm:ss），value-format 已带 T
  const payload = {
    title: f.title.trim(),
    startTime: f.timeRange[0],
    endTime: f.timeRange[1],
    items: f.items.map((it) => ({ productId: it.productId, discountPercent: it.discountPercent, quota: it.quota ?? 0 }))
  }
  dialog.saving = true
  try {
    if (dialog.editId) {
      await client.put(`/admin/flash-sale/${dialog.editId}`, payload)
      ElMessage.success('活动更新成功')
    } else {
      await client.post('/admin/flash-sale', payload)
      ElMessage.success('活动创建成功')
    }
    dialog.visible = false
    load()
  } finally {
    dialog.saving = false
  }
}

async function toggleStatus(row, val) {
  const status = val ? 1 : 0
  try {
    await client.put(`/admin/flash-sale/${row.id}/status`, { status })
    row.status = status
    ElMessage.success(status === 1 ? '活动已启用' : '活动已停用')
  } catch {
    /* 失败保持原状态，client.js 已弹错 */
  }
}

onMounted(load)
</script>

<style scoped>
.tip { font-size: 12px; color: #909399; }
.sub-table { margin: 4px 12px; width: calc(100% - 24px); }
.item-rows { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.item-row { display: flex; align-items: center; gap: 8px; }
.unit { color: #909399; font-size: 12px; }
</style>

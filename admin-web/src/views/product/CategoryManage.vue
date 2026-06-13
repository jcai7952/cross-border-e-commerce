<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate(0)">新增一级类目</el-button>
      <span class="tip">行邮税率适用于海外直邮（DIRECT）商品入境中国的税费试算</span>
    </div>

    <el-table
      :data="tree"
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
      stripe
      v-loading="loading"
    >
      <template #empty><el-empty description="暂无类目" /></template>
      <el-table-column prop="nameZh" label="中文名" min-width="180" />
      <el-table-column prop="nameEn" label="英文名" min-width="180" />
      <el-table-column label="行邮税率" width="110">
        <template #default="{ row }">{{ row.postalTaxRate }}%</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.level === 1" link type="primary" @click="openCreate(row.id)">添加子类目</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="460px">
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="中文名" required>
          <el-input v-model="dialog.form.nameZh" placeholder="如：连衣裙" />
        </el-form-item>
        <el-form-item label="英文名" required>
          <el-input v-model="dialog.form.nameEn" placeholder="如：Dresses" />
        </el-form-item>
        <el-form-item label="行邮税率">
          <el-select v-model="dialog.form.postalTaxRate" style="width: 100%">
            <el-option :value="13" label="13%（书报、食品、金银等）" />
            <el-option :value="20" label="20%（服装、电器、自行车等）" />
            <el-option :value="50" label="50%（烟酒、化妆品、高档手表等）" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialog.form.sort" :min="0" :precision="0" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../../api/client'

const loading = ref(false)
const tree = ref([])
const dialog = reactive({
  visible: false,
  title: '',
  saving: false,
  editId: null,
  form: { parentId: 0, nameZh: '', nameEn: '', postalTaxRate: 20, sort: 0, status: 1 }
})

async function load() {
  loading.value = true
  try {
    tree.value = await client.get('/admin/category/tree')
  } finally {
    loading.value = false
  }
}

function openCreate(parentId) {
  dialog.editId = null
  dialog.title = parentId === 0 ? '新增一级类目' : '添加子类目'
  dialog.form = { parentId, nameZh: '', nameEn: '', postalTaxRate: 20, sort: 0, status: 1 }
  dialog.visible = true
}

function openEdit(row) {
  dialog.editId = row.id
  dialog.title = `编辑类目 - ${row.nameZh}`
  dialog.form = {
    parentId: row.parentId,
    nameZh: row.nameZh,
    nameEn: row.nameEn,
    postalTaxRate: row.postalTaxRate,
    sort: row.sort,
    status: row.status
  }
  dialog.visible = true
}

async function save() {
  if (!dialog.form.nameZh.trim()) return ElMessage.warning('请填写中文名')
  if (!dialog.form.nameEn.trim()) return ElMessage.warning('请填写英文名')
  dialog.saving = true
  try {
    if (dialog.editId) {
      await client.put(`/admin/category/${dialog.editId}`, dialog.form)
      ElMessage.success('类目更新成功')
    } else {
      await client.post('/admin/category', dialog.form)
      ElMessage.success('类目创建成功')
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
    await client.put(`/admin/category/${row.id}`, {
      parentId: row.parentId,
      nameZh: row.nameZh,
      nameEn: row.nameEn,
      postalTaxRate: row.postalTaxRate,
      sort: row.sort,
      status
    })
    row.status = status
    ElMessage.success(status === 1 ? '已启用' : '已停用')
  } catch {
    /* 失败保持原状态，client.js 已弹错 */
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除类目「${row.nameZh}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await client.delete(`/admin/category/${row.id}`)
    ElMessage.success('删除成功')
    load()
  } catch {
    /* 存在子类目或商品时后端拒绝，client.js 已弹后端 message */
  }
}

onMounted(load)
</script>

<style scoped>
.tip { font-size: 12px; color: #909399; }
</style>

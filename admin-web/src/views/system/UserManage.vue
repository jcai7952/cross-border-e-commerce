<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input
        v-model="query.email"
        placeholder="邮箱（模糊查询）"
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
        <el-option label="正常" :value="1" />
        <el-option label="已禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="email" label="邮箱" min-width="220" />
      <el-table-column label="昵称" min-width="120">
        <template #default="{ row }">{{ row.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column prop="locale" label="语言" width="90" />
      <el-table-column prop="currency" label="币种" width="80" />
      <el-table-column label="邮箱验证" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.emailVerified === 1 ? 'success' : 'info'">
            {{ row.emailVerified === 1 ? '已验证' : '未验证' }}
          </el-tag>
        </template>
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
      <el-table-column label="注册时间" width="160">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <template #empty><el-empty description="暂无用户" /></template>
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

const query = reactive({ email: '', status: '', pageNum: 1, pageSize: 20 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const data = await client.get('/admin/user/page', {
      params: {
        email: query.email || undefined,
        status: query.status === '' ? undefined : query.status,
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
  query.email = ''
  query.status = ''
  search()
}

async function toggleStatus(row, val) {
  if (!val) {
    try {
      await ElMessageBox.confirm(
        `确认禁用用户 ${row.email}？禁用后将立即踢出该用户的登录会话。`,
        '禁用确认',
        { confirmButtonText: '确认禁用', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }
  row._switching = true
  try {
    await client.put(`/admin/user/${row.id}/status`, { status: val ? 1 : 0 })
    row.status = val ? 1 : 0
    ElMessage.success(val ? '用户已启用' : '用户已禁用')
  } finally {
    row._switching = false
  }
}

onMounted(load)
</script>

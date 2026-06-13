<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-input v-model="query.productId" placeholder="商品 ID" clearable style="width: 140px" @keyup.enter="search" />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" stripe v-loading="loading">
      <template #empty><el-empty description="暂无评论" /></template>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="productId" label="商品 ID" width="80" />
      <el-table-column prop="userNickname" label="用户昵称" width="110" />
      <el-table-column label="评分" width="140">
        <template #default="{ row }">
          <el-rate :model-value="row.rating" disabled />
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
      <el-table-column label="晒图" width="160">
        <template #default="{ row }">
          <div v-if="row.images && row.images.length" class="img-group">
            <el-image
              v-for="(img, i) in row.images"
              :key="i"
              :src="img"
              :preview-src-list="row.images"
              :initial-index="i"
              preview-teleported
              fit="cover"
              class="thumb"
            />
          </div>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="success" @click="audit(row, true)">通过</el-button>
            <el-button link type="danger" @click="audit(row, false)">拒绝</el-button>
          </template>
          <el-button v-else-if="row.status === 1" link type="warning" @click="takeDown(row)">撤下</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        v-model:current-page="query.pageNum"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../../api/client'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ status: null, productId: '', pageNum: 1, pageSize: 20 })

const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const statusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' }[s] ?? s)
const statusTag = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] ?? 'info')

async function load() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status !== null && query.status !== '') params.status = query.status
    if (query.productId) params.productId = query.productId
    const data = await client.get('/admin/review/page', { params })
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
  query.status = null
  query.productId = ''
  query.pageNum = 1
  load()
}

async function audit(row, approve) {
  try {
    await client.put(`/admin/review/${row.id}/audit`, { approve })
    ElMessage.success(approve ? '已通过' : '已拒绝')
    load()
  } catch {
    /* client.js 已弹错 */
  }
}

async function takeDown(row) {
  try {
    await ElMessageBox.confirm('撤下后将反向扣减商品评分聚合，确认撤下该评论？', '撤下确认', { type: 'warning' })
  } catch {
    return
  }
  audit(row, false)
}

onMounted(load)
</script>

<style scoped>
.img-group { display: flex; gap: 6px; flex-wrap: wrap; }
.thumb { width: 60px; height: 60px; border-radius: 4px; display: block; }
.muted { color: #c0c4cc; }
</style>

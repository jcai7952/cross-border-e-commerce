<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.categoryId" placeholder="全部类目" clearable style="width: 150px">
        <el-option v-for="c in topCategories" :key="c.id" :label="c.nameZh" :value="c.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="英文名 / SPU 编码" clearable style="width: 220px" @keyup.enter="search" />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div style="flex: 1"></div>
      <el-button type="primary" plain @click="$router.push('/product/edit')">新增商品</el-button>
    </div>

    <el-table :data="rows" stripe v-loading="loading">
      <template #empty><el-empty description="暂无商品" /></template>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="主图" width="84">
        <template #default="{ row }">
          <el-image
            :src="row.mainImageUrl"
            :preview-src-list="[row.mainImageUrl]"
            preview-teleported
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 6px; display: block"
          />
        </template>
      </el-table-column>
      <el-table-column label="名称" min-width="200">
        <template #default="{ row }">
          <div class="name-zh">{{ row.nameZh }}</div>
          <div class="name-en">{{ row.nameEn }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="spuCode" label="SPU" width="100" />
      <el-table-column label="类目" width="100">
        <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="价格" width="100">
        <template #default="{ row }">{{ fmtCents(row.minPriceCents) }} 起</template>
      </el-table-column>
      <el-table-column label="贸易模式" width="90">
        <template #default="{ row }">
          <el-tag :type="row.tradeMode === 'BONDED' ? 'success' : 'warning'" size="small">
            {{ row.tradeMode === 'BONDED' ? '保税' : '直邮' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="salesCount" label="销量" width="80" />
      <el-table-column label="评分" width="100">
        <template #default="{ row }">{{ row.ratingAvg }}（{{ row.ratingCount }}）</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/product/edit/${row.id}`)">编辑</el-button>
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
import { ElMessage } from 'element-plus'
import client from '../../api/client'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const topCategories = ref([])
const categoryMap = ref({})
const query = reactive({ categoryId: null, keyword: '', status: null, pageNum: 1, pageSize: 20 })

const fmtCents = (c) => (c == null ? '-' : '$' + (c / 100).toFixed(2))
const categoryName = (id) => categoryMap.value[id] || id

async function loadCategories() {
  const tree = await client.get('/admin/category/tree')
  topCategories.value = tree
  const map = {}
  for (const c of tree) {
    map[c.id] = c.nameZh
    for (const child of c.children || []) map[child.id] = child.nameZh
  }
  categoryMap.value = map
}

async function load() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.categoryId !== null && query.categoryId !== '') params.categoryId = query.categoryId
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await client.get('/admin/product/page', { params })
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
  query.categoryId = null
  query.keyword = ''
  query.status = null
  query.pageNum = 1
  load()
}

async function toggleStatus(row, val) {
  const status = val ? 1 : 0
  try {
    await client.put(`/admin/product/${row.id}/status`, { status })
    row.status = status
    ElMessage.success(status === 1 ? '已上架' : '已下架')
  } catch {
    /* 失败保持原状态，client.js 已弹错 */
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<style scoped>
.name-zh { font-weight: 600; line-height: 1.5; }
.name-en { color: #909399; font-size: 12px; line-height: 1.5; }
</style>

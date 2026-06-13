<template>
  <div class="page-card" v-loading="loading">
    <el-form :model="form" label-width="100px">
      <h3 class="block-title">基本信息</h3>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="类目" required>
            <el-cascader
              v-model="form.categoryId"
              :options="categoryTree"
              :props="cascaderProps"
              placeholder="请选择叶子类目"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="SPU 编码" required>
            <el-input v-model="form.spuCode" placeholder="如 SPU0001" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="品牌">
            <el-input v-model="form.brand" placeholder="品牌名" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="10">
          <el-form-item label="贸易模式">
            <el-radio-group v-model="form.tradeMode">
              <el-radio value="BONDED">保税仓 BONDED</el-radio>
              <el-radio value="DIRECT">海外直邮 DIRECT</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="7">
          <el-form-item label="发货地">
            <el-input v-model="form.originCountry" maxlength="2" placeholder="二位国码，如 CN" style="width: 140px" />
          </el-form-item>
        </el-col>
        <el-col :span="7">
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">上架</el-radio>
              <el-radio :value="0">下架</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <h3 class="block-title">多语言</h3>
      <el-tabs v-model="activeLocale">
        <el-tab-pane v-for="loc in locales" :key="loc.value" :label="loc.label" :name="loc.value">
          <el-form-item label="名称" required>
            <el-input v-model="form.i18n[loc.value].name" :placeholder="loc.value === 'zh-CN' ? '中文商品名' : 'English product name'" />
          </el-form-item>
          <el-form-item label="副标题">
            <el-input v-model="form.i18n[loc.value].subtitle" placeholder="卖点短句" />
          </el-form-item>
          <el-form-item label="详情">
            <el-input v-model="form.i18n[loc.value].detail" type="textarea" :rows="5" placeholder="详情内容，支持 HTML" />
          </el-form-item>
        </el-tab-pane>
      </el-tabs>

      <h3 class="block-title">图册 <span class="tip">第一张为主图，提交时按当前顺序保存</span></h3>
      <div class="gallery">
        <div v-for="(img, i) in form.images" :key="img.key + i" class="gallery-item">
          <el-image
            :src="img.url"
            fit="cover"
            :preview-src-list="form.images.map((x) => x.url)"
            :initial-index="i"
            preview-teleported
            class="gallery-img"
          />
          <el-tag v-if="i === 0" size="small" type="success" class="main-tag">主图</el-tag>
          <span class="del-btn" title="删除" @click="form.images.splice(i, 1)">×</span>
        </div>
        <el-upload :show-file-list="false" :http-request="uploadGallery" accept="image/*" multiple>
          <div class="upload-box" v-loading="uploading">＋</div>
        </el-upload>
      </div>

      <h3 class="block-title">SKU 列表</h3>
      <el-table :data="form.skus" stripe>
        <template #empty><el-empty description="暂无 SKU，请添加" :image-size="60" /></template>
        <el-table-column label="SKU 编码" min-width="150">
          <template #default="{ row }"><el-input v-model="row.skuCode" placeholder="如 SKU0001-RED-M" /></template>
        </el-table-column>
        <el-table-column label="颜色(英)" min-width="110">
          <template #default="{ row }"><el-input v-model="row.color" placeholder="Red" /></template>
        </el-table-column>
        <el-table-column label="颜色(中)" min-width="100">
          <template #default="{ row }"><el-input v-model="row.colorZh" placeholder="红色" /></template>
        </el-table-column>
        <el-table-column label="尺码" width="100">
          <template #default="{ row }"><el-input v-model="row.size" placeholder="M" /></template>
        </el-table-column>
        <el-table-column label="价格(美元)" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.priceUsd" :min="0.01" :precision="2" :step="1" :controls="false" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" :precision="0" :controls="false" style="width: 80px" />
          </template>
        </el-table-column>
        <el-table-column label="重量(g)" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.weightGrams" :min="1" :max="50000" :precision="0" :controls="false" style="width: 80px" />
          </template>
        </el-table-column>
        <el-table-column label="色卡图" width="90">
          <template #default="{ row }">
            <el-upload :show-file-list="false" :http-request="(opt) => uploadSkuImage(opt, row)" accept="image/*">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                fit="cover"
                style="width: 40px; height: 40px; border-radius: 4px; cursor: pointer; display: block"
                title="点击替换"
              />
              <el-button v-else link type="primary" size="small">上传</el-button>
            </el-upload>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="form.skus.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button style="margin-top: 10px" @click="addSku">＋ 添加 SKU</el-button>

      <div class="footer-bar">
        <el-button @click="$router.push('/product')">返 回</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ productId ? '保存修改' : '创建商品' }}</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../../api/client'

const route = useRoute()
const router = useRouter()
const productId = route.params.id ? Number(route.params.id) : null

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const categoryTree = ref([])
const activeLocale = ref('zh-CN')
const locales = [
  { value: 'zh-CN', label: '中文 zh-CN' },
  { value: 'en-US', label: '英文 en-US' }
]
const cascaderProps = { value: 'id', label: 'nameZh', children: 'children', emitPath: false }

const form = reactive({
  categoryId: null,
  spuCode: '',
  brand: '',
  tradeMode: 'BONDED',
  originCountry: 'CN',
  status: 1,
  i18n: {
    'zh-CN': { name: '', subtitle: '', detail: '' },
    'en-US': { name: '', subtitle: '', detail: '' }
  },
  images: [], // [{key, url}]
  skus: []
})

function addSku() {
  form.skus.push({
    id: null,
    skuCode: '',
    color: '',
    colorZh: '',
    size: '',
    priceUsd: null,
    stock: 0,
    weightGrams: 300,
    image: '',
    imageUrl: '',
    status: 1
  })
}

async function uploadFile(file) {
  const fd = new FormData()
  fd.append('file', file)
  // POST /api/admin/product/upload，satoken 由 client.js 统一携带，返回 {key, url}
  return await client.post('/admin/product/upload', fd)
}

async function uploadGallery({ file }) {
  uploading.value = true
  try {
    const res = await uploadFile(file)
    form.images.push({ key: res.key, url: res.url })
  } finally {
    uploading.value = false
  }
}

async function uploadSkuImage({ file }, row) {
  const res = await uploadFile(file)
  row.image = res.key
  row.imageUrl = res.url
}

async function loadCategories() {
  categoryTree.value = await client.get('/admin/category/tree')
}

async function loadDetail() {
  loading.value = true
  try {
    const d = await client.get(`/admin/product/${productId}`)
    form.categoryId = d.categoryId
    form.spuCode = d.spuCode
    form.brand = d.brand || ''
    form.tradeMode = d.tradeMode || 'BONDED'
    form.originCountry = d.originCountry || 'CN'
    form.status = d.status
    for (const it of d.i18ns || []) {
      if (form.i18n[it.locale]) {
        form.i18n[it.locale].name = it.name || ''
        form.i18n[it.locale].subtitle = it.subtitle || ''
        form.i18n[it.locale].detail = it.detail || ''
      }
    }
    form.images = (d.images || []).map((x) => ({ key: x.key, url: x.url }))
    form.skus = (d.skus || []).map((s) => ({
      id: s.id,
      skuCode: s.skuCode,
      color: s.color,
      colorZh: s.colorZh,
      size: s.size,
      priceUsd: s.priceCents / 100,
      stock: s.stock,
      weightGrams: s.weightGrams || 300,
      image: s.image || '',
      imageUrl: s.imageUrl || '',
      status: s.status
    }))
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.categoryId) return ElMessage.warning('请选择叶子类目')
  if (!form.spuCode.trim()) return ElMessage.warning('请填写 SPU 编码')
  if (!form.i18n['zh-CN'].name.trim() || !form.i18n['en-US'].name.trim()) return ElMessage.warning('请填写中/英文商品名称')
  if (!form.images.length) return ElMessage.warning('请至少上传一张图片（第一张为主图）')
  if (!form.skus.length) return ElMessage.warning('请至少添加一个 SKU')
  for (const s of form.skus) {
    if (!s.skuCode || !s.color || !s.colorZh || !s.size) return ElMessage.warning('SKU 编码 / 颜色 / 尺码均为必填')
    if (!s.priceUsd || s.priceUsd <= 0) return ElMessage.warning('SKU 价格必须大于 0')
  }

  const payload = {
    categoryId: form.categoryId,
    spuCode: form.spuCode.trim(),
    brand: form.brand || '',
    tradeMode: form.tradeMode,
    originCountry: (form.originCountry || 'CN').toUpperCase(),
    mainImage: form.images[0].key, // 后端要求对象 key（非完整 URL）
    status: form.status,
    i18ns: locales.map((l) => ({
      locale: l.value,
      name: form.i18n[l.value].name,
      subtitle: form.i18n[l.value].subtitle,
      detail: form.i18n[l.value].detail
    })),
    images: form.images.map((x) => x.key),
    skus: form.skus.map((s) => ({
      id: s.id || undefined,
      skuCode: s.skuCode,
      color: s.color,
      colorZh: s.colorZh,
      size: s.size,
      priceCents: Math.round(s.priceUsd * 100),
      stock: s.stock ?? 0,
      image: s.image || undefined,
      weightGrams: s.weightGrams || 300,
      status: s.status ?? 1
    }))
  }

  saving.value = true
  try {
    if (productId) {
      await client.put(`/admin/product/${productId}`, payload)
      ElMessage.success('商品更新成功')
    } else {
      await client.post('/admin/product', payload)
      ElMessage.success('商品创建成功')
    }
    router.push('/product')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCategories()
  if (productId) loadDetail()
  else addSku()
})
</script>

<style scoped>
.block-title {
  font-size: 15px;
  font-weight: 600;
  margin: 18px 0 14px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.block-title:first-child { margin-top: 0; }
.tip { font-size: 12px; font-weight: 400; color: #909399; margin-left: 8px; }
.gallery { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 8px; }
.gallery-item { position: relative; width: 100px; height: 100px; }
.gallery-img { width: 100px; height: 100px; border-radius: 6px; display: block; }
.main-tag { position: absolute; left: 4px; top: 4px; }
.del-btn {
  position: absolute; right: -6px; top: -6px;
  width: 20px; height: 20px; line-height: 18px; text-align: center;
  background: #f56c6c; color: #fff; border-radius: 50%;
  font-size: 14px; cursor: pointer; user-select: none; z-index: 2;
}
.upload-box {
  width: 100px; height: 100px; border: 1px dashed #d4d7de; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; color: #8c939d; cursor: pointer; background: #fafafa;
}
.upload-box:hover { border-color: #409eff; color: #409eff; }
.footer-bar { margin-top: 24px; display: flex; justify-content: center; gap: 12px; }
</style>

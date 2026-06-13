<template>
  <div>
    <div class="page-card" style="margin-bottom: 16px">
      <div class="toolbar" style="margin-bottom: 0">
        <el-button type="primary" @click="openCreateTemplate">新增模板</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && templates.length === 0" description="暂无运费模板" class="page-card" />

    <div v-for="tpl in templates" :key="tpl.id" v-loading="loading" class="page-card tpl-card">
      <div class="tpl-head">
        <div class="tpl-name">
          {{ tpl.name }}
          <el-tag size="small" :type="tpl.status === 1 ? 'success' : 'info'" style="margin-left: 8px">
            {{ tpl.status === 1 ? '启用中' : '已停用' }}
          </el-tag>
        </div>
        <div class="tpl-actions">
          <el-switch
            :model-value="tpl.status === 1"
            :loading="tpl._switching"
            @change="(val) => toggleTemplate(tpl, val)"
          />
          <el-button type="primary" plain size="small" @click="openCreateZone(tpl)">新增区域</el-button>
        </div>
      </div>

      <el-table :data="tpl.zones || []" stripe size="small">
        <el-table-column prop="zoneName" label="区域名" width="120" />
        <el-table-column label="国家列表" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="c in (row.countries || '').split(',').filter(Boolean)" :key="c" size="small" style="margin: 2px 4px 2px 0">
              {{ c }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首重" width="90">
          <template #default="{ row }">{{ row.firstWeightG }}g</template>
        </el-table-column>
        <el-table-column label="首重费" width="90">
          <template #default="{ row }">{{ usd(row.firstFeeCents) }}</template>
        </el-table-column>
        <el-table-column label="续重" width="90">
          <template #default="{ row }">{{ row.addWeightG }}g</template>
        </el-table-column>
        <el-table-column label="续重费" width="90">
          <template #default="{ row }">{{ usd(row.addFeeCents) }}</template>
        </el-table-column>
        <el-table-column label="时效" width="100">
          <template #default="{ row }">{{ row.estDaysMin }}-{{ row.estDaysMax }} 天</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditZone(tpl, row)">编辑</el-button>
            <el-button type="danger" link @click="deleteZone(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无区域计费，请新增" :image-size="60" /></template>
      </el-table>
    </div>

    <!-- 新增模板 -->
    <el-dialog v-model="tplDialogVisible" title="新增运费模板" width="420px" destroy-on-close>
      <el-form ref="tplFormRef" :model="tplForm" :rules="tplRules" label-width="90px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="tplForm.name" maxlength="64" placeholder="如：标准跨境专线" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tplDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑区域 -->
    <el-dialog v-model="zoneDialogVisible" :title="zoneForm.id ? '编辑区域计费' : '新增区域计费'" width="560px" destroy-on-close>
      <el-form ref="zoneFormRef" :model="zoneForm" :rules="zoneRules" label-width="110px">
        <el-form-item label="区域名" prop="zoneName">
          <el-input v-model="zoneForm.zoneName" maxlength="64" placeholder="如：北美" />
        </el-form-item>
        <el-form-item label="国家列表" prop="countries">
          <el-input v-model="zoneForm.countries" maxlength="500" placeholder="逗号分隔 ISO2 国家码，如 US,CA" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="首重(g)" prop="firstWeightG">
              <el-input-number v-model="zoneForm.firstWeightG" :min="1" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="首重费($)" prop="firstFee">
              <el-input-number v-model="zoneForm.firstFee" :min="0" :precision="2" :step="0.5" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="续重(g)" prop="addWeightG">
              <el-input-number v-model="zoneForm.addWeightG" :min="1" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="续重费($)" prop="addFee">
              <el-input-number v-model="zoneForm.addFee" :min="0" :precision="2" :step="0.5" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="时效最短(天)" prop="estDaysMin">
              <el-input-number v-model="zoneForm.estDaysMin" :min="1" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时效最长(天)" prop="estDaysMax">
              <el-input-number v-model="zoneForm.estDaysMax" :min="1" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="zoneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveZone">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../../api/client'

const templates = ref([])
const loading = ref(false)
const saving = ref(false)

const tplDialogVisible = ref(false)
const tplFormRef = ref(null)
const tplForm = reactive({ name: '' })
const tplRules = { name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }] }

const zoneDialogVisible = ref(false)
const zoneFormRef = ref(null)
const zoneForm = reactive({
  id: null,
  templateId: null,
  zoneName: '',
  countries: '',
  firstWeightG: 500,
  firstFee: 0,
  addWeightG: 500,
  addFee: 0,
  estDaysMin: 5,
  estDaysMax: 10
})
const zoneRules = {
  zoneName: [{ required: true, message: '请输入区域名', trigger: 'blur' }],
  countries: [
    { required: true, message: '请输入国家列表', trigger: 'blur' },
    {
      validator: (rule, val, cb) => {
        if (val && !/^[A-Za-z]{2}(\s*,\s*[A-Za-z]{2})*$/.test(val.trim())) cb(new Error('格式：逗号分隔的 ISO2 国家码，如 US,CA'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

function usd(cents) {
  if (cents == null) return '-'
  return '$' + (Number(cents) / 100).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    templates.value = (await client.get('/admin/logistics/template/list')) || []
  } finally {
    loading.value = false
  }
}

async function toggleTemplate(tpl, val) {
  tpl._switching = true
  try {
    // 后端更新模板需同时携带 name 与 status
    await client.put(`/admin/logistics/template/${tpl.id}`, { name: tpl.name, status: val ? 1 : 0 })
    tpl.status = val ? 1 : 0
    ElMessage.success(val ? '模板已启用' : '模板已停用')
  } finally {
    tpl._switching = false
  }
}

function openCreateTemplate() {
  tplForm.name = ''
  tplDialogVisible.value = true
}

async function saveTemplate() {
  await tplFormRef.value.validate()
  saving.value = true
  try {
    await client.post('/admin/logistics/template', { name: tplForm.name })
    ElMessage.success('模板创建成功')
    tplDialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function fillZoneForm(templateId, zone) {
  zoneForm.id = zone ? zone.id : null
  zoneForm.templateId = templateId
  zoneForm.zoneName = zone ? zone.zoneName : ''
  zoneForm.countries = zone ? zone.countries : ''
  zoneForm.firstWeightG = zone ? zone.firstWeightG : 500
  zoneForm.firstFee = zone ? Number(zone.firstFeeCents) / 100 : 0
  zoneForm.addWeightG = zone ? zone.addWeightG : 500
  zoneForm.addFee = zone ? Number(zone.addFeeCents) / 100 : 0
  zoneForm.estDaysMin = zone ? zone.estDaysMin : 5
  zoneForm.estDaysMax = zone ? zone.estDaysMax : 10
}

function openCreateZone(tpl) {
  fillZoneForm(tpl.id, null)
  zoneDialogVisible.value = true
}

function openEditZone(tpl, zone) {
  fillZoneForm(tpl.id, zone)
  zoneDialogVisible.value = true
}

async function saveZone() {
  await zoneFormRef.value.validate()
  if (zoneForm.estDaysMax < zoneForm.estDaysMin) {
    ElMessage.warning('时效最长不能小于时效最短')
    return
  }
  const body = {
    templateId: zoneForm.templateId,
    zoneName: zoneForm.zoneName,
    countries: zoneForm.countries.replace(/\s/g, '').toUpperCase(),
    firstWeightG: zoneForm.firstWeightG,
    firstFeeCents: Math.round(zoneForm.firstFee * 100),
    addWeightG: zoneForm.addWeightG,
    addFeeCents: Math.round(zoneForm.addFee * 100),
    estDaysMin: zoneForm.estDaysMin,
    estDaysMax: zoneForm.estDaysMax
  }
  saving.value = true
  try {
    if (zoneForm.id) {
      await client.put(`/admin/logistics/zone/${zoneForm.id}`, body)
      ElMessage.success('区域计费更新成功')
    } else {
      await client.post('/admin/logistics/zone', body)
      ElMessage.success('区域计费新增成功')
    }
    zoneDialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function deleteZone(zone) {
  try {
    await ElMessageBox.confirm(`确认删除区域「${zone.zoneName}」？删除后该区域国家将无法计算运费。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await client.delete(`/admin/logistics/zone/${zone.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.tpl-card { margin-bottom: 16px; }
.tpl-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.tpl-name { font-size: 15px; font-weight: 600; color: #303133; display: flex; align-items: center; }
.tpl-actions { display: flex; align-items: center; gap: 12px; }
</style>

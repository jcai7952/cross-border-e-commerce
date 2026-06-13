<template>
  <div class="login-wrap">
    <div class="login-card">
      <h1>KinnShop 运营后台</h1>
      <p class="sub">SHEIN 同型跨境电商 · 管理端</p>
      <el-form @submit.prevent="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })

async function doLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名与密码')
  loading.value = true
  try {
    const data = await client.post('/admin/auth/login', form)
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_info', JSON.stringify(data.admin))
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.login-card {
  width: 380px; padding: 40px 36px;
  background: #fff; border-radius: 12px;
  box-shadow: 0 18px 50px rgba(0,0,0,.35);
}
h1 { font-size: 22px; margin-bottom: 6px; }
.sub { color: #909399; font-size: 13px; margin-bottom: 28px; }
</style>

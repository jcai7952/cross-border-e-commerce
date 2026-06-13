<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="auth-logo">KINNSHOP</router-link>
      <h2 class="auth-title">{{ $t('auth.register') }}</h2>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item :label="$t('auth.email')" prop="email">
          <el-input v-model="form.email" size="large" autocomplete="email" />
        </el-form-item>
        <el-form-item :label="$t('auth.code')" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" size="large" />
            <el-button size="large" :disabled="cd > 0 || sending" :loading="sending" @click="sendCode">
              {{ cd > 0 ? $t('auth.resend', { s: cd }) : $t('auth.sendCode') }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item :label="$t('auth.password')" prop="password">
          <el-input v-model="form.password" type="password" show-password size="large" autocomplete="new-password" />
        </el-form-item>
        <el-form-item :label="$t('auth.nickname')" prop="nickname">
          <el-input v-model="form.nickname" size="large" maxlength="64" />
        </el-form-item>
        <el-button class="auth-submit" type="primary" color="#111111" size="large" :loading="loading" @click="onRegister">
          {{ $t('auth.register') }}
        </el-button>
      </el-form>

      <div class="auth-links">
        <router-link class="auth-link" :to="loginTo">{{ $t('auth.toLogin') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const cd = ref(0)
let timer = null

const form = ref({ email: '', code: '', password: '', nickname: '' })

/** i18n 文件未含的少量文案，按语言就地映射 */
const txt = computed(() =>
  app.locale === 'zh-CN'
    ? { required: '必填', emailFmt: '邮箱格式不正确', pwdLen: '密码长度 6-32 位', codeSent: '验证码已发送' }
    : { required: 'Required', emailFmt: 'Invalid email format', pwdLen: 'Password must be 6-32 characters', codeSent: 'Code sent' }
)

const rules = computed(() => ({
  email: [
    { required: true, message: txt.value.required, trigger: 'blur' },
    { type: 'email', message: txt.value.emailFmt, trigger: 'blur' }
  ],
  code: [{ required: true, message: txt.value.required, trigger: 'blur' }],
  password: [
    { required: true, message: txt.value.required, trigger: 'blur' },
    { min: 6, max: 32, message: txt.value.pwdLen, trigger: 'blur' }
  ]
}))

const loginTo = computed(() =>
  route.query.redirect ? { path: '/login', query: { redirect: route.query.redirect } } : '/login'
)

async function sendCode() {
  await formRef.value.validateField('email')
  sending.value = true
  try {
    await client.post('/auth/email-code', { email: form.value.email, scene: 'register' })
    ElMessage.success(txt.value.codeSent)
    cd.value = 60
    timer = setInterval(() => {
      cd.value -= 1
      if (cd.value <= 0 && timer) { clearInterval(timer); timer = null }
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function onRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    const body = { ...form.value }
    if (!body.nickname) delete body.nickname
    const r = await client.post('/auth/register', body)
    app.setLogin(r.token, r.user)
    router.replace('/')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.auth-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: #f6f6f6; padding: 24px;
}
.auth-card {
  width: 420px; max-width: 100%; background: #fff; padding: 44px 40px 36px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}
.auth-logo {
  display: block; text-align: center; font-size: 28px; font-weight: 900;
  letter-spacing: 4px; color: var(--brand); margin-bottom: 8px;
}
.auth-title {
  text-align: center; font-size: 16px; font-weight: 600; color: #555;
  letter-spacing: 1px; margin-bottom: 26px;
}
.auth-submit { width: 100%; font-weight: 700; letter-spacing: 1px; margin-top: 6px; }
.code-row { display: flex; gap: 10px; width: 100%; }
.code-row .el-input { flex: 1; }
.auth-links { display: flex; justify-content: center; margin-top: 18px; font-size: 13px; }
.auth-link { color: #666; cursor: pointer; }
.auth-link:hover { color: var(--brand-accent); }
</style>

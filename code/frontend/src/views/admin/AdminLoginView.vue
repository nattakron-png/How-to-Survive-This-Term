<script setup>
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { login } from '@/mock/auth'
import { useAuth } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const { setUser } = useAuth()

const username = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = 'กรุณากรอกชื่อผู้ใช้และรหัสผ่าน'
    return
  }
  submitting.value = true
  try {
    const user = await login(username.value, password.value)
    setUser(user)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/admin')
      ? route.query.redirect
      : '/admin'
    router.replace(redirect)
  } catch {
    error.value = 'ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง'
    password.value = ''
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <form class="card" novalidate @submit.prevent="submit">
      <div class="logo">
        <span class="logo-mark">T</span>
        <span class="logo-text">Tournament Hub</span>
      </div>

      <div class="heading">
        <h1 class="title">เข้าสู่ระบบผู้ดูแล</h1>
        <p class="subtitle">สำหรับผู้ดูแลรายการแข่งเท่านั้น ผู้ชมไม่ต้องเข้าสู่ระบบ</p>
      </div>

      <div v-if="error" class="error" role="alert">
        <span class="error-icon" aria-hidden="true">✕</span>
        <span>{{ error }}</span>
      </div>

      <label class="field">
        <span class="label">ชื่อผู้ใช้</span>
        <input
          v-model="username"
          class="input"
          :class="{ invalid: error }"
          type="text"
          name="username"
          autocomplete="username"
          :aria-invalid="Boolean(error)"
        />
      </label>

      <label class="field">
        <span class="label">รหัสผ่าน</span>
        <input
          v-model="password"
          class="input"
          :class="{ invalid: error }"
          type="password"
          name="password"
          autocomplete="current-password"
          :aria-invalid="Boolean(error)"
        />
      </label>

      <button type="submit" class="submit" :disabled="submitting">
        {{ submitting ? 'กำลังเข้าสู่ระบบ…' : 'เข้าสู่ระบบ' }}
      </button>

      <RouterLink to="/" class="back">← กลับไปหน้าผู้ชม</RouterLink>
    </form>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 520px;
  max-width: 100%;
  padding: 48px;
  border: 1px solid var(--color-surface-2);
  border-radius: 20px;
  background: var(--color-surface);
}
.logo { display: flex; align-items: center; gap: 12px; }
.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  color: var(--color-on-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 22px;
}
.logo-text { font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.heading { display: flex; flex-direction: column; gap: 6px; }
.title { font-family: var(--font-heading); font-weight: 600; font-size: 32px; }
.subtitle { color: var(--color-muted); font-size: 15px; }

.error {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: rgba(255, 115, 115, 0.12);
  color: #ff7373;
  font-size: 15px;
}
.error-icon { font-size: 14px; font-weight: 600; line-height: 1.6; }

.field { display: flex; flex-direction: column; gap: 8px; }
.label { color: var(--color-muted); font-size: 14px; font-weight: 500; }
.input {
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  outline: 0;
  background: var(--color-bg);
  color: var(--color-text);
  font: inherit;
  font-size: 16px;
}
.input:focus { border-color: var(--color-accent); }
.input.invalid { border: 1.5px solid #ff7373; padding: 13.5px 15.5px; }

.submit {
  padding: 14px;
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  color: var(--color-on-accent);
  font-size: 16px;
  font-weight: 600;
}
.submit:hover:not(:disabled) { filter: brightness(0.95); }
.submit:disabled { opacity: 0.6; cursor: default; }
.back { align-self: flex-start; color: var(--color-muted); font-size: 14px; font-weight: 500; }
.back:hover { color: var(--color-text); }

@media (max-width: 640px) {
  .card { padding: 28px 20px; }
  .title { font-size: 26px; }
}
</style>

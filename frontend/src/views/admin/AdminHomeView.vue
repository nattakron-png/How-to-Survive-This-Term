<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { currentUser, logout } = useAuth()

function signOut() {
  logout()
  router.replace('/admin/login')
}
</script>

<template>
  <main class="page">
    <div class="box">
      <h1 class="title">แดชบอร์ดผู้ดูแล</h1>
      <p class="text">เข้าสู่ระบบในชื่อ {{ currentUser?.username }} · หน้านี้รอ design</p>
      <button type="button" class="logout" @click="signOut">ออกจากระบบ</button>
    </div>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 40px 16px; }
.box { display: flex; flex-direction: column; align-items: center; gap: 12px; text-align: center; }
.title { font-family: var(--font-heading); font-weight: 600; font-size: 32px; }
.text { color: var(--color-muted); font-size: 15px; }
.logout {
  padding: 10px 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 15px;
}
</style>

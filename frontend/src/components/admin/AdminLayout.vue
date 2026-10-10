<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import '@/assets/admin.css'

const route = useRoute()
const router = useRouter()
const { currentUser, logout } = useAuth()

const menu = [
  { key: 'overview', label: 'ภาพรวม', icon: '◧', to: '/admin', exact: true },
  { key: 'tournaments', label: 'รายการแข่ง', icon: '☰', to: '/admin/tournaments' },
  { key: 'teams', label: 'ทีม', icon: '◉', to: '/admin/teams' },
  { key: 'players', label: 'ผู้เล่น', icon: '◎', to: '/admin/players' },
  { key: 'matches', label: 'แมตช์และผลการแข่ง', icon: '⚑', to: '/admin/matches' },
]

const isActive = (item) => {
  if (item.exact) return route.path === item.to
  if (item.key === 'matches') return route.path.startsWith(item.to) || /^\/admin\/tournaments\/[^/]+\/games/.test(route.path)
  if (item.key === 'tournaments') return route.path.startsWith(item.to) && !/\/games$/.test(route.path)
  return route.path.startsWith(item.to)
}

const displayName = computed(() => {
  const name = currentUser.value?.username ?? 'admin'
  return name.charAt(0).toUpperCase() + name.slice(1)
})
const avatar = computed(() => displayName.value.slice(0, 2).toUpperCase())

function signOut() {
  logout()
  router.replace('/admin/login')
}
</script>

<template>
  <div class="admin">
    <aside class="sidebar">
      <RouterLink to="/admin" class="brand">
        <span class="brand-mark">T</span>
        <span class="brand-text">
          <span class="brand-name">Tournament Hub</span>
          <span class="brand-role">ผู้ดูแลระบบ</span>
        </span>
      </RouterLink>

      <nav class="menu">
        <template v-for="item in menu" :key="item.key">
          <RouterLink v-if="item.to" :to="item.to" class="menu-item" :class="{ 'is-active': isActive(item) }">
            <span class="menu-icon" aria-hidden="true">{{ item.icon }}</span>
            {{ item.label }}
          </RouterLink>
          <span v-else class="menu-item is-disabled" title="ยังไม่มีหน้านี้">
            <span class="menu-icon" aria-hidden="true">{{ item.icon }}</span>
            {{ item.label }}
          </span>
        </template>
      </nav>

      <div class="account">
        <span class="avatar">{{ avatar }}</span>
        <span class="account-text">
          <span class="account-name">{{ displayName }}</span>
          <button type="button" class="logout" @click="signOut">ออกจากระบบ</button>
        </span>
      </div>
    </aside>

    <main class="content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.admin { display: flex; min-height: 100vh; }

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 32px;
  width: 280px;
  height: 100vh;
  flex-shrink: 0;
  padding: 32px 20px;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
}

.brand { display: flex; align-items: center; gap: 12px; padding: 0 8px; }
.brand-mark {
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
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-family: var(--font-heading); font-weight: 600; font-size: 18px; line-height: 1.3; }
.brand-role { color: var(--color-muted); font-size: 12px; }

.menu { display: flex; flex-direction: column; gap: 4px; }
.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  color: var(--color-muted);
  font-size: 16px;
  white-space: nowrap;
}
.menu-item:not(.is-disabled):hover { background: var(--color-surface-2); color: var(--color-text); }
.menu-item.is-active { background: rgba(255, 253, 131, 0.12); color: var(--color-accent); font-weight: 600; }
.menu-item.is-disabled { cursor: not-allowed; opacity: 0.6; }
.menu-icon { width: 16px; text-align: center; font-size: 14px; }

.account {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: auto;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--color-surface-2);
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-pill);
  background: rgba(255, 253, 131, 0.2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
}
.account-text { display: flex; flex-direction: column; align-items: flex-start; min-width: 0; }
.account-name { font-weight: 600; font-size: 15px; }
.logout { padding: 0; border: 0; background: none; color: var(--color-muted); font-size: 12px; }
.logout:hover { color: var(--color-danger); }

.content { flex: 1; min-width: 0; padding: 40px 48px; }

@media (max-width: 1024px) {
  .admin { flex-direction: column; }
  .sidebar {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px 20px;
    width: 100%;
    height: auto;
    padding: 16px;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }
  .brand { padding: 0; }
  .account { order: 2; margin: 0 0 0 auto; padding: 6px 12px 6px 6px; background: none; }
  .avatar { width: 36px; height: 36px; font-size: 13px; }
  .menu {
    order: 3;
    flex-direction: row;
    width: 100%;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .menu::-webkit-scrollbar { display: none; }
  .menu-item { padding: 10px 14px; font-size: 14px; }
  .content { padding: 24px 16px 40px; }
}
</style>

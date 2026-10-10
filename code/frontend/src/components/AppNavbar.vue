<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const isTournamentsActive = computed(() => route.path.startsWith('/tournaments'))

const query = ref(route.query.q ?? '')
watch(() => route.query.q, (q) => { query.value = q ?? '' })

function submitSearch() {
  const q = query.value.trim()
  router.push({ name: 'tournaments', query: q ? { q } : {} })
}
</script>

<template>
  <header class="navbar">
    <RouterLink to="/" class="logo">
      <span class="logo-mark">T</span>
      <span class="logo-text">Tournament Hub</span>
    </RouterLink>

    <nav class="menu">
      <RouterLink to="/" class="menu-link" exact-active-class="is-active">หน้าแรก</RouterLink>
      <RouterLink to="/tournaments" class="menu-link" :class="{ 'is-active': isTournamentsActive }">รายการแข่ง</RouterLink>
    </nav>

    <form class="search" role="search" @submit.prevent="submitSearch">
      <span class="search-icon" aria-hidden="true">⌕</span>
      <input v-model="query" type="search" placeholder="ค้นหารายการแข่ง…" aria-label="ค้นหารายการแข่ง" />
    </form>
  </header>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  height: 88px;
  padding: 0 var(--page-gutter);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
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
.logo-text {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 24px;
  white-space: nowrap;
}

.menu { display: flex; gap: 40px; font-size: 18px; }
.menu-link { color: var(--color-muted); font-weight: 500; white-space: nowrap; }
.menu-link.is-active { color: var(--color-accent); font-weight: 600; }

.search {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 320px;
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-muted);
}
.search-icon { font-size: 18px; }
.search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--color-text);
  font: inherit;
  font-size: 15px;
}
.search input::placeholder { color: var(--color-muted); }
.search:focus-within { border-color: var(--color-accent); }

@media (max-width: 900px) {
  .navbar { height: auto; flex-wrap: wrap; padding-top: 16px; padding-bottom: 16px; }
  .search { width: 100%; order: 3; }
}
</style>

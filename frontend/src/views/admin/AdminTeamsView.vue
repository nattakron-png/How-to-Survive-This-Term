<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import AppPagination from '@/components/AppPagination.vue'
import FilterChip from '@/components/FilterChip.vue'
import TeamLogo from '@/components/TeamLogo.vue'
import { deleteTeam, getAdminTeams } from '@/mock/admin'
import { getGames } from '@/mock/queries'
import { formatDate } from '@/utils/format'

const PAGE_SIZE = 8

const games = getGames()
const version = ref(0)
const all = computed(() => {
  version.value
  return getAdminTeams()
})

const summary = computed(() =>
  [
    `ทั้งหมด ${all.value.length} ทีม`,
    ...games.map((g) => `${g.name} ${all.value.filter((t) => t.gameId === g.id).length}`),
  ].join(' · '),
)

const query = ref('')
const gameFilter = ref('all')
const page = ref(1)

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  return all.value
    .filter((t) => gameFilter.value === 'all' || t.gameId === gameFilter.value)
    .filter((t) => !q || t.name.toLowerCase().includes(q) || t.playerNames.some((n) => n.toLowerCase().includes(q)))
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt) || a.name.localeCompare(b.name))
})
const rows = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))

const matchedPlayer = (team) => {
  const q = query.value.trim().toLowerCase()
  if (!q || team.name.toLowerCase().includes(q)) return null
  return team.playerNames.find((n) => n.toLowerCase().includes(q)) ?? null
}

watch([query, gameFilter], () => { page.value = 1 })
watch(filtered, (list) => {
  const last = Math.max(1, Math.ceil(list.length / PAGE_SIZE))
  if (page.value > last) page.value = last
})

const confirmingId = ref(null)
const deletingId = ref(null)

async function confirmDelete(team) {
  deletingId.value = team.id
  try {
    await deleteTeam(team.id)
    version.value += 1
  } finally {
    deletingId.value = null
    confirmingId.value = null
  }
}
</script>

<template>
  <AdminLayout>
    <div class="admin-page">
      <header class="head">
        <div>
          <h1 class="admin-title">จัดการทีม</h1>
          <p class="admin-subtitle">{{ summary }}</p>
        </div>
        <RouterLink to="/admin/teams/new" class="admin-btn admin-btn-accent">+ เพิ่มทีม</RouterLink>
      </header>

      <div class="toolbar">
        <label class="search">
          <span aria-hidden="true">⌕</span>
          <input v-model="query" type="search" placeholder="ค้นหาชื่อทีมหรือผู้เล่น…" aria-label="ค้นหาชื่อทีมหรือผู้เล่น" />
        </label>
        <div class="chips" role="group" aria-label="กรองตามเกม">
          <FilterChip label="ทุกเกม" variant="outlined" :active="gameFilter === 'all'" @click="gameFilter = 'all'" />
          <FilterChip
            v-for="g in games"
            :key="g.id"
            :label="g.name"
            variant="outlined"
            :active="gameFilter === g.id"
            @click="gameFilter = g.id"
          />
        </div>
      </div>

      <div class="table-wrap">
        <table class="table">
          <colgroup>
            <col class="c-name" />
            <col class="c-game" />
            <col class="c-players" />
            <col class="c-tournaments" />
            <col class="c-date" />
            <col class="c-actions" />
          </colgroup>
          <thead>
            <tr>
              <th>ทีม</th>
              <th>เกม</th>
              <th>ผู้เล่น</th>
              <th>รายการที่ลงแข่ง</th>
              <th>สร้างเมื่อ</th>
              <th>จัดการ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="team in rows" :key="team.id">
              <td>
                <span class="name-cell">
                  <TeamLogo :team="team" :size="32" :font-size="11" class="logo" />
                  <span class="name-text">
                    <RouterLink :to="`/admin/teams/${team.id}/edit`" class="name">{{ team.name }}</RouterLink>
                    <span v-if="matchedPlayer(team)" class="match">ผู้เล่น: {{ matchedPlayer(team) }}</span>
                  </span>
                </span>
              </td>
              <td class="muted">{{ team.game?.name }}</td>
              <td :class="{ short: team.playerCount < (team.game?.minPlayers ?? 1) }">{{ team.playerCount }} คน</td>
              <td>{{ team.tournamentCount }} รายการ</td>
              <td class="muted">{{ formatDate(team.createdAt) }}</td>
              <td>
                <span v-if="confirmingId === team.id" class="actions">
                  <button type="button" class="act danger" :disabled="deletingId === team.id" @click="confirmDelete(team)">
                    {{ deletingId === team.id ? 'กำลังลบ…' : 'ยืนยันลบ' }}
                  </button>
                  <button type="button" class="act" @click="confirmingId = null">ยกเลิก</button>
                </span>
                <span v-else class="actions">
                  <RouterLink :to="`/admin/teams/${team.id}/edit`" class="act accent">แก้ไข</RouterLink>
                  <button v-if="team.canDelete" type="button" class="act danger" @click="confirmingId = team.id">ลบ</button>
                </span>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="6" class="empty">ไม่พบทีมที่ตรงกับเงื่อนไข</td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination v-model="page" :total-items="filtered.length" :page-size="PAGE_SIZE" unit="ทีม" note="ลบได้เฉพาะทีมที่ยังไม่เคยลงแข่ง" class="pager" />
    </div>
  </AdminLayout>
</template>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }

.toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.search {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 420px;
  max-width: 100%;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-muted);
}
.search:focus-within { border-color: var(--color-accent); }
.search input { flex: 1; min-width: 0; padding: 14px 0; border: 0; background: none; color: var(--color-text); font: inherit; font-size: 15px; }
.search input:focus { outline: 0; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; }

.table-wrap { overflow-x: auto; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); }
.table { width: 100%; min-width: 900px; border-collapse: collapse; table-layout: fixed; }
.c-game { width: 140px; }
.c-players { width: 120px; }
.c-tournaments { width: 160px; }
.c-date { width: 170px; }
.c-actions { width: 160px; }
th { padding: 14px 24px; background: var(--color-surface-2); color: var(--color-muted); font-size: 13px; font-weight: 500; text-align: left; }
td { padding: 14px 24px; border-top: 1px solid var(--color-border); font-size: 15px; vertical-align: middle; }
.muted { color: var(--color-muted); }
.short { color: var(--color-danger); }
.name-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.logo { border-radius: 8px; }
.name-text { display: flex; flex-direction: column; min-width: 0; }
.name { overflow: hidden; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.name:hover { color: var(--color-accent); }
.match { color: var(--color-muted); font-size: 12px; }

.actions { display: flex; align-items: center; gap: 14px; }
.act { padding: 0; border: 0; background: none; color: var(--color-text); font-size: 14px; font-weight: 600; cursor: pointer; white-space: nowrap; }
.act:hover { text-decoration: underline; }
.act.accent { color: var(--color-accent); }
.act.danger { color: var(--color-danger); }
.act:disabled { opacity: 0.6; cursor: default; }
.empty { padding: 48px 24px; color: var(--color-muted); text-align: center; }
.pager :deep(.summary) { font-size: 14px; }

@media (max-width: 1024px) {
  .c-name { width: 260px; }
}

@media (max-width: 640px) {
  .search { width: 100%; }
  .chips { flex-wrap: nowrap; overflow-x: auto; scrollbar-width: none; width: 100%; }
  .chips::-webkit-scrollbar { display: none; }
  th, td { padding: 12px 16px; }
}
</style>

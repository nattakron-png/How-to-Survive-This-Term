<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import PlayerDialog from '@/components/admin/PlayerDialog.vue'
import AppPagination from '@/components/AppPagination.vue'
import SelectBox from '@/components/SelectBox.vue'
import TeamLogo from '@/components/TeamLogo.vue'
import { deletePlayer, getAdminPlayers, getAdminTeams } from '@/mock/admin'
import { getGames } from '@/mock/queries'

const PAGE_SIZE = 8

const games = getGames()
const version = ref(0)
const all = computed(() => {
  version.value
  return getAdminPlayers()
})
const teams = computed(() => {
  version.value
  return getAdminTeams()
})
const freeCount = computed(() => all.value.filter((p) => !p.team).length)

const query = ref('')
const teamFilter = ref('all')
const gameFilter = ref('all')
const onlyFree = ref(false)
const page = ref(1)

const teamOptions = computed(() => [
  { value: 'all', label: 'ทีม: ทั้งหมด' },
  ...[...teams.value].sort((a, b) => a.name.localeCompare(b.name)).map((t) => ({ value: String(t.id), label: `ทีม: ${t.name}` })),
])
const gameOptions = [
  { value: 'all', label: 'เกม: ทั้งหมด' },
  ...games.map((g) => ({ value: String(g.id), label: `เกม: ${g.name}` })),
]

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  return all.value
    .filter((p) => !q || p.name.toLowerCase().includes(q))
    .filter((p) => teamFilter.value === 'all' || String(p.teamId) === teamFilter.value)
    .filter((p) => gameFilter.value === 'all' || String(p.game?.id) === gameFilter.value)
    .filter((p) => !onlyFree.value || !p.team)
    .sort((a, b) => Number(!a.team) - Number(!b.team) || (a.teamId ?? 0) - (b.teamId ?? 0) || a.id - b.id)
})
const rows = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))

function toggleFree() {
  onlyFree.value = !onlyFree.value
  if (onlyFree.value) {
    teamFilter.value = 'all'
    gameFilter.value = 'all'
  }
}
watch([teamFilter, gameFilter], ([team, game]) => {
  if (team !== 'all' || game !== 'all') onlyFree.value = false
})
watch([query, teamFilter, gameFilter, onlyFree], () => { page.value = 1 })
watch(filtered, (list) => {
  const last = Math.max(1, Math.ceil(list.length / PAGE_SIZE))
  if (page.value > last) page.value = last
})

function avatarText(name) {
  const base = name.includes('.') ? name.split('.').pop() : name
  const letters = base.replace(/[^A-Za-z0-9ก-๙]/g, '')
  return (letters || name).slice(0, 2).toUpperCase()
}

const dialogOpen = ref(false)
const editing = ref(null)
const flash = ref('')

function openCreate() {
  editing.value = null
  dialogOpen.value = true
}
function openEdit(player) {
  editing.value = player
  dialogOpen.value = true
}
function onSaved(player) {
  flash.value = editing.value ? `บันทึก ${player.name} แล้ว` : `เพิ่ม ${player.name} แล้ว`
  dialogOpen.value = false
  version.value += 1
}

const confirmingId = ref(null)
const deletingId = ref(null)

async function confirmDelete(player) {
  deletingId.value = player.id
  try {
    await deletePlayer(player.id)
    flash.value = `ลบ ${player.name} แล้ว`
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
          <h1 class="admin-title">จัดการผู้เล่น</h1>
          <p class="admin-subtitle">ทั้งหมด {{ all.length }} คน · ยังไม่มีทีม {{ freeCount }} คน</p>
        </div>
        <button type="button" class="admin-btn admin-btn-accent" @click="openCreate">+ เพิ่มผู้เล่น</button>
      </header>

      <div class="toolbar">
        <label class="search">
          <span aria-hidden="true">⌕</span>
          <input v-model="query" type="search" placeholder="ค้นหาชื่อผู้เล่น…" aria-label="ค้นหาชื่อผู้เล่น" />
        </label>
        <SelectBox v-model="teamFilter" :options="teamOptions" aria-label="กรองตามทีม" />
        <SelectBox v-model="gameFilter" :options="gameOptions" aria-label="กรองตามเกม" />
        <button type="button" class="free-toggle" :class="{ active: onlyFree }" :aria-pressed="onlyFree" @click="toggleFree">
          เฉพาะผู้เล่นที่ยังไม่มีทีม
        </button>
      </div>

      <p v-if="flash" class="flash" role="status">{{ flash }}</p>

      <div class="table-wrap">
        <table class="table">
          <colgroup>
            <col class="c-name" />
            <col class="c-role" />
            <col class="c-team" />
            <col class="c-game" />
            <col class="c-actions" />
          </colgroup>
          <thead>
            <tr>
              <th>ผู้เล่น</th>
              <th>ตำแหน่ง</th>
              <th>ทีม</th>
              <th>เกม</th>
              <th>จัดการ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in rows" :key="p.id">
              <td>
                <span class="name-cell">
                  <span class="avatar">{{ avatarText(p.name) }}</span>
                  <span class="name-text">
                    <span class="name">{{ p.name }}</span>
                    <span v-if="p.description" class="desc">{{ p.description }}</span>
                  </span>
                </span>
              </td>
              <td>{{ p.role }}</td>
              <td>
                <RouterLink v-if="p.team" :to="`/admin/teams/${p.team.id}/edit`" class="team-cell">
                  <TeamLogo :team="p.team" :size="22" :font-size="8" class="team-logo" />
                  <span class="team-name">{{ p.team.name }}</span>
                </RouterLink>
                <span v-else class="admin-badge warning">ยังไม่มีทีม</span>
              </td>
              <td class="muted">{{ p.game?.name ?? '–' }}</td>
              <td>
                <span v-if="confirmingId === p.id" class="actions">
                  <button type="button" class="act danger" :disabled="deletingId === p.id" @click="confirmDelete(p)">
                    {{ deletingId === p.id ? 'กำลังลบ…' : 'ยืนยันลบ' }}
                  </button>
                  <button type="button" class="act" @click="confirmingId = null">ยกเลิก</button>
                </span>
                <span v-else class="actions">
                  <button type="button" class="act accent" @click="openEdit(p)">แก้ไข</button>
                  <button type="button" class="act danger" @click="confirmingId = p.id">ลบ</button>
                </span>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="5" class="empty">ไม่พบผู้เล่นที่ตรงกับเงื่อนไข</td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination
        v-model="page"
        :total-items="filtered.length"
        :page-size="PAGE_SIZE"
        unit="คน"
        note="สร้างผู้เล่นก่อนแล้วค่อยใส่ทีมได้ และลบทีมแล้วผู้เล่นยังอยู่"
        class="pager"
      />
    </div>

    <PlayerDialog
      v-if="dialogOpen"
      :player="editing"
      :teams="teams"
      :games="games"
      @close="dialogOpen = false"
      @saved="onSaved"
    />
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
.toolbar :deep(select) { max-width: 260px; text-overflow: ellipsis; }
.free-toggle {
  padding: 11px 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}
.free-toggle.active { border-color: transparent; background: var(--color-accent); color: var(--color-on-accent); }

.flash { padding: 10px 16px; border-radius: var(--radius-sm); background: var(--color-success-bg); color: var(--color-success); font-size: 14px; }

.table-wrap { overflow-x: auto; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); }
.table { width: 100%; min-width: 900px; border-collapse: collapse; table-layout: fixed; }
.c-role { width: 200px; }
.c-team { width: 280px; }
.c-game { width: 160px; }
.c-actions { width: 160px; }
th { padding: 14px 24px; background: var(--color-surface-2); color: var(--color-muted); font-size: 13px; font-weight: 500; text-align: left; }
td { padding: 12px 24px; border-top: 1px solid var(--color-border); font-size: 15px; vertical-align: middle; }
.muted { color: var(--color-muted); }

.name-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--radius-pill);
  background: rgba(255, 253, 131, 0.15);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 11px;
}
.name-text { display: flex; flex-direction: column; min-width: 0; }
.name { overflow: hidden; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.desc { overflow: hidden; color: var(--color-muted); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }

.team-cell { display: inline-flex; align-items: center; gap: 10px; max-width: 100%; }
.team-cell:hover .team-name { color: var(--color-accent); }
.team-logo { border-radius: 6px; }
.team-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.actions { display: flex; align-items: center; gap: 14px; }
.act { padding: 0; border: 0; background: none; color: var(--color-text); font-size: 14px; font-weight: 600; cursor: pointer; white-space: nowrap; }
.act:hover { text-decoration: underline; }
.act.accent { color: var(--color-accent); }
.act.danger { color: var(--color-danger); }
.act:disabled { opacity: 0.6; cursor: default; }
.empty { padding: 48px 24px; color: var(--color-muted); text-align: center; }
.pager :deep(.summary) { font-size: 14px; }

@media (max-width: 1024px) {
  .c-name { width: 240px; }
}

@media (max-width: 640px) {
  .search { width: 100%; }
  .toolbar :deep(.select) { flex: 1 1 calc(50% - 6px); }
  .toolbar :deep(select) { width: 100%; max-width: none; padding: 10px 32px 10px 12px; font-size: 14px; }
  .free-toggle { width: 100%; }
  th, td { padding: 12px 16px; }
}
</style>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import TournamentLogo from '@/components/admin/TournamentLogo.vue'
import AppPagination from '@/components/AppPagination.vue'
import SelectBox from '@/components/SelectBox.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { deleteTournament, getAdminTournaments } from '@/api/admin'
import { getGames } from '@/mock/queries'
import { formatDateRange } from '@/utils/format'

const PAGE_SIZE = 8

const version = ref(0)
const all = computed(() => {
  version.value
  return getAdminTournaments()
})

const query = ref('')
const gameFilter = ref('all')
const statusFilter = ref('all')
const sortBy = ref('date-desc')
const page = ref(1)

const gameOptions = [
  { value: 'all', label: 'เกม: ทั้งหมด' },
  ...getGames().map((g) => ({ value: String(g.id), label: `เกม: ${g.name}` })),
]
const statusOptions = [
  { value: 'all', label: 'สถานะ: ทั้งหมด' },
  { value: 'UPCOMING', label: 'สถานะ: กำลังจะเริ่ม' },
  { value: 'ONGOING', label: 'สถานะ: กำลังแข่ง' },
  { value: 'FINISHED', label: 'สถานะ: จบแล้ว' },
]
const sortOptions = [
  { value: 'date-desc', label: 'เรียงตาม: วันแข่ง' },
  { value: 'date-asc', label: 'เรียงตาม: วันแข่ง (เก่าก่อน)' },
  { value: 'created-desc', label: 'เรียงตาม: สร้างล่าสุด' },
  { value: 'name-asc', label: 'เรียงตาม: ชื่อ A–Z' },
]

const sorters = {
  'date-desc': (a, b) => b.startDate.localeCompare(a.startDate),
  'date-asc': (a, b) => a.startDate.localeCompare(b.startDate),
  'created-desc': (a, b) => b.createdAt.localeCompare(a.createdAt),
  'name-asc': (a, b) => a.name.localeCompare(b.name),
}

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  return all.value
    .filter((t) => !q || t.name.toLowerCase().includes(q))
    .filter((t) => gameFilter.value === 'all' || String(t.gameId) === gameFilter.value)
    .filter((t) => statusFilter.value === 'all' || t.status === statusFilter.value)
    .sort(sorters[sortBy.value])
})

const rows = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))

watch([query, gameFilter, statusFilter, sortBy], () => { page.value = 1 })
watch(filtered, (list) => {
  const last = Math.max(1, Math.ceil(list.length / PAGE_SIZE))
  if (page.value > last) page.value = last
})

const confirmingId = ref(null)
const deletingId = ref(null)

async function confirmDelete(t) {
  deletingId.value = t.id
  try {
    await deleteTournament(t.id)
    version.value += 1
  } catch (e) {
    window.alert(e.status ? `ลบไม่สำเร็จ: ${e.message}` : 'ลบไม่สำเร็จ ลองใหม่อีกครั้ง')
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
          <h1 class="admin-title">จัดการรายการแข่ง</h1>
          <p class="admin-subtitle">ทั้งหมด {{ all.length }} รายการ</p>
        </div>
        <RouterLink to="/admin/tournaments/new" class="admin-btn admin-btn-accent">+ สร้างรายการแข่ง</RouterLink>
      </header>

      <div class="toolbar">
        <label class="search">
          <span class="search-icon" aria-hidden="true">⌕</span>
          <input v-model="query" type="search" placeholder="ค้นหาชื่อรายการ…" aria-label="ค้นหาชื่อรายการ" />
        </label>
        <SelectBox v-model="gameFilter" :options="gameOptions" aria-label="กรองตามเกม" />
        <SelectBox v-model="statusFilter" :options="statusOptions" aria-label="กรองตามสถานะ" />
        <SelectBox v-model="sortBy" :options="sortOptions" aria-label="เรียงลำดับ" />
      </div>

      <div class="table-wrap">
        <table class="table">
          <colgroup>
            <col class="c-name" />
            <col class="c-game" />
            <col class="c-status" />
            <col class="c-teams" />
            <col class="c-date" />
            <col class="c-actions" />
          </colgroup>
          <thead>
            <tr>
              <th>รายการ</th>
              <th>เกม</th>
              <th>สถานะ</th>
              <th>ทีม</th>
              <th>วันแข่ง</th>
              <th>จัดการ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in rows" :key="t.id">
              <td>
                <span class="name-cell">
                  <TournamentLogo :name="t.name" :logo-url="t.logoUrl" />
                  <RouterLink :to="`/admin/tournaments/${t.id}/edit`" class="name">{{ t.name }}</RouterLink>
                </span>
              </td>
              <td class="muted">{{ t.game?.name }}</td>
              <td><StatusBadge :status="t.status" /></td>
              <td>{{ t.teamCount }} ทีม</td>
              <td class="date">{{ formatDateRange(t.startDate, t.endDate) }}</td>
              <td>
                <span v-if="confirmingId === t.id" class="actions">
                  <button type="button" class="act danger" :disabled="deletingId === t.id" @click="confirmDelete(t)">
                    {{ deletingId === t.id ? 'กำลังลบ…' : 'ยืนยันลบ' }}
                  </button>
                  <button type="button" class="act" @click="confirmingId = null">ยกเลิก</button>
                </span>
                <span v-else class="actions">
                  <RouterLink :to="`/admin/tournaments/${t.id}/edit`" class="act accent">แก้ไข</RouterLink>
                  <RouterLink :to="`/admin/tournaments/${t.id}/teams`" class="act">ทีม</RouterLink>
                  <button v-if="t.canDelete" type="button" class="act danger" @click="confirmingId = t.id">ลบ</button>
                </span>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="6" class="empty">ไม่พบรายการที่ตรงกับเงื่อนไข</td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination
        v-model="page"
        :total-items="filtered.length"
        :page-size="PAGE_SIZE"
        note="ลบได้เฉพาะรายการที่ยังไม่เริ่มแข่ง"
        class="pager"
      />
    </div>
  </AdminLayout>
</template>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }

.toolbar { display: flex; flex-wrap: wrap; gap: 12px; }
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
.search input {
  flex: 1;
  min-width: 0;
  padding: 14px 0;
  border: 0;
  background: none;
  color: var(--color-text);
  font: inherit;
  font-size: 15px;
}
.search input:focus { outline: 0; }

.table-wrap {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.table { width: 100%; min-width: 960px; border-collapse: collapse; table-layout: fixed; }
.c-name { width: auto; }
.c-game { width: 130px; }
.c-status { width: 160px; }
.c-teams { width: 110px; }
.c-date { width: 190px; }
.c-actions { width: 196px; }
th {
  padding: 14px 24px;
  background: var(--color-surface-2);
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 500;
  text-align: left;
}
td { padding: 14px 24px; border-top: 1px solid var(--color-border); font-size: 15px; vertical-align: middle; }
.muted { color: var(--color-muted); }
.date { color: var(--color-muted); white-space: nowrap; }
.name-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.name { overflow: hidden; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.name:hover { color: var(--color-accent); }

.actions { display: flex; align-items: center; gap: 14px; }
.act { padding: 0; border: 0; background: none; color: var(--color-text); font-size: 14px; font-weight: 600; cursor: pointer; white-space: nowrap; }
.act:hover { text-decoration: underline; }
.act.accent { color: var(--color-accent); }
.act.danger { color: var(--color-danger); }
.act:disabled { opacity: 0.6; cursor: default; }
.empty { padding: 48px 24px; color: var(--color-muted); text-align: center; }

.pager :deep(.summary) { font-size: 14px; }

@media (max-width: 1024px) {
  .c-name { width: 280px; }
}

@media (max-width: 640px) {
  .search { width: 100%; }
  .toolbar :deep(.select) { flex: 1 1 calc(50% - 6px); }
  .toolbar :deep(select) { width: 100%; padding: 10px 32px 10px 12px; font-size: 14px; }
  th, td { padding: 12px 16px; }
}
</style>

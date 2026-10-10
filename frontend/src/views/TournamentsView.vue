<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppNavbar from '@/components/AppNavbar.vue'
import FilterChip from '@/components/FilterChip.vue'
import SelectBox from '@/components/SelectBox.vue'
import TournamentCard from '@/components/TournamentCard.vue'
import AppPagination from '@/components/AppPagination.vue'
import { getGames, getTournaments } from '@/mock/queries'

const PAGE_SIZE = 8
const ALL = 'ALL'

const route = useRoute()
const searchQuery = computed(() => String(route.query.q ?? '').trim().toLowerCase())

const selectedGame = ref(route.query.game ? String(route.query.game) : ALL)
watch(() => route.query.game, (game) => { selectedGame.value = game ? String(game) : ALL })
const selectedStatus = ref(ALL)
const sortBy = ref('date-desc')
const page = ref(1)

const tournaments = getTournaments()
const gameOptions = [{ value: ALL, label: 'ทุกเกม' }, ...getGames().map((g) => ({ value: g.code, label: g.name }))]

const statusOptions = [
  { value: ALL, label: 'สถานะ: ทั้งหมด' },
  { value: 'ONGOING', label: 'สถานะ: กำลังแข่ง' },
  { value: 'UPCOMING', label: 'สถานะ: กำลังจะเริ่ม' },
  { value: 'FINISHED', label: 'สถานะ: จบแล้ว' },
]

const sortOptions = [
  { value: 'date-desc', label: 'เรียงตาม: วันเริ่มแข่ง (ใหม่ → เก่า)' },
  { value: 'date-asc', label: 'เรียงตาม: วันเริ่มแข่ง (เก่า → ใหม่)' },
  { value: 'name-asc', label: 'เรียงตาม: ชื่อ (A → Z)' },
]

const SORTERS = {
  'date-desc': (a, b) => b.startDate.localeCompare(a.startDate),
  'date-asc': (a, b) => a.startDate.localeCompare(b.startDate),
  'name-asc': (a, b) => a.name.localeCompare(b.name),
}

const filtered = computed(() =>
  tournaments
    .filter((t) => selectedGame.value === ALL || t.game.code === selectedGame.value)
    .filter((t) => selectedStatus.value === ALL || t.status === selectedStatus.value)
    .filter((t) => !searchQuery.value || t.name.toLowerCase().includes(searchQuery.value))
    .toSorted(SORTERS[sortBy.value]),
)

const paged = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))

watch([selectedGame, selectedStatus, sortBy, searchQuery], () => { page.value = 1 })
</script>

<template>
  <div class="page">
    <AppNavbar />

    <main class="content">
      <div class="page-header">
        <h1 class="title">รายการแข่งทั้งหมด</h1>
        <span class="count">{{ filtered.length }} รายการ</span>
      </div>
      <p v-if="searchQuery" class="search-note">ผลการค้นหา “{{ route.query.q }}”</p>

      <div class="toolbar">
        <div class="filters">
          <div class="chips" role="group" aria-label="กรองตามเกม">
            <FilterChip
              v-for="opt in gameOptions"
              :key="opt.value"
              :label="opt.label"
              variant="outlined"
              :active="selectedGame === opt.value"
              @click="selectedGame = opt.value"
            />
          </div>
          <SelectBox v-model="selectedStatus" :options="statusOptions" aria-label="กรองตามสถานะ" />
        </div>
        <SelectBox v-model="sortBy" :options="sortOptions" aria-label="เรียงลำดับ" />
      </div>

      <template v-if="filtered.length">
        <div class="card-grid">
          <TournamentCard v-for="t in paged" :key="t.id" :tournament="t" />
        </div>
        <AppPagination v-model="page" :total-items="filtered.length" :page-size="PAGE_SIZE" />
      </template>
      <p v-else class="empty">ไม่พบรายการแข่งที่ตรงกับตัวกรอง</p>
    </main>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; display: flex; flex-direction: column; }

.content {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 40px var(--page-gutter);
}

.page-header { display: flex; align-items: baseline; gap: 16px; flex-wrap: wrap; }
.title { font-family: var(--font-heading); font-weight: 600; font-size: 40px; }
.count { color: var(--color-muted); font-size: 18px; }
.search-note { margin-top: -12px; color: var(--color-muted); font-size: 15px; }

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.filters { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.empty {
  padding: 48px 24px;
  text-align: center;
  color: var(--color-muted);
  font-size: 15px;
}

@media (max-width: 640px) {
  .title { font-size: 30px; }
}
</style>

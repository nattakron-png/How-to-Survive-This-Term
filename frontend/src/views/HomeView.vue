<script setup>
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppNavbar from '@/components/AppNavbar.vue'
import MatchItem from '@/components/MatchItem.vue'
import TournamentCard from '@/components/TournamentCard.vue'
import FilterChip from '@/components/FilterChip.vue'
import { TODAY, getGames, getLatestTournaments, getMatchesOn } from '@/mock/queries'
import { formatDate } from '@/utils/format'

const ALL = 'ALL'
const games = getGames()
const gameOptions = [{ code: ALL, name: 'ทั้งหมด' }, ...games]

const todayDate = formatDate(TODAY)
const todayMatches = getMatchesOn(TODAY)
const latestTournaments = getLatestTournaments(4)

const selectedGame = ref(ALL)
const filteredTournaments = computed(() =>
  selectedGame.value === ALL
    ? latestTournaments
    : latestTournaments.filter((t) => t.game.code === selectedGame.value),
)
</script>

<template>
  <div class="page">
    <AppNavbar />

    <main class="content">
      <section class="hero">
        <div class="hero-text">
          <p class="eyebrow">ศูนย์รวมข้อมูลการแข่งขัน ROV, Free Fire, Valorant และ Fighting Game</p>
          <h1 class="headline">ติดตามทุกรายการแข่ง<br />อีสปอร์ตในที่เดียว</h1>
          <p class="subtitle">ดูสายการแข่ง ผลแมตช์ และข้อมูลทีม ไม่ต้องสมัครสมาชิก</p>
          <RouterLink to="/tournaments" class="btn-primary">ดูรายการแข่ง</RouterLink>
        </div>

        <div class="today">
          <div class="today-header">
            <h2 class="today-title">แมตช์วันนี้</h2>
            <span class="today-date">{{ todayDate }}</span>
          </div>
          <template v-if="todayMatches.length">
            <MatchItem v-for="match in todayMatches" :key="match.id" :match="match" />
          </template>
          <p v-else class="empty">วันนี้ไม่มีแมตช์</p>
        </div>
      </section>

      <section class="tournaments">
        <div class="section-header">
          <div class="section-left">
            <h2 class="section-title">รายการแข่งล่าสุด</h2>
            <div class="chips" role="group" aria-label="กรองตามเกม">
              <FilterChip
                v-for="game in gameOptions"
                :key="game.code"
                :label="game.name"
                :active="selectedGame === game.code"
                @click="selectedGame = game.code"
              />
            </div>
          </div>
          <RouterLink to="/tournaments" class="see-all">ดูทั้งหมด →</RouterLink>
        </div>

        <div v-if="filteredTournaments.length" class="card-grid">
          <TournamentCard v-for="t in filteredTournaments" :key="t.id" :tournament="t" />
        </div>
        <p v-else class="empty">ยังไม่มีรายการแข่งของเกมนี้</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; display: flex; flex-direction: column; }

.content {
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding: 40px var(--page-gutter);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding: 48px;
  border-radius: var(--radius-xl);
  background: var(--color-surface);
}
.hero-text { display: flex; flex-direction: column; align-items: flex-start; gap: 16px; }
.eyebrow { color: var(--color-accent); font-size: 16px; font-weight: 600; }
.headline {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 50px;
  line-height: 1.2;
}
.subtitle { color: var(--color-muted); font-size: 18px; }
.btn-primary {
  padding: 12px 24px;
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  color: var(--color-on-accent);
  font-size: 18px;
  font-weight: 600;
}
.btn-primary:hover { filter: brightness(0.95); }

.today {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 640px;
  max-width: 100%;
  flex-shrink: 0;
  padding: 24px;
  border-radius: var(--radius-lg);
  background: var(--color-bg);
}
.today-header { display: flex; align-items: center; justify-content: space-between; }
.today-title { font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.today-date { color: var(--color-muted); font-size: 14px; }

.tournaments { display: flex; flex-direction: column; gap: 20px; }
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.section-left { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
.section-title { font-family: var(--font-heading); font-weight: 600; font-size: 30px; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; }
.see-all { color: var(--color-accent); font-size: 17px; font-weight: 600; }

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.empty {
  padding: 24px;
  text-align: center;
  color: var(--color-muted);
  font-size: 15px;
}

@media (max-width: 1280px) {
  .hero { flex-direction: column; align-items: stretch; }
  .today { width: 100%; }
}
@media (max-width: 640px) {
  .hero { padding: 24px; }
  .headline { font-size: 34px; }
}
</style>

<style scoped>
@media (max-width: 640px) {
  .today { padding: 16px; }
}
</style>

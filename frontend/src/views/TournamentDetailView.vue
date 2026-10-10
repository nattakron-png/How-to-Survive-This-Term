<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppNavbar from '@/components/AppNavbar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import ParticipantCard from '@/components/ParticipantCard.vue'
import ResultRow from '@/components/ResultRow.vue'
import TournamentBracket from '@/components/TournamentBracket.vue'
import MatchRoundList from '@/components/MatchRoundList.vue'
import TeamDrawer from '@/components/TeamDrawer.vue'
import FreeFireStandings from '@/components/FreeFireStandings.vue'
import {
  getFreeFireStandings,
  getNextFreeFireGame,
  getPlacementPoints,
  getTournament,
  getTournamentMatches,
} from '@/mock/queries'
import { formatDate, formatDateRange, formatTime, formatTournamentFormat, initials } from '@/utils/format'

const props = defineProps({
  id: { type: String, required: true },
})

const RECENT_LIMIT = 5

const tournament = computed(() => getTournament(props.id))

const isPoints = computed(() => tournament.value?.format === 'POINTS')

const tabs = computed(() =>
  isPoints.value
    ? [
        { key: 'overview', label: 'ภาพรวม' },
        { key: 'standings', label: 'ตารางคะแนน' },
      ]
    : [
        { key: 'overview', label: 'ภาพรวม' },
        { key: 'bracket', label: 'สายการแข่ง' },
        { key: 'matches', label: 'แมตช์ทั้งหมด' },
      ],
)

const standings = computed(() => (isPoints.value ? getFreeFireStandings(props.id) : null))
const placementPoints = computed(() => (isPoints.value ? getPlacementPoints(props.id) : []))
const nextGame = computed(() => (isPoints.value ? getNextFreeFireGame(props.id) : null))

const pointsSummary = computed(() => {
  if (!isPoints.value) return ''
  const t = tournament.value
  const { gamesCompleted, totalGames } = standings.value
  const progress = gamesCompleted >= totalGames
    ? `แข่งครบ ${gamesCompleted} จาก ${totalGames} เกม`
    : `แข่งแล้ว ${gamesCompleted} จาก ${totalGames} เกม`
  return [`${t.teams.length} ทีม`, formatDateRange(t.startDate, t.endDate), progress].join('  ·  ')
})
const route = useRoute()
const router = useRouter()
const requestedTab = String(route.query.tab ?? 'overview')
const activeTab = ref(tabs.value.some((t) => t.key === requestedTab) ? requestedTab : 'overview')
watch(activeTab, (tab) => {
  router.replace({ query: tab === 'overview' ? {} : { tab } })
})

const legend = [
  { label: 'จบแล้ว', tone: 'success' },
  { label: 'รอแข่ง', tone: 'info' },
  { label: 'รอคู่แข่ง', tone: 'neutral' },
]
const matches = computed(() => getTournamentMatches(props.id))

const selectedTeamId = ref(null)
const selectedTeam = computed(() => tournament.value?.teams.find((t) => t.id === selectedTeamId.value) ?? null)

const recentResults = computed(() =>
  matches.value
    .filter((m) => m.result)
    .toSorted((a, b) => b.scheduledAt.localeCompare(a.scheduledAt))
    .slice(0, RECENT_LIMIT),
)

const infoRows = computed(() => {
  const t = tournament.value
  return [
    { key: 'เกม', value: t.game.name },
    { key: 'รูปแบบ', value: formatTournamentFormat(t.format, t.totalGames) },
    { key: 'วันแข่ง', value: formatDateRange(t.startDate, t.endDate) },
    { key: 'จำนวนทีม', value: `${t.teams.length} ทีม` },
    ...(t.champion ? [{ key: 'แชมป์', value: t.champion.name, highlight: true }] : []),
  ]
})

watch(() => props.id, () => {
  activeTab.value = 'overview'
  selectedTeamId.value = null
})
</script>

<template>
  <div class="page">
    <AppNavbar />

    <main v-if="tournament" class="content">
      <nav class="breadcrumb" aria-label="breadcrumb">
        <RouterLink to="/tournaments">รายการแข่ง</RouterLink>
        <span>/</span>
        <RouterLink :to="{ name: 'tournaments', query: { game: tournament.game.code } }">{{ tournament.game.name }}</RouterLink>
        <span>/</span>
        <span class="current" aria-current="page">{{ tournament.name }}</span>
      </nav>

      <header class="header" :class="{ compact: isPoints }">
        <span class="logo">
          <img v-if="tournament.logoUrl" :src="tournament.logoUrl" :alt="tournament.name" />
          <template v-else>{{ initials(tournament.name, 3) }}</template>
        </span>
        <div class="title">
          <div class="tags">
            <span class="game-tag">{{ tournament.game.name }}</span>
            <StatusBadge :status="tournament.status" large />
            <span v-if="isPoints" class="format-tag">{{ formatTournamentFormat(tournament.format, tournament.totalGames) }}</span>
          </div>
          <h1 class="name">{{ tournament.name }}</h1>
          <p v-if="isPoints" class="description">{{ pointsSummary }}</p>
          <p v-else-if="tournament.description" class="description">{{ tournament.description }}</p>
        </div>
        <div v-if="isPoints && tournament.champion" class="side-box champion-box">
          <span class="side-label">แชมป์</span>
          <span class="side-title">{{ tournament.champion.name }}</span>
          <span class="side-sub">แข่งครบ {{ standings.gamesCompleted }} เกม</span>
        </div>
        <div v-else-if="isPoints && nextGame" class="side-box">
          <span class="side-label">เกมถัดไป</span>
          <span class="side-title">เกมที่ {{ nextGame.gameNumber }}</span>
          <span class="side-sub">{{ nextGame.scheduledAt ? `${formatDate(nextGame.scheduledAt)}, ${formatTime(nextGame.scheduledAt)}` : 'รอกำหนดวัน' }}</span>
        </div>
      </header>

      <div class="tabs" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          role="tab"
          class="tab"
          :class="{ active: activeTab === tab.key }"
          :aria-selected="activeTab === tab.key"
          @click="activeTab = tab.key"
        >{{ tab.label }}</button>
      </div>

      <div v-if="activeTab === 'overview'" class="columns">
        <div class="left">
          <section class="section">
            <div class="section-header">
              <h2 class="section-title">ทีมที่เข้าร่วม</h2>
              <span class="section-meta">{{ tournament.teams.length }} ทีม · กดที่ทีมเพื่อดูผู้เล่น</span>
            </div>
            <div v-if="tournament.teams.length" class="participants">
              <ParticipantCard
                v-for="team in tournament.teams"
                :key="team.id"
                :team="team"
                :selected="team.id === selectedTeamId"
                @select="selectedTeamId = team.id"
              />
            </div>
            <p v-else class="empty">ยังไม่มีทีมเข้าร่วม</p>
          </section>

          <section v-if="!isPoints" class="section">
            <div class="section-header">
              <h2 class="section-title">ผลการแข่งล่าสุด</h2>
              <button type="button" class="link" @click="activeTab = 'matches'">ดูแมตช์ทั้งหมด →</button>
            </div>
            <div v-if="recentResults.length" class="panel">
              <ResultRow v-for="match in recentResults" :key="match.id" :match="match" />
            </div>
            <p v-else class="panel empty">ยังไม่มีผลการแข่ง</p>
          </section>
        </div>

        <aside class="infobox">
          <h2 class="infobox-header">ข้อมูลรายการ</h2>
          <div v-for="row in infoRows" :key="row.key" class="info-row">
            <span class="info-key">{{ row.key }}</span>
            <span class="info-value" :class="{ highlight: row.highlight }">{{ row.value }}</span>
          </div>
        </aside>
      </div>

      <section v-else-if="activeTab === 'matches'" class="section">
        <MatchRoundList v-if="matches.length" :matches="matches" :team-count="tournament.teams.length" />
        <p v-else class="panel empty">ยังไม่มีแมตช์ในรายการนี้</p>
      </section>

      <section v-else-if="activeTab === 'standings'" class="section">
        <FreeFireStandings
          :standings="standings"
          :placement-points="placementPoints"
          :points-per-kill="tournament.pointsPerKill"
        />
      </section>

      <section v-else class="section">
        <div class="legend">
          <span class="legend-label">สถานะแมตช์:</span>
          <span v-for="item in legend" :key="item.label" class="legend-badge" :class="item.tone">{{ item.label }}</span>
        </div>
        <TournamentBracket
          :matches="matches"
          :team-count="tournament.teams.length"
          :champion="tournament.champion"
          :tournament-name="tournament.name"
        />
      </section>
    </main>

    <main v-else class="content not-found">
      <h1 class="name">ไม่พบรายการแข่ง</h1>
      <RouterLink to="/tournaments" class="link">← กลับไปหน้ารายการแข่ง</RouterLink>
    </main>

    <Transition name="drawer">
      <TeamDrawer
        v-if="tournament && selectedTeam"
        :team="selectedTeam"
        :tournament="tournament"
        :matches="matches"
        @close="selectedTeamId = null"
      />
    </Transition>
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

.breadcrumb { display: flex; flex-wrap: wrap; gap: 8px; color: var(--color-muted); font-size: 15px; }
.breadcrumb a:hover { color: var(--color-text); }
.breadcrumb .current { color: var(--color-text); font-weight: 500; }

.header { display: flex; align-items: center; gap: 28px; }
.logo {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 120px;
  height: 120px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: var(--color-surface);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 34px;
}
.logo img { width: 100%; height: 100%; object-fit: cover; }
.title { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 10px; }
.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.game-tag {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-2);
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 500;
}
.name { font-family: var(--font-heading); font-weight: 600; font-size: 44px; }
.format-tag {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--color-info-bg);
  color: var(--color-info);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}
.header.compact { gap: 24px; }
.header.compact .logo { width: 88px; height: 88px; border-radius: 20px; font-size: 26px; }
.header.compact .title { gap: 8px; }
.header.compact .name { font-size: 36px; }
.header.compact .description { font-size: 15px; white-space: pre-wrap; }
.side-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  padding: 16px 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  white-space: nowrap;
}
.side-label { color: var(--color-muted); font-size: 13px; }
.side-title { font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.side-sub { color: var(--color-muted); font-size: 14px; }
.champion-box { border-color: var(--color-accent); background: var(--color-accent-bg); }
.champion-box .side-title { color: var(--color-accent); }
.description { color: var(--color-muted); font-size: 16px; }

.tabs { display: flex; gap: 32px; border-bottom: 1px solid var(--color-border); }
.tab {
  padding: 0 0 10px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: none;
  color: var(--color-muted);
  font-size: 17px;
  font-weight: 600;
}
.tab.active { color: var(--color-accent); border-bottom-color: var(--color-accent); }

.columns { display: flex; align-items: flex-start; gap: 24px; }
.left { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 24px; }

.section { display: flex; flex-direction: column; gap: 16px; }
.section-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.section-title { font-family: var(--font-heading); font-weight: 600; font-size: 24px; }
.section-meta { color: var(--color-muted); font-size: 15px; font-weight: 600; }

.participants {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.panel {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.link {
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-accent);
  font-size: 15px;
  font-weight: 600;
}

.infobox {
  width: 440px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.infobox-header {
  padding: 16px 24px;
  background: var(--color-surface-2);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 18px;
}
.info-row {
  display: flex;
  gap: 16px;
  padding: 14px 24px;
  border-top: 1px solid var(--color-border);
  font-size: 15px;
}
.info-key { width: 120px; flex-shrink: 0; color: var(--color-muted); }
.info-value { font-weight: 500; }
.info-value.highlight { color: var(--color-accent); }

.empty {
  padding: 32px 24px;
  text-align: center;
  color: var(--color-muted);
  font-size: 15px;
}

.legend { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }
.legend-label { color: var(--color-muted); font-size: 14px; }
.legend-badge { padding: 4px 12px; border-radius: var(--radius-pill); font-size: 13px; font-weight: 600; }
.legend-badge.success { background: var(--color-success-bg); color: var(--color-success); }
.legend-badge.info { background: var(--color-info-bg); color: var(--color-info); }
.legend-badge.neutral { background: var(--color-neutral-bg); color: var(--color-muted); }

.not-found { align-items: flex-start; }

.drawer-enter-active, .drawer-leave-active { transition: opacity 0.2s ease; }
.drawer-enter-active :deep(.drawer), .drawer-leave-active :deep(.drawer) { transition: transform 0.25s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from :deep(.drawer), .drawer-leave-to :deep(.drawer) { transform: translateX(100%); }

@media (max-width: 1200px) {
  .columns { flex-direction: column-reverse; align-items: stretch; }
  .infobox { width: 100%; }
}
@media (max-width: 640px) {
  .header { flex-direction: column; align-items: flex-start; gap: 16px; }
  .header.compact .name { font-size: 28px; }
  .header.compact .logo { width: 72px; height: 72px; font-size: 22px; }
  .side-box { align-self: stretch; }
  .logo { width: 80px; height: 80px; font-size: 24px; border-radius: var(--radius-lg); }
  .name { font-size: 30px; }
  .tabs { gap: 20px; overflow-x: auto; }
  .tab { white-space: nowrap; }
}
</style>

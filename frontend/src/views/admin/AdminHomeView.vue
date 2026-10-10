<script setup>
import { RouterLink } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import TeamLogo from '@/components/TeamLogo.vue'
import { getAdminOverview, getPendingResults, getUpcomingTournaments } from '@/mock/queries'
import { formatDate, formatRound, formatShortDate, formatTime, initials } from '@/utils/format'

const overview = getAdminOverview()
const pendingResults = getPendingResults().slice(0, 5)
const upcoming = getUpcomingTournaments(3)

const kpis = [
  {
    key: 'tournaments',
    label: 'รายการทั้งหมด',
    value: overview.tournaments.total,
    note: `กำลังแข่ง ${overview.tournaments.ongoing} · กำลังจะเริ่ม ${overview.tournaments.upcoming}`,
    tone: 'accent',
  },
  {
    key: 'teams',
    label: 'ทีมทั้งหมด',
    value: overview.teams.total,
    note: overview.teams.byGame.map((x) => `${x.game.name} ${x.count}`).join(' · '),
    tone: 'info',
  },
  {
    key: 'pending',
    label: 'แมตช์รอกรอกผล',
    value: overview.pendingResults,
    note: 'แข่งจบแล้วแต่ยังไม่มีผล',
    tone: 'warning',
  },
  {
    key: 'today',
    label: 'แมตช์วันนี้',
    value: overview.todayMatches.total,
    note: overview.todayMatches.waiting === overview.todayMatches.total ? 'รอแข่งทั้งหมด' : `รอแข่ง ${overview.todayMatches.waiting}`,
    tone: 'success',
  },
]

const setupBadge = {
  NO_BRACKET: { label: 'ยังไม่สร้างสาย', tone: 'warning' },
  BRACKET_CREATED: { label: 'สร้างสายแล้ว', tone: 'success' },
  NO_SCHEDULE: { label: 'ยังไม่สร้างตารางเกม', tone: 'warning' },
  SCHEDULE_CREATED: { label: 'สร้างตารางเกมแล้ว', tone: 'success' },
}

const isReady = (t) => t.setupStatus === 'BRACKET_CREATED' || t.setupStatus === 'SCHEDULE_CREATED'

function pendingTitle(item) {
  return item.kind === 'MATCH' ? null : `เกมที่ ${item.gameNumber}`
}

function pendingStage(item) {
  return item.kind === 'MATCH' ? formatRound(item.roundNumber, item.teamCount) : `เก็บคะแนน · ${item.teamCount} ทีม`
}
</script>

<template>
  <AdminLayout>
    <div class="page">
      <header class="head">
        <div>
          <h1 class="title">ภาพรวม</h1>
          <p class="updated">อัปเดตล่าสุด {{ formatDate(overview.updatedAt) }}, {{ formatTime(overview.updatedAt) }}</p>
        </div>
        <div class="actions">
          <RouterLink to="/admin/teams/new" class="btn btn-outline">+ เพิ่มทีม</RouterLink>
          <RouterLink to="/admin/tournaments/new" class="btn btn-accent">+ สร้างรายการแข่ง</RouterLink>
        </div>
      </header>

      <section class="kpis">
        <article v-for="kpi in kpis" :key="kpi.key" class="kpi">
          <p class="kpi-label">{{ kpi.label }}</p>
          <p class="kpi-value" :class="`tone-${kpi.tone}`">{{ kpi.value }}</p>
          <p class="kpi-note">{{ kpi.note }}</p>
        </article>
      </section>

      <div class="panels">
        <section class="panel pending">
          <header class="panel-head">
            <h2 class="panel-title">แมตช์ที่ต้องกรอกผล</h2>
            <RouterLink to="/admin/matches" class="link">ดูทั้งหมด →</RouterLink>
          </header>

          <ul v-if="pendingResults.length" class="pending-list">
            <li v-for="item in pendingResults" :key="item.key" class="pending-row">
              <span class="logos">
                <template v-if="item.kind === 'MATCH'">
                  <TeamLogo :team="item.teamA" :size="36" :font-size="11" />
                  <TeamLogo :team="item.teamB" :size="36" :font-size="11" class="logo-overlap" />
                </template>
                <TeamLogo v-else label="FF" :size="36" :font-size="11" />
              </span>
              <span class="pending-text">
                <span class="pending-title">
                  <template v-if="item.kind === 'MATCH'">
                    {{ item.teamA.name }} <span class="vs">vs</span> {{ item.teamB.name }}
                  </template>
                  <template v-else>{{ pendingTitle(item) }}</template>
                </span>
                <span class="pending-meta">
                  {{ item.tournament.name }} · {{ pendingStage(item) }} · แข่งเมื่อ {{ formatShortDate(item.scheduledAt) }}
                  {{ formatTime(item.scheduledAt) }}
                </span>
              </span>
              <RouterLink :to="item.kind === 'MATCH' ? `/admin/matches?match=${item.id}` : `/admin/tournaments/${item.tournament.id}/games`" class="btn btn-accent btn-sm">กรอกผล</RouterLink>
            </li>
          </ul>
          <p v-else class="empty">ไม่มีแมตช์ที่รอกรอกผล</p>
        </section>

        <section class="panel upcoming">
          <header class="panel-head">
            <h2 class="panel-title">รายการที่กำลังจะเริ่ม</h2>
          </header>

          <ul v-if="upcoming.length" class="upcoming-list">
            <li v-for="t in upcoming" :key="t.id" class="upcoming-row">
              <span class="t-logo">
                <img v-if="t.logoUrl" :src="t.logoUrl" :alt="t.name" />
                <template v-else>{{ initials(t.name, 3) }}</template>
              </span>
              <span class="upcoming-text">
                <span class="upcoming-name">{{ t.name }}</span>
                <span class="upcoming-meta">
                  เริ่ม {{ formatDate(t.startDate) }} · {{ t.teams.length }} ทีม
                  <span class="badge" :class="`tone-${setupBadge[t.setupStatus].tone}`">{{ setupBadge[t.setupStatus].label }}</span>
                </span>
              </span>
              <RouterLink :to="`/admin/tournaments/${t.id}/teams`" class="btn btn-outline btn-sm">{{ isReady(t) ? 'จัดการ' : 'จัดการทีม' }}</RouterLink>
            </li>
          </ul>
          <p v-else class="empty">ไม่มีรายการที่กำลังจะเริ่ม</p>
        </section>
      </div>
    </div>
  </AdminLayout>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 24px; }

.head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.title { font-family: var(--font-heading); font-weight: 600; font-size: 36px; line-height: 1.2; }
.updated { margin-top: 4px; color: var(--color-muted); font-size: 14px; }
.actions { display: flex; gap: 12px; flex-wrap: wrap; }

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 24px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-family: var(--font-heading);
  font-weight: 500;
  font-size: 15px;
  white-space: nowrap;
}
.btn-accent { background: var(--color-accent); color: var(--color-on-accent); }
.btn-accent:hover { filter: brightness(0.92); }
.btn-outline { border-color: var(--color-border); background: transparent; color: var(--color-text); }
.btn-outline:hover { background: var(--color-surface-2); }
.btn-sm { padding: 6px 14px; font-size: 13px; }

.kpis { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 24px; }
.kpi {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.kpi-label { color: var(--color-muted); font-size: 14px; }
.kpi-value { font-family: var(--font-heading); font-weight: 600; font-size: 44px; line-height: 1.2; }
.kpi-note { color: var(--color-muted); font-size: 13px; }

.tone-accent { color: var(--color-accent); }
.tone-info { color: var(--color-info); }
.tone-warning { color: var(--color-warning); }
.tone-success { color: var(--color-success); }

.panels { display: flex; align-items: flex-start; gap: 24px; }
.panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.pending { flex: 1; min-width: 0; }
.upcoming { width: 600px; flex-shrink: 0; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.panel-title { font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.link { padding: 0; border: 0; background: none; color: var(--color-accent); font-size: 14px; font-weight: 500; }
.empty { padding: 24px 0; color: var(--color-muted); text-align: center; font-size: 14px; }

.pending-list,
.upcoming-list { margin: 0; padding: 0; list-style: none; }
.pending-list { border-top: 1px solid var(--color-border); }
.pending-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
}
.pending-row:last-child { border-bottom: 0; padding-bottom: 0; }
.logos { display: flex; flex-shrink: 0; width: 64px; }
.logo-overlap { margin-left: -8px; }
.pending-text { display: flex; flex: 1; flex-direction: column; gap: 4px; min-width: 0; }
.pending-title { font-weight: 600; font-size: 16px; }
.vs { margin: 0 4px; color: var(--color-muted); font-weight: 500; }
.pending-meta { color: var(--color-muted); font-size: 13px; }

.upcoming-list { display: flex; flex-direction: column; gap: 12px; }
.upcoming-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-md);
  background: var(--color-surface-2);
}
.t-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 13px;
}
.t-logo img { width: 100%; height: 100%; object-fit: cover; }
.upcoming-text { display: flex; flex: 1; flex-direction: column; gap: 6px; min-width: 0; }
.upcoming-name { font-weight: 600; font-size: 16px; }
.upcoming-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 8px; color: var(--color-muted); font-size: 13px; }
.badge { padding: 2px 10px; border-radius: var(--radius-pill); font-size: 12px; font-weight: 500; }
.badge.tone-warning { background: var(--color-warning-bg); }
.badge.tone-success { background: var(--color-success-bg); }

@media (max-width: 1440px) {
  .panels { flex-direction: column; align-items: stretch; }
  .upcoming { width: auto; }
}

@media (max-width: 1200px) {
  .kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
}

@media (max-width: 640px) {
  .title { font-size: 28px; }
  .actions { width: 100%; }
  .actions .btn { flex: 1; padding: 10px 12px; }
  .kpis { grid-template-columns: 1fr 1fr; gap: 12px; }
  .kpi { padding: 16px; gap: 4px; }
  .kpi-value { font-size: 32px; }
  .kpi-note { font-size: 12px; }
  .panel { padding: 16px; }
  .panel-title { font-size: 18px; }
  .pending-row,
  .upcoming-row { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 10px 12px; }
  .pending-row .btn,
  .upcoming-row .btn { grid-column: 2; justify-self: start; }
  .upcoming-row { padding: 12px; }
}
</style>

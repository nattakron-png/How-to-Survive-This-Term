<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import AppNavbar from '@/components/AppNavbar.vue'
import TeamLogo from '@/components/TeamLogo.vue'
import { getMatch } from '@/mock/queries'
import { formatDate, formatRound, formatTime, initials } from '@/utils/format'
import { MATCH_STATES, emptySlotLabel, getMatchState } from '@/utils/match'

const props = defineProps({
  id: { type: String, required: true },
  matchId: { type: String, required: true },
})

const match = computed(() => getMatch(props.id, props.matchId))

const state = computed(() => (match.value ? getMatchState(match.value) : null))

const roundLabel = computed(() => formatRound(match.value.roundNumber, match.value.teamCount))

const totalRounds = computed(() => Math.ceil(Math.log2(Math.max(match.value.teamCount, 2))))

const scheduleText = computed(() => {
  const at = match.value.scheduledAt
  return at ? `${formatDate(at)}, ${formatTime(at)}` : 'รอกำหนดวัน'
})

const metaText = computed(() =>
  [
    match.value.tournament.name,
    roundLabel.value,
    `แมตช์ ${match.value.displayNumber}`,
    ...(state.value === 'BYE' ? [] : [scheduleText.value]),
  ].join('  ·  '),
)

const winnerId = computed(() => match.value.result?.winnerTeamId ?? null)
const winnerTeam = computed(() => {
  const m = match.value
  if (state.value === 'BYE') return m.teamA ?? m.teamB
  return [m.teamA, m.teamB].find((t) => t && t.id === winnerId.value) ?? null
})

const progressText = computed(() => {
  const isFinal = match.value.roundNumber >= totalRounds.value
  const next = isFinal ? `คว้าแชมป์ ${match.value.tournament.name}` : `เข้า${formatRound(match.value.roundNumber + 1, match.value.teamCount)}`
  if (winnerTeam.value) return `${winnerTeam.value.name} ${next}`
  return `ผู้ชนะ${next}`
})

function side(team, score, feeder, players) {
  const done = state.value === 'DONE'
  return {
    team,
    name: team?.name ?? emptySlotLabel(match.value, feeder),
    score: done ? score : '–',
    players,
    tone: !team ? 'placeholder' : done && team.id !== winnerId.value ? 'loser' : done ? 'winner' : 'normal',
  }
}

const sides = computed(() => {
  const m = match.value
  return [
    side(m.teamA, m.result?.teamAScore, m.feederA, m.teamAPlayers),
    side(m.teamB, m.result?.teamBScore, m.feederB, m.teamBPlayers),
  ]
})
</script>

<template>
  <div class="page">
    <AppNavbar />

    <main v-if="match" class="content">
      <nav class="breadcrumb" aria-label="breadcrumb">
        <RouterLink to="/tournaments">รายการแข่ง</RouterLink>
        <span>/</span>
        <RouterLink :to="{ name: 'tournament-detail', params: { id: match.tournament.id }, query: { tab: 'matches' } }">
          {{ match.tournament.name }}
        </RouterLink>
        <span>/</span>
        <span class="current" aria-current="page">แมตช์ {{ match.displayNumber }}</span>
      </nav>

      <section class="scoreboard">
        <div class="meta">
          <span class="meta-text">{{ metaText }}</span>
          <span class="badge" :class="MATCH_STATES[state].tone">{{ MATCH_STATES[state].label }}</span>
        </div>

        <div class="score-row">
          <div v-for="(s, i) in sides" :key="i" class="team" :class="[s.tone, i === 0 ? 'team-a' : 'team-b']">
            <span class="team-logo">
              <img v-if="s.team?.logoUrl" :src="s.team.logoUrl" :alt="s.team.name" />
              <template v-else-if="s.team">{{ initials(s.team.name) }}</template>
              <template v-else>?</template>
            </span>
            <span class="team-name">{{ s.name }}</span>
          </div>

          <div class="score">
            <template v-if="state === 'BYE'">
              <span class="score-vs">BYE</span>
            </template>
            <template v-else>
              <span class="score-num" :class="sides[0].tone">{{ sides[0].score }}</span>
              <span class="score-vs">vs</span>
              <span class="score-num" :class="sides[1].tone">{{ sides[1].score }}</span>
            </template>
          </div>
        </div>

        <p class="progress">{{ progressText }}</p>
      </section>

      <section class="rosters">
        <div v-for="(s, i) in sides" :key="i" class="roster">
          <div class="roster-header">
            <TeamLogo v-if="s.team" :team="s.team" :size="32" :font-size="11" />
            <span class="roster-name" :class="{ placeholder: !s.team }">{{ s.name }}</span>
          </div>
          <template v-if="s.players.length">
            <div v-for="player in s.players" :key="player.id" class="player">
              <span class="player-name">{{ player.name }}</span>
              <span class="player-role">{{ player.role }}</span>
            </div>
          </template>
          <p v-else class="roster-empty">{{ s.team ? 'ยังไม่มีรายชื่อผู้เล่น' : 'ยังไม่ทราบทีม' }}</p>
        </div>
      </section>
    </main>

    <main v-else class="content not-found">
      <h1 class="not-found-title">ไม่พบแมตช์นี้</h1>
      <RouterLink :to="{ name: 'tournament-detail', params: { id } }" class="back">← กลับไปหน้ารายการแข่ง</RouterLink>
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

.breadcrumb { display: flex; flex-wrap: wrap; gap: 8px; color: var(--color-muted); font-size: 15px; }
.breadcrumb a:hover { color: var(--color-text); }
.breadcrumb .current { color: var(--color-text); font-weight: 500; }

.scoreboard {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 40px;
  border-radius: var(--radius-xl);
  background: var(--color-surface);
}
.meta { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 12px; text-align: center; }
.meta-text { color: var(--color-muted); font-size: 15px; white-space: pre-wrap; }
.badge { padding: 4px 12px; border-radius: var(--radius-pill); font-size: 14px; font-weight: 600; white-space: nowrap; }
.success { background: var(--color-success-bg); color: var(--color-success); }
.info { background: var(--color-info-bg); color: var(--color-info); }
.neutral { background: var(--color-neutral-bg); color: var(--color-muted); }

.score-row { display: grid; grid-template-columns: 360px auto 360px; align-items: center; gap: 64px; }
.team-a { grid-column: 1; grid-row: 1; }
.team-b { grid-column: 3; grid-row: 1; }
.score { grid-column: 2; grid-row: 1; }

.team { align-self: start; display: flex; flex-direction: column; align-items: center; gap: 12px; min-width: 0; text-align: center; }
.team-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 96px;
  height: 96px;
  border-radius: var(--radius-xl);
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 33px;
}
.team-logo img { width: 100%; height: 100%; object-fit: cover; }
.team-name { max-width: 100%; font-family: var(--font-heading); font-weight: 600; font-size: 28px; overflow-wrap: anywhere; }
.team.loser .team-name, .team.placeholder .team-name { color: var(--color-muted); }
.team.placeholder .team-logo { color: var(--color-muted); }

.score {
  align-self: start;
  height: 96px;
  display: flex;
  align-items: center;
  gap: 24px;
  font-family: var(--font-heading);
  font-weight: 600;
  color: var(--color-muted);
  white-space: nowrap;
}
.score-num { font-size: 84px; line-height: 1; }
.score-num.winner { color: var(--color-accent); }
.score-vs { font-size: 64px; line-height: 1; }

.progress { color: var(--color-muted); font-size: 14px; text-align: center; }

.rosters {
  display: flex;
  gap: 24px;
  padding: 32px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.roster { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 12px; }
.roster-header { display: flex; align-items: center; gap: 10px; }
.roster-name { font-family: var(--font-heading); font-weight: 600; font-size: 18px; }
.roster-name.placeholder { color: var(--color-muted); }
.player { display: flex; align-items: baseline; gap: 10px; }
.player-name { font-size: 16px; }
.player-role { color: var(--color-muted); font-size: 13px; }
.roster-empty { color: var(--color-muted); font-size: 14px; }

.not-found { align-items: flex-start; }
.not-found-title { font-family: var(--font-heading); font-weight: 600; font-size: 36px; }
.back { color: var(--color-accent); font-size: 15px; font-weight: 600; }

@media (max-width: 1100px) {
  .score-row { grid-template-columns: 1fr auto 1fr; gap: 32px; }
  .score-num { font-size: 64px; }
  .score-vs { font-size: 44px; }
}
@media (max-width: 640px) {
  .scoreboard { padding: 24px 16px; }
  .score-row { gap: 12px; }
  .team-logo { width: 64px; height: 64px; font-size: 22px; border-radius: var(--radius-lg); }
  .team-name { font-size: 18px; }
  .score { gap: 8px; }
  .score-num { font-size: 40px; }
  .score-vs { font-size: 24px; }
  .rosters { flex-direction: column; padding: 20px; }
}
</style>

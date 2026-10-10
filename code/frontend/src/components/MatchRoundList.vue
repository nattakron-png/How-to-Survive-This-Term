<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { formatRound, formatShortDate, formatTime } from '@/utils/format'
import { MATCH_STATES, emptySlotLabel, getMatchState } from '@/utils/match'

const props = defineProps({
  matches: { type: Array, required: true },
  teamCount: { type: Number, required: true },
})

const rounds = computed(() => {
  const numbers = [...new Set(props.matches.map((m) => m.roundNumber))].sort((a, b) => a - b)
  return numbers.map((roundNumber) => ({
    roundNumber,
    label: formatRound(roundNumber, props.teamCount),
    matches: props.matches.filter((m) => m.roundNumber === roundNumber),
  }))
})

function rowOf(match) {
  const state = getMatchState(match)
  return {
    state: MATCH_STATES[state],
    schedule: match.scheduledAt
      ? `${formatShortDate(match.scheduledAt)} ${formatTime(match.scheduledAt)}`
      : state === 'BYE' ? '-' : 'รอกำหนดวัน',
    teamA: match.teamA?.name ?? emptySlotLabel(match, match.feederA),
    teamB: match.teamB?.name ?? emptySlotLabel(match, match.feederB),
    score: match.result ? `${match.result.teamAScore} – ${match.result.teamBScore}` : '–',
  }
}
</script>

<template>
  <div class="rounds">
    <section v-for="round in rounds" :key="round.roundNumber" class="round">
      <h3 class="round-header">{{ round.label }}</h3>
      <div v-for="match in round.matches" :key="match.id" class="row">
        <span class="number">แมตช์ {{ match.displayNumber }}</span>
        <span class="schedule"><span class="schedule-number">แมตช์ {{ match.displayNumber }} · </span>{{ rowOf(match).schedule }}</span>
        <span class="team team-a" :class="{ placeholder: !match.teamA }">{{ rowOf(match).teamA }}</span>
        <span class="score" :class="{ final: match.result }">{{ rowOf(match).score }}</span>
        <span class="team team-b" :class="{ placeholder: !match.teamB }">{{ rowOf(match).teamB }}</span>
        <span class="badge" :class="rowOf(match).state.tone">{{ rowOf(match).state.label }}</span>
        <RouterLink
          class="detail"
          :to="{ name: 'match-detail', params: { id: match.tournamentId, matchId: match.id } }"
        >ดูรายละเอียด ›</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.rounds { display: flex; flex-direction: column; gap: 24px; }
.round {
  overflow: hidden;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}
.round-header {
  padding: 14px 24px;
  background: var(--color-surface-2);
  font-size: 16px;
  font-weight: 600;
}
.row {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-surface-2);
}
.number { width: 120px; flex-shrink: 0; color: var(--color-muted); font-size: 15px; font-weight: 500; }
.schedule { width: 160px; flex-shrink: 0; color: var(--color-muted); font-size: 15px; }
.schedule-number { display: none; }
.team {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.team-a { width: 300px; flex-shrink: 0; text-align: right; }
.team-b { flex: 1; min-width: 0; }
.team.placeholder { color: var(--color-muted); }
.score {
  width: 100px;
  flex-shrink: 0;
  text-align: center;
  color: var(--color-muted);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 20px;
}
.score.final { color: var(--color-accent); }
.badge {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}
.success { background: var(--color-success-bg); color: var(--color-success); }
.info { background: var(--color-info-bg); color: var(--color-info); }
.neutral { background: var(--color-neutral-bg); color: var(--color-muted); }
.detail { flex-shrink: 0; color: var(--color-accent); font-size: 14px; font-weight: 500; white-space: nowrap; }
.detail:hover { text-decoration: underline; }

@media (max-width: 1200px) {
  .team-a { width: auto; flex: 1; min-width: 0; }
  .schedule { width: 120px; }
  .number { width: 72px; }
}
@media (max-width: 760px) {
  .row {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    grid-template-areas:
      'meta meta badge'
      'teamA score teamB'
      'detail detail detail';
    gap: 10px 12px;
    padding: 14px 16px;
  }
  .number { display: none; }
  .schedule-number { display: inline; }
  .schedule { grid-area: meta; width: auto; font-size: 13px; }
  .badge { grid-area: badge; justify-self: end; }
  .team { font-size: 14px; }
  .team-a { grid-area: teamA; width: auto; }
  .team-b { grid-area: teamB; }
  .score { grid-area: score; width: auto; font-size: 17px; }
  .detail { grid-area: detail; justify-self: end; font-size: 13px; }
}
</style>

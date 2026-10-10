<script setup>
import { computed } from 'vue'
import TeamLogo from './TeamLogo.vue'
import { formatRound, formatShortDate, formatTime } from '@/utils/format'

const props = defineProps({
  match: { type: Object, required: true },
})

const winnerId = computed(() => props.match.result?.winnerTeamId ?? null)
const isBye = computed(() => props.match.status === 'COMPLETED' && (!props.match.teamA || !props.match.teamB))
const isWinner = (team) => (isBye.value ? Boolean(team) : winnerId.value === null || team?.id === winnerId.value)
const emptyLabel = computed(() => (isBye.value ? 'BYE' : 'รอผล'))
</script>

<template>
  <div class="row">
    <div class="round">
      <span class="round-name">{{ formatRound(match.roundNumber, match.teamCount) }} · แมตช์ {{ match.displayNumber ?? match.matchNumber }}</span>
      <span class="round-date">{{ isBye ? 'ผ่านอัตโนมัติ' : match.scheduledAt ? formatShortDate(match.scheduledAt) : 'รอกำหนดวัน' }}</span>
    </div>

    <div class="team team-a" :class="{ loser: !isWinner(match.teamA) }">
      <span class="team-name" :class="{ pending: !match.teamA }">{{ match.teamA?.name ?? emptyLabel }}</span>
      <TeamLogo v-if="match.teamA" :team="match.teamA" :size="32" :font-size="11" />
    </div>

    <div class="score">
      <template v-if="match.result">{{ match.result.teamAScore }} – {{ match.result.teamBScore }}</template>
      <span v-else-if="isBye" class="vs">–</span>
      <span v-else-if="match.scheduledAt" class="time">{{ formatTime(match.scheduledAt) }}</span>
      <span v-else class="vs">vs</span>
    </div>

    <div class="team" :class="{ loser: !isWinner(match.teamB) }">
      <TeamLogo v-if="match.teamB" :team="match.teamB" :size="32" :font-size="11" />
      <span class="team-name" :class="{ pending: !match.teamB }">{{ match.teamB?.name ?? emptyLabel }}</span>
    </div>
  </div>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 24px;
}
.row + .row { border-top: 1px solid var(--color-border); }
.round {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 200px;
  flex-shrink: 0;
  color: var(--color-muted);
}
.round-name { font-size: 14px; }
.round-date { font-size: 13px; }
.team { flex: 1 0 0; min-width: 0; display: flex; align-items: center; gap: 10px; }
.team-a { justify-content: flex-end; }
.team-name {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.team-name.pending { color: var(--color-muted); font-weight: 400; }
.loser .team-name { color: var(--color-muted); font-weight: 400; }
.score {
  width: 80px;
  flex-shrink: 0;
  text-align: center;
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 20px;
  white-space: nowrap;
}
.time { color: var(--color-muted); font-family: var(--font-body); font-size: 15px; }
.vs { color: var(--color-muted); }

@media (max-width: 760px) {
  .row { flex-wrap: wrap; gap: 8px 12px; padding: 14px 16px; }
  .round { width: 100%; flex-direction: row; gap: 8px; }
  .score { width: 56px; font-size: 17px; }
  .team-name { font-size: 14px; }
}
</style>

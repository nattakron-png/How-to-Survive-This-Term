<script setup>
import { computed } from 'vue'
import { MATCH_STATES as STATES, emptySlotLabel, getMatchState } from '@/utils/match'

const props = defineProps({
  match: { type: Object, required: true },
})

const state = computed(() => getMatchState(props.match))

function slot(team, score, feeder) {
  const m = props.match
  if (state.value === 'BYE') {
    return team
      ? { name: team.name, score: '–', kind: 'winner' }
      : { name: 'BYE', score: '–', kind: 'bye' }
  }
  if (!team) {
    return { name: emptySlotLabel(m, feeder), score: '–', kind: 'placeholder' }
  }
  if (m.result) {
    return { name: team.name, score, kind: team.id === m.result.winnerTeamId ? 'winner' : 'loser' }
  }
  return { name: team.name, score: '–', kind: 'pending' }
}

const rows = computed(() => [
  slot(props.match.teamA, props.match.result?.teamAScore, props.match.feederA),
  slot(props.match.teamB, props.match.result?.teamBScore, props.match.feederB),
])
</script>

<template>
  <article class="match">
    <div class="meta">
      <span class="number">แมตช์ {{ match.displayNumber }}</span>
      <span class="badge" :class="STATES[state].tone">{{ STATES[state].label }}</span>
    </div>
    <div v-for="(row, i) in rows" :key="i" class="team-row" :class="row.kind">
      <span class="team-name">{{ row.name }}</span>
      <span class="score">{{ row.score }}</span>
    </div>
  </article>
</template>

<style scoped>
.match {
  display: flex;
  flex-direction: column;
  width: 320px;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}
.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
}
.number { color: var(--color-muted); font-size: 13px; }
.badge {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}
.success { background: var(--color-success-bg); color: var(--color-success); }
.info { background: var(--color-info-bg); color: var(--color-info); }
.neutral { background: var(--color-neutral-bg); color: var(--color-muted); }

.team-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  border-top: 1px solid var(--color-border);
  font-size: 15px;
}
.team-name { min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.score { font-weight: 600; }

.winner { font-weight: 600; }
.winner .team-name { color: var(--color-text); }
.winner .score { color: var(--color-accent); }
.loser, .placeholder, .bye { color: var(--color-muted); }
.pending .team-name { color: var(--color-text); }
.pending .score { color: var(--color-muted); }
.bye { opacity: 0.55; }
</style>

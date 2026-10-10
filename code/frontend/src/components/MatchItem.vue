<script setup>
import TeamLogo from './TeamLogo.vue'
import { formatRound, formatTime } from '@/utils/format'

defineProps({
  match: { type: Object, required: true },
})
</script>

<template>
  <article class="match">
    <div class="meta">
      <span class="meta-text">{{ match.tournament.name }} · {{ formatRound(match.roundNumber, match.teamCount) }}</span>
      <span class="time">{{ formatTime(match.scheduledAt) }}</span>
    </div>
    <div class="teams">
      <div class="team team-a">
        <span class="team-name">{{ match.teamA?.name ?? 'รอผล' }}</span>
        <TeamLogo v-if="match.teamA" :team="match.teamA" />
      </div>
      <span class="vs">vs</span>
      <div class="team">
        <TeamLogo v-if="match.teamB" :team="match.teamB" />
        <span class="team-name">{{ match.teamB?.name ?? 'รอผล' }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.match {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}
.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--color-muted);
  font-size: 13px;
}
.time {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-2);
  font-size: 12px;
  font-weight: 600;
}
.teams { display: flex; align-items: center; gap: 16px; }
.team { flex: 1 0 0; min-width: 0; display: flex; align-items: center; gap: 10px; }
.team-a { justify-content: flex-end; }
.team-name {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.vs {
  width: 72px;
  flex-shrink: 0;
  text-align: center;
  color: var(--color-muted);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 20px;
}

@media (max-width: 640px) {
  .teams { gap: 8px; }
  .team { gap: 6px; }
  .team-name { font-size: 14px; }
  .vs { width: 32px; font-size: 16px; }
}
</style>

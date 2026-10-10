<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import TeamLogo from './TeamLogo.vue'
import StatusBadge from './StatusBadge.vue'
import { formatDateRange, formatTournamentFormat, initials } from '@/utils/format'

const props = defineProps({
  tournament: { type: Object, required: true },
})

const MAX_LOGOS = 5
const shownTeams = computed(() => props.tournament.teams.slice(0, MAX_LOGOS))
const extraCount = computed(() => props.tournament.teams.length - MAX_LOGOS)
const logoText = computed(() => initials(props.tournament.name, 3))
</script>

<template>
  <RouterLink :to="{ name: 'tournament-detail', params: { id: tournament.id } }" class="card">
    <div class="top">
      <span class="logo">
        <img v-if="tournament.logoUrl" :src="tournament.logoUrl" :alt="tournament.name" />
        <template v-else>{{ logoText }}</template>
      </span>
      <div class="tags">
        <span class="game-tag">{{ tournament.game.name }}</span>
        <StatusBadge :status="tournament.status" />
      </div>
    </div>

    <h3 class="name">{{ tournament.name }}</h3>
    <p v-if="tournament.champion" class="info champion">แชมป์: {{ tournament.champion.name }}</p>
    <p v-else class="info">
      {{ formatDateRange(tournament.startDate, tournament.endDate) }}&nbsp;&nbsp;·&nbsp;&nbsp;{{ formatTournamentFormat(tournament.format, tournament.totalGames) }}
    </p>

    <div class="teams">
      <div class="logo-stack">
        <TeamLogo v-for="team in shownTeams" :key="team.id" :team="team" :size="32" :font-size="11" />
        <TeamLogo v-if="extraCount > 0" :label="`+${extraCount}`" :size="32" :font-size="11" muted />
      </div>
      <span class="team-count">{{ tournament.teams.length }} ทีม</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  transition: border-color 0.15s;
}
.card:hover { border-color: var(--color-muted); }
.top { display: flex; align-items: center; gap: 14px; }
.logo {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 17px;
}
.logo img { width: 100%; height: 100%; object-fit: cover; }
.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.game-tag {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-2);
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}
.name {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 22px;
}
.info { color: var(--color-muted); font-size: 15px; }
.champion { color: var(--color-accent); }
.teams { display: flex; align-items: center; justify-content: space-between; }
.logo-stack { display: flex; }
.logo-stack > :not(:last-child) { margin-right: -8px; }
.team-count { color: var(--color-muted); font-size: 14px; font-weight: 600; }
</style>

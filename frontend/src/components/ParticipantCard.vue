<script setup>
import { initials } from '@/utils/format'

defineProps({
  team: { type: Object, required: true },
})
</script>

<template>
  <article class="participant">
    <span class="logo">
      <img v-if="team.logoUrl" :src="team.logoUrl" :alt="team.name" />
      <template v-else>{{ initials(team.name) }}</template>
    </span>
    <div class="text">
      <span class="name">{{ team.name }}</span>
      <span class="players">ผู้เล่น {{ team.playerCount }} คน</span>
    </div>
    <span class="chevron" aria-hidden="true">›</span>
  </article>
</template>

<style scoped>
.participant {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: border-color 0.15s;
}
.participant:hover { border-color: var(--color-muted); }
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
  font-size: 19px;
}
.logo img { width: 100%; height: 100%; object-fit: cover; }
.text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.name {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.players { color: var(--color-muted); font-size: 13px; }
.chevron { color: var(--color-muted); font-size: 22px; font-weight: 600; }
</style>

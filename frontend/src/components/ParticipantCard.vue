<script setup>
import { initials } from '@/utils/format'

defineProps({
  team: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  points: { type: Number, default: null },
})
defineEmits(['select'])
</script>

<template>
  <button type="button" class="participant" :class="{ selected, compact }" :aria-pressed="selected" @click="$emit('select', team)">
    <span class="logo">
      <img v-if="team.logoUrl" :src="team.logoUrl" :alt="team.name" />
      <template v-else>{{ initials(team.name) }}</template>
    </span>
    <div class="text">
      <span class="name">{{ team.name }}</span>
      <span class="players">ผู้เล่น {{ team.playerCount }} คน<template v-if="points !== null"> · {{ points }} คะแนน</template></span>
    </div>
    <span class="chevron" aria-hidden="true">›</span>
  </button>
</template>

<style scoped>
.participant {
  width: 100%;
  color: inherit;
  font: inherit;
  text-align: left;
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
.participant.selected { border: 2px solid var(--color-accent); padding: 15px; }
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
.participant.compact {
  padding: 14px;
  border: 2px solid transparent;
}
.participant.compact .logo { width: 48px; height: 48px; border-radius: 8px; font-family: var(--font-body); font-size: 16px; }
.participant.compact .chevron { font-size: 18px; }
.participant.compact:hover { border-color: var(--color-border); }
.participant.compact.selected { border-color: var(--color-accent); padding: 14px; }
</style>

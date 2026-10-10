<script setup>
import { computed } from 'vue'
import { initials } from '@/utils/format'

const props = defineProps({
  team: { type: Object, default: null },
  label: { type: String, default: '' },
  size: { type: Number, default: 36 },
  fontSize: { type: Number, default: 12 },
  muted: { type: Boolean, default: false },
})

const text = computed(() => props.label || (props.team ? initials(props.team.name) : ''))
</script>

<template>
  <span
    class="team-logo"
    :class="{ muted }"
    :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${fontSize}px` }"
    :title="team?.name"
  >
    <img v-if="team?.logoUrl" :src="team.logoUrl" :alt="team.name" />
    <template v-else>{{ text }}</template>
  </span>
</template>

<style scoped>
.team-logo {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2px solid var(--color-surface);
  border-radius: var(--radius-pill);
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
}
.team-logo img { width: 100%; height: 100%; object-fit: cover; }
.team-logo.muted {
  background: var(--color-bg);
  color: var(--color-muted);
  font-family: var(--font-body);
}
</style>

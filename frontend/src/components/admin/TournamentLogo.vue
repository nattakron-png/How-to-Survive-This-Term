<script setup>
import { computed } from 'vue'
import { initials } from '@/utils/format'

const props = defineProps({
  name: { type: String, default: '' },
  logoUrl: { type: String, default: null },
  size: { type: Number, default: 32 },
  fontSize: { type: Number, default: 11 },
  surface: { type: Boolean, default: false },
})

const text = computed(() => (props.name.trim() ? initials(props.name, 3) : '?'))
</script>

<template>
  <span
    class="t-logo"
    :class="{ surface }"
    :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${fontSize}px` }"
  >
    <img v-if="logoUrl" :src="logoUrl" :alt="name" />
    <template v-else>{{ text }}</template>
  </span>
</template>

<style scoped>
.t-logo {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 25%;
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
}
.t-logo.surface { background: var(--color-bg); border: 1px solid var(--color-border); }
.t-logo img { width: 100%; height: 100%; object-fit: cover; }
</style>

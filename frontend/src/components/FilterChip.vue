<script setup>
defineProps({
  label: { type: String, required: true },
  active: { type: Boolean, default: false },
  variant: { type: String, default: 'filled' },
})
defineEmits(['click'])
</script>

<template>
  <button
    type="button"
    class="chip"
    :class="[variant, { active }]"
    :aria-pressed="active"
    @click="$emit('click')"
  >{{ label }}</button>
</template>

<style scoped>
.chip {
  border: 0;
  border-radius: var(--radius-pill);
  color: var(--color-text);
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
}
.filled { padding: 8px 18px; background: var(--color-surface-2); }
.outlined {
  padding: 9px 19px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}
.chip.active {
  border-color: transparent;
  background: var(--color-accent);
  color: var(--color-on-accent);
}
</style>

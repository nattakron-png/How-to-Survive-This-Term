<script setup>
defineProps({
  options: { type: Array, required: true },
  ariaLabel: { type: String, default: '' },
})
const model = defineModel({ type: String, required: true })
</script>

<template>
  <label class="select">
    <select v-model="model" :aria-label="ariaLabel">
      <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
    </select>
    <span class="arrow" aria-hidden="true">▾</span>
  </label>
</template>

<style scoped>
.select { position: relative; display: inline-flex; }
select {
  appearance: none;
  padding: 12px 40px 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}
select:focus-visible { outline: 0; border-color: var(--color-accent); }
.arrow {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-muted);
  font-size: 15px;
  pointer-events: none;
}
</style>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
})

const STATUS = {
  ONGOING: { label: 'กำลังแข่ง', tone: 'warning' },
  UPCOMING: { label: 'กำลังจะเริ่ม', tone: 'info' },
  FINISHED: { label: 'จบแล้ว', tone: 'neutral' },
}

const config = computed(() => STATUS[props.status] ?? STATUS.FINISHED)
</script>

<template>
  <span class="badge" :class="config.tone">{{ config.label }}</span>
</template>

<style scoped>
.badge {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}
.warning { background: var(--color-warning-bg); color: var(--color-warning); }
.info { background: var(--color-info-bg); color: var(--color-info); }
.neutral { background: var(--color-neutral-bg); color: var(--color-muted); }
</style>

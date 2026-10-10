<script setup>
import { computed } from 'vue'

const props = defineProps({
  totalItems: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  note: { type: String, default: '' },
  unit: { type: String, default: 'รายการ' },
})
const page = defineModel({ type: Number, required: true })

const totalPages = computed(() => Math.max(1, Math.ceil(props.totalItems / props.pageSize)))
const from = computed(() => (props.totalItems === 0 ? 0 : (page.value - 1) * props.pageSize + 1))
const to = computed(() => Math.min(page.value * props.pageSize, props.totalItems))

const pages = computed(() => {
  const total = totalPages.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const current = page.value
  const start = Math.max(2, Math.min(current - 1, total - 4))
  const end = Math.min(total - 1, Math.max(current + 1, 5))
  const list = [1]
  if (start > 2) list.push('start-gap')
  for (let p = start; p <= end; p += 1) list.push(p)
  if (end < total - 1) list.push('end-gap')
  list.push(total)
  return list
})

function go(p) {
  if (p >= 1 && p <= totalPages.value) page.value = p
}
</script>

<template>
  <div class="pagination">
    <p class="summary">แสดง {{ from }}–{{ to }} จาก {{ totalItems }} {{ unit }}<template v-if="note"> · {{ note }}</template></p>
    <nav class="pages" aria-label="เปลี่ยนหน้า">
      <button type="button" class="page" :disabled="page === 1" aria-label="หน้าก่อนหน้า" @click="go(page - 1)">‹</button>
      <template v-for="p in pages" :key="p">
        <span v-if="typeof p === 'string'" class="gap" aria-hidden="true">…</span>
        <button
          v-else
          type="button"
          class="page"
          :class="{ active: p === page }"
          :aria-current="p === page ? 'page' : undefined"
          @click="go(p)"
        >{{ p }}</button>
      </template>
      <button type="button" class="page" :disabled="page === totalPages" aria-label="หน้าถัดไป" @click="go(page + 1)">›</button>
    </nav>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.summary { color: var(--color-muted); font-size: 15px; }
.pages { display: flex; gap: 8px; }
.page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 15px;
  font-weight: 600;
}
.page.active { border-color: transparent; background: var(--color-accent); color: var(--color-on-accent); }
.page:disabled { opacity: 0.4; cursor: default; }
.gap { display: flex; align-items: flex-end; justify-content: center; width: 20px; color: var(--color-muted); }
@media (max-width: 640px) {
  .pages { gap: 6px; }
  .page { width: 36px; height: 36px; font-size: 14px; }
}
</style>

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

function go(p) {
  if (p >= 1 && p <= totalPages.value) page.value = p
}
</script>

<template>
  <div class="pagination">
    <p class="summary">แสดง {{ from }}–{{ to }} จาก {{ totalItems }} {{ unit }}<template v-if="note"> · {{ note }}</template></p>
    <nav class="pages" aria-label="เปลี่ยนหน้า">
      <button type="button" class="page" :disabled="page === 1" aria-label="หน้าก่อนหน้า" @click="go(page - 1)">‹</button>
      <button
        v-for="p in totalPages"
        :key="p"
        type="button"
        class="page"
        :class="{ active: p === page }"
        :aria-current="p === page ? 'page' : undefined"
        @click="go(p)"
      >{{ p }}</button>
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
</style>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

defineProps({
  title: { type: String, required: true },
  rows: { type: Array, default: () => [] },
  message: { type: String, default: '' },
  confirmLabel: { type: String, default: 'ยืนยัน' },
  cancelLabel: { type: String, default: 'กลับไปแก้ไข' },
  busy: { type: Boolean, default: false },
  error: { type: String, default: '' },
})
const emit = defineEmits(['cancel', 'confirm'])

const cancelButton = ref(null)

function onKeydown(event) {
  if (event.key === 'Escape') emit('cancel')
}

onMounted(async () => {
  document.addEventListener('keydown', onKeydown)
  await nextTick()
  cancelButton.value?.focus()
})
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="overlay" @click.self="emit('cancel')">
    <div class="dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title">
      <span class="icon" aria-hidden="true">!</span>
      <h2 id="confirm-title" class="title">{{ title }}</h2>
      <dl v-if="rows.length" class="rows">
        <div v-for="row in rows" :key="row.label" class="row">
          <dt>{{ row.label }}</dt>
          <dd>{{ row.value }}</dd>
        </div>
      </dl>
      <p v-if="message" class="message">{{ message }}</p>
      <p v-if="error" class="admin-error">{{ error }}</p>
      <div class="actions">
        <button ref="cancelButton" type="button" class="admin-btn admin-btn-outline" :disabled="busy" @click="emit('cancel')">{{ cancelLabel }}</button>
        <button type="button" class="admin-btn admin-btn-accent" :disabled="busy" @click="emit('confirm')">
          {{ busy ? 'กำลังบันทึก…' : confirmLabel }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.65);
}
.dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 560px;
  max-width: 100%;
  padding: 32px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-pill);
  background: var(--color-warning-bg);
  color: var(--color-warning);
  font-weight: 700;
  font-size: 18px;
}
.title { font-family: var(--font-heading); font-weight: 600; font-size: 24px; }
.rows { display: flex; flex-direction: column; gap: 12px; margin: 0; padding: 16px 20px; border-radius: var(--radius-md); background: var(--color-bg); }
.row { display: flex; justify-content: space-between; gap: 16px; font-size: 14px; }
.row dt { color: var(--color-muted); }
.row dd { margin: 0; font-weight: 600; text-align: right; }
.message { color: var(--color-text); font-size: 14px; line-height: 1.6; }
.actions { display: flex; justify-content: flex-end; gap: 12px; }

@media (max-width: 640px) {
  .dialog { padding: 20px; }
  .title { font-size: 20px; }
  .actions .admin-btn { flex: 1; }
}
</style>

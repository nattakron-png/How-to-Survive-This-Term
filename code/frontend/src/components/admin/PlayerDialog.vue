<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ROLE_SUGGESTIONS, savePlayer } from '@/api/admin'

const props = defineProps({
  player: { type: Object, default: null },
  teams: { type: Array, required: true },
  games: { type: Array, required: true },
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => Boolean(props.player))
const form = reactive({
  name: props.player?.name ?? '',
  role: props.player?.role ?? '',
  description: props.player?.description ?? '',
  teamId: props.player?.teamId == null ? '' : String(props.player.teamId),
})

const groupedTeams = computed(() =>
  props.games
    .map((g) => ({ game: g, teams: props.teams.filter((t) => t.gameId === g.id).sort((a, b) => a.name.localeCompare(b.name)) }))
    .filter((group) => group.teams.length),
)
const selectedTeam = computed(() => props.teams.find((t) => String(t.id) === form.teamId) ?? null)
const selectedGame = computed(() => props.games.find((g) => g.id === selectedTeam.value?.gameId) ?? null)
const roleOptions = computed(() =>
  selectedGame.value ? ROLE_SUGGESTIONS[selectedGame.value.code] ?? [] : [...new Set(Object.values(ROLE_SUGGESTIONS).flat())],
)

const submitted = ref(false)
const saving = ref(false)
const errors = ref({})
const saveError = ref('')
const nameInput = ref(null)

function clientErrors() {
  const list = {}
  if (!form.name.trim()) list.name = 'กรุณากรอกชื่อผู้เล่น'
  if (!form.role.trim()) list.role = 'กรุณากรอกตำแหน่ง'
  return list
}

async function submit() {
  submitted.value = true
  saveError.value = ''
  errors.value = clientErrors()
  if (Object.keys(errors.value).length) return
  saving.value = true
  try {
    const saved = await savePlayer(props.player?.id ?? null, {
      ...form,
      teamId: form.teamId === '' ? null : Number(form.teamId),
    })
    emit('saved', saved)
  } catch (e) {
    if (e.fields) errors.value = e.fields
    else saveError.value = e.status ? `บันทึกไม่สำเร็จ: ${e.message}` : 'บันทึกไม่สำเร็จ ลองใหม่อีกครั้ง'
  } finally {
    saving.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Escape') emit('close')
}

onMounted(async () => {
  document.addEventListener('keydown', onKeydown)
  await nextTick()
  nameInput.value?.focus()
})
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="player-dialog-title" novalidate @submit.prevent="submit">
      <header class="dialog-head">
        <h2 id="player-dialog-title" class="admin-panel-title">{{ isEdit ? 'แก้ไขผู้เล่น' : 'เพิ่มผู้เล่น' }}</h2>
        <button type="button" class="close" aria-label="ปิด" @click="emit('close')">✕</button>
      </header>

      <div class="admin-field">
        <label class="admin-label" for="player-name">ชื่อผู้เล่น</label>
        <input
          id="player-name"
          ref="nameInput"
          v-model="form.name"
          class="admin-input"
          :class="{ invalid: submitted && errors.name }"
          maxlength="150"
          placeholder="ชื่อในเกม เช่น FF.Tan"
        />
        <p v-if="submitted && errors.name" class="admin-error">{{ errors.name }}</p>
      </div>

      <div class="admin-field">
        <label class="admin-label" for="player-team">ทีม</label>
        <select id="player-team" v-model="form.teamId" class="admin-input">
          <option value="">ยังไม่มีทีม</option>
          <optgroup v-for="group in groupedTeams" :key="group.game.id" :label="group.game.name">
            <option v-for="t in group.teams" :key="t.id" :value="String(t.id)">{{ t.name }}</option>
          </optgroup>
        </select>
        <p class="admin-hint">เว้นว่างไว้ก่อนได้ แล้วค่อยใส่ทีมทีหลังจากหน้านี้หรือหน้าแก้ไขทีม</p>
      </div>

      <div class="admin-field">
        <label class="admin-label" for="player-role">ตำแหน่ง</label>
        <input
          id="player-role"
          v-model="form.role"
          class="admin-input"
          :class="{ invalid: submitted && errors.role }"
          maxlength="100"
          list="player-role-options"
          :placeholder="selectedGame ? `ตำแหน่งใน ${selectedGame.name}` : 'เช่น Support'"
        />
        <datalist id="player-role-options">
          <option v-for="r in roleOptions" :key="r" :value="r"></option>
        </datalist>
        <p v-if="submitted && errors.role" class="admin-error">{{ errors.role }}</p>
      </div>

      <div class="admin-field">
        <label class="admin-label" for="player-desc">คำอธิบาย</label>
        <textarea id="player-desc" v-model="form.description" class="admin-input" rows="2" placeholder="เช่น กัปตันทีม หรือเกมที่ถนัด"></textarea>
      </div>

      <footer class="dialog-actions">
        <p v-if="saveError" class="admin-error">{{ saveError }}</p>
        <button type="button" class="admin-btn admin-btn-outline" @click="emit('close')">ยกเลิก</button>
        <button type="submit" class="admin-btn admin-btn-accent" :disabled="saving">{{ saving ? 'กำลังบันทึก…' : 'บันทึก' }}</button>
      </footer>
    </form>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.6);
}
.dialog {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 520px;
  max-width: 100%;
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  padding: 28px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}
.dialog-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.close { padding: 4px 8px; border: 0; background: none; color: var(--color-muted); font-size: 16px; cursor: pointer; }
.close:hover { color: var(--color-text); }
.dialog-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 12px; }
.dialog-actions .admin-error { margin-right: auto; }

@media (max-width: 640px) {
  .dialog { padding: 20px; }
  .dialog-actions .admin-btn { flex: 1; }
}
</style>

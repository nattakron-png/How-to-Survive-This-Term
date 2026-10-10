<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import TournamentLogo from '@/components/admin/TournamentLogo.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import {
  DEFAULT_PLACEMENT_POINTS,
  createTournament,
  formatForGame,
  getTournamentForm,
  updateTournament,
  updateTournamentStatus,
} from '@/api/admin'
import { getGames } from '@/mock/queries'
import { formatDate, formatTournamentFormat } from '@/utils/format'

const props = defineProps({
  id: { type: String, default: null },
})

const router = useRouter()
const games = getGames()
const isEdit = computed(() => props.id !== null)
const source = ref(isEdit.value ? getTournamentForm(props.id) : null)
const notFound = computed(() => isEdit.value && !source.value)

const form = reactive({
  name: source.value?.name ?? '',
  gameId: source.value ? String(source.value.gameId) : '',
  startDate: source.value?.startDate ?? '',
  endDate: source.value?.endDate ?? '',
  description: source.value?.description ?? '',
  logoUrl: source.value?.logoUrl ?? null,
  totalGames: source.value?.totalGames ?? 10,
  pointsPerKill: source.value?.pointsPerKill ?? 1,
  placementPoints: [...(source.value?.placementPoints ?? DEFAULT_PLACEMENT_POINTS)],
})

const errors = ref({})
const submitted = ref(false)
const saving = ref(false)
const saveError = ref('')
const logoError = ref('')

const selectedGame = computed(() => games.find((g) => String(g.id) === form.gameId) ?? null)
const format = computed(() => (form.gameId ? formatForGame(form.gameId) : null))
const isPoints = computed(() => format.value === 'POINTS')
const gameLocked = computed(() => source.value?.gameLocked ?? false)
const pointsLocked = computed(() => source.value?.pointsLocked ?? false)
const usingDefaultPoints = computed(() => form.placementPoints.every((p, i) => Number(p) === DEFAULT_PLACEMENT_POINTS[i]))

const liveErrors = computed(() => {
  const list = { ...errors.value }
  if (form.startDate && form.endDate && form.endDate < form.startDate) list.endDate = 'วันจบต้องไม่ก่อนวันเริ่มแข่ง'
  else if (list.endDate === 'วันจบต้องไม่ก่อนวันเริ่มแข่ง') delete list.endDate
  return list
})

const gameHint = computed(() => {
  if (gameLocked.value) return 'เปลี่ยนเกมไม่ได้ เพราะมีทีมในรายการแล้ว'
  if (isPoints.value) return 'Free Fire ใช้แบบเก็บคะแนน จึงมีช่องตั้งค่าเพิ่มด้านล่าง'
  return 'ROV, Valorant, Fighting Game แข่งแบบแพ้คัดออก · Free Fire แข่งแบบเก็บคะแนน (จะมีช่องจำนวนเกมเพิ่ม)'
})

const previewMeta = computed(() => {
  const parts = []
  if (form.startDate) parts.push(formatDate(form.startDate))
  if (format.value) parts.push(formatTournamentFormat(format.value, form.totalGames))
  return parts.join(' · ') || 'ยังไม่ได้เลือกวันและเกม'
})

const statusSteps = [
  { status: 'UPCOMING', text: 'ผู้ชมเห็นแล้ว เพิ่มทีมและสร้างสายได้' },
  { status: 'ONGOING', text: 'Admin กรอกผลแต่ละแมตช์' },
  { status: 'FINISHED', text: 'ประกาศแชมป์ ผลถูกล็อก' },
]
const currentStatus = computed(() => source.value?.status ?? 'UPCOMING')
const nextStep = computed(() => {
  if (!isEdit.value) return null
  if (currentStatus.value === 'UPCOMING') {
    return {
      status: 'ONGOING',
      label: 'เริ่มการแข่งขัน',
      blocked: source.value.setupCreated ? '' : isPoints.value ? 'ต้องสร้างตารางเกมก่อน' : 'ต้องสร้างสายการแข่งก่อน',
    }
  }
  if (currentStatus.value === 'ONGOING') {
    return {
      status: 'FINISHED',
      label: 'จบรายการและประกาศแชมป์',
      blocked: source.value.complete ? '' : isPoints.value ? 'ต้องกรอกผลครบทุกเกมก่อน' : 'ต้องกรอกผลนัดชิงก่อน',
    }
  }
  return null
})
const changingStatus = ref(false)

async function advanceStatus() {
  if (!nextStep.value || nextStep.value.blocked) return
  changingStatus.value = true
  try {
    await updateTournamentStatus(props.id, nextStep.value.status)
    source.value = getTournamentForm(props.id)
  } finally {
    changingStatus.value = false
  }
}

function onLogoChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  logoError.value = ''
  if (!file) return
  if (!['image/png', 'image/jpeg'].includes(file.type)) {
    logoError.value = 'รองรับเฉพาะไฟล์ PNG หรือ JPG'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    logoError.value = 'ไฟล์ต้องไม่เกิน 2 MB'
    return
  }
  const reader = new FileReader()
  reader.onload = () => { form.logoUrl = reader.result }
  reader.readAsDataURL(file)
}

function resetPoints() {
  form.placementPoints = [...DEFAULT_PLACEMENT_POINTS]
}

function clientErrors() {
  const list = {}
  if (!form.name.trim()) list.name = 'กรุณากรอกชื่อรายการ'
  if (!form.gameId) list.gameId = 'กรุณาเลือกเกม'
  if (!form.startDate) list.startDate = 'กรุณาเลือกวันเริ่มแข่ง'
  if (!form.endDate) list.endDate = 'กรุณาเลือกวันจบ'
  return list
}

async function submit() {
  submitted.value = true
  saveError.value = ''
  errors.value = clientErrors()
  if (Object.keys(liveErrors.value).length) return
  saving.value = true
  try {
    const payload = { ...form, placementPoints: form.placementPoints.map(Number) }
    if (isEdit.value) {
      await updateTournament(props.id, payload)
      router.push('/admin/tournaments')
    } else {
      const created = await createTournament(payload)
      router.push(`/admin/tournaments/${created.id}/teams`)
    }
  } catch (e) {
    if (e.fields) errors.value = e.fields
    else saveError.value = e.status ? `บันทึกไม่สำเร็จ: ${e.message}` : 'บันทึกไม่สำเร็จ ลองใหม่อีกครั้ง'
  } finally {
    saving.value = false
  }
}

const showError = (key) => (submitted.value || key === 'endDate') && liveErrors.value[key]
</script>

<template>
  <AdminLayout>
    <div class="admin-page">
      <nav class="admin-crumbs" aria-label="breadcrumb">
        <RouterLink to="/admin/tournaments">รายการแข่ง</RouterLink>
        <span>/</span>
        <span class="current">{{ isEdit ? 'แก้ไขรายการแข่ง' : 'สร้างรายการแข่ง' }}</span>
      </nav>
      <h1 class="admin-title">{{ isEdit ? 'แก้ไขรายการแข่ง' : 'สร้างรายการแข่ง' }}</h1>

      <section v-if="notFound" class="admin-panel missing">
        <p>ไม่พบรายการแข่งนี้</p>
        <RouterLink to="/admin/tournaments" class="admin-btn admin-btn-outline">กลับไปหน้ารายการ</RouterLink>
      </section>

      <div v-else class="layout">
        <form class="admin-panel form" novalidate @submit.prevent="submit">
          <div class="top">
            <label class="logo-drop" :class="{ filled: form.logoUrl }">
              <input type="file" accept="image/png,image/jpeg" class="sr-only" @change="onLogoChange" />
              <img v-if="form.logoUrl" :src="form.logoUrl" alt="โลโก้รายการ" class="logo-img" />
              <template v-else>
                <span class="logo-icon" aria-hidden="true">⇪</span>
                <span class="logo-title">อัปโหลดโลโก้รายการ</span>
                <span class="logo-hint">PNG, JPG ไม่เกิน 2 MB</span>
              </template>
            </label>

            <div class="top-fields">
              <div class="admin-field">
                <label class="admin-label" for="t-name">ชื่อรายการ</label>
                <input
                  id="t-name"
                  v-model="form.name"
                  class="admin-input"
                  :class="{ invalid: showError('name') }"
                  maxlength="100"
                  placeholder="เช่น KKU ROV Cup 2027"
                />
                <p v-if="showError('name')" class="admin-error">{{ liveErrors.name }}</p>
              </div>
              <div class="admin-field">
                <label class="admin-label" for="t-game">เกม</label>
                <select
                  id="t-game"
                  v-model="form.gameId"
                  class="admin-input"
                  :class="{ invalid: showError('gameId') }"
                  :disabled="gameLocked"
                >
                  <option value="" disabled>เลือกเกม</option>
                  <option v-for="g in games" :key="g.id" :value="String(g.id)">{{ g.name }}</option>
                </select>
                <p v-if="showError('gameId')" class="admin-error">{{ liveErrors.gameId }}</p>
                <p v-else class="admin-hint">{{ gameHint }}</p>
              </div>
            </div>
          </div>
          <p v-if="logoError" class="admin-error">{{ logoError }}</p>
          <button v-if="form.logoUrl" type="button" class="text-btn" @click="form.logoUrl = null">ลบโลโก้</button>

          <div class="two-col">
            <div class="admin-field">
              <label class="admin-label" for="t-start">วันเริ่มแข่ง</label>
              <input id="t-start" v-model="form.startDate" type="date" class="admin-input" :class="{ invalid: showError('startDate') }" />
              <p v-if="showError('startDate')" class="admin-error">{{ liveErrors.startDate }}</p>
            </div>
            <div class="admin-field">
              <label class="admin-label" for="t-end">วันจบ</label>
              <input
                id="t-end"
                v-model="form.endDate"
                type="date"
                class="admin-input"
                :class="{ invalid: showError('endDate') }"
                :min="form.startDate || undefined"
              />
              <p v-if="showError('endDate')" class="admin-error">{{ liveErrors.endDate }}</p>
            </div>
          </div>

          <section v-if="isPoints" class="points">
            <div>
              <h2 class="points-title">ตั้งค่าแบบเก็บคะแนน</h2>
              <p class="admin-hint">
                แสดงเฉพาะเกม Free Fire · บันทึกเป็น total_games, points_per_kill และ tournament_placement_points
              </p>
            </div>
            <p v-if="pointsLocked" class="locked-note">สร้างตารางเกมแล้ว จึงแก้ค่าส่วนนี้ไม่ได้</p>

            <div class="points-row">
              <div class="admin-field">
                <span class="admin-label">จำนวนเกม</span>
                <div class="segmented" role="radiogroup" aria-label="จำนวนเกม">
                  <button
                    v-for="n in [10, 15]"
                    :key="n"
                    type="button"
                    role="radio"
                    :aria-checked="form.totalGames === n"
                    :class="{ active: form.totalGames === n }"
                    :disabled="pointsLocked"
                    @click="form.totalGames = n"
                  >{{ n }} เกม</button>
                </div>
              </div>
              <div class="admin-field kill-field">
                <label class="admin-label" for="t-kill">คะแนนต่อ 1 kill</label>
                <input
                  id="t-kill"
                  v-model.number="form.pointsPerKill"
                  type="number"
                  min="0"
                  step="1"
                  class="admin-input"
                  :class="{ invalid: showError('pointsPerKill') }"
                  :disabled="pointsLocked"
                />
              </div>
            </div>
            <p v-if="showError('pointsPerKill')" class="admin-error">{{ liveErrors.pointsPerKill }}</p>

            <div class="placement-head">
              <span class="admin-label">คะแนนตามอันดับ</span>
              <span v-if="usingDefaultPoints" class="admin-badge accent">ค่าเริ่มต้น FFWS</span>
              <button v-else-if="!pointsLocked" type="button" class="text-btn" @click="resetPoints">ใช้ค่าเริ่มต้น FFWS</button>
            </div>
            <div class="placement-grid">
              <label v-for="(_, i) in form.placementPoints" :key="i" class="placement" :class="{ first: i === 0 }">
                <span class="placement-label">อันดับ {{ i + 1 }}</span>
                <input
                  v-model.number="form.placementPoints[i]"
                  type="number"
                  min="0"
                  step="1"
                  class="placement-input"
                  :disabled="pointsLocked"
                  :aria-label="`คะแนนอันดับ ${i + 1}`"
                />
              </label>
            </div>
            <p v-if="showError('placementPoints')" class="admin-error">{{ liveErrors.placementPoints }}</p>
            <p class="admin-hint">
              ทีมรับคะแนนอันดับ + (kill × คะแนนต่อ kill) ต่อเกม · คะแนนเท่ากันตัดสินด้วย Booyah → Kills → อันดับเกมล่าสุด ·
              แก้ได้จนกว่าจะสร้างตารางเกม
            </p>
          </section>

          <div class="admin-field">
            <label class="admin-label" for="t-desc">รายละเอียดและกติกา</label>
            <textarea
              id="t-desc"
              v-model="form.description"
              class="admin-input"
              rows="3"
              placeholder="กติกาการแข่ง รางวัล และช่องทางติดต่อผู้จัด"
            ></textarea>
          </div>

          <div class="form-actions">
            <p v-if="saveError" class="admin-error">{{ saveError }}</p>
            <RouterLink to="/admin/tournaments" class="admin-btn admin-btn-outline">ยกเลิก</RouterLink>
            <button type="submit" class="admin-btn admin-btn-accent" :disabled="saving">{{ saving ? 'กำลังบันทึก…' : 'บันทึก' }}</button>
          </div>
        </form>

        <aside class="side">
          <section class="admin-panel">
            <h2 class="side-title">ตัวอย่างการ์ดที่ผู้ชมเห็น</h2>
            <div class="preview">
              <div class="preview-chips">
                <TournamentLogo :name="form.name" :logo-url="form.logoUrl" :size="40" :font-size="12" />
                <span v-if="selectedGame" class="admin-badge neutral">{{ selectedGame.name }}</span>
                <StatusBadge :status="currentStatus" />
              </div>
              <p class="preview-name">{{ form.name.trim() || 'ชื่อรายการ' }}</p>
              <p class="preview-meta">{{ previewMeta }}</p>
            </div>
          </section>

          <section class="admin-panel">
            <h2 class="side-title">ลำดับสถานะของรายการ</h2>
            <ol class="steps">
              <li
                v-for="(step, i) in statusSteps"
                :key="step.status"
                class="step"
                :class="{ current: isEdit && step.status === currentStatus }"
              >
                <span class="step-no">{{ i + 1 }}</span>
                <StatusBadge :status="step.status" />
                <span class="step-text">{{ step.text }}</span>
              </li>
            </ol>
            <div v-if="nextStep" class="advance">
              <button
                type="button"
                class="admin-btn admin-btn-outline admin-btn-block"
                :disabled="Boolean(nextStep.blocked) || changingStatus"
                @click="advanceStatus"
              >{{ changingStatus ? 'กำลังเปลี่ยนสถานะ…' : nextStep.label }}</button>
              <p v-if="nextStep.blocked" class="admin-hint">{{ nextStep.blocked }}</p>
            </div>
            <RouterLink v-if="isEdit" :to="`/admin/tournaments/${id}/teams`" class="side-link">
              {{ isPoints ? 'จัดการทีมและตารางเกม →' : 'จัดการทีมและสายการแข่ง →' }}
            </RouterLink>
          </section>
        </aside>
      </div>
    </div>
  </AdminLayout>
</template>

<style scoped>
.layout { display: flex; align-items: flex-start; gap: 24px; }
.form { flex: 1; min-width: 0; gap: 20px; padding: 32px; }
.side { display: flex; flex-direction: column; gap: 24px; width: 420px; flex-shrink: 0; }
.missing { align-items: flex-start; }

.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }

.top { display: flex; gap: 24px; }
.top-fields { display: flex; flex: 1; flex-direction: column; gap: 16px; min-width: 0; }
.logo-drop {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 168px;
  height: 168px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1.5px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  text-align: center;
  cursor: pointer;
}
.logo-drop:hover { border-color: var(--color-accent); }
.logo-drop.filled { border-style: solid; }
.logo-img { width: 100%; height: 100%; object-fit: cover; }
.logo-icon { color: var(--color-accent); font-size: 22px; }
.logo-title { font-weight: 600; font-size: 13px; }
.logo-hint { color: var(--color-muted); font-size: 11px; }
.text-btn { align-self: flex-start; padding: 0; border: 0; background: none; color: var(--color-accent); font-size: 13px; cursor: pointer; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }

.points {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 253, 131, 0.35);
  border-radius: var(--radius-md);
  background: var(--color-bg);
}
.points-title { margin-bottom: 4px; font-family: var(--font-heading); font-weight: 600; font-size: 20px; }
.locked-note { padding: 10px 14px; border-radius: var(--radius-sm); background: var(--color-warning-bg); color: var(--color-warning); font-size: 13px; }
.points-row { display: flex; flex-wrap: wrap; gap: 20px; }
.kill-field { width: 160px; }
.segmented { display: flex; padding: 4px; border-radius: var(--radius-sm); background: var(--color-surface); }
.segmented button {
  min-width: 88px;
  padding: 10px 18px;
  border: 0;
  border-radius: 8px;
  background: none;
  color: var(--color-text);
  font-family: var(--font-heading);
  font-weight: 500;
  font-size: 15px;
  cursor: pointer;
}
.segmented button.active { background: var(--color-accent); color: var(--color-on-accent); }
.segmented button:disabled { cursor: not-allowed; opacity: 0.6; }
.segmented button.active:disabled { opacity: 0.8; }

.placement-head { display: flex; align-items: center; gap: 10px; }
.placement-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px; }
.placement {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  cursor: text;
}
.placement:focus-within { border-color: var(--color-accent); }
.placement-label { color: var(--color-muted); font-size: 12px; }
.placement-input {
  width: 100%;
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-text);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 20px;
  color-scheme: dark;
}
.placement-input:focus { outline: 0; }
.placement-input:disabled { cursor: not-allowed; opacity: 0.7; }
.placement.first .placement-input { color: var(--color-accent); }

.form-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 12px; }
.form-actions .admin-error { margin-right: auto; }

.side-title { color: var(--color-text); font-size: 15px; font-weight: 600; }
.preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
}
.preview-chips { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.preview-name { overflow-wrap: anywhere; font-family: var(--font-heading); font-weight: 600; font-size: 20px; }
.preview-meta { color: var(--color-muted); font-size: 13px; }

.steps { display: flex; flex-direction: column; gap: 12px; margin: 0; padding: 0; list-style: none; }
.step { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 4px 0; font-size: 13px; }
.step-no { width: 14px; color: var(--color-muted); font-weight: 600; }
.step-text { color: var(--color-muted); }
.step.current { margin: 0 -10px; padding: 8px 10px; border-radius: var(--radius-sm); background: var(--color-surface-2); }
.step.current .step-text { color: var(--color-text); }
.advance { display: flex; flex-direction: column; gap: 6px; }
.side-link { color: var(--color-accent); font-size: 14px; font-weight: 500; }

@media (max-width: 1280px) {
  .layout { flex-direction: column; align-items: stretch; }
  .side { width: auto; display: grid; grid-template-columns: 1fr 1fr; }
}

@media (max-width: 760px) {
  .form { padding: 16px; }
  .top { flex-direction: column; }
  .logo-drop { width: 100%; height: 120px; }
  .two-col { grid-template-columns: 1fr; gap: 16px; }
  .points { padding: 16px; }
  .placement-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .side { grid-template-columns: 1fr; }
  .form-actions .admin-btn { flex: 1; }
}
</style>

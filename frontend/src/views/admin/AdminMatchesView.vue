<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import ConfirmDialog from '@/components/admin/ConfirmDialog.vue'
import TeamLogo from '@/components/TeamLogo.vue'
import { getFreeFireTournamentsToRecord, getMatchForResult, getMatchesToRecord, recordMatchResult } from '@/api/admin'
import { formatDate, formatRound, formatShortDate, formatTime, formatTournamentFormat } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const version = ref(0)
const lists = computed(() => {
  version.value
  return getMatchesToRecord()
})
const freeFire = computed(() => {
  version.value
  return getFreeFireTournamentsToRecord()
})
const allReady = computed(() => [...lists.value.overdue, ...lists.value.today])

const selectedId = computed(() => {
  const fromQuery = Number(route.query.match)
  if (allReady.value.some((m) => m.id === fromQuery)) return fromQuery
  return allReady.value[0]?.id ?? null
})
const match = computed(() => {
  version.value
  return selectedId.value ? getMatchForResult(selectedId.value) : null
})

function select(id) {
  router.replace({ query: { ...route.query, match: id } })
}

const form = reactive({ a: '', b: '', winner: '' })
watch(selectedId, () => {
  form.a = ''
  form.b = ''
  form.winner = ''
  saveError.value = ''
}, { immediate: false })

const scoreA = computed(() => (form.a === '' ? null : Number(form.a)))
const scoreB = computed(() => (form.b === '' ? null : Number(form.b)))
const filled = computed(() => scoreA.value !== null && scoreB.value !== null)

watch([scoreA, scoreB], ([a, b]) => {
  if (a === null || b === null || a === b || !match.value) return
  form.winner = String(a > b ? match.value.teamAId : match.value.teamBId)
})

const isValidScore = (x) => x !== null && Number.isInteger(x) && x >= 0
const checks = computed(() => {
  const m = match.value
  const nonNegative = isValidScore(scoreA.value) && isValidScore(scoreB.value)
  const notDraw = filled.value && scoreA.value !== scoreB.value
  const expected = !m || !notDraw ? null : scoreA.value > scoreB.value ? m.teamAId : m.teamBId
  const winnerOk = Boolean(expected) && Number(form.winner) === expected
  return [
    { key: 'neg', ok: nonNegative, text: 'คะแนนไม่ติดลบ' },
    { key: 'draw', ok: notDraw, text: 'คะแนนห้ามเสมอ' },
    { key: 'winner', ok: winnerOk, text: 'ผู้ชนะต้องเป็นทีมในแมตช์และคะแนนมากกว่า' },
  ]
})
const allOk = computed(() => checks.value.every((c) => c.ok))
const problems = computed(() => {
  if (!filled.value) return []
  const list = []
  if (!checks.value[0].ok) list.push('คะแนนต้องเป็นจำนวนเต็มตั้งแต่ 0')
  if (!checks.value[1].ok) list.push('คะแนนห้ามเสมอ แบบแพ้คัดออกต้องมีผู้ชนะ')
  if (checks.value[1].ok && !checks.value[2].ok) list.push('ทีมที่ชนะต้องมีคะแนนมากกว่า')
  return list
})
const scoreInvalid = computed(() => filled.value && (!checks.value[0].ok || !checks.value[1].ok))

const winnerTeam = computed(() => {
  const m = match.value
  if (!m) return null
  return [m.teamA, m.teamB].find((t) => t && String(t.id) === form.winner) ?? null
})
const loserSide = computed(() => {
  if (!winnerTeam.value || !match.value) return null
  return winnerTeam.value.id === match.value.teamAId ? 'B' : 'A'
})

function resetForm() {
  form.a = ''
  form.b = ''
  form.winner = ''
  saveError.value = ''
}

const confirming = ref(false)
const saving = ref(false)
const saveError = ref('')
const flash = ref(null)

const ERROR_TEXT = {
  ALREADY_RECORDED: 'แมตช์นี้มีผลแล้ว',
  NOT_READY: 'แมตช์นี้ยังกรอกผลไม่ได้',
  DRAW: 'คะแนนห้ามเสมอ',
  WRONG_WINNER: 'ทีมที่ชนะต้องมีคะแนนมากกว่า',
  INVALID_SCORE: 'คะแนนต้องเป็นจำนวนเต็มตั้งแต่ 0',
}

const confirmRows = computed(() => {
  const m = match.value
  if (!m) return []
  return [
    { label: 'แมตช์', value: `${m.teamA.name} vs ${m.teamB.name}` },
    { label: 'สกอร์', value: `${scoreA.value} – ${scoreB.value}` },
    { label: 'ผู้ชนะ', value: winnerTeam.value?.name ?? '-' },
  ]
})
const confirmMessage = computed(() => {
  const name = winnerTeam.value?.name ?? ''
  if (match.value?.isFinal) return `บันทึกแล้วแก้ไขไม่ได้ นี่คือนัดชิง ${name} จะเป็นแชมป์และรายการจะเปลี่ยนเป็น "จบแล้ว"`
  return `บันทึกแล้วแก้ไขไม่ได้ และ ${name} จะถูกส่งเข้าแมตช์ถัดไปของสายการแข่งทันที`
})

async function save() {
  saving.value = true
  saveError.value = ''
  const m = match.value
  try {
    const result = await recordMatchResult(m.id, {
      teamAScore: scoreA.value,
      teamBScore: scoreB.value,
      winnerTeamId: Number(form.winner),
    })
    flash.value = {
      text: result.finished
        ? `บันทึกผลแล้ว · ${winnerTeam.value.name} เป็นแชมป์ ${m.tournament.name}`
        : `บันทึกผลแล้ว · ${winnerTeam.value.name} เข้ารอบถัดไป`,
      tournamentId: m.tournament.id,
    }
    confirming.value = false
    resetForm()
    router.replace({ query: {} })
    version.value += 1
  } catch (e) {
    saveError.value = ERROR_TEXT[e.message] ?? (e.status ? `บันทึกไม่สำเร็จ: ${e.message}` : 'บันทึกไม่สำเร็จ ลองใหม่อีกครั้ง')
  } finally {
    saving.value = false
  }
}

const roundLabel = (m) => formatRound(m.roundNumber, m.teamCount)
</script>

<template>
  <AdminLayout>
    <div class="admin-page">
      <header>
        <h1 class="admin-title">กรอกผลแมตช์</h1>
        <p class="admin-subtitle">
          รอกรอกผล {{ lists.overdue.length }} แมตช์<template v-if="lists.today.length"> · แข่งวันนี้ {{ lists.today.length }} แมตช์</template>
        </p>
      </header>

      <p v-if="flash" class="flash" role="status">
        {{ flash.text }}
        <RouterLink :to="`/tournaments/${flash.tournamentId}?tab=bracket`" class="flash-link">ดูสายในหน้าผู้ชม</RouterLink>
      </p>

      <div class="layout">
        <aside class="list">
          <section v-if="lists.overdue.length" class="group">
            <h2 class="group-title">แข่งแล้ว รอกรอกผล</h2>
            <button
              v-for="m in lists.overdue"
              :key="m.id"
              type="button"
              class="card"
              :class="{ active: m.id === selectedId }"
              @click="select(m.id)"
            >
              <span class="logos">
                <TeamLogo :team="m.teamA" :size="30" :font-size="10" class="sq" />
                <TeamLogo :team="m.teamB" :size="30" :font-size="10" class="sq" />
              </span>
              <span class="card-text">
                <span class="card-title">{{ m.teamA.name }} <span class="vs">vs</span> {{ m.teamB.name }}</span>
                <span class="card-meta">{{ m.tournament.name }} · {{ m.scheduledAt ? `${formatShortDate(m.scheduledAt)} ${formatTime(m.scheduledAt)}` : 'ยังไม่กำหนดเวลา' }}</span>
              </span>
            </button>
          </section>

          <section v-if="lists.today.length" class="group">
            <h2 class="group-title">แข่งวันนี้</h2>
            <button
              v-for="m in lists.today"
              :key="m.id"
              type="button"
              class="card"
              :class="{ active: m.id === selectedId }"
              @click="select(m.id)"
            >
              <span class="logos">
                <TeamLogo :team="m.teamA" :size="30" :font-size="10" class="sq" />
                <TeamLogo :team="m.teamB" :size="30" :font-size="10" class="sq" />
              </span>
              <span class="card-text">
                <span class="card-title">{{ m.teamA.name }} <span class="vs">vs</span> {{ m.teamB.name }}</span>
                <span class="card-meta">{{ m.tournament.name }} · {{ m.scheduledAt ? formatTime(m.scheduledAt) : 'ยังไม่กำหนดเวลา' }}</span>
              </span>
            </button>
          </section>

          <section v-if="freeFire.length" class="group">
            <h2 class="group-title">เกม Free Fire</h2>
            <RouterLink
              v-for="x in freeFire"
              :key="x.tournament.id"
              :to="`/admin/tournaments/${x.tournament.id}/games`"
              class="card"
            >
              <span class="logos"><TeamLogo label="FF" :size="30" :font-size="10" class="sq" /></span>
              <span class="card-text">
                <span class="card-title">{{ x.tournament.name }}</span>
                <span class="card-meta">ถัดไป เกมที่ {{ x.nextGame.gameNumber }} · บันทึกแล้ว {{ x.completed }}/{{ x.tournament.totalGames }} เกม</span>
              </span>
              <span class="arrow" aria-hidden="true">→</span>
            </RouterLink>
          </section>

          <p v-if="!allReady.length && !freeFire.length" class="admin-panel empty-list">ไม่มีแมตช์ที่รอกรอกผล</p>
        </aside>

        <section v-if="match" class="admin-panel detail">
          <header class="detail-head">
            <div>
              <h2 class="detail-title">{{ match.tournament.name }} · {{ roundLabel(match) }}</h2>
              <p class="detail-meta">
                {{ formatTournamentFormat(match.tournament.format) }} · {{ match.scheduledAt ? `${formatDate(match.scheduledAt)}, ${formatTime(match.scheduledAt)}` : 'ยังไม่กำหนดเวลา' }}
              </p>
            </div>
            <span class="admin-badge warning">{{ match.isFinal ? 'นัดชิง · รอกรอกผล' : 'รอกรอกผล' }}</span>
          </header>

          <div class="preview">
            <span class="side side-a" :class="{ lost: loserSide === 'A' }">
              <span class="side-name">{{ match.teamA.name }}</span>
              <TeamLogo :team="match.teamA" :size="60" :font-size="18" class="sq-lg" />
            </span>
            <span class="score-box">
              <span class="score">{{ scoreA ?? '–' }} <span class="dash">–</span> {{ scoreB ?? '–' }}</span>
              <span class="score-hint">ตัวอย่างผลที่จะบันทึก</span>
            </span>
            <span class="side side-b" :class="{ lost: loserSide === 'B' }">
              <TeamLogo :team="match.teamB" :size="60" :font-size="18" class="sq-lg" />
              <span class="side-name">{{ match.teamB.name }}</span>
            </span>
          </div>

          <div class="inputs">
            <div class="admin-field">
              <label class="admin-label" for="score-a">คะแนน {{ match.teamA.name }}</label>
              <input id="score-a" v-model="form.a" type="number" min="0" step="1" inputmode="numeric" class="admin-input big" :class="{ invalid: scoreInvalid }" />
            </div>
            <div class="admin-field">
              <label class="admin-label" for="score-b">คะแนน {{ match.teamB.name }}</label>
              <input id="score-b" v-model="form.b" type="number" min="0" step="1" inputmode="numeric" class="admin-input big" :class="{ invalid: scoreInvalid }" />
            </div>
            <div class="admin-field">
              <label class="admin-label" for="winner">ทีมที่ชนะ</label>
              <select id="winner" v-model="form.winner" class="admin-input big" :class="{ invalid: filled && checks[1].ok && !checks[2].ok }">
                <option value="" disabled>เลือกทีมที่ชนะ</option>
                <option :value="String(match.teamA.id)">{{ match.teamA.name }}</option>
                <option :value="String(match.teamB.id)">{{ match.teamB.name }}</option>
              </select>
            </div>
          </div>

          <ul class="checks">
            <li v-for="c in checks" :key="c.key" :class="filled ? (c.ok ? 'pass' : 'fail') : 'idle'">
              <span aria-hidden="true">{{ filled && !c.ok ? '✕' : '✓' }}</span> {{ c.text }}
            </li>
          </ul>

          <p v-if="problems.length" class="problem" role="alert">✕ {{ problems.join(' · ') }}</p>

          <footer class="detail-actions">
            <p class="admin-hint">
              {{ match.isFinal ? 'นี่คือนัดชิง บันทึกแล้วรายการจะจบและประกาศแชมป์' : 'บันทึกได้ครั้งเดียว ระบบจะส่งผู้ชนะไปแมตช์ถัดไปให้อัตโนมัติ' }}
            </p>
            <button type="button" class="admin-btn admin-btn-outline" @click="resetForm">ยกเลิก</button>
            <button type="button" class="admin-btn admin-btn-accent" :disabled="!allOk" @click="confirming = true">บันทึกผล</button>
          </footer>
        </section>

        <section v-else class="admin-panel detail placeholder">
          <p>เลือกแมตช์ทางซ้ายเพื่อกรอกผล</p>
        </section>
      </div>
    </div>

    <ConfirmDialog
      v-if="confirming && match"
      title="ยืนยันบันทึกผลแมตช์?"
      :rows="confirmRows"
      :message="confirmMessage"
      confirm-label="ยืนยันบันทึกผล"
      :busy="saving"
      :error="saveError"
      @cancel="confirming = false"
      @confirm="save"
    />
  </AdminLayout>
</template>

<style scoped>
.flash { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding: 12px 16px; border-radius: var(--radius-sm); background: var(--color-success-bg); color: var(--color-success); font-size: 14px; }
.flash-link { color: var(--color-text); font-weight: 600; text-decoration: underline; }

.layout { display: flex; align-items: flex-start; gap: 24px; }
.list { display: flex; flex-direction: column; gap: 20px; width: 440px; flex-shrink: 0; }
.group { display: flex; flex-direction: column; gap: 10px; }
.group-title { color: var(--color-muted); font-size: 13px; font-weight: 500; }
.card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
}
.card:hover { border-color: var(--color-muted); }
.card.active { border-color: var(--color-accent); background: rgba(255, 253, 131, 0.06); }
.logos { display: flex; flex-shrink: 0; gap: 2px; padding: 4px; border-radius: 10px; background: var(--color-surface-2); }
.sq { border: 0; border-radius: 8px; }
.card-text { display: flex; flex: 1; flex-direction: column; gap: 2px; min-width: 0; }
.card-title { overflow: hidden; font-weight: 600; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.vs { margin: 0 4px; color: var(--color-muted); font-weight: 500; }
.card-meta { overflow: hidden; color: var(--color-muted); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.arrow { color: var(--color-accent); }
.empty-list { color: var(--color-muted); text-align: center; }

.detail { flex: 1; min-width: 0; gap: 20px; padding: 28px 32px; }
.placeholder { align-items: center; justify-content: center; min-height: 240px; color: var(--color-muted); }
.detail-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.detail-title { font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.detail-meta { margin-top: 4px; color: var(--color-muted); font-size: 14px; }

.preview {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 24px;
  padding: 32px 24px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
}
.side { display: flex; align-items: center; gap: 16px; min-width: 0; }
.side-a { justify-content: flex-end; text-align: right; }
.side-name { overflow-wrap: anywhere; font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.side.lost .side-name { color: var(--color-muted); }
.sq-lg { border: 0; border-radius: 14px; }
.score-box { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.score { color: var(--color-accent); font-family: var(--font-heading); font-weight: 600; font-size: 44px; line-height: 1; white-space: nowrap; }
.dash { margin: 0 4px; }
.score-hint { color: var(--color-muted); font-size: 11px; }

.inputs { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 24px; }
.big { padding: 16px; font-size: 17px; font-weight: 600; }

.checks { display: flex; flex-wrap: wrap; gap: 8px 20px; margin: 0; padding: 0; list-style: none; font-size: 13px; }
.checks .idle { color: var(--color-muted); }
.checks .pass { color: var(--color-success); }
.checks .fail { color: var(--color-danger); }
.problem { padding: 12px 16px; border-radius: var(--radius-sm); background: var(--color-danger-bg); color: var(--color-danger); font-size: 14px; }

.detail-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 12px; }
.detail-actions .admin-hint { margin-right: auto; font-size: 13px; }

@media (max-width: 1280px) {
  .list { width: 360px; }
  .inputs { grid-template-columns: 1fr 1fr; }
  .inputs .admin-field:last-child { grid-column: 1 / -1; }
}

@media (max-width: 1024px) {
  .layout { flex-direction: column; align-items: stretch; }
  .list { width: auto; }
}

@media (max-width: 640px) {
  .detail { padding: 16px; }
  .preview { grid-template-columns: 1fr; gap: 12px; padding: 20px 16px; }
  .side-a, .side-b { justify-content: center; text-align: center; }
  .side-a { flex-direction: row-reverse; }
  .side-name { font-size: 18px; }
  .score { font-size: 36px; }
  .inputs { grid-template-columns: 1fr 1fr; gap: 12px; }
  .detail-actions .admin-btn { flex: 1; }
}
</style>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import ConfirmDialog from '@/components/admin/ConfirmDialog.vue'
import TeamLogo from '@/components/TeamLogo.vue'
import { getFreeFireResultBoard, recordFreeFireGameResult } from '@/api/admin'
import { formatDate, formatTime, formatTournamentFormat } from '@/utils/format'

const props = defineProps({
  id: { type: String, required: true },
})

const route = useRoute()
const router = useRouter()

const version = ref(0)
const board = computed(() => {
  version.value
  return getFreeFireResultBoard(props.id)
})
const tournament = computed(() => board.value?.tournament)
const teamCount = computed(() => board.value?.teams.length ?? 0)
const perKill = computed(() => tournament.value?.pointsPerKill ?? 1)

const selectedGame = computed(() => {
  const list = board.value?.games ?? []
  const fromQuery = list.find((g) => g.gameNumber === Number(route.query.game))
  return fromQuery ?? list.find((g) => g.state === 'NEXT') ?? list.at(-1) ?? null
})

const gamesNav = ref(null)
watch(
  () => selectedGame.value?.id,
  async () => {
    await nextTick()
    gamesNav.value?.querySelector('.game.active')?.scrollIntoView({ block: 'nearest', inline: 'center' })
  },
  { immediate: true },
)

function select(game) {
  router.replace({ query: { game: game.gameNumber } })
}

const confirming = ref(false)
const saving = ref(false)
const saveError = ref('')
const flash = ref('')

const entries = ref([])
function resetEntries() {
  entries.value = (board.value?.teams ?? []).map((team) => ({ team, placement: '', kills: '' }))
  saveError.value = ''
}
watch(() => [selectedGame.value?.id, version.value], resetEntries, { immediate: true })

const pointsFor = (placement) => board.value?.placementPoints.get(Number(placement)) ?? 0
const rowNum = (v) => (v === '' || v === null ? null : Number(v))

const rows = computed(() =>
  entries.value.map((e) => {
    const placement = rowNum(e.placement)
    const kills = rowNum(e.kills)
    const placementPoints = placement ? pointsFor(placement) : null
    const total = placement && kills !== null ? placementPoints + kills * perKill.value : null
    return { ...e, placementValue: placement, killsValue: kills, placementPoints, total }
  }),
)

const duplicatePlacements = computed(() => {
  const count = new Map()
  rows.value.forEach((r) => {
    if (r.placementValue !== null) count.set(r.placementValue, (count.get(r.placementValue) ?? 0) + 1)
  })
  return new Set([...count].filter(([, n]) => n > 1).map(([p]) => p))
})

const placementBad = (r) =>
  r.placementValue !== null &&
  (duplicatePlacements.value.has(r.placementValue) || !Number.isInteger(r.placementValue) || r.placementValue < 1 || r.placementValue > teamCount.value)
const killsBad = (r) => r.killsValue !== null && (!Number.isInteger(r.killsValue) || r.killsValue < 0)

const checks = computed(() => [
  {
    key: 'all',
    ok: rows.value.every((r) => r.placementValue !== null && r.killsValue !== null),
    text: `ครบ ${teamCount.value} ทีม`,
  },
  { key: 'placement', ok: rows.value.every((r) => r.placementValue !== null && !placementBad(r)), text: `อันดับไม่ซ้ำ 1–${teamCount.value}` },
  { key: 'kills', ok: rows.value.every((r) => r.killsValue !== null && !killsBad(r)), text: 'kill ไม่ติดลบ' },
])
const allOk = computed(() => checks.value.every((c) => c.ok))
const touched = computed(() => rows.value.some((r) => r.placementValue !== null || r.killsValue !== null))

const problems = computed(() => {
  const list = []
  for (const p of duplicatePlacements.value) {
    const names = rows.value.filter((r) => r.placementValue === p).map((r) => r.team.name)
    list.push(`อันดับ ${p} ซ้ำกัน (${names.join(', ')})`)
  }
  if (rows.value.some((r) => r.placementValue !== null && !duplicatePlacements.value.has(r.placementValue) && placementBad(r))) {
    list.push(`อันดับต้องอยู่ในช่วง 1–${teamCount.value}`)
  }
  if (rows.value.some(killsBad)) list.push('Kills ห้ามติดลบ')
  return list
})

const booyah = computed(() => rows.value.find((r) => r.placementValue === 1)?.team ?? null)
const isLastGame = computed(() => {
  const list = board.value?.games ?? []
  return list.length === tournament.value?.totalGames && list.filter((g) => g.state !== 'DONE').length === 1
})


const confirmRows = computed(() => {
  const g = selectedGame.value
  if (!g) return []
  return [
    { label: 'เกม', value: `เกมที่ ${g.gameNumber}${g.scheduledAt ? ` · ${formatDate(g.scheduledAt)}` : ''}` },
    { label: 'ทีมที่กรอก', value: `${rows.value.filter((r) => r.placementValue !== null).length} / ${teamCount.value} ทีม` },
    { label: 'Booyah', value: booyah.value?.name ?? '-' },
  ]
})
const confirmMessage = computed(() =>
  isLastGame.value
    ? 'บันทึกแล้วแก้ไขไม่ได้ นี่คือเกมสุดท้าย ตารางคะแนนจะสรุปผลและรายการจะเปลี่ยนเป็น "จบแล้ว"'
    : 'บันทึกแล้วแก้ไขไม่ได้ ตารางคะแนนรวมจะอัปเดตทันที',
)

const ERROR_TEXT = {
  OUT_OF_ORDER: 'ต้องบันทึกเกมก่อนหน้าให้ครบก่อน',
  INVALID_PLACEMENT: 'อันดับต้องไม่ซ้ำและอยู่ในช่วงที่ถูกต้อง',
  INVALID_KILLS: 'Kills ต้องเป็นจำนวนเต็มตั้งแต่ 0',
  MISSING_TEAMS: 'ต้องกรอกครบทุกทีม',
  NOT_READY: 'เกมนี้บันทึกผลไปแล้ว',
}

async function save() {
  saving.value = true
  saveError.value = ''
  const g = selectedGame.value
  try {
    const result = await recordFreeFireGameResult(
      g.id,
      rows.value.map((r) => ({ teamId: r.team.id, placement: r.placementValue, kills: r.killsValue })),
    )
    flash.value = result.finished
      ? `บันทึกผลเกมที่ ${g.gameNumber} แล้ว · ครบทุกเกม รายการจบแล้ว`
      : `บันทึกผลเกมที่ ${g.gameNumber} แล้ว`
    confirming.value = false
    router.replace({ query: {} })
    version.value += 1
  } catch (e) {
    saveError.value = ERROR_TEXT[e.message] ?? (e.status ? `บันทึกไม่สำเร็จ: ${e.message}` : 'บันทึกไม่สำเร็จ ลองใหม่อีกครั้ง')
  } finally {
    saving.value = false
  }
}

const stateLabel = { DONE: 'บันทึกแล้ว', NEXT: 'รอกรอกผล', LOCKED: 'ยังไม่แข่ง' }
const stateTone = { DONE: 'success', NEXT: 'warning', LOCKED: 'neutral' }
const nextGameNumber = computed(() => board.value?.games.find((g) => g.state === 'NEXT')?.gameNumber ?? null)
</script>

<template>
  <AdminLayout>
    <div v-if="!board" class="admin-page">
      <h1 class="admin-title">ไม่พบรายการแบบเก็บคะแนนนี้</h1>
      <RouterLink to="/admin/matches" class="admin-btn admin-btn-outline back">กลับไปหน้าแมตช์</RouterLink>
    </div>

    <div v-else class="admin-page">
      <nav class="admin-crumbs" aria-label="breadcrumb">
        <RouterLink to="/admin/matches">แมตช์และผลการแข่ง</RouterLink>
        <span>/</span>
        <span class="current">{{ tournament.name }}</span>
      </nav>
      <header>
        <h1 class="admin-title">กรอกผลเกม: {{ tournament.name }}</h1>
        <p class="admin-subtitle">
          {{ tournament.game?.name }} · {{ formatTournamentFormat(tournament.format, tournament.totalGames) }} · {{ teamCount }} ทีม
        </p>
      </header>

      <p v-if="flash" class="flash" role="status">
        {{ flash }}
        <RouterLink :to="`/tournaments/${tournament.id}?tab=standings`" class="flash-link">ดูตารางคะแนนในหน้าผู้ชม</RouterLink>
      </p>

      <p v-if="!board.games.length" class="admin-panel empty">
        ยังไม่ได้สร้างตารางเกม
        <RouterLink :to="`/admin/tournaments/${tournament.id}/teams`" class="admin-btn admin-btn-outline">ไปสร้างตารางเกม</RouterLink>
      </p>

      <div v-else class="layout">
        <nav ref="gamesNav" class="games" aria-label="เลือกเกม">
          <button
            v-for="g in board.games"
            :key="g.id"
            type="button"
            class="game"
            :class="{ active: selectedGame?.id === g.id }"
            :aria-current="selectedGame?.id === g.id ? 'true' : undefined"
            @click="select(g)"
          >
            <span class="game-name">เกมที่ {{ g.gameNumber }}</span>
            <span class="admin-badge" :class="stateTone[g.state]">{{ stateLabel[g.state] }}</span>
          </button>
        </nav>

        <section v-if="selectedGame" class="admin-panel detail">
          <header class="detail-head">
            <h2 class="detail-title">
              เกมที่ {{ selectedGame.gameNumber }}
              <template v-if="selectedGame.scheduledAt"> · {{ formatDate(selectedGame.scheduledAt) }}, {{ formatTime(selectedGame.scheduledAt) }}</template>
            </h2>
            <p class="admin-hint">
              {{
                selectedGame.state === 'NEXT'
                  ? `กรอกอันดับและ kill ครบ ${teamCount} ทีม ระบบคิดคะแนนให้`
                  : selectedGame.state === 'DONE'
                    ? 'ผลที่บันทึกแล้ว แก้ไขไม่ได้'
                    : `ต้องบันทึกเกมที่ ${nextGameNumber} ก่อน`
              }}
            </p>
          </header>

          <div v-if="selectedGame.state === 'LOCKED'" class="locked">
            เกมนี้ยังกรอกผลไม่ได้ ผลต้องบันทึกเรียงตามลำดับเกม
            <button v-if="nextGameNumber" type="button" class="admin-btn admin-btn-outline admin-btn-sm" @click="select({ gameNumber: nextGameNumber })">
              ไปที่เกมที่ {{ nextGameNumber }}
            </button>
          </div>

          <div v-else class="table-wrap">
            <table class="table">
              <colgroup>
                <col />
                <col class="c-num" />
                <col class="c-num" />
                <col class="c-pts" />
                <col class="c-pts" />
              </colgroup>
              <thead>
                <tr>
                  <th>ทีม</th>
                  <th class="center">อันดับ</th>
                  <th class="center">Kills</th>
                  <th class="center">คะแนนอันดับ</th>
                  <th class="center">คะแนนเกมนี้</th>
                </tr>
              </thead>
              <tbody v-if="selectedGame.state === 'DONE'">
                <tr v-for="r in selectedGame.results" :key="r.id">
                  <td>
                    <span class="team-cell">
                      <TeamLogo :team="r.team" :size="24" :font-size="8" class="sq" />
                      <span class="team-name">{{ r.team.name }}</span>
                    </span>
                  </td>
                  <td class="center"><span class="cell-static">{{ r.placement }}</span></td>
                  <td class="center"><span class="cell-static">{{ r.kills }}</span></td>
                  <td class="center muted">{{ pointsFor(r.placement) }}</td>
                  <td class="center total">{{ pointsFor(r.placement) + r.kills * perKill }}</td>
                </tr>
              </tbody>
              <tbody v-else>
                <tr v-for="(r, i) in rows" :key="r.team.id" :class="{ bad: placementBad(r) || killsBad(r) }">
                  <td>
                    <span class="team-cell">
                      <TeamLogo :team="r.team" :size="24" :font-size="8" class="sq" />
                      <span class="team-name">{{ r.team.name }}</span>
                    </span>
                  </td>
                  <td class="center">
                    <input
                      v-model="entries[i].placement"
                      type="number"
                      min="1"
                      :max="teamCount"
                      step="1"
                      inputmode="numeric"
                      class="cell-input"
                      :class="{ invalid: placementBad(r) }"
                      :aria-label="`อันดับของ ${r.team.name}`"
                    />
                  </td>
                  <td class="center">
                    <input
                      v-model="entries[i].kills"
                      type="number"
                      min="0"
                      step="1"
                      inputmode="numeric"
                      class="cell-input"
                      :class="{ invalid: killsBad(r) }"
                      :aria-label="`Kills ของ ${r.team.name}`"
                    />
                  </td>
                  <td class="center muted">{{ r.placementPoints ?? '–' }}</td>
                  <td class="center total">{{ r.total ?? '–' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <template v-if="selectedGame.state === 'NEXT'">
            <p v-if="problems.length" class="problem" role="alert">✕ {{ problems.join(' · ') }}</p>
            <footer class="detail-actions">
              <ul class="checks">
                <li v-for="c in checks" :key="c.key" :class="touched ? (c.ok ? 'pass' : 'fail') : 'idle'">
                  <span aria-hidden="true">{{ touched && !c.ok ? '✕' : '✓' }}</span> {{ c.text }}
                </li>
              </ul>
              <button type="button" class="admin-btn admin-btn-outline" @click="resetEntries">ยกเลิก</button>
              <button type="button" class="admin-btn admin-btn-accent" :disabled="!allOk" @click="confirming = true">
                บันทึกผลเกมที่ {{ selectedGame.gameNumber }}
              </button>
            </footer>
          </template>
        </section>
      </div>
    </div>

    <ConfirmDialog
      v-if="confirming && selectedGame"
      :title="`ยืนยันบันทึกผลเกมที่ ${selectedGame.gameNumber}?`"
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
.back { align-self: flex-start; }
.flash { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding: 12px 16px; border-radius: var(--radius-sm); background: var(--color-success-bg); color: var(--color-success); font-size: 14px; }
.flash-link { color: var(--color-text); font-weight: 600; text-decoration: underline; }
.empty { flex-direction: row; align-items: center; justify-content: space-between; color: var(--color-muted); }

.layout { display: flex; align-items: flex-start; gap: 24px; }
.games { display: flex; flex-direction: column; gap: 8px; width: 300px; flex-shrink: 0; }
.game {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  cursor: pointer;
}
.game:hover { border-color: var(--color-muted); }
.game.active { border-color: var(--color-accent); background: rgba(255, 253, 131, 0.06); }
.game-name { font-weight: 600; font-size: 15px; }

.detail { flex: 1; min-width: 0; gap: 16px; }
.detail-head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 8px 16px; }
.detail-title { font-family: var(--font-heading); font-weight: 600; font-size: 20px; }
.locked { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; padding: 24px; border-radius: var(--radius-md); background: var(--color-bg); color: var(--color-muted); font-size: 14px; }

.table-wrap { overflow-x: auto; border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.table { width: 100%; min-width: 560px; border-collapse: collapse; table-layout: fixed; }
.c-num { width: 110px; }
.c-pts { width: 120px; }
th { padding: 10px 16px; background: var(--color-surface-2); color: var(--color-muted); font-size: 12px; font-weight: 500; text-align: left; }
td { padding: 6px 16px; border-top: 1px solid var(--color-border); font-size: 14px; }
.center { text-align: center; }
.muted { color: var(--color-muted); }
.total { color: var(--color-accent); font-family: var(--font-heading); font-weight: 600; }
tr.bad { background: var(--color-danger-bg); }
.team-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.sq { border: 0; border-radius: 6px; }
.team-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cell-input,
.cell-static {
  display: inline-block;
  width: 54px;
  padding: 6px 4px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text);
  font: inherit;
  font-weight: 600;
  text-align: center;
  color-scheme: dark;
  appearance: textfield;
}
.cell-input::-webkit-inner-spin-button,
.cell-input::-webkit-outer-spin-button { appearance: none; margin: 0; }
.cell-input:focus { outline: 0; border-color: var(--color-accent); }
.cell-input.invalid { border-color: var(--color-danger); color: var(--color-danger); }
.cell-static { border-color: transparent; background: var(--color-surface-2); }

.problem { padding: 12px 16px; border-radius: var(--radius-sm); background: var(--color-danger-bg); color: var(--color-danger); font-size: 14px; }
.detail-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 12px; }
.checks { display: flex; flex-wrap: wrap; gap: 6px 16px; margin: 0 auto 0 0; padding: 0; list-style: none; font-size: 13px; }
.checks .idle { color: var(--color-muted); }
.checks .pass { color: var(--color-success); }
.checks .fail { color: var(--color-danger); }

@media (max-width: 1024px) {
  .layout { flex-direction: column; align-items: stretch; }
  .games { flex-direction: row; width: auto; overflow-x: auto; scrollbar-width: none; }
  .games::-webkit-scrollbar { display: none; }
  .game { flex-direction: column; align-items: flex-start; gap: 6px; flex-shrink: 0; }
}

@media (max-width: 640px) {
  .table { min-width: 0; }
  .c-num { width: 64px; }
  .c-pts { width: 56px; }
  th, td { padding: 6px 6px; }
  th:first-child, td:first-child { padding-left: 12px; }
  .cell-input, .cell-static { width: 44px; }
  .sq { display: none; }
  th { font-size: 11px; }
  .detail-actions .admin-btn { flex: 1; }
}
</style>

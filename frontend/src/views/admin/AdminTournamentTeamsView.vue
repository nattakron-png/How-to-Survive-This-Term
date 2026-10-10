<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import TournamentLogo from '@/components/admin/TournamentLogo.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TeamLogo from '@/components/TeamLogo.vue'
import {
  MAX_POINTS_TEAMS,
  addTeamToTournament,
  bracketSizeFor,
  draftFreeFireSchedule,
  generateBracket,
  generateFreeFireSchedule,
  getTournamentTeamsAdmin,
  removeTeamFromTournament,
  resetBracket,
  resetFreeFireSchedule,
  searchTeamsForTournament,
} from '@/mock/admin'
import { formatDate, formatDateRange, formatShortDate, formatTime, formatTournamentFormat } from '@/utils/format'

const props = defineProps({
  id: { type: String, required: true },
})

const version = ref(0)
const data = computed(() => {
  version.value
  return getTournamentTeamsAdmin(props.id)
})
const refresh = () => { version.value += 1 }

const tournament = computed(() => data.value?.tournament)
const isPoints = computed(() => tournament.value?.format === 'POINTS')
const minPlayers = computed(() => tournament.value?.game?.minPlayers ?? 1)
const locked = computed(() => data.value?.locked ?? true)

const order = ref([])
watch(
  () => data.value?.teams.map((t) => t.id) ?? [],
  (ids) => {
    const kept = order.value.filter((id) => ids.includes(id))
    order.value = [...kept, ...ids.filter((id) => !kept.includes(id))]
  },
  { immediate: true },
)
const orderedTeams = computed(() => {
  const byId = new Map((data.value?.teams ?? []).map((t) => [t.id, t]))
  return order.value.map((id) => byId.get(id)).filter(Boolean)
})

function moveSeed(teamId, event) {
  const target = Math.min(Math.max(Number(event.target.value) || 1, 1), order.value.length)
  const list = order.value.filter((id) => id !== teamId)
  list.splice(target - 1, 0, teamId)
  order.value = list
  event.target.value = list.indexOf(teamId) + 1
}

const query = ref('')
const results = computed(() => {
  version.value
  return searchTeamsForTournament(props.id, query.value)
})
const busyTeamId = ref(null)
const actionError = ref('')

const ERROR_TEXT = {
  TEAMS_LOCKED: 'แก้ไขทีมไม่ได้แล้ว เพราะสร้างสายหรือตารางเกมไปแล้ว',
  DATE_CLASH: 'ทีมนี้มีรายการอื่นที่วันแข่งทับกัน',
  FULL: `รายการแบบเก็บคะแนนรับได้สูงสุด ${MAX_POINTS_TEAMS} ทีม`,
  HAS_RESULTS: 'มีผลการแข่งแล้ว จึงล้างไม่ได้',
}
const errorText = (e) => ERROR_TEXT[e.message] ?? 'ทำรายการไม่สำเร็จ ลองใหม่อีกครั้ง'

async function run(fn) {
  actionError.value = ''
  try {
    await fn()
    refresh()
  } catch (e) {
    actionError.value = errorText(e)
  }
}

async function addTeam(teamId) {
  busyTeamId.value = teamId
  await run(() => addTeamToTournament(props.id, teamId))
  busyTeamId.value = null
}

async function removeTeam(teamId) {
  busyTeamId.value = teamId
  await run(() => removeTeamFromTournament(props.id, teamId))
  busyTeamId.value = null
}

const reasonText = (reason) =>
  typeof reason === 'string'
    ? reason
    : `วันแข่งทับกับ ${reason.clash.name} (${formatDateRange(reason.clash.startDate, reason.clash.endDate)})`

const teamCount = computed(() => orderedTeams.value.length)
const bracketSize = computed(() => bracketSizeFor(teamCount.value))
const byes = computed(() => (teamCount.value >= 2 ? bracketSize.value - teamCount.value : 0))

const checks = computed(() => {
  const list = orderedTeams.value
  const short = list.filter((t) => t.playerCount < minPlayers.value)
  const clashing = list.filter((t) => t.clash)
  const items = [
    { key: 'min', ok: list.length >= 2, text: 'มีทีมอย่างน้อย 2 ทีม' },
    {
      key: 'players',
      ok: list.length > 0 && short.length === 0,
      text: `ทุกทีมมีผู้เล่นอย่างน้อย ${minPlayers.value} คน`,
      detail: short.length ? `ยังไม่ครบ: ${short.map((t) => t.name).join(', ')}` : '',
    },
    {
      key: 'game',
      ok: list.length > 0 && list.every((t) => t.gameId === tournament.value.gameId),
      text: `ทุกทีมเป็นทีม ${tournament.value?.game?.name}`,
    },
    {
      key: 'clash',
      ok: clashing.length === 0,
      text: 'ไม่มีทีมที่วันแข่งทับกับรายการอื่น',
      detail: clashing.length ? clashing.map((t) => `${t.name} ทับกับ ${t.clash.name}`).join(', ') : '',
    },
  ]
  if (isPoints.value) {
    items.push({
      key: 'max',
      ok: list.length <= MAX_POINTS_TEAMS,
      text: `ไม่เกิน ${MAX_POINTS_TEAMS} ทีม (คะแนนอันดับมีถึงอันดับ ${MAX_POINTS_TEAMS})`,
    })
  }
  return items
})
const canGenerate = computed(() => !locked.value && checks.value.every((c) => c.ok))

const byeText = computed(() => {
  if (!byes.value) return ''
  const seeds = byes.value === 1 ? 'ทีมลำดับ 1' : `ทีมลำดับ 1–${byes.value}`
  return `${teamCount.value} ทีม ต้องใช้สาย ${bracketSize.value} ช่อง ${seeds} ได้บาย`
})

const schedule = ref([])
const editingGame = ref(null)
watch(
  () => [tournament.value?.id, tournament.value?.totalGames, tournament.value?.startDate],
  () => {
    schedule.value = tournament.value && isPoints.value ? draftFreeFireSchedule(tournament.value) : []
  },
  { immediate: true },
)
const scheduleError = computed(() => {
  const t = tournament.value
  if (!t || !isPoints.value) return ''
  const list = schedule.value
  if (list.some((x) => !x)) return 'กรุณากำหนดวันเวลาให้ครบทุกเกม'
  if (list.some((x) => x.slice(0, 10) < t.startDate || x.slice(0, 10) > t.endDate)) {
    return `วันแข่งทุกเกมต้องอยู่ระหว่าง ${formatDateRange(t.startDate, t.endDate)}`
  }
  if (list.some((x, i) => i > 0 && x <= list[i - 1])) return 'เวลาของแต่ละเกมต้องเรียงจากเกมแรกไปเกมสุดท้าย'
  return ''
})
const scheduleValid = computed(() => !scheduleError.value)

function updateGameTime(i, value) {
  if (value) schedule.value[i] = value.length === 16 ? `${value}:00` : value
}

const generating = ref(false)
async function createSetup() {
  if (!canGenerate.value) return
  generating.value = true
  await run(() =>
    isPoints.value ? generateFreeFireSchedule(props.id, [...schedule.value]) : generateBracket(props.id, order.value),
  )
  generating.value = false
}

const resetting = ref(false)
async function resetSetup() {
  resetting.value = true
  await run(() => (isPoints.value ? resetFreeFireSchedule(props.id) : resetBracket(props.id)))
  resetting.value = false
}

const gameStatusLabel = { COMPLETED: 'จบแล้ว', SCHEDULED: 'รอแข่ง' }
</script>

<template>
  <AdminLayout>
    <div v-if="!data" class="admin-page">
      <h1 class="admin-title">ไม่พบรายการแข่งนี้</h1>
      <RouterLink to="/admin/tournaments" class="admin-btn admin-btn-outline back">กลับไปหน้ารายการ</RouterLink>
    </div>

    <div v-else class="admin-page">
      <nav class="admin-crumbs" aria-label="breadcrumb">
        <RouterLink to="/admin/tournaments">รายการแข่ง</RouterLink>
        <span>/</span>
        <RouterLink :to="`/admin/tournaments/${tournament.id}/edit`">{{ tournament.name }}</RouterLink>
        <span>/</span>
        <span class="current">{{ isPoints ? 'ทีมและตารางเกม' : 'ทีมและสายการแข่ง' }}</span>
      </nav>

      <header class="hero">
        <TournamentLogo :name="tournament.name" :logo-url="tournament.logoUrl" :size="64" :font-size="16" surface />
        <div class="hero-text">
          <h1 class="hero-title">{{ tournament.name }}</h1>
          <p class="hero-meta">
            <StatusBadge :status="tournament.status" />
            <span>{{ tournament.game?.name }}</span>
            <span>·</span>
            <span>{{ formatTournamentFormat(tournament.format, tournament.totalGames) }}</span>
            <span>·</span>
            <span>เริ่ม {{ formatDate(tournament.startDate) }}</span>
          </p>
        </div>
      </header>

      <p v-if="actionError" class="alert" role="alert">{{ actionError }}</p>

      <div class="layout">
        <section class="admin-panel teams">
          <header class="panel-head">
            <h2 class="admin-panel-title">ทีมในรายการ</h2>
            <span class="count">{{ teamCount }} ทีม</span>
          </header>

          <p v-if="locked" class="locked-note">
            {{
              data.setup.created
                ? isPoints
                  ? 'สร้างตารางเกมแล้ว เพิ่มหรือลบทีมไม่ได้จนกว่าจะล้างตารางเกม'
                  : 'สร้างสายแล้ว เพิ่มหรือลบทีมไม่ได้จนกว่าจะล้างสาย'
                : 'รายการนี้เริ่มแข่งแล้ว จึงเพิ่มหรือลบทีมไม่ได้'
            }}
          </p>

          <div v-else class="search-box" :class="{ open: query.trim() }">
            <label class="search">
              <span class="search-icon" aria-hidden="true">⌕</span>
              <input
                v-model="query"
                type="search"
                :placeholder="`ค้นหาทีม ${tournament.game?.name} เพื่อเพิ่ม…`"
                :aria-label="`ค้นหาทีม ${tournament.game?.name}`"
              />
            </label>
            <ul v-if="query.trim()" class="results">
              <li v-for="r in results" :key="r.team.id" class="result" :class="{ disabled: r.reason }">
                <TeamLogo :team="r.team" :size="28" :font-size="9" />
                <span class="result-text">
                  <span class="result-name">{{ r.team.name }}</span>
                  <span class="result-sub">{{ r.reason ? reasonText(r.reason) : `ผู้เล่น ${r.playerCount} คน` }}</span>
                </span>
                <span v-if="r.reason" class="cannot" :class="{ danger: typeof r.reason !== 'string' }">เพิ่มไม่ได้</span>
                <button
                  v-else
                  type="button"
                  class="admin-btn admin-btn-accent admin-btn-sm"
                  :disabled="busyTeamId === r.team.id"
                  @click="addTeam(r.team.id)"
                >+ เพิ่ม</button>
              </li>
              <li v-if="!results.length" class="result empty-result">ไม่พบทีม {{ tournament.game?.name }} ที่ชื่อตรงกับ "{{ query.trim() }}"</li>
            </ul>
          </div>

          <div class="table-wrap">
            <table class="table">
              <colgroup>
                <col class="c-seed" />
                <col />
                <col class="c-players" />
                <col v-if="!locked" class="c-remove" />
              </colgroup>
              <thead>
                <tr>
                  <th>ลำดับ</th>
                  <th>ทีม</th>
                  <th>ผู้เล่น</th>
                  <th v-if="!locked"><span class="sr-only">ลบ</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(team, i) in orderedTeams" :key="team.id">
                  <td>
                    <input
                      v-if="!locked && !isPoints"
                      class="seed"
                      type="number"
                      min="1"
                      :max="orderedTeams.length"
                      :value="i + 1"
                      :aria-label="`ลำดับของ ${team.name}`"
                      @change="moveSeed(team.id, $event)"
                    />
                    <span v-else class="seed seed-static">{{ i + 1 }}</span>
                  </td>
                  <td>
                    <span class="team-cell">
                      <TeamLogo :team="team" :size="30" :font-size="10" />
                      <span class="team-name">{{ team.name }}</span>
                    </span>
                  </td>
                  <td :class="team.playerCount >= minPlayers ? 'ok' : 'bad'">
                    {{ team.playerCount }} คน
                    <template v-if="team.playerCount < minPlayers"> (ขาด {{ minPlayers - team.playerCount }})</template>
                  </td>
                  <td v-if="!locked" class="remove-cell">
                    <button
                      type="button"
                      class="remove"
                      :disabled="busyTeamId === team.id"
                      :aria-label="`นำ ${team.name} ออกจากรายการ`"
                      @click="removeTeam(team.id)"
                    >✕</button>
                  </td>
                </tr>
                <tr v-if="!orderedTeams.length">
                  <td :colspan="locked ? 3 : 4" class="empty">ยังไม่มีทีม ค้นหาด้านบนเพื่อเพิ่มทีม</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-if="!locked && !isPoints && orderedTeams.length > 1" class="admin-hint">
            แก้ตัวเลขลำดับเพื่อจัดอันดับทีม (seed) ก่อนสร้างสาย ทีมลำดับต้นๆ จะได้บายก่อนเมื่อจำนวนทีมไม่พอดีสาย
          </p>
        </section>

        <aside class="admin-panel setup">
          <template v-if="!data.setup.created">
            <h2 class="admin-panel-title">{{ isPoints ? 'สร้างตารางเกม' : 'สร้างสายการแข่ง' }}</h2>
            <dl class="summary">
              <div><dt>รูปแบบ</dt><dd>{{ formatTournamentFormat(tournament.format, tournament.totalGames) }}</dd></div>
              <div><dt>ทีมในรายการ</dt><dd>{{ teamCount }} ทีม</dd></div>
              <div v-if="isPoints"><dt>จำนวนเกม</dt><dd>{{ tournament.totalGames }} เกม · 1 ห้อง</dd></div>
              <div v-else>
                <dt>ขนาดสาย</dt>
                <dd>{{ teamCount >= 2 ? `${bracketSize} ช่อง${byes ? ` (บาย ${byes} ช่อง)` : ''}` : '-' }}</dd>
              </div>
            </dl>

            <div class="checks">
              <h3 class="checks-title">ตรวจก่อน{{ isPoints ? 'สร้างตารางเกม' : 'สร้างสาย' }}</h3>
              <ul class="check-list">
                <li v-for="c in checks" :key="c.key" class="check" :class="c.ok ? 'pass' : 'fail'">
                  <span class="check-icon" aria-hidden="true">{{ c.ok ? '✓' : '✕' }}</span>
                  <span>
                    {{ c.text }}
                    <span v-if="c.detail" class="check-detail">{{ c.detail }}</span>
                  </span>
                </li>
                <li v-if="byeText" class="check warn">
                  <span class="check-icon" aria-hidden="true">!</span>
                  <span>{{ byeText }}</span>
                </li>
              </ul>
            </div>

            <div v-if="isPoints" class="schedule">
              <h3 class="checks-title">ตารางเกมที่จะสร้าง (แก้วันเวลาได้ทีละเกม)</h3>
              <div class="schedule-grid">
                <div v-for="(slot, i) in schedule" :key="i" class="slot">
                  <span class="slot-name">เกมที่ {{ i + 1 }}</span>
                  <input
                    v-if="editingGame === i"
                    class="slot-input"
                    type="datetime-local"
                    :value="slot.slice(0, 16)"
                    :aria-label="`วันเวลาเกมที่ ${i + 1}`"
                    autofocus
                    @change="updateGameTime(i, $event.target.value)"
                    @blur="editingGame = null"
                    @keydown.enter="editingGame = null"
                  />
                  <button v-else type="button" class="slot-time" :disabled="locked" @click="editingGame = i">
                    {{ formatShortDate(slot) }} {{ formatTime(slot) }}
                  </button>
                </div>
              </div>
              <p v-if="scheduleError" class="admin-error schedule-error">{{ scheduleError }}</p>
            </div>

            <button
              type="button"
              class="admin-btn admin-btn-accent admin-btn-block"
              :disabled="!canGenerate || generating || (isPoints && !scheduleValid)"
              @click="createSetup"
            >
              {{ generating ? 'กำลังสร้าง…' : isPoints ? `สร้างตารางเกม ${tournament.totalGames} เกม` : 'สร้างสายการแข่ง' }}
            </button>
            <p class="admin-hint">
              {{
                isPoints
                  ? 'สร้างแล้วจะเพิ่มหรือลบทีมไม่ได้ และรายการจะเปลี่ยนเป็น "กำลังแข่ง"'
                  : 'สร้างแล้วจะเพิ่มหรือลบทีมไม่ได้ จนกว่าจะล้างสาย'
              }}
            </p>
          </template>

          <template v-else>
            <header class="panel-head">
              <h2 class="admin-panel-title">{{ isPoints ? 'ตารางเกม' : 'สายการแข่ง' }}</h2>
              <span class="admin-badge success">{{ isPoints ? 'สร้างตารางเกมแล้ว' : 'สร้างสายแล้ว' }}</span>
            </header>
            <dl class="summary">
              <div><dt>รูปแบบ</dt><dd>{{ formatTournamentFormat(tournament.format, tournament.totalGames) }}</dd></div>
              <div><dt>ทีมในรายการ</dt><dd>{{ teamCount }} ทีม</dd></div>
              <div v-if="!isPoints"><dt>ขนาดสาย</dt><dd>{{ bracketSize }} ช่อง{{ byes ? ` (บาย ${byes} ช่อง)` : '' }}</dd></div>
            </dl>

            <div v-if="isPoints" class="schedule-list">
              <div v-for="g in data.games" :key="g.id" class="slot">
                <span class="slot-name">เกมที่ {{ g.gameNumber }}</span>
                <span class="slot-static">
                  {{ g.scheduledAt ? `${formatShortDate(g.scheduledAt)} ${formatTime(g.scheduledAt)}` : '-' }}
                  <span class="slot-status" :class="{ done: g.status === 'COMPLETED' }">{{ gameStatusLabel[g.status] ?? g.status }}</span>
                </span>
              </div>
            </div>

            <RouterLink
              :to="`/tournaments/${tournament.id}?tab=${isPoints ? 'games' : 'bracket'}`"
              class="admin-btn admin-btn-outline admin-btn-block"
            >{{ isPoints ? 'ดูตารางเกมในหน้าผู้ชม' : 'ดูสายการแข่งในหน้าผู้ชม' }}</RouterLink>
            <button
              type="button"
              class="admin-btn admin-btn-danger admin-btn-block"
              :disabled="data.setup.hasResults || resetting"
              @click="resetSetup"
            >{{ resetting ? 'กำลังล้าง…' : isPoints ? 'ล้างตารางเกม' : 'ล้างสาย' }}</button>
            <p class="admin-hint">
              {{
                data.setup.hasResults
                  ? 'มีผลการแข่งแล้ว จึงล้างไม่ได้'
                  : isPoints
                    ? 'ล้างตารางเกมแล้วรายการจะกลับเป็น "กำลังจะเริ่ม" และแก้ไขทีมได้อีกครั้ง'
                    : 'ล้างสายแล้วจะแก้ไขทีมและลำดับได้อีกครั้ง'
              }}
            </p>
          </template>
        </aside>
      </div>
    </div>
  </AdminLayout>
</template>

<style scoped>
.back { align-self: flex-start; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }

.hero { display: flex; align-items: center; gap: 16px; }
.hero-text { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.hero-title { font-family: var(--font-heading); font-weight: 600; font-size: 32px; line-height: 1.2; overflow-wrap: anywhere; }
.hero-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 8px; color: var(--color-muted); font-size: 14px; }

.alert { padding: 12px 16px; border-radius: var(--radius-sm); background: var(--color-danger-bg); color: var(--color-danger); font-size: 14px; }

.layout { display: flex; align-items: flex-start; gap: 24px; }
.teams { flex: 1; min-width: 0; }
.setup { width: 460px; flex-shrink: 0; gap: 20px; }

.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.count { color: var(--color-muted); font-size: 14px; font-weight: 600; }
.locked-note { padding: 12px 16px; border-radius: var(--radius-sm); background: var(--color-warning-bg); color: var(--color-warning); font-size: 14px; }

.search-box { overflow: hidden; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-bg); }
.search-box:focus-within,
.search-box.open { border-color: var(--color-accent); }
.search { display: flex; align-items: center; gap: 10px; padding: 0 16px; color: var(--color-muted); }
.search input {
  flex: 1;
  min-width: 0;
  padding: 14px 0;
  border: 0;
  background: none;
  color: var(--color-text);
  font: inherit;
  font-size: 14px;
}
.search input:focus { outline: 0; }
.results { margin: 0; padding: 0; list-style: none; }
.result { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-top: 1px solid var(--color-border); }
.result.disabled { background: var(--color-surface); }
.result.disabled .result-name { color: var(--color-muted); }
.result-text { display: flex; flex: 1; flex-direction: column; min-width: 0; }
.result-name { font-weight: 600; font-size: 14px; }
.result-sub { color: var(--color-muted); font-size: 12px; }
.cannot { color: var(--color-muted); font-size: 12px; white-space: nowrap; }
.cannot.danger { color: var(--color-danger); }
.empty-result { color: var(--color-muted); font-size: 13px; }

.table-wrap { overflow-x: auto; border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.table { width: 100%; min-width: 420px; border-collapse: collapse; table-layout: fixed; }
.c-seed { width: 92px; }
.c-players { width: 150px; }
.c-remove { width: 80px; }
th { padding: 12px 16px; background: var(--color-surface-2); color: var(--color-muted); font-size: 13px; font-weight: 500; text-align: left; }
td { padding: 10px 16px; border-top: 1px solid var(--color-border); font-size: 14px; }
.seed {
  width: 44px;
  height: 32px;
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
.seed::-webkit-inner-spin-button,
.seed::-webkit-outer-spin-button { appearance: none; margin: 0; }
.seed:focus { outline: 0; border-color: var(--color-accent); }
.seed-static { display: inline-flex; align-items: center; justify-content: center; }
.team-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.team-cell :deep(.team-logo) { border-radius: 8px; }
.team-name { overflow: hidden; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.ok { color: var(--color-success); }
.bad { color: var(--color-danger); }
.remove-cell { text-align: center; }
.remove { padding: 4px 8px; border: 0; background: none; color: var(--color-danger); font-size: 15px; cursor: pointer; }
.remove:disabled { opacity: 0.4; }
.empty { padding: 32px 16px; color: var(--color-muted); text-align: center; }

.summary { display: flex; flex-direction: column; gap: 14px; margin: 0; padding-bottom: 20px; border-bottom: 1px solid var(--color-border); }
.summary div { display: flex; justify-content: space-between; gap: 12px; font-size: 14px; }
.summary dt { color: var(--color-muted); }
.summary dd { margin: 0; font-weight: 600; text-align: right; }

.checks-title { margin-bottom: 12px; font-size: 14px; font-weight: 600; }
.check-list { display: flex; flex-direction: column; gap: 12px; margin: 0; padding: 0; list-style: none; }
.check { display: flex; align-items: flex-start; gap: 10px; font-size: 14px; }
.check-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 700;
}
.check.pass .check-icon { background: var(--color-success-bg); color: var(--color-success); }
.check.fail { color: var(--color-danger); }
.check.fail .check-icon { background: var(--color-danger-bg); color: var(--color-danger); }
.check.warn { color: var(--color-warning); }
.check.warn .check-icon { background: var(--color-warning-bg); color: var(--color-warning); }
.check-detail { display: block; margin-top: 2px; color: var(--color-muted); font-size: 12px; }

.schedule-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.schedule-list { display: flex; flex-direction: column; gap: 6px; }
.schedule-error { margin-top: 10px; }
.slot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 40px;
  padding: 6px 12px;
  border-radius: 8px;
  background: var(--color-bg);
  font-size: 13px;
}
.slot-name { font-weight: 600; white-space: nowrap; }
.slot-time { padding: 2px 0; border: 0; border-bottom: 1px dashed transparent; background: none; color: var(--color-muted); font: inherit; cursor: pointer; white-space: nowrap; }
.slot-time:not(:disabled):hover { border-bottom-color: var(--color-accent); color: var(--color-text); }
.slot-input {
  min-width: 0;
  width: 150px;
  padding: 2px 4px;
  border: 1px solid var(--color-accent);
  border-radius: 6px;
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  font-size: 12px;
  color-scheme: dark;
}
.slot-static { display: flex; align-items: center; gap: 8px; color: var(--color-muted); white-space: nowrap; }
.slot-status { padding: 1px 8px; border-radius: var(--radius-pill); background: var(--color-info-bg); color: var(--color-info); font-size: 11px; }
.slot-status.done { background: var(--color-neutral-bg); color: var(--color-muted); }

@media (max-width: 1280px) {
  .layout { flex-direction: column; align-items: stretch; }
  .setup { width: auto; }
}

@media (max-width: 640px) {
  .hero-title { font-size: 24px; }
  .schedule-grid { grid-template-columns: 1fr; }
  .table { min-width: 0; }
  .c-seed { width: 60px; }
  .c-players { width: 92px; }
  .c-remove { width: 44px; }
  th, td { padding: 10px 8px; }
  th:first-child, td:first-child { padding-left: 12px; }
  .seed { width: 36px; }
  .team-cell :deep(.team-logo) { display: none; }
}
</style>

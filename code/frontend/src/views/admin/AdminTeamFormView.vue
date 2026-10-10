<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AdminLayout from '@/components/admin/AdminLayout.vue'
import { ROLE_SUGGESTIONS, getFreePlayers, getTeamForm, saveTeam } from '@/api/admin'
import { getGames } from '@/mock/queries'
import { initials } from '@/utils/format'

const props = defineProps({
  id: { type: String, default: null },
})

const router = useRouter()
const games = getGames()
const isEdit = computed(() => props.id !== null)
const source = isEdit.value ? getTeamForm(props.id) : null
const notFound = isEdit.value && !source

let rowKey = 0
const toRow = (p) => ({ key: (rowKey += 1), id: p.id ?? null, name: p.name ?? '', role: p.role ?? '', description: p.description ?? '' })

const form = reactive({
  name: source?.name ?? '',
  gameId: source ? String(source.gameId) : '',
  description: source?.description ?? '',
  logoUrl: source?.logoUrl ?? null,
})
const rows = ref((source?.players ?? []).map(toRow))
const originalPlayers = source?.players ?? []
const freePlayers = getFreePlayers()

const gameLocked = source?.gameLocked ?? false
const selectedGame = computed(() => games.find((g) => String(g.id) === form.gameId) ?? null)
const minPlayers = computed(() => selectedGame.value?.minPlayers ?? null)
const roleOptions = computed(() => ROLE_SUGGESTIONS[selectedGame.value?.code] ?? [])
const logoText = computed(() => (form.name.trim() ? initials(form.name) : '?'))
const titleName = source?.name ?? ''

const pickerOpen = ref(false)
const pickerQuery = ref('')
const available = computed(() => {
  const used = new Set(rows.value.map((r) => r.id).filter(Boolean))
  const removed = originalPlayers.filter((p) => !used.has(p.id))
  const q = pickerQuery.value.trim().toLowerCase()
  return [...removed, ...freePlayers]
    .filter((p) => !used.has(p.id))
    .filter((p) => !q || p.name.toLowerCase().includes(q) || p.role.toLowerCase().includes(q))
})

function addNewPlayer() {
  rows.value.push(toRow({}))
}

function pickPlayer(p) {
  rows.value.push(toRow(p))
}

function removeRow(key) {
  rows.value = rows.value.filter((r) => r.key !== key)
}

const logoError = ref('')
const fileInput = ref(null)

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

const submitted = ref(false)
const saving = ref(false)
const errors = ref({})
const saveError = ref('')

const rowInvalid = (row, field) => submitted.value && !row[field].trim()

function clientErrors() {
  const list = {}
  if (!form.name.trim()) list.name = 'กรุณากรอกชื่อทีม'
  if (!form.gameId) list.gameId = 'กรุณาเลือกเกม'
  if (rows.value.some((r) => !r.name.trim() || !r.role.trim())) list.players = 'ผู้เล่นทุกคนต้องมีชื่อและตำแหน่ง'
  return list
}

async function submit() {
  submitted.value = true
  saveError.value = ''
  errors.value = clientErrors()
  if (Object.keys(errors.value).length) return
  saving.value = true
  try {
    await saveTeam(isEdit.value ? props.id : null, {
      ...form,
      players: rows.value.map(({ id, name, role, description }) => ({ id, name, role, description })),
    })
    router.push('/admin/teams')
  } catch (e) {
    if (e.fields) errors.value = e.fields
    else saveError.value = e.status ? `บันทึกไม่สำเร็จ: ${e.message}` : 'บันทึกไม่สำเร็จ ลองใหม่อีกครั้ง'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AdminLayout>
    <div class="admin-page">
      <nav class="admin-crumbs" aria-label="breadcrumb">
        <RouterLink to="/admin/teams">ทีม</RouterLink>
        <span>/</span>
        <span class="current">{{ isEdit ? 'แก้ไขทีม' : 'เพิ่มทีม' }}</span>
      </nav>

      <section v-if="notFound" class="admin-panel missing">
        <h1 class="admin-title">ไม่พบทีมนี้</h1>
        <RouterLink to="/admin/teams" class="admin-btn admin-btn-outline">กลับไปหน้าทีม</RouterLink>
      </section>

      <form v-else class="admin-page" novalidate @submit.prevent="submit">
        <header class="head">
          <h1 class="admin-title">{{ isEdit ? `แก้ไขทีม: ${titleName}` : 'เพิ่มทีม' }}</h1>
          <div class="head-actions">
            <p v-if="saveError" class="admin-error">{{ saveError }}</p>
            <RouterLink to="/admin/teams" class="admin-btn admin-btn-outline">ยกเลิก</RouterLink>
            <button type="submit" class="admin-btn admin-btn-accent" :disabled="saving">{{ saving ? 'กำลังบันทึก…' : 'บันทึก' }}</button>
          </div>
        </header>

        <div class="layout">
          <section class="admin-panel info">
            <h2 class="admin-panel-title">ข้อมูลทีม</h2>

            <div class="logo-row">
              <span class="logo-box">
                <img v-if="form.logoUrl" :src="form.logoUrl" alt="โลโก้ทีม" />
                <template v-else>{{ logoText }}</template>
              </span>
              <div class="logo-text">
                <span class="logo-title">โลโก้ทีม</span>
                <span class="admin-hint">PNG หรือ JPG สี่เหลี่ยมจัตุรัส ไม่เกิน 2 MB</span>
                <div class="logo-actions">
                  <input ref="fileInput" type="file" accept="image/png,image/jpeg" class="sr-only" @change="onLogoChange" />
                  <button type="button" class="admin-btn admin-btn-outline admin-btn-sm" @click="fileInput?.click()">
                    {{ form.logoUrl ? 'เปลี่ยนโลโก้' : 'อัปโหลดโลโก้' }}
                  </button>
                  <button v-if="form.logoUrl" type="button" class="admin-btn admin-btn-outline admin-btn-sm" @click="form.logoUrl = null">ลบโลโก้</button>
                </div>
                <p v-if="logoError" class="admin-error">{{ logoError }}</p>
              </div>
            </div>

            <div class="admin-field">
              <label class="admin-label" for="team-name">ชื่อทีม</label>
              <input
                id="team-name"
                v-model="form.name"
                class="admin-input"
                :class="{ invalid: submitted && errors.name }"
                maxlength="150"
                placeholder="เช่น Falcon Five"
              />
              <p v-if="submitted && errors.name" class="admin-error">{{ errors.name }}</p>
            </div>

            <div class="admin-field">
              <label class="admin-label" for="team-game">เกม</label>
              <select
                id="team-game"
                v-model="form.gameId"
                class="admin-input"
                :class="{ invalid: submitted && errors.gameId }"
                :disabled="gameLocked"
              >
                <option value="" disabled>เลือกเกม</option>
                <option v-for="g in games" :key="g.id" :value="String(g.id)">{{ g.name }}</option>
              </select>
              <p v-if="submitted && errors.gameId" class="admin-error">{{ errors.gameId }}</p>
              <p v-else-if="gameLocked" class="admin-hint">เปลี่ยนเกมไม่ได้ เพราะทีมเคยลงแข่งแล้ว</p>
            </div>

            <div class="admin-field">
              <label class="admin-label" for="team-desc">คำอธิบายทีม</label>
              <textarea id="team-desc" v-model="form.description" class="admin-input" rows="3" placeholder="คณะ ปีที่ก่อตั้ง หรือข้อมูลอื่นที่ผู้ชมเห็น"></textarea>
            </div>
          </section>

          <section class="admin-panel players">
            <header class="players-head">
              <div>
                <h2 class="admin-panel-title">ผู้เล่น</h2>
                <p class="admin-hint">
                  <template v-if="selectedGame">
                    {{ selectedGame.name }} ต้องมีผู้เล่นอย่างน้อย {{ minPlayers }} คน ทีมถึงจะเข้ารายการได้ ·
                    <span :class="rows.length >= minPlayers ? 'ok' : 'bad'">ตอนนี้ {{ rows.length }} คน</span>
                  </template>
                  <template v-else>เลือกเกมก่อนเพื่อดูจำนวนผู้เล่นขั้นต่ำ</template>
                </p>
              </div>
              <div class="players-actions">
                <button
                  type="button"
                  class="admin-btn admin-btn-outline admin-btn-sm"
                  :aria-expanded="pickerOpen"
                  @click="pickerOpen = !pickerOpen"
                >เลือกจากผู้เล่นที่ยังไม่มีทีม</button>
                <button type="button" class="admin-btn admin-btn-accent admin-btn-sm" @click="addNewPlayer">+ ผู้เล่นใหม่</button>
              </div>
            </header>

            <div v-if="pickerOpen" class="picker">
              <label class="picker-search">
                <span aria-hidden="true">⌕</span>
                <input v-model="pickerQuery" type="search" placeholder="ค้นหาชื่อหรือตำแหน่ง…" aria-label="ค้นหาผู้เล่นที่ยังไม่มีทีม" />
              </label>
              <ul class="picker-list">
                <li v-for="p in available" :key="p.id" class="picker-item">
                  <span class="picker-text">
                    <span class="picker-name">{{ p.name }}</span>
                    <span class="picker-sub">{{ p.role }}<template v-if="p.description"> · {{ p.description }}</template></span>
                  </span>
                  <button type="button" class="admin-btn admin-btn-accent admin-btn-sm" @click="pickPlayer(p)">+ เพิ่ม</button>
                </li>
                <li v-if="!available.length" class="picker-empty">ไม่มีผู้เล่นที่ยังไม่มีทีม</li>
              </ul>
            </div>

            <div class="table-wrap">
              <table class="table">
                <colgroup>
                  <col class="c-name" />
                  <col class="c-role" />
                  <col class="c-desc" />
                  <col class="c-remove" />
                </colgroup>
                <thead>
                  <tr>
                    <th>ชื่อผู้เล่น</th>
                    <th>ตำแหน่ง</th>
                    <th>คำอธิบาย</th>
                    <th><span class="sr-only">นำออก</span></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in rows" :key="row.key">
                    <td>
                      <input
                        v-model="row.name"
                        class="cell-input"
                        :class="{ invalid: rowInvalid(row, 'name') }"
                        maxlength="150"
                        placeholder="ชื่อในเกม"
                        :aria-label="`ชื่อผู้เล่นคนที่ ${i + 1}`"
                      />
                    </td>
                    <td>
                      <input
                        v-model="row.role"
                        class="cell-input"
                        :class="{ invalid: rowInvalid(row, 'role') }"
                        maxlength="100"
                        list="role-options"
                        placeholder="ตำแหน่ง"
                        :aria-label="`ตำแหน่งผู้เล่นคนที่ ${i + 1}`"
                      />
                    </td>
                    <td>
                      <input v-model="row.description" class="cell-input" placeholder="ไม่ระบุ" :aria-label="`คำอธิบายผู้เล่นคนที่ ${i + 1}`" />
                    </td>
                    <td class="remove-cell">
                      <button type="button" class="remove" @click="removeRow(row.key)">นำออก</button>
                    </td>
                  </tr>
                  <tr v-if="!rows.length">
                    <td colspan="4" class="empty">ยังไม่มีผู้เล่น กด "+ ผู้เล่นใหม่" หรือเลือกจากผู้เล่นที่ยังไม่มีทีม</td>
                  </tr>
                </tbody>
              </table>
              <datalist id="role-options">
                <option v-for="r in roleOptions" :key="r" :value="r"></option>
              </datalist>
            </div>
            <p v-if="submitted && errors.players" class="admin-error">{{ errors.players }}</p>
            <p class="admin-hint">"นำออก" ถอดผู้เล่นออกจากทีมเท่านั้น ข้อมูลผู้เล่นยังอยู่ในหน้าผู้เล่น · การเปลี่ยนแปลงจะมีผลเมื่อกดบันทึก</p>
          </section>
        </div>
      </form>
    </div>
  </AdminLayout>
</template>

<style scoped>
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
.missing { align-items: flex-start; }

.head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 16px; }
.head .admin-title { overflow-wrap: anywhere; }
.head-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }

.layout { display: flex; align-items: flex-start; gap: 24px; min-width: 0; }
.layout > * { min-width: 0; }
.info { width: 520px; flex-shrink: 0; gap: 20px; padding: 32px; }
.players { flex: 1; min-width: 0; padding: 32px 0 24px; }
.players > :not(.table-wrap) { margin: 0 32px; }

.logo-row { display: flex; align-items: center; gap: 20px; }
.logo-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 36px;
}
.logo-box img { width: 100%; height: 100%; object-fit: cover; }
.logo-text { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.logo-title { font-weight: 600; font-size: 15px; }
.logo-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }

.players-head { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: 12px; }
.players-head .admin-hint { margin-top: 4px; }
.players-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.ok { color: var(--color-success); }
.bad { color: var(--color-danger); }

.picker { overflow: hidden; border: 1px solid var(--color-accent); border-radius: var(--radius-md); background: var(--color-bg); }
.picker-search { display: flex; align-items: center; gap: 10px; padding: 0 16px; color: var(--color-muted); }
.picker-search input { flex: 1; min-width: 0; padding: 12px 0; border: 0; background: none; color: var(--color-text); font: inherit; font-size: 14px; }
.picker-search input:focus { outline: 0; }
.picker-list { max-height: 240px; margin: 0; padding: 0; overflow-y: auto; list-style: none; }
.picker-item { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-top: 1px solid var(--color-border); }
.picker-text { display: flex; flex: 1; flex-direction: column; min-width: 0; }
.picker-name { font-weight: 600; font-size: 14px; }
.picker-sub { overflow: hidden; color: var(--color-muted); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.picker-empty { padding: 14px 16px; border-top: 1px solid var(--color-border); color: var(--color-muted); font-size: 13px; }

.table-wrap { overflow-x: auto; }
.table { width: 100%; min-width: 640px; border-collapse: collapse; table-layout: fixed; }
.c-role { width: 22%; }
.c-desc { width: 30%; }
.c-remove { width: 88px; }
th { padding: 12px 8px; background: var(--color-surface-2); color: var(--color-muted); font-size: 13px; font-weight: 500; text-align: left; }
th:first-child, td:first-child { padding-left: 32px; }
td { padding: 10px 8px; border-bottom: 1px solid var(--color-border); }
.cell-input {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text);
  font: inherit;
  font-size: 14px;
}
.cell-input::placeholder { color: var(--color-muted); }
.cell-input:focus { outline: 0; border-color: var(--color-accent); }
.cell-input.invalid { border-color: var(--color-danger); }
.remove-cell { text-align: left; }
.remove { padding: 4px; border: 0; background: none; color: var(--color-danger); font-size: 14px; font-weight: 600; cursor: pointer; white-space: nowrap; }
.remove:hover { text-decoration: underline; }
.empty { padding: 32px 16px; color: var(--color-muted); font-size: 14px; text-align: center; }

@media (max-width: 1280px) {
  .layout { flex-direction: column; align-items: stretch; }
  .info { width: auto; }
}

@media (max-width: 640px) {
  .head-actions { width: 100%; }
  .head-actions .admin-btn { flex: 1; }
  .info { padding: 16px; }
  .players { padding: 16px 0; }
  .players > :not(.table-wrap) { margin: 0 16px; }
  .logo-box { width: 88px; height: 88px; font-size: 28px; }
  th:first-child, td:first-child { padding-left: 16px; }
  .table { min-width: 0; }
  .table thead { display: none; }
  .table, .table tbody { display: block; }
  .table tr {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
    gap: 8px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--color-border);
  }
  .table td { display: block; padding: 0; border: 0; }
  .table td:first-child { padding-left: 0; }
  .table td:nth-child(3) { grid-column: 1 / 3; }
  .table td.remove-cell { grid-column: 3; grid-row: 1; align-self: center; }
  .table td:nth-child(2) { grid-column: 2; grid-row: 1; }
  .table td.empty { grid-column: 1 / -1; padding: 24px 0; }
  .table tr:has(.empty) { display: block; }
}
</style>

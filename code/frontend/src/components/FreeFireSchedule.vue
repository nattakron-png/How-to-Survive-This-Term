<script setup>
import { computed } from 'vue'
import { formatDate, formatTime, initials } from '@/utils/format'

const props = defineProps({
  games: { type: Array, required: true },
})
defineEmits(['open-game'])

const nextGameId = computed(() => props.games.find((g) => g.status !== 'COMPLETED')?.id ?? null)

function statusOf(game) {
  if (game.status === 'COMPLETED') return { label: 'จบแล้ว', tone: 'success' }
  if (game.id === nextGameId.value) return { label: 'เกมถัดไป', tone: 'accent' }
  return { label: 'รอแข่ง', tone: 'info' }
}

const schedule = (at) => (at ? `${formatDate(at)}, ${formatTime(at)}` : 'รอกำหนดวัน')
</script>

<template>
  <div class="table">
    <div class="row head">
      <span class="col-game">เกม</span>
      <span class="col-time">วันเวลา</span>
      <span class="col-status">สถานะ</span>
      <span class="col-booyah">Booyah (อันดับ 1)</span>
      <span class="col-action"></span>
    </div>
    <div v-for="game in games" :key="game.id" class="row" :class="{ next: game.id === nextGameId }">
      <span class="col-game game-number">เกมที่ {{ game.gameNumber }}</span>
      <span class="col-time muted">{{ schedule(game.scheduledAt) }}</span>
      <span class="col-status">
        <span class="badge" :class="statusOf(game).tone">{{ statusOf(game).label }}</span>
      </span>
      <span class="col-booyah">
        <template v-if="game.booyahTeam">
          <span class="logo">
            <img v-if="game.booyahTeam.logoUrl" :src="game.booyahTeam.logoUrl" :alt="game.booyahTeam.name" />
            <template v-else>{{ initials(game.booyahTeam.name) }}</template>
          </span>
          <span class="team-name">{{ game.booyahTeam.name }}</span>
        </template>
        <span v-else class="dash">–</span>
      </span>
      <span class="col-action">
        <button
          v-if="game.status === 'COMPLETED'"
          type="button"
          class="link"
          @click="$emit('open-game', game.gameNumber)"
        >ดูผลเกม ›</button>
      </span>
    </div>
    <p v-if="!games.length" class="empty">ยังไม่มีตารางเกม</p>
  </div>
</template>

<style scoped>
.table { overflow: hidden; border-radius: var(--radius-md); background: var(--color-surface); }
.row {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 24px;
  border-top: 1px solid var(--color-surface-2);
  font-size: 15px;
}
.row.head {
  border-top: 0;
  background: var(--color-surface-2);
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 500;
}
.row.next { background: rgba(122, 184, 255, 0.06); }
.col-game { width: 120px; flex-shrink: 0; }
.col-time { width: 200px; flex-shrink: 0; }
.col-status { width: 140px; flex-shrink: 0; display: flex; }
.col-booyah { flex: 1; min-width: 0; display: flex; align-items: center; gap: 10px; }
.col-action { flex-shrink: 0; min-width: 64px; text-align: right; }
.game-number { font-weight: 600; }
.muted { color: var(--color-muted); }
.badge { padding: 3px 10px; border-radius: var(--radius-pill); font-size: 13px; font-weight: 500; white-space: nowrap; }
.success { background: var(--color-success-bg); color: var(--color-success); }
.info { background: var(--color-info-bg); color: var(--color-info); }
.accent { background: rgba(255, 253, 131, 0.15); color: var(--color-accent); }
.logo {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-size: 9px;
  font-weight: 600;
}
.logo img { width: 100%; height: 100%; object-fit: cover; }
.team-name { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dash { color: #6b6b73; }
.link {
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-accent);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}
.link:hover { text-decoration: underline; }
.empty { padding: 32px 24px; text-align: center; color: var(--color-muted); }

@media (max-width: 900px) {
  .row.head { display: none; }
  .row { flex-wrap: wrap; gap: 6px 12px; padding: 14px 16px; }
  .col-game, .col-time, .col-status { width: auto; }
  .col-time { flex: 1; font-size: 14px; }
  .col-booyah { flex-basis: 60%; }
  .row:nth-child(2) { border-top: 0; }
}
</style>

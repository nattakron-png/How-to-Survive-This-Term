<script setup>
import { computed } from 'vue'
import { formatDateRange, formatShortDate, formatTime, formatTournamentFormat, initials } from '@/utils/format'

const props = defineProps({
  tournament: { type: Object, required: true },
  standings: { type: Object, required: true },
  recentGames: { type: Array, required: true },
  placementPoints: { type: Array, required: true },
})
defineEmits(['show-standings'])

const isFinished = computed(() => props.standings.gamesCompleted >= props.standings.totalGames)

const topThree = computed(() =>
  props.standings.gamesCompleted > 0 ? props.standings.standings.slice(0, 3) : [],
)

const rankingTitle = computed(() => {
  if (isFinished.value) return 'อันดับสุดท้าย'
  if (props.standings.gamesCompleted === 0) return 'อันดับปัจจุบัน'
  return `อันดับปัจจุบัน (หลังเกมที่ ${props.standings.gamesCompleted})`
})

const infoRows = computed(() => {
  const t = props.tournament
  const first = props.placementPoints.find((p) => p.placement === 1)
  return [
    { key: 'เกม', value: t.game.name },
    { key: 'รูปแบบ', value: formatTournamentFormat(t.format, t.totalGames) },
    { key: 'วันแข่ง', value: formatDateRange(t.startDate, t.endDate) },
    { key: 'จำนวนทีม', value: `${t.teams.length} ทีม` },
    { key: 'คะแนนต่อ kill', value: `${t.pointsPerKill} คะแนน` },
    ...(first ? [{ key: 'คะแนนอันดับ 1', value: `${first.points} คะแนน (Booyah)` }] : []),
    ...(t.champion ? [{ key: 'แชมป์', value: t.champion.name, highlight: true }] : []),
  ]
})

const gameTime = (at) => (at ? `${formatShortDate(at)} ${formatTime(at)}` : '-')
</script>

<template>
  <div class="columns">
    <div class="left">
      <div class="section-header">
        <h2 class="section-title">{{ rankingTitle }}</h2>
        <button v-if="topThree.length" type="button" class="link" @click="$emit('show-standings')">ดูตารางคะแนนเต็ม →</button>
      </div>

      <div v-if="topThree.length" class="top-three">
        <div v-for="row in topThree" :key="row.teamId" class="podium" :class="{ first: row.rank === 1 }">
          <span class="podium-rank">อันดับ {{ row.rank }}</span>
          <span class="podium-logo">
            <img v-if="row.team.logoUrl" :src="row.team.logoUrl" :alt="row.teamName" />
            <template v-else>{{ initials(row.teamName) }}</template>
          </span>
          <span class="podium-name">{{ row.teamName }}</span>
          <span class="podium-points">{{ row.totalPoints }} คะแนน</span>
        </div>
      </div>
      <p v-else class="empty">ยังไม่เริ่มแข่ง</p>

      <div class="section-header">
        <h2 class="section-title">เกมล่าสุด</h2>
      </div>

      <template v-if="recentGames.length">
        <div v-for="game in recentGames" :key="game.id" class="game">
          <span class="game-number">เกมที่ {{ game.gameNumber }}</span>
          <span class="game-time">{{ gameTime(game.scheduledAt) }}</span>
          <template v-if="game.booyahTeam">
            <span class="game-label">Booyah</span>
            <span class="game-logo">
              <img v-if="game.booyahTeam.logoUrl" :src="game.booyahTeam.logoUrl" :alt="game.booyahTeam.name" />
              <template v-else>{{ initials(game.booyahTeam.name) }}</template>
            </span>
            <span class="game-team">{{ game.booyahTeam.name }}</span>
          </template>
          <span v-else class="game-team muted">ยังไม่มีผล</span>
          <span class="game-detail">ดูผลเกม ›</span>
        </div>
      </template>
      <p v-else class="empty">ยังไม่มีเกมที่แข่งจบ</p>
    </div>

    <aside class="infobox">
      <h2 class="infobox-header">ข้อมูลรายการ</h2>
      <div v-for="row in infoRows" :key="row.key" class="info-row">
        <span class="info-key">{{ row.key }}</span>
        <span class="info-value" :class="{ highlight: row.highlight }">{{ row.value }}</span>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.columns { display: flex; align-items: flex-start; gap: 24px; }
.left { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 16px; }

.section-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.section-title { font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.link {
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-accent);
  font-size: 14px;
  font-weight: 500;
}

.top-three { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.podium {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 20px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  text-align: center;
}
.podium.first { border-color: rgba(255, 253, 131, 0.5); }
.podium-rank { color: var(--color-muted); font-size: 14px; font-weight: 500; }
.podium.first .podium-rank { color: var(--color-accent); }
.podium-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-size: 19px;
  font-weight: 600;
}
.podium-logo img, .game-logo img { width: 100%; height: 100%; object-fit: cover; }
.podium-name { max-width: 100%; font-size: 17px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.podium-points { font-family: var(--font-heading); font-weight: 600; font-size: 20px; }
.podium.first .podium-points { color: var(--color-accent); }

.game {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}
.game-number { width: 100px; flex-shrink: 0; font-size: 15px; font-weight: 600; }
.game-time { width: 140px; flex-shrink: 0; color: var(--color-muted); font-size: 14px; }
.game-label { color: var(--color-muted); font-size: 13px; font-weight: 500; }
.game-logo {
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
.game-team { flex: 1; min-width: 0; font-size: 15px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.game-team.muted { color: var(--color-muted); }
.game-detail { flex-shrink: 0; color: var(--color-accent); font-size: 14px; font-weight: 500; }

.infobox {
  width: 440px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}
.infobox-header { padding: 14px 20px; background: var(--color-surface-2); font-size: 16px; font-weight: 600; }
.info-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  border-top: 1px solid var(--color-surface-2);
  font-size: 14px;
}
.info-key { color: var(--color-muted); }
.info-value { font-weight: 500; text-align: right; }
.info-value.highlight { color: var(--color-accent); }

.empty {
  padding: 32px 24px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  text-align: center;
  color: var(--color-muted);
  font-size: 15px;
}

@media (max-width: 1200px) {
  .columns { flex-direction: column-reverse; align-items: stretch; }
  .infobox { width: 100%; }
}
@media (max-width: 760px) {
  .top-three { grid-template-columns: 1fr; }
  .game { flex-wrap: wrap; gap: 6px 12px; padding: 14px 16px; }
  .game-number, .game-time { width: auto; }
  .game-detail { margin-left: auto; }
}
</style>

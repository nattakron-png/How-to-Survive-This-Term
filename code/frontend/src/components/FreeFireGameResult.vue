<script setup>
import { formatDate, formatTime, initials } from '@/utils/format'

const props = defineProps({
  result: { type: Object, required: true },
})
defineEmits(['back'])

const schedule = (at) => (at ? `${formatDate(at)}, ${formatTime(at)}` : 'รอกำหนดวัน')
</script>

<template>
  <div class="wrapper">
    <div class="game-header">
      <button type="button" class="back" @click="$emit('back')">‹ ตารางเกม</button>
      <h2 class="game-title">เกมที่ {{ result.game.gameNumber }}</h2>
      <span class="game-time">{{ schedule(result.game.scheduledAt) }}</span>
      <span class="badge">จบแล้ว</span>
    </div>

    <div class="scroller">
      <div class="table">
        <div class="row head">
          <span class="col-rank">อันดับ</span>
          <span class="col-team">ทีม</span>
          <span class="col-num">Kills</span>
          <span class="col-wide">คะแนนอันดับ</span>
          <span class="col-wide">คะแนน kill</span>
          <span class="col-num">รวม</span>
        </div>
        <div v-for="row in result.rows" :key="row.id" class="row" :class="{ booyah: row.placement === 1 }">
          <span class="col-rank rank">{{ row.placement }}</span>
          <span class="col-team team">
            <span class="logo">
              <img v-if="row.team.logoUrl" :src="row.team.logoUrl" :alt="row.team.name" />
              <template v-else>{{ initials(row.team.name) }}</template>
            </span>
            <span class="team-name">{{ row.team.name }}</span>
            <span v-if="row.placement === 1" class="booyah-badge">Booyah</span>
          </span>
          <span class="col-num">{{ row.kills }}</span>
          <span class="col-wide muted">{{ row.placementPoints }}</span>
          <span class="col-wide muted">{{ row.killPoints }}</span>
          <span class="col-num total">{{ row.total }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrapper { display: flex; flex-direction: column; gap: 20px; }
.game-header { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; }
.back {
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-accent);
  font-size: 14px;
  font-weight: 500;
}
.back:hover { text-decoration: underline; }
.game-title { font-family: var(--font-heading); font-weight: 600; font-size: 24px; }
.game-time { color: var(--color-muted); font-size: 15px; }
.badge {
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  background: var(--color-success-bg);
  color: var(--color-success);
  font-size: 13px;
  font-weight: 500;
}

.scroller { overflow-x: auto; border-radius: var(--radius-md); scrollbar-width: none; }
.scroller::-webkit-scrollbar { display: none; }
.table { min-width: 860px; background: var(--color-surface); }
.row {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 24px;
  border-top: 1px solid var(--color-surface-2);
  font-size: 15px;
}
.row.head {
  padding-top: 10px;
  padding-bottom: 10px;
  border-top: 0;
  background: var(--color-surface-2);
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 500;
}
.row.booyah { background: rgba(255, 253, 131, 0.08); }
.col-rank { width: 80px; flex-shrink: 0; }
.col-team { width: 420px; flex-shrink: 0; }
.col-num { width: 100px; flex-shrink: 0; text-align: center; }
.col-wide { width: 140px; flex-shrink: 0; text-align: center; }
.rank { font-family: var(--font-heading); font-weight: 600; font-size: 16px; }
.row.booyah .rank { color: var(--color-accent); }
.team { display: flex; align-items: center; gap: 10px; min-width: 0; }
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
.booyah-badge {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  background: rgba(255, 253, 131, 0.15);
  color: var(--color-accent);
  font-size: 13px;
  font-weight: 500;
}
.muted { color: var(--color-muted); }
.total { color: var(--color-accent); font-weight: 600; font-size: 16px; }
</style>

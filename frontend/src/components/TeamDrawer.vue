<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatRound, formatShortDate, initials } from '@/utils/format'

const props = defineProps({
  team: { type: Object, required: true },
  tournament: { type: Object, required: true },
  matches: { type: Array, required: true },
})
const emit = defineEmits(['close'])

const closeButton = ref(null)

const players = computed(() => props.team.players ?? [])

const RESULT_TONES = {
  WIN: { label: 'ชนะ', tone: 'success' },
  LOSS: { label: 'แพ้', tone: 'danger' },
  BYE: { label: 'ผ่านอัตโนมัติ', tone: 'success' },
  READY: { label: 'รอแข่ง', tone: 'info' },
  WAITING: { label: 'รอคู่แข่ง', tone: 'neutral' },
}

const results = computed(() =>
  props.matches
    .filter((m) => m.teamA?.id === props.team.id || m.teamB?.id === props.team.id)
    .map((m) => {
      const isA = m.teamA?.id === props.team.id
      const opponent = isA ? m.teamB : m.teamA
      const opponentFeeder = isA ? m.feederB : m.feederA
      const round = formatRound(m.roundNumber, m.teamCount)
      let kind
      if (m.status === 'COMPLETED') kind = !opponent ? 'BYE' : m.result?.winnerTeamId === props.team.id ? 'WIN' : 'LOSS'
      else kind = opponent ? 'READY' : 'WAITING'

      const text = kind === 'BYE'
        ? `${round} ผ่านอัตโนมัติ (BYE)`
        : `${round} พบ ${opponent?.name ?? (opponentFeeder ? `ผู้ชนะแมตช์ ${opponentFeeder}` : 'รอทีม')}`

      let side = null
      if (m.result) {
        const own = isA ? m.result.teamAScore : m.result.teamBScore
        const other = isA ? m.result.teamBScore : m.result.teamAScore
        side = { type: 'score', value: `${own} – ${other}` }
      } else if (kind !== 'BYE') {
        side = { type: 'date', value: m.scheduledAt ? formatShortDate(m.scheduledAt) : 'รอกำหนดวัน' }
      }

      return { id: m.id, badge: RESULT_TONES[kind], text, side }
    }),
)

function playerInitials(name) {
  const base = name.includes('.') ? name.split('.').pop() : name
  return base.replace(/[^A-Za-z]/g, '').slice(0, 2).toUpperCase() || '?'
}

function onKeydown(event) {
  if (event.key === 'Escape') emit('close')
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  document.body.style.overflow = 'hidden'
  nextTick(() => closeButton.value?.focus())
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
watch(() => props.team.id, () => nextTick(() => closeButton.value?.focus()))
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <aside class="drawer" role="dialog" aria-modal="true" :aria-label="team.name">
      <div class="top">
        <span class="context">ทีมในรายการ {{ tournament.name }}</span>
        <button ref="closeButton" type="button" class="close" aria-label="ปิด" @click="emit('close')">✕</button>
      </div>

      <div class="team-header">
        <span class="team-logo">
          <img v-if="team.logoUrl" :src="team.logoUrl" :alt="team.name" />
          <template v-else>{{ initials(team.name) }}</template>
        </span>
        <div class="team-info">
          <h2 class="team-name">{{ team.name }}</h2>
          <span class="game-tag">{{ tournament.game.name }}</span>
        </div>
      </div>

      <p v-if="team.description" class="description">{{ team.description }}</p>

      <section v-if="results.length" class="results">
        <h3 class="results-title">ผลในรายการนี้</h3>
        <div v-for="r in results" :key="r.id" class="result">
          <span class="badge" :class="r.badge.tone">{{ r.badge.label }}</span>
          <span class="result-text">{{ r.text }}</span>
          <span v-if="r.side?.type === 'score'" class="result-score" :class="{ lost: r.badge.tone === 'danger' }">{{ r.side.value }}</span>
          <span v-else-if="r.side?.type === 'date'" class="result-date">{{ r.side.value }}</span>
        </div>
      </section>

      <div class="players-header">
        <h3 class="players-title">ผู้เล่น</h3>
        <span class="players-count">{{ players.length }} คน</span>
      </div>
      <div v-if="players.length" class="players">
        <div v-for="player in players" :key="player.id" class="player">
          <span class="avatar">{{ playerInitials(player.name) }}</span>
          <span class="player-name">{{ player.name }}</span>
          <span class="player-role">{{ player.role }}</span>
        </div>
      </div>
      <p v-else class="empty">ยังไม่มีรายชื่อผู้เล่น</p>
    </aside>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(0, 0, 0, 0.55);
}
.drawer {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 560px;
  max-width: 100%;
  height: 100%;
  overflow-y: auto;
  padding: 40px;
  border-left: 1px solid var(--color-border);
  background: var(--color-surface);
}

.top { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: var(--color-muted); }
.context { font-size: 14px; }
.close {
  padding: 4px 8px;
  border: 0;
  border-radius: var(--radius-sm);
  background: none;
  color: var(--color-muted);
  font-size: 20px;
  font-weight: 600;
  line-height: 1;
}
.close:hover { color: var(--color-text); background: var(--color-surface-2); }

.team-header { display: flex; align-items: center; gap: 20px; }
.team-logo {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 88px;
  height: 88px;
  border-radius: 22px;
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 30px;
}
.team-logo img { width: 100%; height: 100%; object-fit: cover; }
.team-info { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.team-name { font-family: var(--font-heading); font-weight: 600; font-size: 30px; overflow-wrap: anywhere; }
.game-tag {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-2);
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 500;
}
.description { color: var(--color-muted); font-size: 15px; }

.results {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
}
.results-title { font-size: 15px; font-weight: 600; }
.result { display: flex; align-items: center; gap: 10px; }
.badge { flex-shrink: 0; padding: 4px 12px; border-radius: var(--radius-pill); font-size: 12px; font-weight: 600; white-space: nowrap; }
.success { background: var(--color-success-bg); color: var(--color-success); }
.danger { background: var(--color-danger-bg); color: var(--color-danger); }
.info { background: var(--color-info-bg); color: var(--color-info); }
.neutral { background: var(--color-neutral-bg); color: var(--color-muted); }
.result-text { flex: 1; min-width: 0; font-size: 15px; }
.result-score { color: var(--color-accent); font-family: var(--font-heading); font-weight: 600; font-size: 18px; white-space: nowrap; }
.result-score.lost { color: var(--color-muted); }
.result-date { color: var(--color-muted); font-size: 14px; white-space: nowrap; }

.players-header { display: flex; align-items: baseline; justify-content: space-between; }
.players-title { font-family: var(--font-heading); font-weight: 600; font-size: 20px; }
.players-count { color: var(--color-muted); font-size: 14px; font-weight: 600; }
.players { overflow: hidden; border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.player { display: flex; align-items: center; gap: 12px; padding: 12px 16px; }
.player + .player { border-top: 1px solid var(--color-border); }
.avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-pill);
  background: rgba(255, 253, 131, 0.15);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 12px;
}
.player-name { flex: 1; min-width: 0; font-size: 16px; font-weight: 600; }
.player-role { color: var(--color-muted); font-size: 14px; white-space: nowrap; }
.empty { color: var(--color-muted); font-size: 14px; }

@media (max-width: 640px) {
  .drawer { padding: 24px 16px; }
  .team-logo { width: 64px; height: 64px; font-size: 22px; border-radius: var(--radius-lg); }
  .team-name { font-size: 24px; }
}
</style>

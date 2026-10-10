<script setup>
import { computed } from 'vue'
import BracketMatch from './BracketMatch.vue'
import { formatRound } from '@/utils/format'

const props = defineProps({
  matches: { type: Array, required: true },
  teamCount: { type: Number, required: true },
  champion: { type: Object, default: null },
  tournamentName: { type: String, required: true },
})

const COLUMN_GAP = 440
const MATCH_WIDTH = 320
const MATCH_HEIGHT = 142
const ROW_STEP = 174
const HEADER = 40
const CHAMPION_WIDTH = 300
const CHAMPION_HEIGHT = 110

const totalRounds = computed(() => Math.ceil(Math.log2(Math.max(props.teamCount, 2))))

const rounds = computed(() =>
  Array.from({ length: totalRounds.value }, (_, i) =>
    props.matches
      .filter((m) => m.roundNumber === i + 1)
      .sort((a, b) => a.matchNumber - b.matchNumber),
  ),
)

const isComplete = computed(() =>
  rounds.value.every((list, i) => list.length === 2 ** (totalRounds.value - i - 1)),
)

const centerY = (round, index) =>
  HEADER + MATCH_HEIGHT / 2 + (index * 2 ** round + (2 ** round - 1) / 2) * ROW_STEP

const positioned = computed(() =>
  rounds.value.flatMap((list, r) =>
    list.map((match, i) => ({
      match,
      left: r * COLUMN_GAP,
      top: centerY(r, i) - MATCH_HEIGHT / 2,
    })),
  ),
)

const finalCenter = computed(() => centerY(totalRounds.value - 1, 0))

const connectors = computed(() => {
  const paths = []
  for (let r = 0; r < totalRounds.value - 1; r += 1) {
    const x1 = r * COLUMN_GAP + MATCH_WIDTH
    const xMid = x1 + (COLUMN_GAP - MATCH_WIDTH) / 2
    const x2 = (r + 1) * COLUMN_GAP
    for (let i = 0; i < rounds.value[r + 1].length; i += 1) {
      const yA = centerY(r, i * 2)
      const yB = centerY(r, i * 2 + 1)
      const yNext = centerY(r + 1, i)
      paths.push(`M${x1} ${yA}H${xMid}V${yB}M${x1} ${yB}H${xMid}M${xMid} ${yNext}H${x2}`)
    }
  }
  const xFinal = (totalRounds.value - 1) * COLUMN_GAP + MATCH_WIDTH
  paths.push(`M${xFinal} ${finalCenter.value}H${totalRounds.value * COLUMN_GAP}`)
  return paths
})

const canvasWidth = computed(() => totalRounds.value * COLUMN_GAP + CHAMPION_WIDTH)
const canvasHeight = computed(() => HEADER + (2 ** (totalRounds.value - 1)) * ROW_STEP - (ROW_STEP - MATCH_HEIGHT))

const roundLabels = computed(() => [
  ...Array.from({ length: totalRounds.value }, (_, i) => formatRound(i + 1, props.teamCount)),
  'แชมป์',
])
</script>

<template>
  <div v-if="isComplete" class="scroller">
    <div class="canvas" :style="{ width: `${canvasWidth}px`, height: `${canvasHeight}px` }">
      <span
        v-for="(label, i) in roundLabels"
        :key="label"
        class="round-label"
        :style="{ left: `${i * COLUMN_GAP}px` }"
      >{{ label }}</span>

      <svg class="lines" :width="canvasWidth" :height="canvasHeight" aria-hidden="true">
        <path v-for="(d, i) in connectors" :key="i" :d="d" />
      </svg>

      <BracketMatch
        v-for="item in positioned"
        :key="item.match.id"
        :match="item.match"
        class="node"
        :style="{ left: `${item.left}px`, top: `${item.top}px` }"
      />

      <div
        class="champion"
        :style="{
          left: `${totalRounds * COLUMN_GAP}px`,
          top: `${finalCenter - CHAMPION_HEIGHT / 2}px`,
          width: `${CHAMPION_WIDTH}px`,
          minHeight: `${CHAMPION_HEIGHT}px`,
        }"
      >
        <template v-if="champion">
          <span class="champion-title">แชมป์ {{ tournamentName }}</span>
          <span class="champion-name">{{ champion.name }}</span>
        </template>
        <template v-else>
          <span class="champion-title">ผู้ชนะเลิศ</span>
          <span class="champion-wait">รอผลรอบชิงชนะเลิศ</span>
        </template>
      </div>
    </div>
  </div>
  <p v-else class="empty">ยังไม่ได้จัดสายการแข่ง</p>
</template>

<style scoped>
.scroller {
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.scroller::-webkit-scrollbar { display: none; }
.canvas { position: relative; }
.round-label {
  position: absolute;
  top: 0;
  color: var(--color-muted);
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}
.lines { position: absolute; inset: 0; pointer-events: none; }
.lines path { fill: none; stroke: var(--color-border); stroke-width: 2; }
.node { position: absolute; }
.champion {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-md);
  background: var(--color-accent-bg);
  text-align: center;
}
.champion-title { color: var(--color-accent); font-size: 15px; font-weight: 600; }
.champion-name { color: var(--color-accent); font-family: var(--font-heading); font-weight: 600; font-size: 22px; }
.champion-wait { color: var(--color-muted); font-size: 14px; }
.empty {
  padding: 32px 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  text-align: center;
  color: var(--color-muted);
  font-size: 15px;
}
</style>

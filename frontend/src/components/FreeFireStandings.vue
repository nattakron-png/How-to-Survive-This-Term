<script setup>
import { computed } from 'vue'
import { initials } from '@/utils/format'

const props = defineProps({
  standings: { type: Object, required: true },
  placementPoints: { type: Array, required: true },
  pointsPerKill: { type: Number, default: 1 },
})

const gameColumns = computed(() => Array.from({ length: props.standings.totalGames }, (_, i) => `G${i + 1}`))

const ordinal = (n) => {
  const suffix = n % 10 === 1 && n % 100 !== 11 ? 'st' : n % 10 === 2 && n % 100 !== 12 ? 'nd' : n % 10 === 3 && n % 100 !== 13 ? 'rd' : 'th'
  return `${n}${suffix}`
}

const scoringNote = computed(() => {
  const list = props.placementPoints
  if (!list.length) return ''
  const scoring = list.filter((p) => p.points > 0)
  const zero = list.filter((p) => p.points === 0)
  const head = scoring.slice(0, 3).map((p) => `${ordinal(p.placement)} ${p.points}`)
  const tail = scoring.length > 3 ? ['…', `${ordinal(scoring.at(-1).placement)} ${scoring.at(-1).points}`] : []
  const zeroPart = zero.length
    ? [`${zero[0].placement}${zero.length > 1 ? `–${zero.at(-1).placement}` : ''}th 0`]
    : []
  const placement = [...head, ...tail, ...zeroPart].join(' · ')
  return `คะแนนต่อเกม = คะแนนอันดับ (${placement}) + ${props.pointsPerKill} คะแนนต่อ kill  ·  คะแนนเท่ากันตัดสินด้วย Booyah → Kills → อันดับในเกมล่าสุด  ·  "–" = ยังไม่แข่ง`
})
</script>

<template>
  <div class="wrapper">
    <div class="scroller">
      <table class="table">
        <thead>
          <tr>
            <th class="col-rank">#</th>
            <th class="col-team">ทีม</th>
            <th class="col-total highlight">คะแนนรวม</th>
            <th class="col-booyah">Booyah</th>
            <th class="col-kills">Kills</th>
            <th v-for="g in gameColumns" :key="g" class="col-game">{{ g }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in standings.standings" :key="row.teamId" :class="{ leader: row.rank === 1 && standings.gamesCompleted > 0 }">
            <td class="col-rank">
              <span class="rank" :class="{ first: row.rank === 1 && standings.gamesCompleted > 0 }">{{ row.rank }}</span>
            </td>
            <td class="col-team">
              <span class="team">
                <span class="team-logo">
                  <img v-if="row.team.logoUrl" :src="row.team.logoUrl" :alt="row.teamName" />
                  <template v-else>{{ initials(row.teamName) }}</template>
                </span>
                <span class="team-name">{{ row.teamName }}</span>
              </span>
            </td>
            <td class="col-total total">{{ row.totalPoints }}</td>
            <td class="col-booyah" :class="{ muted: row.booyahs === 0 }">{{ row.booyahs }}</td>
            <td class="col-kills">{{ row.kills }}</td>
            <td v-for="(points, i) in row.pointsPerGame" :key="i" class="col-game" :class="{ muted: points === null }">
              {{ points ?? '–' }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-if="scoringNote" class="note">{{ scoringNote }}</p>
  </div>
</template>

<style scoped>
.wrapper { display: flex; flex-direction: column; gap: 20px; }
.scroller {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  scrollbar-width: none;
}
.scroller::-webkit-scrollbar { display: none; }
.table { width: 100%; min-width: 1100px; border-collapse: collapse; table-layout: fixed; }
th, td { box-sizing: border-box; padding: 0; text-align: center; white-space: nowrap; }
thead tr { background: var(--color-surface-2); }
th { padding-top: 12px; padding-bottom: 12px; color: var(--color-muted); font-size: 13px; font-weight: 600; }
th.highlight { color: var(--color-accent); }
tbody tr { border-top: 1px solid var(--color-border); }
tbody td { padding-top: 8px; padding-bottom: 8px; font-size: 15px; }
tr.leader { background: rgba(255, 253, 131, 0.06); }

.col-rank { width: 80px; padding-left: 24px; text-align: left; }
.col-team { text-align: left; overflow: hidden; text-overflow: ellipsis; }
.col-total { width: 104px; }
.col-booyah { width: 80px; }
.col-kills { width: 72px; }
.col-game { width: 56px; font-size: 14px; }
th:last-child, td:last-child { padding-right: 24px; width: 80px; }
.col-team .team-name { overflow: hidden; text-overflow: ellipsis; }

.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-2);
  font-size: 13px;
  font-weight: 600;
}
.rank.first { background: var(--color-accent); color: var(--color-on-accent); }

.team { display: inline-flex; align-items: center; gap: 10px; }
.team-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: var(--color-surface-2);
  color: var(--color-accent);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 10px;
}
.team-logo img { width: 100%; height: 100%; object-fit: cover; }
.team-name { font-weight: 600; }
.total { color: var(--color-accent); font-family: var(--font-heading); font-weight: 600; font-size: 18px; }
.muted { color: var(--color-muted); }
.note { color: var(--color-muted); font-size: 13px; }
</style>

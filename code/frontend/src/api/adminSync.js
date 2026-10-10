// โหลดข้อมูลจริงจาก backend มาใส่ใน array ของ src/mock/* แบบแทนที่ในที่เดิม
// ทำให้ฟังก์ชันอ่านข้อมูลใน src/api/admin.js และ src/mock/queries.js ใช้ข้อมูลจริงได้โดยไม่ต้องแก้
import { teams } from '@/mock/teams'
import { players } from '@/mock/players'
import { tournaments, tournamentTeams } from '@/mock/tournaments'
import { freeFireGames, matches, matchResults } from '@/mock/matches'
import { freeFireGameResults, tournamentPlacementPoints } from '@/mock/freeFire'
import { replaceRosters } from '@/mock/rosters'
import { getOrNull, http } from './http'

const PAGE_ALL = 'page=0&size=1000'

function refill(target, rows) {
  target.splice(0, target.length, ...rows)
}

// backend ใช้ COMPLETED ส่วนหน้าแอดมินใช้ FINISHED
const toAdminStatus = (status) => (status === 'COMPLETED' ? 'FINISHED' : status)

async function loadTournamentDetail(t) {
  const rosters = await http.get(`/tournaments/${t.id}/teams`)
  if (t.format === 'POINTS') {
    const [games, points] = await Promise.all([
      http.get(`/tournaments/${t.id}/free-fire-games`),
      http.get(`/tournaments/${t.id}/placement-points`),
    ])
    const done = games.filter((g) => g.status === 'COMPLETED')
    const results = await Promise.all(done.map((g) => http.get(`/free-fire-games/${g.id}/results`)))
    return { rosters, games, points, results, matches: [], matchResults: [] }
  }
  const rows = await http.get(`/tournaments/${t.id}/matches`)
  const done = rows.filter((m) => m.status === 'COMPLETED')
  const results = (await Promise.all(done.map((m) => getOrNull(`/matches/${m.id}/result`)))).filter(Boolean)
  return { rosters, games: [], points: [], results: [], matches: rows, matchResults: results }
}

let loading = null

export function loadAdminData() {
  // ถ้ามีการโหลดค้างอยู่ ใช้ promise เดิม ไม่ยิงซ้ำ
  loading ??= doLoad().finally(() => {
    loading = null
  })
  return loading
}

async function doLoad() {
  const [tournamentRows, teamPage, playerPage] = await Promise.all([
    http.get('/tournaments'),
    http.get(`/teams?${PAGE_ALL}`),
    http.get(`/players?${PAGE_ALL}`),
  ])
  const details = await Promise.all(tournamentRows.map(loadTournamentDetail))

  refill(tournaments, tournamentRows.map((t) => ({ ...t, status: toAdminStatus(t.status), createdAt: t.createdAt ?? '' })))
  refill(teams, teamPage.content)
  refill(players, playerPage.content)

  const regs = []
  const snapshots = []
  const allMatches = []
  const allMatchResults = []
  const allGames = []
  const allGameResults = []
  const allPoints = []
  let resultId = 1

  tournamentRows.forEach((t, i) => {
    const d = details[i]
    d.rosters.forEach((r, order) => {
      // API คืนตามลำดับที่สมัคร จึงใช้ลำดับนี้แทน joined_at
      regs.push({ tournamentId: t.id, teamId: r.teamId, joinedAt: String(order).padStart(4, '0') })
      snapshots.push({
        tournamentId: t.id,
        teamId: r.teamId,
        team: { id: r.teamId, name: r.teamName, description: r.teamDescription, logoUrl: r.teamLogoUrl, gameId: t.gameId },
        players: r.players.map((p) => ({ id: p.playerId, name: p.name, role: p.role, teamId: r.teamId })),
      })
    })
    allMatches.push(...d.matches)
    allMatchResults.push(...d.matchResults)
    allGames.push(...d.games.map((g) => ({ ...g, tournamentId: t.id })))
    d.results.forEach((game) => {
      game.results.forEach((row) => {
        allGameResults.push({ id: resultId++, gameId: game.gameId, tournamentId: t.id, teamId: row.teamId, placement: row.placement, kills: row.kills })
      })
    })
    allPoints.push(...d.points.map((p) => ({ tournamentId: t.id, placement: p.placement, points: p.points })))
  })

  refill(tournamentTeams, regs)
  refill(matches, allMatches)
  refill(matchResults, allMatchResults)
  refill(freeFireGames, allGames)
  refill(freeFireGameResults, allGameResults)
  refill(tournamentPlacementPoints, allPoints)
  replaceRosters(snapshots)
}

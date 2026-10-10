import { games } from './games'
import { tournaments, tournamentTeams } from './tournaments'
import { freeFireGames, matches, matchResults } from './matches'
import { freeFireGameResults, tournamentPlacementPoints } from './freeFire'
import { players } from './players'
import { teams } from './teams'
import { getRegisteredPlayers, getRegisteredTeam } from './rosters'

// วันที่วันนี้ตามเวลาเครื่อง (รูปแบบ YYYY-MM-DD)
export const TODAY = new Date().toLocaleDateString('sv-SE')

const byId = (list) => ({ get: (id) => list.find((x) => x.id === id) })
const gameById = byId(games)
const tournamentById = byId(tournaments)

function teamsOf(tournamentId) {
  return tournamentTeams
    .filter((tt) => tt.tournamentId === tournamentId)
    .sort((a, b) => a.joinedAt.localeCompare(b.joinedAt))
    .map((tt) => getRegisteredTeam(tt.tournamentId, tt.teamId))
}

function championOf(tournament) {
  if (tournament.status !== 'FINISHED') return null
  if (tournament.format === 'POINTS') {
    const top = getFreeFireStandings(tournament.id).standings[0]
    return top && top.totalPoints > 0 ? getRegisteredTeam(tournament.id, top.teamId) : null
  }
  const final = matches
    .filter((m) => m.tournamentId === tournament.id)
    .sort((a, b) => b.roundNumber - a.roundNumber)[0]
  const result = final && matchResults.find((r) => r.matchId === final.id)
  return result ? getRegisteredTeam(tournament.id, result.winnerTeamId) : null
}

function toTournamentView(t) {
  return {
    ...t,
    game: gameById.get(t.gameId),
    teams: teamsOf(t.id),
    champion: championOf(t),
  }
}

export function getGames() {
  return games.filter((g) => g.isActive)
}

export function getTournaments() {
  return tournaments.map(toTournamentView)
}

export function getLatestTournaments(limit = 4) {
  return [...tournaments]
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, limit)
    .map(toTournamentView)
}

export function getMatchesOn(date = TODAY) {
  return matches
    .filter((m) => m.scheduledAt?.startsWith(date))
    .sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt))
    .map((m) => ({
      ...m,
      tournament: tournamentById.get(m.tournamentId),
      teamCount: teamsOf(m.tournamentId).length,
      teamA: getRegisteredTeam(m.tournamentId, m.teamAId),
      teamB: getRegisteredTeam(m.tournamentId, m.teamBId),
    }))
}

export function getTournament(id) {
  const tournament = tournamentById.get(Number(id))
  if (!tournament) return null
  const view = toTournamentView(tournament)
  return {
    ...view,
    teams: view.teams.map((team) => ({
      ...team,
      playerCount: getRegisteredPlayers(id, team.id).length,
    })),
  }
}

export function getTournamentMatches(tournamentId) {
  const id = Number(tournamentId)
  const teamCount = teamsOf(id).length
  const sorted = matches
    .filter((m) => m.tournamentId === id)
    .sort((a, b) => a.roundNumber - b.roundNumber || a.matchNumber - b.matchNumber)
  const displayNumberOf = new Map(sorted.map((m, i) => [m.id, i + 1]))
  const feedersOf = (matchId) =>
    sorted.filter((m) => m.nextMatchId === matchId).map((m) => displayNumberOf.get(m.id))

  return sorted.map((m) => {
    const [feederA = null, feederB = null] = feedersOf(m.id)
    return {
      ...m,
      displayNumber: displayNumberOf.get(m.id),
      teamCount,
      teamA: getRegisteredTeam(id, m.teamAId),
      teamB: getRegisteredTeam(id, m.teamBId),
      result: matchResults.find((r) => r.matchId === m.id) ?? null,
      feederA,
      feederB,
    }
  })
}

export function getMatch(tournamentId, matchId) {
  const tournament = getTournament(tournamentId)
  if (!tournament || tournament.format !== 'SINGLE_ELIMINATION') return null
  const match = getTournamentMatches(tournament.id).find((m) => m.id === Number(matchId))
  if (!match) return null
  const playersOf = (team) => (team ? getRegisteredPlayers(tournament.id, team.id) : [])
  return {
    ...match,
    tournament,
    teamAPlayers: playersOf(match.teamA),
    teamBPlayers: playersOf(match.teamB),
  }
}

export function getPlayersOfTeam(teamId, tournamentId) {
  return tournamentId == null
    ? players.filter((p) => p.teamId === Number(teamId))
    : getRegisteredPlayers(Number(tournamentId), Number(teamId))
}

export function getPlacementPoints(tournamentId) {
  return tournamentPlacementPoints
    .filter((p) => p.tournamentId === Number(tournamentId))
    .sort((a, b) => a.placement - b.placement)
}

export function getFreeFireGames(tournamentId) {
  return freeFireGames
    .filter((g) => g.tournamentId === Number(tournamentId))
    .sort((a, b) => a.gameNumber - b.gameNumber)
}

export function getFreeFireStandings(tournamentId) {
  const id = Number(tournamentId)
  const tournament = tournamentById.get(id)
  const games = getFreeFireGames(id)
  const completed = games.filter((g) => g.status === 'COMPLETED')
  const pointsFor = new Map(getPlacementPoints(id).map((p) => [p.placement, p.points]))
  const perKill = tournament?.pointsPerKill ?? 1
  const lastGameId = completed.at(-1)?.id

  const rows = teamsOf(id).map((team) => {
    const pointsPerGame = []
    let totalPoints = 0
    let booyahs = 0
    let kills = 0
    let lastPlacement = Infinity
    for (let n = 1; n <= (tournament?.totalGames ?? games.length); n += 1) {
      const game = completed.find((g) => g.gameNumber === n)
      const result = game && freeFireGameResults.find((r) => r.gameId === game.id && r.teamId === team.id)
      if (!result) {
        pointsPerGame.push(null)
        continue
      }
      const points = (pointsFor.get(result.placement) ?? 0) + result.kills * perKill
      pointsPerGame.push(points)
      totalPoints += points
      kills += result.kills
      if (result.placement === 1) booyahs += 1
      if (game.id === lastGameId) lastPlacement = result.placement
    }
    return { team, teamId: team.id, teamName: team.name, totalPoints, booyahs, kills, pointsPerGame, lastPlacement }
  })

  rows.sort(
    (a, b) =>
      b.totalPoints - a.totalPoints ||
      b.booyahs - a.booyahs ||
      b.kills - a.kills ||
      a.lastPlacement - b.lastPlacement,
  )

  return {
    tournamentId: id,
    totalGames: tournament?.totalGames ?? games.length,
    gamesCompleted: completed.length,
    standings: rows.map(({ lastPlacement, ...row }, i) => ({ rank: i + 1, ...row })),
  }
}

export function getNextFreeFireGame(tournamentId) {
  return getFreeFireGames(tournamentId).find((g) => g.status !== 'COMPLETED') ?? null
}

function withBooyah(game) {
  const booyah = freeFireGameResults.find((r) => r.gameId === game.id && r.placement === 1)
  return { ...game, booyahTeam: booyah ? getRegisteredTeam(game.tournamentId, booyah.teamId) : null }
}

export function getRecentFreeFireGames(tournamentId, limit = 3) {
  return getFreeFireGames(tournamentId)
    .filter((g) => g.status === 'COMPLETED')
    .reverse()
    .slice(0, limit)
    .map(withBooyah)
}

export function getFreeFireSchedule(tournamentId) {
  return getFreeFireGames(tournamentId).map(withBooyah)
}

export function getFreeFireGameResult(tournamentId, gameNumber) {
  const id = Number(tournamentId)
  const game = getFreeFireGames(id).find((g) => g.gameNumber === Number(gameNumber))
  if (!game || game.status !== 'COMPLETED') return null
  const pointsFor = new Map(getPlacementPoints(id).map((p) => [p.placement, p.points]))
  const perKill = tournamentById.get(id)?.pointsPerKill ?? 1
  const rows = freeFireGameResults
    .filter((r) => r.gameId === game.id)
    .sort((a, b) => a.placement - b.placement)
    .map((r) => {
      const placementPoints = pointsFor.get(r.placement) ?? 0
      const killPoints = r.kills * perKill
      return {
        id: r.id,
        placement: r.placement,
        team: getRegisteredTeam(id, r.teamId),
        kills: r.kills,
        placementPoints,
        killPoints,
        total: placementPoints + killPoints,
      }
    })
  return { game, rows }
}

const isPlayedWithoutResult = (item) =>
  item.status !== 'COMPLETED' && item.scheduledAt && item.scheduledAt.slice(0, 10) < TODAY

export function getPendingResults() {
  const matchItems = matches
    .filter((m) => isPlayedWithoutResult(m) && m.teamAId && m.teamBId)
    .map((m) => ({
      kind: 'MATCH',
      key: `m-${m.id}`,
      id: m.id,
      scheduledAt: m.scheduledAt,
      tournament: tournamentById.get(m.tournamentId),
      roundNumber: m.roundNumber,
      teamCount: teamsOf(m.tournamentId).length,
      teamA: getRegisteredTeam(m.tournamentId, m.teamAId),
      teamB: getRegisteredTeam(m.tournamentId, m.teamBId),
    }))
  const gameItems = freeFireGames
    .filter(isPlayedWithoutResult)
    .map((g) => ({
      kind: 'FREE_FIRE_GAME',
      key: `g-${g.id}`,
      id: g.id,
      scheduledAt: g.scheduledAt,
      tournament: tournamentById.get(g.tournamentId),
      gameNumber: g.gameNumber,
      teamCount: teamsOf(g.tournamentId).length,
    }))
  return [...matchItems, ...gameItems].sort((a, b) => b.scheduledAt.localeCompare(a.scheduledAt))
}

function setupStatusOf(tournament) {
  if (tournament.format === 'POINTS') {
    return freeFireGames.some((g) => g.tournamentId === tournament.id) ? 'SCHEDULE_CREATED' : 'NO_SCHEDULE'
  }
  return matches.some((m) => m.tournamentId === tournament.id) ? 'BRACKET_CREATED' : 'NO_BRACKET'
}

export function getUpcomingTournaments(limit = 3) {
  return tournaments
    .filter((t) => t.status === 'UPCOMING')
    .sort((a, b) => a.startDate.localeCompare(b.startDate))
    .slice(0, limit)
    .map((t) => ({ ...toTournamentView(t), setupStatus: setupStatusOf(t) }))
}

export function getAdminOverview() {
  const countBy = (list, key) => list.reduce((acc, x) => acc.set(x[key], (acc.get(x[key]) ?? 0) + 1), new Map())
  const tournamentsByStatus = countBy(tournaments, 'status')
  const teamsByGame = countBy(teams, 'gameId')
  const todayMatches = getMatchesOn(TODAY)
  return {
    updatedAt: `${TODAY}T21:00:00`,
    tournaments: {
      total: tournaments.length,
      ongoing: tournamentsByStatus.get('ONGOING') ?? 0,
      upcoming: tournamentsByStatus.get('UPCOMING') ?? 0,
    },
    teams: {
      total: teams.length,
      byGame: games.map((g) => ({ game: g, count: teamsByGame.get(g.id) ?? 0 })),
    },
    pendingResults: getPendingResults().length,
    todayMatches: {
      total: todayMatches.length,
      waiting: todayMatches.filter((m) => m.status !== 'COMPLETED').length,
    },
  }
}

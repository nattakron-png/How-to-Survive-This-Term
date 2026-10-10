import { games } from '@/mock/games'

const gameById = new Map(games.map((game) => [game.id, game]))

async function get(path) {
  const response = await fetch(`/api/v1${path}`)
  if (!response.ok) throw new Error(`HTTP ${response.status}: ${path}`)
  return response.json()
}

export function gameFor(id) {
  return gameById.get(id) ?? { code: String(id), name: `เกม #${id}` }
}

export function tournamentView(tournament, teams = null) {
  return { ...tournament, game: gameFor(tournament.gameId), teams, champion: null }
}

function teamView(roster) {
  return {
    id: roster.teamId,
    name: roster.teamName,
    description: roster.teamDescription,
    logoUrl: roster.teamLogoUrl,
    playerCount: roster.players.length,
    players: roster.players.map((player) => ({ ...player, id: player.playerId })),
  }
}

function matchViews(rows, tournament, teams, results) {
  const byTeam = new Map(teams.map((team) => [team.id, team]))
  const sorted = [...rows].sort((a, b) => a.roundNumber - b.roundNumber || a.matchNumber - b.matchNumber)
  const display = new Map(sorted.map((row, index) => [row.id, index + 1]))
  return sorted.map((row) => {
    const feeders = sorted.filter((candidate) => candidate.nextMatchId === row.id)
    return {
      ...row,
      displayNumber: display.get(row.id),
      teamCount: teams.length,
      teamA: row.teamAId ? byTeam.get(row.teamAId) ?? { id: row.teamAId, name: row.teamAName } : null,
      teamB: row.teamBId ? byTeam.get(row.teamBId) ?? { id: row.teamBId, name: row.teamBName } : null,
      result: results.get(row.id) ?? null,
      feederA: feeders[0] ? display.get(feeders[0].id) : null,
      feederB: feeders[1] ? display.get(feeders[1].id) : null,
      tournament,
    }
  })
}

export async function listPublicTournaments() {
  const rows = await get('/tournaments')
  return rows.map((row) => tournamentView(row))
}

export async function loadTournament(id) {
  const [raw, rosters] = await Promise.all([
    get(`/tournaments/${id}`),
    get(`/tournaments/${id}/teams`),
  ])
  const teams = rosters.map(teamView)
  const tournament = tournamentView(raw, teams)
  if (raw.format === 'POINTS') {
    const [rawStandings, rawGames, placementPoints] = await Promise.all([
      get(`/tournaments/${id}/standings`),
      get(`/tournaments/${id}/free-fire-games`),
      get(`/tournaments/${id}/placement-points`),
    ])
    const byTeam = new Map(teams.map((team) => [team.id, team]))
    const standings = {
      ...rawStandings,
      standings: rawStandings.standings.map((row) => ({
        ...row,
        team: byTeam.get(row.teamId) ?? { id: row.teamId, name: row.teamName },
        teamName: byTeam.get(row.teamId)?.name ?? row.teamName,
      })),
    }
    const schedule = rawGames.map((game) => ({
      ...game,
      booyahTeam: game.booyahTeamId
        ? byTeam.get(game.booyahTeamId) ?? { id: game.booyahTeamId, name: game.booyahTeamName }
        : null,
    }))
    const completed = schedule.filter((game) => game.status === 'COMPLETED')
    if (raw.status === 'COMPLETED' && completed.length && standings.standings.length) {
      tournament.champion = standings.standings[0].team
    }
    return { tournament, matches: [], standings, schedule, placementPoints }
  }

  const rows = await get(`/tournaments/${id}/matches`)
  const resultPairs = await Promise.all(rows.filter((row) => row.status === 'COMPLETED')
    .map(async (row) => {
      try { return [row.id, await get(`/matches/${row.id}/result`)] }
      catch (error) { if (error.message.startsWith('HTTP 404:')) return [row.id, null]; throw error }
    }))
  const results = new Map(resultPairs)
  const matches = matchViews(rows, tournament, teams, results)
  if (raw.status === 'COMPLETED') {
    const final = [...matches].sort((a, b) => b.roundNumber - a.roundNumber)[0]
    tournament.champion = teams.find((team) => team.id === final?.result?.winnerTeamId) ?? null
  }
  return { tournament, matches, standings: null, schedule: [], placementPoints: [] }
}

export async function loadFreeFireGame(game, teams) {
  const result = await get(`/free-fire-games/${game.id}/results`)
  const byTeam = new Map(teams.map((team) => [team.id, team]))
  return {
    game,
    rows: result.results.map((row) => ({
      id: row.teamId,
      ...row,
      team: byTeam.get(row.teamId) ?? { id: row.teamId, name: row.teamName },
      total: row.totalPoints,
    })),
  }
}

export async function loadTodayMatches(date) {
  const tournaments = await listPublicTournaments()
  const brackets = tournaments.filter((tournament) => tournament.format === 'SINGLE_ELIMINATION')
  const lists = await Promise.all(brackets.map(async (tournament) => {
    const rows = await get(`/tournaments/${tournament.id}/matches`)
    return rows.filter((row) => row.scheduledAt?.startsWith(date)).map((row) => ({
      ...row,
      tournament,
      teamCount: 0,
      teamA: row.teamAId ? { id: row.teamAId, name: row.teamAName } : null,
      teamB: row.teamBId ? { id: row.teamBId, name: row.teamBName } : null,
    }))
  }))
  return lists.flat().sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt))
}

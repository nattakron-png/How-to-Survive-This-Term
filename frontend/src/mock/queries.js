import { games } from './games'
import { teams } from './teams'
import { tournaments, tournamentTeams } from './tournaments'
import { matches, matchResults } from './matches'
import { freeFireStandings } from './freeFireStandings'

export const TODAY = '2026-10-16'

const byId = (list) => new Map(list.map((x) => [x.id, x]))
const gameById = byId(games)
const teamById = byId(teams)
const tournamentById = byId(tournaments)

function teamsOf(tournamentId) {
  return tournamentTeams
    .filter((tt) => tt.tournamentId === tournamentId)
    .sort((a, b) => a.joinedAt.localeCompare(b.joinedAt))
    .map((tt) => teamById.get(tt.teamId))
}

function championOf(tournament) {
  if (tournament.status !== 'FINISHED') return null
  if (tournament.format === 'POINTS') {
    const top = freeFireStandings.find((s) => s.tournamentId === tournament.id)?.standings[0]
    return top ? teamById.get(top.teamId) : null
  }
  const final = matches
    .filter((m) => m.tournamentId === tournament.id)
    .sort((a, b) => b.roundNumber - a.roundNumber)[0]
  const result = final && matchResults.find((r) => r.matchId === final.id)
  return result ? teamById.get(result.winnerTeamId) : null
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
      teamA: teamById.get(m.teamAId),
      teamB: teamById.get(m.teamBId),
    }))
}

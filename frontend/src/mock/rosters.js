import { players } from './players.js'
import { teams } from './teams.js'
import { tournamentTeams } from './tournaments.js'

const key = (tournamentId, teamId) => `${tournamentId}/${teamId}`
const snapshots = new Map()

export function captureRoster(tournamentId, teamId) {
  const team = teams.find((item) => item.id === Number(teamId))
  snapshots.set(key(tournamentId, teamId), {
    team: team ? { ...team } : null,
    players: players.filter((player) => player.teamId === Number(teamId)).map((player) => ({ ...player })),
  })
}

export function removeRoster(tournamentId, teamId) {
  snapshots.delete(key(tournamentId, teamId))
}

export function getRegisteredTeam(tournamentId, teamId) {
  return snapshots.get(key(tournamentId, teamId))?.team ?? null
}

export function getRegisteredPlayers(tournamentId, teamId) {
  return snapshots.get(key(tournamentId, teamId))?.players ?? []
}

for (const registration of tournamentTeams) {
  captureRoster(registration.tournamentId, registration.teamId)
}

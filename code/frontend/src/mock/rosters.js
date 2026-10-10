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

// ใช้ตอนโหลดข้อมูลจริงจาก backend (snapshot ทีมและผู้เล่น ณ วันที่สมัคร)
export function replaceRosters(entries) {
  snapshots.clear()
  for (const entry of entries) {
    snapshots.set(key(entry.tournamentId, entry.teamId), { team: entry.team, players: entry.players })
  }
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

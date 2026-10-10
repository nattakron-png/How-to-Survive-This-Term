import { games } from '@/mock/games'

const gameById = new Map(games.map((game) => [game.id, game]))

export async function listTournaments() {
  const response = await fetch('/api/v1/tournaments')
  if (!response.ok) throw new Error(`HTTP ${response.status}`)

  const tournaments = await response.json()
  if (!Array.isArray(tournaments)) throw new Error('Invalid tournament response')

  return tournaments.map((tournament) => ({
    ...tournament,
    game: gameById.get(tournament.gameId) ?? { code: String(tournament.gameId), name: `เกม #${tournament.gameId}` },
    teams: null,
    champion: null,
  }))
}

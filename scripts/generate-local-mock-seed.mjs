import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

import { games } from '../frontend/src/mock/games.js'
import { teams } from '../frontend/src/mock/teams.js'
import { players } from '../frontend/src/mock/players.js'
import { tournaments, tournamentTeams } from '../frontend/src/mock/tournaments.js'
import { matches, matchResults, freeFireGames } from '../frontend/src/mock/matches.js'
import { freeFireGameResults, tournamentPlacementPoints } from '../frontend/src/mock/freeFire.js'

// One-off local fixture. Never include frontend/src/mock/users.js: its login is only a UI mock.
const tournamentTeamRosters = tournamentTeams.flatMap((registration) =>
  players.filter((player) => player.teamId === registration.teamId).map((player) => ({
    tournamentId: registration.tournamentId,
    teamId: registration.teamId,
    playerId: player.id,
    playerName: player.name,
    playerRole: player.role,
  })))
const fixture = {
  teams,
  players,
  tournaments,
  tournament_teams: tournamentTeams,
  tournament_team_rosters: tournamentTeamRosters,
  matches,
  match_results: matchResults,
  free_fire_games: freeFireGames,
  free_fire_game_results: freeFireGameResults,
  tournament_placement_points: tournamentPlacementPoints,
}

function indexById(rows, label) {
  const result = new Map()
  for (const row of rows) {
    assert.ok(Number.isSafeInteger(row.id) && row.id > 0, `${label}: invalid id`)
    assert.ok(!result.has(row.id), `${label}: duplicate id ${row.id}`)
    result.set(row.id, row)
  }
  return result
}

const gameById = indexById(games, 'games')
const teamById = indexById(teams, 'teams')
const tournamentById = indexById(tournaments, 'tournaments')
const matchById = indexById(matches, 'matches')
const freeFireGameById = indexById(freeFireGames, 'free_fire_games')
for (const [label, rows] of Object.entries(fixture)) {
  if (rows[0] && 'id' in rows[0]) indexById(rows, label)
}

const participantKeys = new Set()
for (const row of tournamentTeams) {
  const tournament = tournamentById.get(row.tournamentId)
  const team = teamById.get(row.teamId)
  assert.ok(tournament && team, `tournament_teams: missing reference ${row.tournamentId}/${row.teamId}`)
  assert.equal(team.gameId, tournament.gameId, `tournament_teams: wrong game ${row.tournamentId}/${row.teamId}`)
  const key = `${row.tournamentId}/${row.teamId}`
  assert.ok(!participantKeys.has(key), `tournament_teams: duplicate ${key}`)
  participantKeys.add(key)
}
for (const row of players) {
  assert.ok(row.teamId == null || teamById.has(row.teamId), `players: missing team ${row.teamId}`)
}
for (const row of tournaments) {
  assert.ok(gameById.has(row.gameId), `tournaments: missing game ${row.gameId}`)
  assert.equal(row.format === 'POINTS', row.totalGames != null, `tournaments: invalid totalGames ${row.id}`)
}
const matchSlots = new Set()
for (const row of matches) {
  assert.ok(tournamentById.has(row.tournamentId), `matches: missing tournament ${row.tournamentId}`)
  for (const teamId of [row.teamAId, row.teamBId]) {
    assert.ok(teamId == null || participantKeys.has(`${row.tournamentId}/${teamId}`),
      `matches: team ${teamId} not registered in tournament ${row.tournamentId}`)
  }
  assert.ok(row.nextMatchId == null || matchById.get(row.nextMatchId)?.tournamentId === row.tournamentId,
    `matches: invalid nextMatchId ${row.id}`)
  const slot = `${row.tournamentId}/${row.roundNumber}/${row.matchNumber}`
  assert.ok(!matchSlots.has(slot), `matches: duplicate slot ${slot}`)
  matchSlots.add(slot)
}
for (const row of matchResults) {
  const match = matchById.get(row.matchId)
  assert.ok(match, `match_results: missing match ${row.matchId}`)
  assert.ok([match.teamAId, match.teamBId].includes(row.winnerTeamId),
    `match_results: winner ${row.winnerTeamId} not in match ${row.matchId}`)
}
const freeFireGameKeys = new Set()
for (const row of freeFireGames) {
  assert.equal(tournamentById.get(row.tournamentId)?.format, 'POINTS',
    `free_fire_games: invalid tournament ${row.tournamentId}`)
  const key = `${row.tournamentId}/${row.gameNumber}`
  assert.ok(!freeFireGameKeys.has(key), `free_fire_games: duplicate ${key}`)
  freeFireGameKeys.add(key)
}
for (const row of freeFireGameResults) {
  assert.equal(freeFireGameById.get(row.gameId)?.tournamentId, row.tournamentId,
    `free_fire_game_results: invalid game/tournament ${row.id}`)
  assert.ok(participantKeys.has(`${row.tournamentId}/${row.teamId}`),
    `free_fire_game_results: team ${row.teamId} not registered in tournament ${row.tournamentId}`)
}
for (const row of tournamentPlacementPoints) {
  assert.equal(tournamentById.get(row.tournamentId)?.format, 'POINTS',
    `tournament_placement_points: invalid tournament ${row.tournamentId}`)
}

const counts = Object.fromEntries(Object.entries(fixture).map(([table, rows]) => [table, rows.length]))
if (!process.argv[2]) {
  console.log(JSON.stringify(counts, null, 2))
  process.exit(0)
}

function value(input) {
  if (input === null || input === undefined) return 'NULL'
  if (typeof input === 'number') {
    assert.ok(Number.isSafeInteger(input), `Unsafe SQL number: ${input}`)
    return String(input)
  }
  if (typeof input === 'boolean') return input ? 'TRUE' : 'FALSE'
  assert.equal(typeof input, 'string')
  return `'${input.replaceAll("'", "''")}'`
}

function insert(table, columns, rows, fields) {
  if (!rows.length) return ''
  const tuples = rows.map((row) => `  (${fields.map((field) => value(row[field])).join(', ')})`)
  return `INSERT INTO ${table} (${columns.join(', ')}) VALUES\n${tuples.join(',\n')};\n`
}

const sql = [
  '-- Local demo data generated from frontend/src/mock. Do not run on Railway or commit as a Flyway migration.',
  '-- All inserts happen in one transaction. A second run is a no-op; partial or unrelated data aborts.',
  'BEGIN;',
  'DO $seed$',
  'BEGIN',
  "  PERFORM pg_advisory_xact_lock(6733802909);",
  `  IF (SELECT count(*) FROM tournaments WHERE id = ${tournaments[0].id} AND name = ${value(tournaments[0].name)}) = 1 THEN`,
  `    IF ${Object.entries(counts).map(([table, count]) => `(SELECT count(*) FROM ${table}) = ${count}`).join('\n      AND ')} THEN`,
  "      RAISE NOTICE 'Mock data already present; nothing inserted';",
  '      RETURN;',
  '    END IF;',
  "    RAISE EXCEPTION 'Mock data appears incomplete or has changed; refusing to seed';",
  '  END IF;',
  `  IF ${Object.keys(counts).map((table) => `EXISTS (SELECT 1 FROM ${table})`).join('\n      OR ')} THEN`,
  "    RAISE EXCEPTION 'Target tables are not empty; refusing to seed over existing data';",
  '  END IF;',
  `  IF EXISTS (SELECT 1 FROM (VALUES ${games.map((game) => `(${value(game.id)}, ${value(game.code)})`).join(', ')}) AS expected(id, code)`,
  '             LEFT JOIN games ON games.id = expected.id AND games.code = expected.code',
  '             WHERE games.id IS NULL) THEN',
  "    RAISE EXCEPTION 'Flyway game IDs differ from mock data';",
  '  END IF;',
  insert('teams', ['id', 'name', 'description', 'game_id', 'logo_url', 'created_at'], teams,
    ['id', 'name', 'description', 'gameId', 'logoUrl', 'createdAt']),
  insert('players', ['id', 'name', 'role', 'description', 'team_id'], players,
    ['id', 'name', 'role', 'description', 'teamId']),
  insert('tournaments', ['id', 'name', 'description', 'start_date', 'end_date', 'status', 'created_at', 'game_id', 'format', 'logo_url', 'total_games', 'points_per_kill'],
    tournaments.map((row) => ({ ...row, status: row.status === 'FINISHED' ? 'COMPLETED' : row.status })),
    ['id', 'name', 'description', 'startDate', 'endDate', 'status', 'createdAt', 'gameId', 'format', 'logoUrl', 'totalGames', 'pointsPerKill']),
  insert('tournament_teams', ['tournament_id', 'team_id', 'joined_at', 'team_name', 'team_description', 'team_logo_url'],
    tournamentTeams.map((row) => ({
      ...row,
      teamName: teamById.get(row.teamId).name,
      teamDescription: teamById.get(row.teamId).description,
      teamLogoUrl: teamById.get(row.teamId).logoUrl,
    })),
    ['tournamentId', 'teamId', 'joinedAt', 'teamName', 'teamDescription', 'teamLogoUrl']),
  insert('tournament_team_rosters', ['tournament_id', 'team_id', 'player_id', 'player_name', 'player_role'],
    tournamentTeamRosters, ['tournamentId', 'teamId', 'playerId', 'playerName', 'playerRole']),
  insert('matches', ['id', 'tournament_id', 'team_a_id', 'team_b_id', 'round_number', 'match_number', 'scheduled_at', 'status'], matches,
    ['id', 'tournamentId', 'teamAId', 'teamBId', 'roundNumber', 'matchNumber', 'scheduledAt', 'status']),
  ...matches.filter((row) => row.nextMatchId != null)
    .map((row) => `UPDATE matches SET next_match_id = ${value(row.nextMatchId)} WHERE id = ${value(row.id)};`),
  insert('match_results', ['id', 'match_id', 'team_a_score', 'team_b_score', 'winner_team_id'], matchResults,
    ['id', 'matchId', 'teamAScore', 'teamBScore', 'winnerTeamId']),
  insert('tournament_placement_points', ['tournament_id', 'placement', 'points'], tournamentPlacementPoints,
    ['tournamentId', 'placement', 'points']),
  insert('free_fire_games', ['id', 'tournament_id', 'game_number', 'scheduled_at', 'status'], freeFireGames,
    ['id', 'tournamentId', 'gameNumber', 'scheduledAt', 'status']),
  insert('free_fire_game_results', ['id', 'game_id', 'tournament_id', 'team_id', 'placement', 'kills'], freeFireGameResults,
    ['id', 'gameId', 'tournamentId', 'teamId', 'placement', 'kills']),
  ...['teams', 'players', 'tournaments', 'matches', 'match_results', 'free_fire_games', 'free_fire_game_results']
    .map((table) => `  PERFORM setval(pg_get_serial_sequence('${table}', 'id'), (SELECT max(id) FROM ${table}), true);`),
  'END',
  '$seed$;',
  process.argv.includes('--rollback') ? 'ROLLBACK;' : 'COMMIT;',
  '',
].join('\n')

const output = resolve(process.argv[2])
mkdirSync(dirname(output), { recursive: true })
writeFileSync(output, sql, 'utf8')
console.log(`Wrote ${output}; counts: ${JSON.stringify(counts)}`)

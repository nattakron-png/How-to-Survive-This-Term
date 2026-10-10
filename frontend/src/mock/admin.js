import { games } from './games'
import { teams } from './teams'
import { players } from './players'
import { tournaments, tournamentTeams } from './tournaments'
import { freeFireGames, matches, matchResults } from './matches'
import { freeFireGameResults, tournamentPlacementPoints } from './freeFire'
import { TODAY } from './queries'

export const DEFAULT_PLACEMENT_POINTS = [12, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0]
export const MAX_POINTS_TEAMS = DEFAULT_PLACEMENT_POINTS.length

const delay = (ms = 300) => new Promise((resolve) => setTimeout(resolve, ms))
const nextId = (list) => list.reduce((max, x) => Math.max(max, x.id), 0) + 1
const removeWhere = (list, predicate) => {
  for (let i = list.length - 1; i >= 0; i -= 1) if (predicate(list[i])) list.splice(i, 1)
}
const findTournament = (id) => tournaments.find((t) => t.id === Number(id))
const gameOf = (gameId) => games.find((g) => g.id === Number(gameId))
const playerCountOf = (teamId) => players.filter((p) => p.teamId === teamId).length
const overlaps = (a, b) => a.startDate <= b.endDate && b.startDate <= a.endDate

export function formatForGame(gameId) {
  return gameOf(gameId)?.code === 'FREE_FIRE' ? 'POINTS' : 'SINGLE_ELIMINATION'
}

function setupOf(tournament) {
  if (tournament.format === 'POINTS') {
    const list = freeFireGames.filter((g) => g.tournamentId === tournament.id)
    return { created: list.length > 0, hasResults: list.some((g) => g.status === 'COMPLETED') }
  }
  const list = matches.filter((m) => m.tournamentId === tournament.id)
  return {
    created: list.length > 0,
    hasResults: list.some((m) => matchResults.some((r) => r.matchId === m.id)),
  }
}

function isComplete(tournament) {
  if (tournament.format === 'POINTS') {
    const list = freeFireGames.filter((g) => g.tournamentId === tournament.id)
    return list.length === tournament.totalGames && list.every((g) => g.status === 'COMPLETED')
  }
  const final = matches
    .filter((m) => m.tournamentId === tournament.id)
    .sort((a, b) => b.roundNumber - a.roundNumber)[0]
  return Boolean(final && matchResults.some((r) => r.matchId === final.id))
}

function teamIdsOf(tournamentId) {
  return tournamentTeams
    .filter((tt) => tt.tournamentId === tournamentId)
    .sort((a, b) => a.joinedAt.localeCompare(b.joinedAt))
    .map((tt) => tt.teamId)
}

export function getAdminTournaments() {
  return tournaments.map((t) => ({
    ...t,
    game: gameOf(t.gameId),
    teamCount: teamIdsOf(t.id).length,
    canDelete: t.status === 'UPCOMING',
  }))
}

export function getTournamentForm(id) {
  const t = findTournament(id)
  if (!t) return null
  const points = tournamentPlacementPoints
    .filter((p) => p.tournamentId === t.id)
    .sort((a, b) => a.placement - b.placement)
    .map((p) => p.points)
  const setup = setupOf(t)
  return {
    ...t,
    placementPoints: points.length ? points : [...DEFAULT_PLACEMENT_POINTS],
    gameLocked: teamIdsOf(t.id).length > 0,
    pointsLocked: t.format === 'POINTS' && setup.created,
    setupCreated: setup.created,
    complete: isComplete(t),
  }
}

function validate(payload, currentId = null) {
  const errors = {}
  const name = payload.name?.trim() ?? ''
  if (!name) errors.name = 'กรุณากรอกชื่อรายการ'
  else if (tournaments.some((t) => t.id !== currentId && t.name.toLowerCase() === name.toLowerCase())) {
    errors.name = 'มีรายการชื่อนี้อยู่แล้ว'
  }
  if (!gameOf(payload.gameId)) errors.gameId = 'กรุณาเลือกเกม'
  if (!payload.startDate) errors.startDate = 'กรุณาเลือกวันเริ่มแข่ง'
  if (!payload.endDate) errors.endDate = 'กรุณาเลือกวันจบ'
  else if (payload.startDate && payload.endDate < payload.startDate) errors.endDate = 'วันจบต้องไม่ก่อนวันเริ่มแข่ง'
  if (formatForGame(payload.gameId) === 'POINTS') {
    if (![10, 15].includes(Number(payload.totalGames))) errors.totalGames = 'เลือกจำนวนเกม'
    if (!Number.isInteger(Number(payload.pointsPerKill)) || Number(payload.pointsPerKill) < 0) {
      errors.pointsPerKill = 'ต้องเป็นจำนวนเต็มตั้งแต่ 0'
    }
    if (payload.placementPoints.some((p) => !Number.isInteger(Number(p)) || Number(p) < 0)) {
      errors.placementPoints = 'คะแนนแต่ละอันดับต้องเป็นจำนวนเต็มตั้งแต่ 0'
    }
  }
  return errors
}

class ValidationError extends Error {
  constructor(fields) {
    super('VALIDATION_FAILED')
    this.fields = fields
  }
}

function writePlacementPoints(tournamentId, points) {
  removeWhere(tournamentPlacementPoints, (p) => p.tournamentId === tournamentId)
  points.forEach((value, i) => {
    tournamentPlacementPoints.push({ tournamentId, placement: i + 1, points: Number(value) })
  })
}

function toRow(payload) {
  const format = formatForGame(payload.gameId)
  return {
    name: payload.name.trim(),
    description: payload.description?.trim() || null,
    startDate: payload.startDate,
    endDate: payload.endDate,
    gameId: Number(payload.gameId),
    format,
    totalGames: format === 'POINTS' ? Number(payload.totalGames) : null,
    pointsPerKill: format === 'POINTS' ? Number(payload.pointsPerKill) : 1,
    logoUrl: payload.logoUrl ?? null,
  }
}

export async function createTournament(payload) {
  await delay()
  const errors = validate(payload)
  if (Object.keys(errors).length) throw new ValidationError(errors)
  const row = { id: nextId(tournaments), ...toRow(payload), status: 'UPCOMING', createdAt: `${TODAY}T21:00:00` }
  tournaments.push(row)
  if (row.format === 'POINTS') writePlacementPoints(row.id, payload.placementPoints)
  return row
}

export async function updateTournament(id, payload) {
  await delay()
  const t = findTournament(id)
  if (!t) throw new Error('NOT_FOUND')
  const form = getTournamentForm(id)
  const safePayload = {
    ...payload,
    gameId: form.gameLocked ? t.gameId : payload.gameId,
    totalGames: form.pointsLocked ? t.totalGames : payload.totalGames,
    pointsPerKill: form.pointsLocked ? t.pointsPerKill : payload.pointsPerKill,
    placementPoints: form.pointsLocked ? form.placementPoints : payload.placementPoints,
  }
  const errors = validate(safePayload, t.id)
  if (Object.keys(errors).length) throw new ValidationError(errors)
  Object.assign(t, toRow(safePayload))
  if (t.format === 'POINTS') writePlacementPoints(t.id, safePayload.placementPoints)
  else removeWhere(tournamentPlacementPoints, (p) => p.tournamentId === t.id)
  return t
}

export async function updateTournamentStatus(id, status) {
  await delay()
  const t = findTournament(id)
  if (!t) throw new Error('NOT_FOUND')
  if (status === 'ONGOING' && !setupOf(t).created) throw new Error('SETUP_REQUIRED')
  if (status === 'FINISHED' && !isComplete(t)) throw new Error('NOT_COMPLETE')
  t.status = status
  return t
}

export async function deleteTournament(id) {
  await delay()
  const t = findTournament(id)
  if (!t || t.status !== 'UPCOMING') throw new Error('CANNOT_DELETE')
  const matchIds = new Set(matches.filter((m) => m.tournamentId === t.id).map((m) => m.id))
  removeWhere(matchResults, (r) => matchIds.has(r.matchId))
  removeWhere(matches, (m) => m.tournamentId === t.id)
  removeWhere(freeFireGameResults, (r) => r.tournamentId === t.id)
  removeWhere(freeFireGames, (g) => g.tournamentId === t.id)
  removeWhere(tournamentPlacementPoints, (p) => p.tournamentId === t.id)
  removeWhere(tournamentTeams, (tt) => tt.tournamentId === t.id)
  removeWhere(tournaments, (x) => x.id === t.id)
}

function clashOf(team, tournament) {
  const otherIds = tournamentTeams
    .filter((tt) => tt.teamId === team.id && tt.tournamentId !== tournament.id)
    .map((tt) => tt.tournamentId)
  return tournaments.find((t) => otherIds.includes(t.id) && overlaps(t, tournament)) ?? null
}

export function getTournamentTeamsAdmin(id) {
  const t = findTournament(id)
  if (!t) return null
  const game = gameOf(t.gameId)
  const setup = setupOf(t)
  const list = teamIdsOf(t.id).map((teamId) => {
    const team = teams.find((x) => x.id === teamId)
    return { ...team, playerCount: playerCountOf(teamId), clash: clashOf(team, t) }
  })
  return {
    tournament: { ...t, game },
    teams: list,
    setup,
    locked: setup.created || t.status !== 'UPCOMING',
    games: t.format === 'POINTS'
      ? freeFireGames.filter((g) => g.tournamentId === t.id).sort((a, b) => a.gameNumber - b.gameNumber)
      : [],
  }
}

export function searchTeamsForTournament(id, query) {
  const t = findTournament(id)
  const q = query.trim().toLowerCase()
  if (!t || !q) return []
  const joined = new Set(teamIdsOf(t.id))
  const full = t.format === 'POINTS' && joined.size >= MAX_POINTS_TEAMS
  return teams
    .filter((team) => team.gameId === t.gameId && team.name.toLowerCase().includes(q))
    .slice(0, 6)
    .map((team) => {
      const clash = clashOf(team, t)
      let reason = null
      if (joined.has(team.id)) reason = 'อยู่ในรายการนี้แล้ว'
      else if (clash) reason = { clash }
      else if (full) reason = `รายการเต็มแล้ว (${MAX_POINTS_TEAMS} ทีม)`
      return { team, playerCount: playerCountOf(team.id), reason }
    })
}

function assertEditable(t) {
  if (!t) throw new Error('NOT_FOUND')
  if (setupOf(t).created || t.status !== 'UPCOMING') throw new Error('TEAMS_LOCKED')
}

export async function addTeamToTournament(id, teamId) {
  await delay(200)
  const t = findTournament(id)
  assertEditable(t)
  const team = teams.find((x) => x.id === Number(teamId))
  if (!team || team.gameId !== t.gameId) throw new Error('WRONG_GAME')
  if (tournamentTeams.some((tt) => tt.tournamentId === t.id && tt.teamId === team.id)) throw new Error('ALREADY_JOINED')
  if (clashOf(team, t)) throw new Error('DATE_CLASH')
  if (t.format === 'POINTS' && teamIdsOf(t.id).length >= MAX_POINTS_TEAMS) throw new Error('FULL')
  const stamp = new Date(Date.parse(`${TODAY}T21:00:00Z`) + tournamentTeams.length * 1000).toISOString().slice(0, 19)
  tournamentTeams.push({ tournamentId: t.id, teamId: team.id, joinedAt: stamp })
}

export async function removeTeamFromTournament(id, teamId) {
  await delay(200)
  const t = findTournament(id)
  assertEditable(t)
  removeWhere(tournamentTeams, (tt) => tt.tournamentId === t.id && tt.teamId === Number(teamId))
}

export function bracketSizeFor(teamCount) {
  return 2 ** Math.ceil(Math.log2(Math.max(teamCount, 2)))
}

function seedPositions(size) {
  let order = [1]
  while (order.length < size) {
    const n = order.length * 2
    order = order.flatMap((s) => [s, n + 1 - s])
  }
  return order
}

export async function generateBracket(id, orderedTeamIds) {
  await delay()
  const t = findTournament(id)
  assertEditable(t)
  if (t.format !== 'SINGLE_ELIMINATION') throw new Error('WRONG_FORMAT')
  const joined = new Set(teamIdsOf(t.id))
  const seeds = orderedTeamIds.map(Number).filter((x) => joined.has(x))
  if (seeds.length !== joined.size || seeds.length < 2) throw new Error('INVALID_SEEDS')

  const size = bracketSizeFor(seeds.length)
  const rounds = Math.log2(size)
  let id0 = nextId(matches)
  const byRound = []
  for (let r = 1; r <= rounds; r += 1) {
    const count = size / 2 ** r
    byRound.push(
      Array.from({ length: count }, (_, i) => ({
        id: id0++,
        tournamentId: t.id,
        teamAId: null,
        teamBId: null,
        roundNumber: r,
        matchNumber: i + 1,
        nextMatchId: null,
        scheduledAt: null,
        status: 'PENDING',
      })),
    )
  }
  byRound.forEach((round, r) => {
    round.forEach((m, i) => {
      const next = byRound[r + 1]?.[Math.floor(i / 2)]
      if (next) m.nextMatchId = next.id
    })
  })

  const positions = seedPositions(size)
  byRound[0].forEach((m, i) => {
    m.teamAId = seeds[positions[i * 2] - 1] ?? null
    m.teamBId = seeds[positions[i * 2 + 1] - 1] ?? null
    if (m.teamAId && m.teamBId) {
      m.status = 'SCHEDULED'
      return
    }
    m.status = 'COMPLETED'
    const next = byRound[1]?.[Math.floor(i / 2)]
    if (!next) return
    if (i % 2 === 0) next.teamAId = m.teamAId ?? m.teamBId
    else next.teamBId = m.teamAId ?? m.teamBId
  })
  byRound[1]?.forEach((m) => {
    if (m.teamAId && m.teamBId) m.status = 'SCHEDULED'
  })

  matches.push(...byRound.flat())
}

export async function resetBracket(id) {
  await delay()
  const t = findTournament(id)
  if (!t || setupOf(t).hasResults) throw new Error('HAS_RESULTS')
  removeWhere(matches, (m) => m.tournamentId === t.id)
}

export function draftFreeFireSchedule(tournament) {
  const start = new Date(`${tournament.startDate}T00:00:00Z`)
  return Array.from({ length: tournament.totalGames ?? 0 }, (_, i) => {
    const day = new Date(start.getTime() + Math.floor(i / 2) * 2 * 86400000).toISOString().slice(0, 10)
    return `${day}T${i % 2 === 0 ? '19:00' : '20:00'}:00`
  })
}

export async function generateFreeFireSchedule(id, schedule) {
  await delay()
  const t = findTournament(id)
  assertEditable(t)
  if (t.format !== 'POINTS' || schedule.length !== t.totalGames) throw new Error('INVALID_SCHEDULE')
  let gameId = nextId(freeFireGames)
  schedule.forEach((scheduledAt, i) => {
    freeFireGames.push({ id: gameId++, tournamentId: t.id, gameNumber: i + 1, scheduledAt, status: 'SCHEDULED' })
  })
  t.status = 'ONGOING'
}

export async function resetFreeFireSchedule(id) {
  await delay()
  const t = findTournament(id)
  if (!t || setupOf(t).hasResults) throw new Error('HAS_RESULTS')
  removeWhere(freeFireGames, (g) => g.tournamentId === t.id)
  t.status = 'UPCOMING'
}

export const ROLE_SUGGESTIONS = {
  ROV: ['Slayer', 'Jungle', 'Mid', 'Abyssal', 'Support', 'Substitute'],
  FREE_FIRE: ['Rusher', 'Sniper', 'Support', 'IGL', 'Substitute'],
  VALORANT: ['Duelist', 'Initiator', 'Controller', 'Sentinel', 'Flex', 'Substitute'],
  FIGHTING_GAME: ['Player', 'Substitute'],
}

const tournamentCountOf = (teamId) => tournamentTeams.filter((tt) => tt.teamId === teamId).length

export function getAdminTeams() {
  return teams.map((team) => {
    const tournamentCount = tournamentCountOf(team.id)
    return {
      ...team,
      game: gameOf(team.gameId),
      playerCount: playerCountOf(team.id),
      playerNames: players.filter((p) => p.teamId === team.id).map((p) => p.name),
      tournamentCount,
      canDelete: tournamentCount === 0,
    }
  })
}

export function getTeamForm(id) {
  const team = teams.find((t) => t.id === Number(id))
  if (!team) return null
  return {
    ...team,
    players: players.filter((p) => p.teamId === team.id).map((p) => ({ ...p })),
    gameLocked: tournamentCountOf(team.id) > 0,
  }
}

export function getFreePlayers() {
  return players.filter((p) => p.teamId == null).map((p) => ({ ...p }))
}

function validateTeam(payload, currentId) {
  const errors = {}
  const name = payload.name?.trim() ?? ''
  if (!name) errors.name = 'กรุณากรอกชื่อทีม'
  else if (name.length > 150) errors.name = 'ชื่อทีมยาวเกิน 150 ตัวอักษร'
  else if (teams.some((t) => t.id !== currentId && t.name.toLowerCase() === name.toLowerCase())) {
    errors.name = 'มีทีมชื่อนี้อยู่แล้ว'
  }
  if (!gameOf(payload.gameId)) errors.gameId = 'กรุณาเลือกเกม'
  const rows = payload.players ?? []
  if (rows.some((p) => !p.name?.trim() || !p.role?.trim())) errors.players = 'ผู้เล่นทุกคนต้องมีชื่อและตำแหน่ง'
  return errors
}

export async function saveTeam(id, payload) {
  await delay()
  const current = id == null ? null : teams.find((t) => t.id === Number(id))
  if (id != null && !current) throw new Error('NOT_FOUND')
  const gameLocked = current ? tournamentCountOf(current.id) > 0 : false
  const safe = { ...payload, gameId: gameLocked ? current.gameId : payload.gameId }
  const errors = validateTeam(safe, current?.id ?? null)
  if (Object.keys(errors).length) throw new ValidationError(errors)

  const fields = {
    name: safe.name.trim(),
    description: safe.description?.trim() || null,
    gameId: Number(safe.gameId),
    logoUrl: safe.logoUrl ?? null,
  }
  let team = current
  if (team) Object.assign(team, fields)
  else {
    team = { id: nextId(teams), ...fields, createdAt: `${TODAY}T21:00:00` }
    teams.push(team)
  }

  const keep = new Set()
  let playerId = nextId(players)
  for (const row of safe.players) {
    const data = { name: row.name.trim(), role: row.role.trim(), description: row.description?.trim() || null }
    const existing = row.id ? players.find((p) => p.id === row.id) : null
    if (existing) {
      Object.assign(existing, data, { teamId: team.id })
      keep.add(existing.id)
    } else {
      players.push({ id: playerId, ...data, teamId: team.id })
      keep.add(playerId)
      playerId += 1
    }
  }
  players.forEach((p) => {
    if (p.teamId === team.id && !keep.has(p.id)) p.teamId = null
  })
  return team
}

export async function deleteTeam(id) {
  await delay()
  const team = teams.find((t) => t.id === Number(id))
  if (!team || tournamentCountOf(team.id) > 0) throw new Error('CANNOT_DELETE')
  players.forEach((p) => {
    if (p.teamId === team.id) p.teamId = null
  })
  removeWhere(teams, (t) => t.id === team.id)
}

export function getAdminPlayers() {
  return players.map((p) => {
    const team = p.teamId == null ? null : teams.find((t) => t.id === p.teamId) ?? null
    return { ...p, team, game: team ? gameOf(team.gameId) : null }
  })
}

function validatePlayer(payload) {
  const errors = {}
  const name = payload.name?.trim() ?? ''
  const role = payload.role?.trim() ?? ''
  if (!name) errors.name = 'กรุณากรอกชื่อผู้เล่น'
  else if (name.length > 150) errors.name = 'ชื่อยาวเกิน 150 ตัวอักษร'
  if (!role) errors.role = 'กรุณากรอกตำแหน่ง'
  else if (role.length > 100) errors.role = 'ตำแหน่งยาวเกิน 100 ตัวอักษร'
  if (payload.teamId != null && !teams.some((t) => t.id === Number(payload.teamId))) errors.teamId = 'ไม่พบทีมนี้'
  return errors
}

export async function savePlayer(id, payload) {
  await delay()
  const errors = validatePlayer(payload)
  if (Object.keys(errors).length) throw new ValidationError(errors)
  const data = {
    name: payload.name.trim(),
    role: payload.role.trim(),
    description: payload.description?.trim() || null,
    teamId: payload.teamId == null ? null : Number(payload.teamId),
  }
  if (id == null) {
    const row = { id: nextId(players), ...data }
    players.push(row)
    return row
  }
  const player = players.find((p) => p.id === Number(id))
  if (!player) throw new Error('NOT_FOUND')
  Object.assign(player, data)
  return player
}

export async function deletePlayer(id) {
  await delay()
  removeWhere(players, (p) => p.id === Number(id))
}

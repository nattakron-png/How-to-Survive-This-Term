// ฟังก์ชันของหน้าผู้ดูแล เชื่อมกับ backend จริง
// - ฟังก์ชันอ่านข้อมูล (get...) อ่านจาก array ใน src/mock/* ซึ่ง loadAdminData() เติมข้อมูลจริงจาก API ไว้แล้ว
// - ฟังก์ชันแก้ข้อมูลตรวจกฎเบื้องต้นก่อน (ได้ข้อความภาษาไทยทันที) แล้วเรียก API และโหลดข้อมูลใหม่
import { games } from '@/mock/games'
import { teams } from '@/mock/teams'
import { players } from '@/mock/players'
import { tournaments, tournamentTeams } from '@/mock/tournaments'
import { freeFireGames, matches, matchResults } from '@/mock/matches'
import { freeFireGameResults, tournamentPlacementPoints } from '@/mock/freeFire'
import { TODAY } from '@/mock/queries'
import { ApiError, http } from './http'
import { loadAdminData } from './adminSync'

export const DEFAULT_PLACEMENT_POINTS = [12, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0]
export const MAX_POINTS_TEAMS = DEFAULT_PLACEMENT_POINTS.length

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

// แปลงข้อมูลฟอร์มเป็น TournamentRequest ของ backend
// โลโก้ที่เป็นไฟล์ (data URL) ยังไม่มี API อัปโหลดสำหรับรายการแข่ง จึงไม่ส่งไป
function toRequest(payload) {
  const row = toRow(payload)
  return { ...row, logoUrl: row.logoUrl?.startsWith('data:') ? null : row.logoUrl }
}

function tournamentFieldError(e) {
  if (e instanceof ApiError && e.status === 409 && /name/i.test(e.message)) {
    throw new ValidationError({ name: 'มีรายการชื่อนี้อยู่แล้ว' })
  }
  throw e
}

async function uploadTeamLogo(teamId, dataUrl) {
  const blob = await (await fetch(dataUrl)).blob()
  const form = new FormData()
  form.append('file', blob, blob.type === 'image/png' ? 'logo.png' : 'logo.jpg')
  await http.put(`/teams/${teamId}/logo`, form)
}

export async function createTournament(payload) {
  const errors = validate(payload)
  if (Object.keys(errors).length) throw new ValidationError(errors)
  const created = await http.post('/tournaments', toRequest(payload)).catch(tournamentFieldError)
  await loadAdminData()
  return created
}

export async function updateTournament(id, payload) {
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
  const updated = await http.put(`/tournaments/${t.id}`, toRequest(safePayload)).catch(tournamentFieldError)
  await loadAdminData()
  return updated
}

// backend เปลี่ยนสถานะให้เอง: สร้างสาย/ตารางเกม → ONGOING, กรอกผลนัดชิงหรือเกมสุดท้าย → COMPLETED
// ฟังก์ชันนี้จึงแค่โหลดข้อมูลใหม่แล้วเช็กว่าสถานะเป็นตามที่ต้องการแล้วหรือยัง
export async function updateTournamentStatus(id, status) {
  await loadAdminData()
  const t = findTournament(id)
  if (!t) throw new Error('NOT_FOUND')
  if (t.status === status) return t
  throw new Error(status === 'ONGOING' ? 'SETUP_REQUIRED' : 'NOT_COMPLETE')
}

export async function deleteTournament(id) {
  const t = findTournament(id)
  if (!t || t.status !== 'UPCOMING') throw new Error('CANNOT_DELETE')
  await http.del(`/tournaments/${t.id}`)
  await loadAdminData()
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
  const t = findTournament(id)
  assertEditable(t)
  const team = teams.find((x) => x.id === Number(teamId))
  if (!team || team.gameId !== t.gameId) throw new Error('WRONG_GAME')
  if (tournamentTeams.some((tt) => tt.tournamentId === t.id && tt.teamId === team.id)) throw new Error('ALREADY_JOINED')
  if (clashOf(team, t)) throw new Error('DATE_CLASH')
  if (t.format === 'POINTS' && teamIdsOf(t.id).length >= MAX_POINTS_TEAMS) throw new Error('FULL')
  await http.post(`/tournaments/${t.id}/teams`, { teamId: team.id })
  await loadAdminData()
}

export async function removeTeamFromTournament(id, teamId) {
  const t = findTournament(id)
  assertEditable(t)
  await http.del(`/tournaments/${t.id}/teams/${Number(teamId)}`)
  await loadAdminData()
}

export function bracketSizeFor(teamCount) {
  return 2 ** Math.ceil(Math.log2(Math.max(teamCount, 2)))
}

// backend จัด seed ตามลำดับที่ทีมสมัคร (ยังไม่รับลำดับ seed จากหน้าเว็บ)
export async function generateBracket(id, orderedTeamIds) {
  const t = findTournament(id)
  assertEditable(t)
  if (t.format !== 'SINGLE_ELIMINATION') throw new Error('WRONG_FORMAT')
  if (teamIdsOf(t.id).length < 2) throw new Error('INVALID_SEEDS')
  void orderedTeamIds
  await http.post(`/tournaments/${t.id}/schedule`)
  await loadAdminData()
}

// backend ยังไม่มี API ล้างสาย
export async function resetBracket() {
  throw new Error('NOT_SUPPORTED')
}

export function draftFreeFireSchedule(tournament) {
  const start = new Date(`${tournament.startDate}T00:00:00Z`)
  return Array.from({ length: tournament.totalGames ?? 0 }, (_, i) => {
    const day = new Date(start.getTime() + Math.floor(i / 2) * 2 * 86400000).toISOString().slice(0, 10)
    return `${day}T${i % 2 === 0 ? '19:00' : '20:00'}:00`
  })
}

// backend สร้างเกม 1..totalGames ให้ ส่วนวันเวลาแต่ละเกมยังไม่มี API รับ
export async function generateFreeFireSchedule(id, schedule) {
  const t = findTournament(id)
  assertEditable(t)
  if (t.format !== 'POINTS' || schedule.length !== t.totalGames) throw new Error('INVALID_SCHEDULE')
  await http.post(`/tournaments/${t.id}/schedule`)
  await loadAdminData()
}

// backend ยังไม่มี API ล้างตารางเกม
export async function resetFreeFireSchedule() {
  throw new Error('NOT_SUPPORTED')
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
  const current = id == null ? null : teams.find((t) => t.id === Number(id))
  if (id != null && !current) throw new Error('NOT_FOUND')
  const gameLocked = current ? tournamentCountOf(current.id) > 0 : false
  const safe = { ...payload, gameId: gameLocked ? current.gameId : payload.gameId }
  const errors = validateTeam(safe, current?.id ?? null)
  if (Object.keys(errors).length) throw new ValidationError(errors)

  const body = { name: safe.name.trim(), description: safe.description?.trim() || null, gameId: Number(safe.gameId) }
  const team = await (current ? http.put(`/teams/${current.id}`, body) : http.post('/teams', body)).catch((e) => {
    if (e instanceof ApiError && e.status === 409) throw new ValidationError({ name: 'มีทีมชื่อนี้อยู่แล้ว' })
    throw e
  })

  // ผู้เล่น: แก้/เพิ่มผ่าน /players แล้วผูกกับทีมนี้ คนที่ถูกนำออกจะถูกปลดจากทีม (ไม่ลบ)
  const keep = new Set()
  for (const row of safe.players) {
    const data = { name: row.name.trim(), role: row.role.trim(), description: row.description?.trim() || null, teamId: team.id }
    const saved = row.id ? await http.put(`/players/${row.id}`, data) : await http.post('/players', data)
    keep.add(saved.id)
  }
  const leaving = players.filter((p) => p.teamId === team.id && !keep.has(p.id))
  for (const p of leaving) await http.del(`/teams/${team.id}/players/${p.id}`)

  if (safe.logoUrl?.startsWith('data:')) await uploadTeamLogo(team.id, safe.logoUrl)
  await loadAdminData()
  return team
}

export async function deleteTeam(id) {
  const team = teams.find((t) => t.id === Number(id))
  if (!team || tournamentCountOf(team.id) > 0) throw new Error('CANNOT_DELETE')
  await http.del(`/teams/${team.id}`)
  await loadAdminData()
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
  const errors = validatePlayer(payload)
  if (Object.keys(errors).length) throw new ValidationError(errors)
  const data = {
    name: payload.name.trim(),
    role: payload.role.trim(),
    description: payload.description?.trim() || null,
    teamId: payload.teamId == null || payload.teamId === '' ? null : Number(payload.teamId),
  }
  const saved = id == null ? await http.post('/players', data) : await http.put(`/players/${Number(id)}`, data)
  await loadAdminData()
  return saved
}

export async function deletePlayer(id) {
  await http.del(`/players/${Number(id)}`)
  await loadAdminData()
}

function matchView(m) {
  const t = findTournament(m.tournamentId)
  return {
    ...m,
    tournament: t,
    teamCount: teamIdsOf(t.id).length,
    isFinal: m.nextMatchId == null,
    teamA: teams.find((x) => x.id === m.teamAId) ?? null,
    teamB: teams.find((x) => x.id === m.teamBId) ?? null,
  }
}

const isReadyMatch = (m) => m.teamAId && m.teamBId && m.status !== 'COMPLETED'

export function getMatchesToRecord() {
  const ready = matches.filter(isReadyMatch).map(matchView)
  const byTime = (a, b) => (a.scheduledAt ?? '').localeCompare(b.scheduledAt ?? '')
  return {
    // backend ยังไม่มี API ตั้งเวลาแข่ง แมตช์ที่ยังไม่มีเวลาจึงถือว่าพร้อมกรอกผลได้เลย
    overdue: ready.filter((m) => !m.scheduledAt || m.scheduledAt.slice(0, 10) < TODAY).sort((a, b) => byTime(b, a)),
    today: ready.filter((m) => m.scheduledAt?.startsWith(TODAY)).sort(byTime),
  }
}

export function getFreeFireTournamentsToRecord() {
  return tournaments
    .filter((t) => t.format === 'POINTS' && t.status !== 'FINISHED')
    .map((t) => {
      const list = freeFireGames.filter((g) => g.tournamentId === t.id).sort((a, b) => a.gameNumber - b.gameNumber)
      const next = list.find((g) => g.status !== 'COMPLETED') ?? null
      return { tournament: t, nextGame: next, completed: list.filter((g) => g.status === 'COMPLETED').length }
    })
    .filter((x) => x.nextGame)
}

export function getMatchForResult(id) {
  const m = matches.find((x) => x.id === Number(id))
  return m ? matchView(m) : null
}

export async function recordMatchResult(matchId, { teamAScore, teamBScore, winnerTeamId }) {
  const m = matches.find((x) => x.id === Number(matchId))
  if (!m || !isReadyMatch(m)) throw new Error('NOT_READY')
  if (matchResults.some((r) => r.matchId === m.id)) throw new Error('ALREADY_RECORDED')
  const a = Number(teamAScore)
  const b = Number(teamBScore)
  if (![a, b].every((x) => Number.isInteger(x) && x >= 0)) throw new Error('INVALID_SCORE')
  if (a === b) throw new Error('DRAW')
  const expectedWinner = a > b ? m.teamAId : m.teamBId
  if (Number(winnerTeamId) !== expectedWinner) throw new Error('WRONG_WINNER')

  await http.post(`/matches/${m.id}/result`, { teamAScore: a, teamBScore: b, winnerTeamId: expectedWinner })
  await loadAdminData()
  const t = findTournament(m.tournamentId)
  return { winnerTeamId: expectedWinner, nextMatchId: m.nextMatchId ?? null, finished: t?.status === 'FINISHED' }
}

export function getFreeFireResultBoard(tournamentId) {
  const t = findTournament(tournamentId)
  if (!t || t.format !== 'POINTS') return null
  const list = freeFireGames.filter((g) => g.tournamentId === t.id).sort((a, b) => a.gameNumber - b.gameNumber)
  const nextId0 = list.find((g) => g.status !== 'COMPLETED')?.id ?? null
  const points = tournamentPlacementPoints
    .filter((p) => p.tournamentId === t.id)
    .reduce((map, p) => map.set(p.placement, p.points), new Map())
  return {
    tournament: { ...t, game: gameOf(t.gameId) },
    teams: teamIdsOf(t.id).map((id) => teams.find((x) => x.id === id)),
    placementPoints: points,
    games: list.map((g) => ({
      ...g,
      state: g.status === 'COMPLETED' ? 'DONE' : g.id === nextId0 ? 'NEXT' : 'LOCKED',
      results: freeFireGameResults
        .filter((r) => r.gameId === g.id)
        .sort((a, b) => a.placement - b.placement)
        .map((r) => ({ ...r, team: teams.find((x) => x.id === r.teamId) })),
    })),
  }
}

export async function recordFreeFireGameResult(gameId, rows) {
  const g = freeFireGames.find((x) => x.id === Number(gameId))
  if (!g || g.status === 'COMPLETED') throw new Error('NOT_READY')
  const t = findTournament(g.tournamentId)
  const earlier = freeFireGames.filter((x) => x.tournamentId === t.id && x.gameNumber < g.gameNumber)
  if (earlier.some((x) => x.status !== 'COMPLETED')) throw new Error('OUT_OF_ORDER')
  const ids = teamIdsOf(t.id)
  const placements = rows.map((r) => Number(r.placement))
  if (rows.length !== ids.length || !rows.every((r) => ids.includes(Number(r.teamId)))) throw new Error('MISSING_TEAMS')
  if (new Set(placements).size !== placements.length || placements.some((p) => !Number.isInteger(p) || p < 1 || p > ids.length)) {
    throw new Error('INVALID_PLACEMENT')
  }
  if (rows.some((r) => !Number.isInteger(Number(r.kills)) || Number(r.kills) < 0)) throw new Error('INVALID_KILLS')

  await http.post(`/free-fire-games/${g.id}/results`, {
    results: rows.map((r) => ({ teamId: Number(r.teamId), placement: Number(r.placement), kills: Number(r.kills) })),
  })
  await loadAdminData()
  return { finished: findTournament(t.id)?.status === 'FINISHED' }
}

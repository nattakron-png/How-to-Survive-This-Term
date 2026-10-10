export const MATCH_STATES = {
  BYE: { label: 'ผ่านอัตโนมัติ', tone: 'success' },
  DONE: { label: 'จบแล้ว', tone: 'success' },
  READY: { label: 'รอแข่ง', tone: 'info' },
  WAITING: { label: 'รอคู่แข่ง', tone: 'neutral' },
}

export function getMatchState(match) {
  if (match.status === 'COMPLETED') return match.teamA && match.teamB ? 'DONE' : 'BYE'
  return match.teamA && match.teamB ? 'READY' : 'WAITING'
}

export function emptySlotLabel(match, feeder) {
  if (getMatchState(match) === 'BYE') return 'BYE'
  return feeder ? `รอผู้ชนะแมตช์ ${feeder}` : 'รอทีม'
}

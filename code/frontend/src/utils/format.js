const TH_MONTHS = ['ม.ค.', 'ก.พ.', 'มี.ค.', 'เม.ย.', 'พ.ค.', 'มิ.ย.', 'ก.ค.', 'ส.ค.', 'ก.ย.', 'ต.ค.', 'พ.ย.', 'ธ.ค.']

function parseDate(iso) {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  return { y, m, d }
}

export function formatDate(iso) {
  const { y, m, d } = parseDate(iso)
  return `${d} ${TH_MONTHS[m - 1]} ${y}`
}

export function formatShortDate(iso) {
  const { m, d } = parseDate(iso)
  return `${d} ${TH_MONTHS[m - 1]}`
}

export function formatDateRange(startIso, endIso) {
  const s = parseDate(startIso)
  const e = parseDate(endIso)
  if (startIso === endIso) return formatDate(startIso)
  if (s.y === e.y && s.m === e.m) return `${s.d}–${e.d} ${TH_MONTHS[e.m - 1]} ${e.y}`
  if (s.y === e.y) return `${s.d} ${TH_MONTHS[s.m - 1]} – ${e.d} ${TH_MONTHS[e.m - 1]} ${e.y}`
  return `${formatDate(startIso)} – ${formatDate(endIso)}`
}

export function formatTime(isoDateTime) {
  return isoDateTime ? isoDateTime.slice(11, 16) : '-'
}

export function formatTournamentFormat(format, totalGames) {
  if (format === 'POINTS') return `เก็บคะแนน ${totalGames} เกม`
  return 'แพ้คัดออก'
}

export function formatRound(roundNumber, teamCount) {
  const totalRounds = Math.ceil(Math.log2(Math.max(teamCount, 2)))
  const teamsLeft = 2 ** (totalRounds - roundNumber + 1)
  if (teamsLeft <= 2) return 'รอบชิงชนะเลิศ'
  if (teamsLeft === 4) return 'รอบรองชนะเลิศ'
  return `รอบ ${teamsLeft} ทีม`
}

export function initials(name, max = 2) {
  const words = name.split(/[\s-]+/).filter((w) => /^[A-Za-z]/.test(w))
  if (words.length === 1) return words[0].slice(0, max).toUpperCase()
  return words.slice(0, max).map((w) => w[0].toUpperCase()).join('')
}

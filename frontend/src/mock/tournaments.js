// Mock data ชั่วคราว — ยังไม่เชื่อม backend
// ตอนเชื่อมจริง ให้เปลี่ยนไปดึงจาก REST API (/api/v1/...) แทน
export const tournaments = [
  { id: 1, name: 'Free Fire Cup 2026', game: 'Free Fire', format: 'POINTS', status: 'ONGOING', teamCount: 12, startDate: '2026-10-15' },
  { id: 2, name: 'ROV Campus League', game: 'ROV', format: 'SINGLE_ELIMINATION', status: 'UPCOMING', teamCount: 8, startDate: '2026-11-01' },
  { id: 3, name: 'Valorant Night Series', game: 'Valorant', format: 'SINGLE_ELIMINATION', status: 'FINISHED', teamCount: 16, startDate: '2026-09-05' },
]

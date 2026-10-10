import { users } from './users'

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export async function login(username, password) {
  await delay(400)
  const user = users.find((u) => u.username === username.trim() && u.password === password)
  if (!user || user.role !== 'ADMIN') {
    throw new Error('INVALID_CREDENTIALS')
  }
  return { id: user.id, username: user.username, role: user.role }
}

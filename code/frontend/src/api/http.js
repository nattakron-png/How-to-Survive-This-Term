// ตัวช่วยเรียก backend ทุก endpoint อยู่ใต้ /api/v1 (dev ใช้ proxy ของ Vite ไป :8080)
export class ApiError extends Error {
  constructor(status, message) {
    super(message || `HTTP ${status}`)
    this.status = status
  }
}

async function request(method, path, body) {
  const isForm = body instanceof FormData
  const response = await fetch(`/api/v1${path}`, {
    method,
    headers: body && !isForm ? { 'Content-Type': 'application/json' } : undefined,
    body: body == null ? undefined : isForm ? body : JSON.stringify(body),
  })
  if (!response.ok) {
    let message = ''
    try {
      message = (await response.json()).message ?? ''
    } catch {
      message = ''
    }
    throw new ApiError(response.status, message)
  }
  if (response.status === 204) return null
  const text = await response.text()
  return text ? JSON.parse(text) : null
}

export const http = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body ?? {}),
  put: (path, body) => request('PUT', path, body ?? {}),
  del: (path) => request('DELETE', path),
}

// GET ที่ตอบเป็น 404 ให้ถือว่า "ไม่มีข้อมูล"
export async function getOrNull(path) {
  try {
    return await http.get(path)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

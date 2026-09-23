const BASE = '/api'

async function request(path, { method = 'GET', body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })

  let payload = null
  try {
    payload = await res.json()
  } catch {
    /* cuerpo vacío */
  }

  if (!res.ok) {
    const message = payload?.message || payload?.error || `Error ${res.status} ${res.statusText}`
    throw new Error(message)
  }

  return payload?.data ?? payload
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  del: (path) => request(path, { method: 'DELETE' }),
}
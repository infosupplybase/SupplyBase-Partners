// Thin fetch wrapper: same-origin session cookie auth + CSRF header pair,
// matching the backend's SecurityConfig (CookieCsrfTokenRepository with
// XSRF-TOKEN readable by JS, echoed back as X-XSRF-TOKEN).

function readCookie(name) {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'))
  return match ? decodeURIComponent(match[2]) : null
}

class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Request failed with status ${status}`)
    this.status = status
    this.body = body
  }
}

async function request(path, { method = 'GET', body, isFormData = false } = {}) {
  const headers = {}
  if (!isFormData) {
    headers['Content-Type'] = 'application/json'
  }
  const csrfToken = readCookie('XSRF-TOKEN')
  if (csrfToken && method !== 'GET') {
    headers['X-XSRF-TOKEN'] = csrfToken
  }

  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
  })

  if (response.status === 204) {
    return null
  }
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    throw new ApiError(response.status, payload)
  }
  return payload
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  patch: (path, body) => request(path, { method: 'PATCH', body }),
  del: (path) => request(path, { method: 'DELETE' }),
  postForm: (path, formData) => request(path, { method: 'POST', body: formData, isFormData: true }),
}

export { ApiError }

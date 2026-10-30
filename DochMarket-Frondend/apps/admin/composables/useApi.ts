export const useApi = () => {
  const config = useRuntimeConfig()
  const { token, user } = useAuth()

  // This helper reads the token safely.
  // Problem: useState hydrates from localStorage only once at app start.
  // If the component mounts before useState hydrates, token.value can be null
  // even though localStorage has a valid token.
  // Solution: always fall back to reading localStorage directly.
  const getToken = (): string | null => {
    if (token.value) return token.value
    if (import.meta.client) return localStorage.getItem('token')
    return null
  }

  // Builds the Authorization header. If there's no token, returns empty
  // headers (unauthenticated request — will 403 on protected routes).
  const authHeaders = (): HeadersInit => {
    const t = getToken()
    const headers: Record<string, string> = {}
    if (t) headers.Authorization = `Bearer ${t}`
    return headers
  }

  // Retries only genuine network failures (no response reached us at all —
  // timeouts, Render cold starts, DNS hiccups). Real server responses
  // (400/401/403/404/500 etc.) are returned/thrown immediately since
  // retrying won't fix them.
  async function withRetry<T>(fn: () => Promise<T>, attempts = 3, delayMs = 4000): Promise<T> {
    let lastErr: any
    for (let i = 0; i < attempts; i++) {
      try {
        return await fn()
      } catch (e: any) {
        lastErr = e
        const isNetworkFailure = e?.status === undefined && e?.response === undefined
        if (!isNetworkFailure || i === attempts - 1) throw e
        await new Promise(r => setTimeout(r, delayMs))
      }
    }
    throw lastErr
  }

  // If a stale/expired token gets us a 401 or 403 from a protected route,
  // the previous behavior was to show a broken/empty page with no
  // explanation (the exact bug reported: dashboard loads, all data comes
  // back "network error", and only logging out and back in fixes it).
  // Instead: clear the dead session immediately and send the person back to
  // login with a plain explanation — no cache-clearing or devtools needed,
  // this runs automatically the moment any API call detects the problem.
  async function withSessionGuard<T>(fn: () => Promise<T>): Promise<T> {
    try {
      return await fn()
    } catch (e: any) {
      if (e?.status === 401 && import.meta.client) {
        const hadToken = !!getToken()
        token.value = null
        user.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        document.cookie = 'auth_token=; path=/; max-age=0; SameSite=Lax'
        if (hadToken && !window.location.pathname.startsWith('/login')) {
          navigateTo('/login?expired=1', { replace: true })
        }
      }
      throw e
    }
  }

  const get = async <T = any>(endpoint: string): Promise<T> => {
    return await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, {
      headers: authHeaders()
    })))
  }

  const post = async <T = any>(endpoint: string, body: any): Promise<T> => {
    return await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, {
      method: 'POST',
      headers: authHeaders(),
      body
    })))
  }

  const put = async <T = any>(endpoint: string, body: any): Promise<T> => {
    return await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, {
      method: 'PUT',
      headers: authHeaders(),
      body
    })))
  }

  const patch = async <T = any>(endpoint: string, body?: any): Promise<T> => {
    return await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, {
      method: 'PATCH',
      headers: authHeaders(),
      body
    })))
  }

  const del = async <T = any>(endpoint: string): Promise<T> => {
    return await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, {
      method: 'DELETE',
      headers: authHeaders()
    })))
  }

  return { get, post, put, patch, del }
}
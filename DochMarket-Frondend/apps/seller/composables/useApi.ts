export const useApi = () => {
  const config = useRuntimeConfig()
  const { token, user } = useAuth()

  // useState hydrates from localStorage once at app start; if a component mounts
  // before that, token.value can be null even with a valid stored token — so we
  // always fall back to reading localStorage directly.
  const getToken = (): string | null => {
    if (token.value) return token.value
    if (process.client) return localStorage.getItem('token')
    return null
  }

  const authHeaders = () => {
    const t = getToken()
    return t ? { Authorization: `Bearer ${t}` } : {}
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

  // If a stale/expired token gets us a 401/403 on a protected route, the
  // dashboard used to just show a blank/broken page with no explanation —
  // the exact "everything shows network error, only logging out and back in
  // fixes it" bug. Instead: clear the dead session immediately and send the
  // person back to login — no cache-clearing or devtools needed, this runs
  // automatically the moment any API call detects the problem.
  async function withSessionGuard<T>(fn: () => Promise<T>): Promise<T> {
    try {
      return await fn()
    } catch (e: any) {
      if (e?.status === 401 && process.client) {
        const hadToken = !!getToken()
        token.value = null
        user.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        document.cookie = 'seller_token=; path=/; max-age=0; SameSite=Lax'
        if (hadToken && !window.location.pathname.startsWith('/login')) {
          navigateTo('/login?expired=1', { replace: true })
        }
      }
      throw e
    }
  }

  const get = async <T = any>(endpoint: string): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, { headers: authHeaders() })))

  const post = async <T = any>(endpoint: string, body: any): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, { method: 'POST', headers: authHeaders(), body })))

  const put = async <T = any>(endpoint: string, body: any): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, { method: 'PUT', headers: authHeaders(), body })))

  const patch = async <T = any>(endpoint: string, body?: any): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, { method: 'PATCH', headers: authHeaders(), body })))

  const del = async <T = any>(endpoint: string): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${config.public.apiBase}${endpoint}`, { method: 'DELETE', headers: authHeaders() })))

  return { get, post, put, patch, del }
}

// Xender API helper. JSON for most calls; createDelivery uses raw multipart
// (the backend expects a `request` JSON part + `images` file parts).
export const useApi = () => {
  const config = useRuntimeConfig()
  const { token, user } = useAuth()

  const getToken = (): string | null => {
    if (token.value) return token.value
    if (import.meta.client) return localStorage.getItem('xender-token')
    return null
  }
  const authHeaders = (): HeadersInit | undefined => {
    const t = getToken()
    return t ? { Authorization: `Bearer ${t}` } : undefined
  }
  const base = () => config.public.apiBase

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

  // If a stale/expired token gets us a 401/403 on a protected route, clear
  // the dead session immediately and send the person back to login — no
  // cache-clearing or devtools needed, this runs automatically the moment
  // any API call detects the problem.
  async function withSessionGuard<T>(fn: () => Promise<T>): Promise<T> {
    try {
      return await fn()
    } catch (e: any) {
      if (e?.status === 401 && import.meta.client) {
        const hadToken = !!getToken()
        token.value = null
        user.value = null
        localStorage.removeItem('xender-token')
        localStorage.removeItem('xender-user')
        document.cookie = 'xender_token=; path=/; max-age=0; SameSite=Lax'
        if (hadToken && !window.location.pathname.startsWith('/login')) {
          navigateTo('/login?expired=1', { replace: true })
        }
      }
      throw e
    }
  }

  const get = async <T = any>(endpoint: string): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${base()}${endpoint}`, { headers: authHeaders() })))

  const post = async <T = any>(endpoint: string, body?: any): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${base()}${endpoint}`, { method: 'POST', headers: authHeaders(), body })))

  const put = async <T = any>(endpoint: string, body?: any): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${base()}${endpoint}`, { method: 'PUT', headers: authHeaders(), body })))

  // Multipart POST — pass a ready FormData. We do NOT set Content-Type so the
  // browser adds the multipart boundary.
  const postMultipart = async <T = any>(endpoint: string, formData: FormData): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${base()}${endpoint}`, { method: 'POST', headers: authHeaders(), body: formData })))

  const imageUrl = (imageId?: string | null): string =>
    imageId ? `${base()}/api/product/image/download/${imageId}` : ''

  return { get, post, put, postMultipart, imageUrl, base }
}

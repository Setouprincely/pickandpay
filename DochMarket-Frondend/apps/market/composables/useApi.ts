// Market API helper. Public reads (products, categories, sellers) work without a
// token; authed calls (orders, profile, wallet) attach the buyer's JWT.
export const useApi = () => {
  const config = useRuntimeConfig()
  const { token, user } = useAuth()

  const getToken = (): string | null => {
    if (token.value) return token.value
    if (process.client) return localStorage.getItem('market-token')
    return null
  }

  const authHeaders = () => {
    const t = getToken()
    return t ? { Authorization: `Bearer ${t}` } : {}
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

  // If a stale/expired token gets us a 401/403 on a protected call, clear
  // the dead session immediately and send the person back to login — no
  // cache-clearing or devtools needed, this runs the moment any API call
  // detects the problem. Market has public browsing, so this only fires
  // for calls that were actually sending a token in the first place.
  async function withSessionGuard<T>(fn: () => Promise<T>): Promise<T> {
    try {
      return await fn()
    } catch (e: any) {
      if (e?.status === 401 && process.client && getToken()) {
        token.value = null
        user.value = null
        localStorage.removeItem('market-token')
        localStorage.removeItem('market-user')
        document.cookie = 'market_token=; path=/; max-age=0; SameSite=Lax'
        if (!window.location.pathname.startsWith('/login')) {
          navigateTo(`/login?expired=1&redirect=${encodeURIComponent(window.location.pathname)}`, { replace: true })
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

  const del = async <T = any>(endpoint: string): Promise<T> =>
    await withSessionGuard(() => withRetry(() => $fetch<T>(`${base()}${endpoint}`, { method: 'DELETE', headers: authHeaders() })))

  // Build a product image URL from an imageId (backend serves the bytes).
  const imageUrl = (imageId?: string | null): string =>
    imageId ? `${base()}/api/product/image/download/${imageId}` : '/images/market/products/placeholder.jpg'

  return { get, post, put, del, imageUrl, base }
}

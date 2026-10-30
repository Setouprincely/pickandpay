// Buyer auth for the market storefront — real backend, cookie-based so the
// middleware can gate account/checkout/orders pages on SSR + client.
export interface AuthUser {
  id?: string
  name: string
  email: string
  phone?: string
  role?: string
}

export function useAuth() {
  const config = useRuntimeConfig()

  // True while a request is being retried after a network-level failure
  // (e.g. Render free-tier cold start). Pages can show a "waking up the
  // server, this can take up to a minute" hint while this is true.
  const serverWaking = useState<boolean>('auth_server_waking', () => false)

  // Retries only on genuine network failures (no response reached us at
  // all — timeouts, cold starts, DNS hiccups). Real server responses
  // (400/401/403/500 etc.) are NOT retried since retrying won't fix them.
  async function fetchWithRetry<T>(url: string, opts: any, attempts = 3, delayMs = 4000): Promise<T> {
    let lastErr: any
    for (let i = 0; i < attempts; i++) {
      try {
        const result = await $fetch<T>(url, opts)
        serverWaking.value = false
        return result
      } catch (e: any) {
        lastErr = e
        const isNetworkFailure = e?.status === undefined && e?.response === undefined
        if (!isNetworkFailure || i === attempts - 1) {
          serverWaking.value = false
          throw e
        }
        serverWaking.value = true
        await new Promise(r => setTimeout(r, delayMs))
      }
    }
    throw lastErr
  }

  const tokenCookie = useCookie<string | null>('market_token', {
    sameSite: 'lax',
    maxAge: 60 * 60 * 24,
  })

  const token = useState<string | null>('market-auth-token', () => {
    if (tokenCookie.value) return tokenCookie.value
    if (process.client) return localStorage.getItem('market-token')
    return null
  })

  const user = useState<AuthUser | null>('market-user', () => {
    if (process.client) {
      try {
        const saved = localStorage.getItem('market-user')
        return saved ? JSON.parse(saved) : null
      } catch { return null }
    }
    return null
  })

  const isLoggedIn = computed(() => !!token.value)

  function persist(jwt: string, u: AuthUser) {
    token.value = jwt
    user.value = u
    tokenCookie.value = jwt
    if (process.client) {
      localStorage.setItem('market-token', jwt)
      localStorage.setItem('market-user', JSON.stringify(u))
    }
  }

  // Real login. Buyers are ROLE_BUYER.
  async function login(email: string, password: string) {
    const res: any = await fetchWithRetry(`${config.public.apiBase}/api/auth/login`, {
      method: 'POST',
      body: { email, password },
    })
    const roles: string[] = res.roles ?? (res.role ? [res.role] : [])
    if (!roles.includes('ROLE_BUYER') && !roles.includes('ROLE_SELLER')) {
      throw new Error('This account is not a shopper account.')
    }
    persist(res.access_token, { id: res.user_id, email, name: email.split('@')[0], role: res.role })
    return res
  }

  // Real register — does NOT auto-login (account needs email verification first).
  async function register(payload: {
    email: string
    firstName: string
    lastName: string
    userName: string
    password: string
    phone: string
    doB: string
  }) {
    const res: any = await fetchWithRetry(`${config.public.apiBase}/api/auth/register`, {
      method: 'POST',
      body: { ...payload, role: 'ROLE_BUYER' },
    })
    return res
  }

  // Verify the 6-digit OTP sent by email. On success the account is enabled
  // and can log in immediately.
  async function verifyOtp(email: string, otp: string) {
    return await $fetch(`${config.public.apiBase}/api/auth/verify`, {
      method: 'POST',
      body: { email, token: otp },
    })
  }

  // Ask the backend to send a fresh OTP (invalidates any previous one).
  async function resendOtp(email: string) {
    return await $fetch(`${config.public.apiBase}/api/auth/resend`, {
      method: 'GET',
      query: { email },
    })
  }

  // Sends a password-reset code to the given email. Backend expects this as
  // a query param, not a JSON body.
  async function forgotPassword(email: string) {
    return await fetchWithRetry(`${config.public.apiBase}/api/auth/forgot-password`, {
      method: 'POST',
      query: { email },
    })
  }

  async function resetPassword(email: string, otpCode: string, newPassword: string) {
    return await fetchWithRetry(`${config.public.apiBase}/api/auth/reset-password`, {
      method: 'POST',
      body: { email, otpCode, newPassword },
    })
  }

  async function logout() {
    // Clear local state and free the UI immediately — don't block sign-out
    // on a slow/cold-starting backend. The server call still fires, just
    // in the background.
    const jwt = token.value
    token.value = null
    user.value = null
    tokenCookie.value = null
    if (process.client) {
      localStorage.removeItem('market-token')
      localStorage.removeItem('market-user')
    }
    $fetch(`${config.public.apiBase}/api/auth/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${jwt}` },
    }).catch(() => {})
  }

  return { user, token, isLoggedIn, login, register, verifyOtp, resendOtp, forgotPassword, resetPassword, logout, serverWaking }
}

// Xender sender auth — real backend, cookie-based (xender_token) so the
// middleware can gate on SSR + client. Senders register/login as ROLE_BUYER.
export interface AuthUser {
  id?: string
  name: string
  email?: string
  phone?: string
  role?: string
}

export function useAuth() {
  const config = useRuntimeConfig()
  const isClient = import.meta.client


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
  const tokenCookie = useCookie<string | null>('xender_token', {
    sameSite: 'lax',
    maxAge: 60 * 60 * 24,
  })

  const token = useState<string | null>('xender-auth-token', () => {
    if (tokenCookie.value) return tokenCookie.value
    if (isClient) return localStorage.getItem('xender-token')
    return null
  })

  const user = useState<AuthUser | null>('xender-user', () => {
    if (isClient) {
      try {
        const saved = localStorage.getItem('xender-user')
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
    if (isClient) {
      localStorage.setItem('xender-token', jwt)
      localStorage.setItem('xender-user', JSON.stringify(u))
    }
  }

  async function login(email: string, password: string) {
  const res: any = await fetchWithRetry(`${config.public.apiBase}/api/auth/login`, {
    method: 'POST',
    body: { email, password },
  })
  persist(res.access_token, { id: res.user_id, email, name: deriveName(email), role: res.role || '' })
  return res
}

  // Real register — no auto-login (email verification first).
  async function register(payload: {
    email: string; firstName: string; lastName: string
    userName: string; password: string; phone: string; doB: string
  }) {
    return await fetchWithRetry(`${config.public.apiBase}/api/auth/register`, {
      method: 'POST',
      body: { ...payload, role: 'ROLE_BUYER' },
    })
  }

  async function forgotPassword(email: string) {
    return await fetchWithRetry(`${config.public.apiBase}/api/auth/forgot-password`, {
      method: 'POST', query: { email },
    })
  }

  async function resetPassword(email: string, otpCode: string, newPassword: string) {
    return await fetchWithRetry(`${config.public.apiBase}/api/auth/reset-password`, {
      method: 'POST', body: { email, otpCode, newPassword },
    })
  }

  async function verifyOtp(email: string, otp: string) {
    return await $fetch(`${config.public.apiBase}/api/auth/verify`, {
      method: 'POST', body: { email, token: otp },
    })
  }

  async function resendOtp(email: string) {
    return await $fetch(`${config.public.apiBase}/api/auth/resend`, {
      method: 'GET', query: { email },
    })
  }

  async function logout() {
    // Clear local state immediately — don't block sign-out on a slow/cold
    // backend. The server call still fires, just in the background.
    const jwt = token.value
    token.value = null
    user.value = null
    tokenCookie.value = null
    if (isClient) {
      localStorage.removeItem('xender-token')
      localStorage.removeItem('xender-user')
    }
    $fetch(`${config.public.apiBase}/api/auth/logout`, {
      method: 'POST', headers: { Authorization: `Bearer ${jwt}` },
    }).catch(() => {})
  }

  // Back-compat shim: old code called signIn(userObj) after a fake OTP. Kept so
  // nothing breaks, but real auth goes through login()/register() above.
  function signIn(u: AuthUser) {
    user.value = u
    if (isClient) localStorage.setItem('xender-user', JSON.stringify(u))
  }
  function nameForPhone() { return null }

  function deriveName(email: string): string {
  const local = email.split('@')[0] ?? ''
  return (
    local
      .split(/[._-]+/)
      .filter(Boolean)
      .map(s => s.charAt(0).toUpperCase() + s.slice(1))
      .join(' ') || 'User'
  )
}

  return {
    user, token, isLoggedIn,
    login, register, verifyOtp, resendOtp, forgotPassword, resetPassword, logout,
    signIn, nameForPhone, serverWaking,
  }
}

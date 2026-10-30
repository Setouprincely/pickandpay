export const useAuth = () => {
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
  // Cookie unique to the seller app, readable on SSR + client so the middleware
  // can block direct-URL access (localStorage alone is client-only).
  const tokenCookie = useCookie<string | null>('seller_token', {
    sameSite: 'lax',
    maxAge: 60 * 60 * 24,
  })

  const token = useState<string | null>('auth_token', () => {
    if (tokenCookie.value) return tokenCookie.value
    if (import.meta.client) return localStorage.getItem('token')
    return null
  })

  const user = useState<any | null>('auth_user', () => {
    if (import.meta.client) {
      const stored = localStorage.getItem('user')
      return stored ? JSON.parse(stored) : null
    }
    return null
  })

  function persist(jwt: string, u: any) {
    token.value = jwt
    user.value = u
    tokenCookie.value = jwt
    if (import.meta.client) {
      localStorage.setItem('token', jwt)
      localStorage.setItem('user', JSON.stringify(u))
    }
  }

  const login = async (email: string, password: string) => {
  const res: any = await fetchWithRetry(`${config.public.apiBase}/api/auth/login`, {
    method: 'POST',
    body: { email, password },
  })

  const roles: string[] = res.roles ?? (res.role ? [res.role] : [])
  if (!roles.includes('ROLE_SELLER')) {
    throw new Error('This account does not have seller access.')
  }

  persist(res.access_token, { id: res.user_id, role: res.role, name: res.name || res.firstName || deriveName(email) })
  return res
}

  const register = async (payload: {
    email: string
    firstName: string
    lastName: string
    userName: string
    password: string
    phone: string
    contactNumber: string
    dob: string
    storeName: string
    city: string
    address: string
  }) => {
    // Seller self-registration uses the dedicated seller endpoint. Field names
    // must match SellerRegistration exactly on the backend (storeName, doB,
    // userName, contactNumber, etc.) — this endpoint has no "role" field.
    const res: any = await $fetch(`${config.public.apiBase}/api/auth/seller/register`, {
      method: 'POST',
      body: {
        email: payload.email,
        firstName: payload.firstName,
        lastName: payload.lastName,
        userName: payload.userName,
        password: payload.password,
        phone: payload.phone,
        contactNumber: payload.contactNumber,
        doB: payload.dob,
        storeName: payload.storeName,
        city: payload.city,
        address: payload.address,
      },
    })

    const jwt = res.access_token
if (jwt) {
  persist(jwt, {
    id: res.user_id,
    role: res.role ?? 'ROLE_SELLER',
    name: `${payload.firstName} ${payload.lastName}`,
    email: payload.email,
    storeName: payload.storeName,
    location: payload.city,
  })
}
    return res
  }

  const verifyOtp = async (email: string, otp: string) => {
    return await $fetch(`${config.public.apiBase}/api/auth/verify`, {
      method: 'POST',
      body: { email, token: otp },
    })
  }

  const resendOtp = async (email: string) => {
    return await $fetch(`${config.public.apiBase}/api/auth/resend`, {
      method: 'GET',
      query: { email },
    })
  }

  const forgotPassword = async (email: string) => {
    return await fetchWithRetry(`${config.public.apiBase}/api/auth/forgot-password`, {
      method: 'POST',
      query: { email },
    })
  }

  const resetPassword = async (email: string, otpCode: string, newPassword: string) => {
    return await fetchWithRetry(`${config.public.apiBase}/api/auth/reset-password`, {
      method: 'POST',
      body: { email, otpCode, newPassword },
    })
  }

  const logout = async () => {
    // Clear local state and navigate immediately — don't block the sign-out
    // button on a slow/cold-starting backend. The server call still fires,
    // just in the background.
    const jwt = token.value
    token.value = null
    user.value = null
    tokenCookie.value = null
    if (import.meta.client) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
    navigateTo('/login')

    $fetch(`${config.public.apiBase}/api/auth/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${jwt}` },
    }).catch(() => {})
  }

  function deriveName(email: string): string {
  const local = email.split('@')[0] ?? ''
  return (
    local
      .split(/[._-]+/)
      .filter(Boolean)
      .map(s => s.charAt(0).toUpperCase() + s.slice(1))
      .join(' ') || 'Seller'
  )
}

  const isLoggedIn = computed(() => !!token.value)

  return { token, user, login, register, verifyOtp, resendOtp, forgotPassword, resetPassword, logout, isLoggedIn, serverWaking }
}

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
        const result = await $fetch<T>(url, { ...opts, timeout: 15000 })
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
  const token = useState<string | null>('auth_token', () => {
    if (typeof window !== 'undefined') return localStorage.getItem('token')
    return null
  })

  const register = async (payload: { name: string; email: string; password: string }) => {
  return await $fetch(`${config.public.apiBase}/api/auth/register`, {
    method: 'POST',
    body: {
      email: payload.email,
      firstName: payload.name.split(' ')[0] || payload.name,
      lastName: payload.name.split(' ').slice(1).join(' ') || '',
      userName: payload.email.split('@')[0],
      password: payload.password,
      role: 'ROLE_ADMIN',
    },
  })
}

  const user = useState<any | null>('auth_user', () => {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('user')
      return stored ? JSON.parse(stored) : null
    }
    return null
  })

  const login = async (email: string, password: string) => {
    const res: any = await fetchWithRetry(`${config.public.apiBase}/api/auth/login`, {
      method: 'POST',
      body: { email, password }
    }, 2, 1000)

    // Blocklist (not allowlist) so custom staff roles created by a Super Admin still get in.
    const roles: string[] = res.roles ?? (res.role ? [res.role] : [])
    if (roles.every(r => ['ROLE_SELLER', 'ROLE_BUYER', 'ROLE_DELIVERY'].includes(r))) {
      throw createError({ statusCode: 403, data: { message: 'This account does not have admin access.' } })
    }

    const jwt = res.access_token || res.token
    token.value = jwt
    user.value = { id: res.user_id, role: res.role }

    if (typeof window !== 'undefined') {
      localStorage.setItem('token', jwt)
      localStorage.setItem('user', JSON.stringify(user.value))
      // Cookie is required for SSR middleware to block direct URL access
      document.cookie = `auth_token=${jwt}; path=/; max-age=86400; SameSite=Lax`
    }
    return res
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

  // Completes an invited staff/driver account: verifies the OTP from the
  // invite email and sets their permanent password in one call.
  const completeStaffSetup = async (email: string, otpCode: string, newPassword: string) => {
    return await $fetch(`${config.public.apiBase}/api/auth/staff-setup`, {
      method: 'POST',
      body: { email, otpCode, newPassword },
    })
  }

  const logout = async () => {
    // Clear local state and navigate immediately — don't make the button
    // hang if the backend is slow/cold-starting. The server-side logout
    // call still fires, just without blocking the user.
    const jwt = token.value
    token.value = null
    user.value = null
    if (typeof window !== 'undefined') {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      document.cookie = 'auth_token=; path=/; max-age=0; SameSite=Lax'
    }
    await navigateTo('/login', { replace: true })

    $fetch(`${config.public.apiBase}/api/auth/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${jwt}` }
    }).catch(() => {})
  }

  const isLoggedIn = computed(() => !!token.value)
  return { token, user, login, register, completeStaffSetup, forgotPassword, resetPassword, logout, isLoggedIn, serverWaking }
}
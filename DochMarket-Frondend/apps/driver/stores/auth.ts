type DriverUser = {
  name: string

 
  email: string
  userId: string
  role: string
}

export const useAuth = () => {
  const config = useRuntimeConfig()

  // True while a request is being retried after a network-level failure
  // (e.g. Render free-tier cold start). Pages can show a "waking up the
  // server, this can take up to a minute" hint while this is true.
  //
  // NOTE: useState() must be called from inside this function (during
  // setup/composable execution) — calling it at module top-level, before
  // useAuth() ever runs, crashes every single page with "composable called
  // outside of a plugin, Nuxt hook, or Vue setup function". That was the
  // actual cause of every Driver page failing to load.
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

  const token = useState<string | null>('driver-token', () => {
    if (import.meta.client) return localStorage.getItem('driver-token')
    return null
  })

  const user = useState<DriverUser | null>('driver-user', () => {
    if (import.meta.client) {
      const raw = localStorage.getItem('driver-user')
      return raw ? JSON.parse(raw) : null
    }
    return null
  })

  const isAuthenticated = computed(() => !!token.value)
  const displayName = computed(() => user.value?.name || 'Driver')
  const initials = computed(() => {
    const name = user.value?.name?.trim()
    if (!name) return 'DR'
    const parts = name.split(/\s+/)
    const firstChar = parts[0]?.[0] ?? ''
    const secondChar = parts[1]?.[0] ?? ''
    const result = (firstChar + secondChar).toUpperCase()
    return result || 'DR'
  })

  function restore() {
    if (!import.meta.client) return
    token.value = localStorage.getItem('driver-token')
    const raw = localStorage.getItem('driver-user')
    user.value = raw ? JSON.parse(raw) : null
  }

  async function signIn(email: string, password: string) {
    // Real API call — POST /api/auth/login
    const res: any = await fetchWithRetry(`${config.public.apiBase}/api/auth/login`, {
      method: 'POST',
      body: { email, password }
    })

    // Validate driver role
    const roles: string[] = res.roles ?? (res.role ? [res.role] : [])
    if (!roles.includes('ROLE_DELIVERY')) {
      throw new Error('Not a driver account')
    }

    const t = res.access_token || res.token
    const u: DriverUser = {
      name: deriveName(email),
      email,
      userId: res.user_id,
      role: res.role
    }

    token.value = t
    user.value = u

    if (import.meta.client) {
      localStorage.setItem('driver-token', t)
      localStorage.setItem('driver-user', JSON.stringify(u))
      // Cookie must be set so SSR middleware blocks direct URL access
      document.cookie = `driver_token=${t}; path=/; max-age=86400; SameSite=Lax`
    }
    return res
  }

  async function register(input: { name: string; email: string; password: string }) {
    // Registration goes through admin onboarding — drivers can't self-register
    // This UI flow exists but should inform the user to contact admin
    throw new Error('Driver accounts are created by admin. Please contact your administrator.')
  }

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

  async function completeStaffSetup(email: string, otpCode: string, newPassword: string) {
    return await $fetch(`${config.public.apiBase}/api/auth/staff-setup`, {
      method: 'POST',
      body: { email, otpCode, newPassword },
    })
  }

  function signOut() {
    token.value = null
    user.value = null
    if (import.meta.client) {
      localStorage.removeItem('driver-token')
      localStorage.removeItem('driver-user')
      document.cookie = 'driver_token=; path=/; max-age=0; SameSite=Lax'
    }
    navigateTo('/login', { replace: true })
  }

  return { isAuthenticated, token, user, displayName, initials, restore, signIn, register, completeStaffSetup, forgotPassword, resetPassword, signOut, serverWaking }
}

function deriveName(email: string): string {
  const local = email.split('@')[0] ?? ''
  return (
    local
      .split(/[._-]+/)
      .filter(Boolean)
      .map(s => s.charAt(0).toUpperCase() + s.slice(1))
      .join(' ') || 'Driver'
  )
}
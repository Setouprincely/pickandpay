// plugins/auth.global.ts
// Global route guard for the driver app. Runs on EVERY navigation — including
// direct URL pastes — so auth cannot be bypassed by typing a URL.
//
// IMPORTANT: registered under a UNIQUE name ('driver-auth-guard'), NOT 'auth'.
// The file middleware/auth.ts already owns the name 'auth'; registering a second
// 'auth' here used to collide and the broken localStorage check won. With a
// distinct name + { global: true } this guard runs on every route on top of any
// page-level middleware.
export default defineNuxtPlugin(() => {
  addRouteMiddleware(
    'driver-auth-guard',
    (to) => {
      const open = ['/login', '/signup']
      const isOpen = open.includes(to.path)
      const token = useCookie('driver_token').value

      if (!token && !isOpen) {
        return navigateTo('/login', { replace: true })
      }
      if (token && to.path === '/login') {
        return navigateTo('/', { replace: true })
      }
    },
    { global: true },
  )
})
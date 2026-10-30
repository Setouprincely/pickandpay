// Senders must be signed in to use Xender. Cookie-based (xender_token) so it
// works on SSR + client. /login, /signup, /forgot, /reset and the public
// /track/:id receiver link stay open.
export default defineNuxtRouteMiddleware((to) => {
  const open = ['/login', '/signup', '/forgot-password', '/reset-password', '/payment-status']
  const isPublic = (p: string) => open.includes(p) || p.startsWith('/track') || p.startsWith('/otp-validation')

  const token = useCookie('xender_token').value

  if (!token && !isPublic(to.path)) {
    return navigateTo(`/login?redirect=${encodeURIComponent(to.path)}`, { replace: true })
  }
  if (token && open.includes(to.path)) {
    return navigateTo('/', { replace: true })
  }
})

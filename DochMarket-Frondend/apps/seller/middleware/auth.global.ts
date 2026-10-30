// Sellers must be signed in to use the dashboard. Cookie-based (seller
// token, stored via the seller_token cookie in useAuth.ts) so it works on
// SSR + client. Only login, signup, forgot-password and the email/OTP
// verification step stay open to signed-out visitors.
export default defineNuxtRouteMiddleware((to) => {
  const open = ['/login', '/signup', '/forgot-password']
  const isOpen = open.includes(to.path) || to.path.startsWith('/otp-validation')

  const token = useCookie('seller_token').value

  if (!token && !isOpen) {
    return navigateTo(`/login?redirect=${encodeURIComponent(to.path)}`, { replace: true })
  }
  if (token && open.includes(to.path)) {
    return navigateTo('/', { replace: true })
  }
})

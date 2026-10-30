// Market is a public storefront: browsing products, categories, product detail
// and the cart stay OPEN to signed-out visitors. Only account, checkout and
// order pages require a buyer session. Cookie-based so it works on SSR.
const PROTECTED_PREFIXES = ['/account', '/checkout', '/orders']

export default defineNuxtRouteMiddleware((to) => {
  const token = useCookie('market_token').value

  const needsAuth = PROTECTED_PREFIXES.some(p => to.path === p || to.path.startsWith(p + '/'))

  if (to.path.startsWith('/otp-validation')) return
  if (to.path === '/payment-status') return

  if (needsAuth && !token) {
    return navigateTo(`/login?redirect=${encodeURIComponent(to.path)}`, { replace: true })
  }
  if (token && (to.path === '/login' || to.path === '/signup')) {
    return navigateTo('/', { replace: true })
  }
})

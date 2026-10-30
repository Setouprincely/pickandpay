export default defineNuxtRouteMiddleware((to) => {
  const open = ['/login', '/signup', '/forgot-password']
  const isOpen = open.includes(to.path) || to.path.startsWith('/staff-setup')

  const token = useCookie('driver_token').value

  if (!token && !isOpen) {
    return navigateTo('/login', { replace: true })
  }
  if (token && to.path === '/login') {
    return navigateTo('/', { replace: true })
  }
})
export default defineNuxtRouteMiddleware((to) => {
  const isLoginPage = to.path === '/login'
  const isSignupPage = to.path === '/signup'
  const isStaffSetup = to.path.startsWith('/staff-setup')
  const isForgotPassword = to.path === '/forgot-password'
  const isPublicPage = isLoginPage || isSignupPage || isStaffSetup || isForgotPassword
  const token = useCookie('auth_token').value

  if (!token && !isPublicPage) {
    return navigateTo('/login', { replace: true })
  }
  if (token && isLoginPage) {
    return navigateTo('/', { replace: true })
  }
})
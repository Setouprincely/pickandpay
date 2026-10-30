<script setup lang="ts">
useHead({ title: 'Sign In — Doch Market' })

const { notify } = useNotify()
const { login } = useAuth()

const form = reactive({ email: '', password: '', remember: true })
const showPassword = ref(false)
const submitted = ref(false)
const signing = ref(false)

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))
const isValid = computed(() => emailValid.value && form.password.length >= 1)

function fieldError(condition: boolean) {
  return { 'field--error': submitted.value && condition }
}

// Derive a friendly name from the email local-part, or reuse a previously
// registered profile if one is saved.
function resolveName(email: string): string {
  if (import.meta.client) {
    try {
      const saved = JSON.parse(localStorage.getItem('market-user') || 'null')
      if (saved?.email === email && saved?.name) return saved.name
    } catch { /* ignore */ }
  }
  const local = email.split('@')[0].replace(/[._-]+/g, ' ')
  return local.replace(/\b\w/g, c => c.toUpperCase()) || 'Shopper'
}

async function signIn() {
  submitted.value = true
  if (!isValid.value) {
    notify({ title: 'Check your details', message: 'Enter a valid email and your password.', variant: 'error' })
    return
  }

  signing.value = true
  const email = form.email.trim()
  try {
    // Real backend login; sets the market_token cookie used by the route guard.
    await login(email, form.password)
    notify({ title: 'Welcome back!', message: `Signed in as ${email}.`, variant: 'success' })
    const redirect = (useRoute().query.redirect as string) || '/'
    await navigateTo(redirect)
  } catch (e) {
    const err = e as any
    const msg = err?.status === 401 || err?.status === 403
      ? 'Wrong email or password, or your account is not yet verified.'
      : err?.message || 'Could not sign in. Please try again.'
    notify({ title: 'Sign in failed', message: msg, variant: 'error' })
  } finally {
    signing.value = false
  }
}
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: 'Sign In' }]" />

    <section class="lg">
      <div class="lg__card">
        <!-- Brand panel -->
        <aside class="lg-brand">
          <div class="lg-brand__top">
            <NuxtLink to="/" class="lg-brand__logo">
              <img src="/images/logos/Logo-03.png" alt="Doch Market" class="lg-brand__logo-img" />
            </NuxtLink>
            <h2 class="lg-brand__headline">Welcome back.</h2>
            <p class="lg-brand__sub">
              Sign in to pick up where you left off — your cart, wishlist and orders are waiting.
            </p>
          </div>

          <div class="lg-quote">
            <UIcon name="i-heroicons-chat-bubble-bottom-center-text" class="lg-quote__mark" />
            <p class="lg-quote__text">“Fast delivery and unbeatable prices. Doch Market is my go-to for everything.”</p>
            <p class="lg-quote__by">— Amina, Douala</p>
          </div>

          <p class="lg-brand__foot">
            <UIcon name="i-heroicons-lock-closed" /> Encrypted, secure sign-in.
          </p>
        </aside>

        <!-- Form panel -->
        <div class="lg-form-wrap">
          <header class="lg-head">
            <h1 class="lg-head__title">Sign in to your account</h1>
            <p class="lg-head__sub">
              New here?
              <NuxtLink to="/signup" class="lg-link">Create an account</NuxtLink>
            </p>
          </header>

          <div class="lg-social">
            <button type="button" class="lg-social__btn" @click="notify({ title: 'Coming soon', message: 'Social sign-in will be available shortly.', variant: 'info' })">
              <UIcon name="i-simple-icons-google" class="lg-social__icon" /> Google
            </button>
            <button type="button" class="lg-social__btn" @click="notify({ title: 'Coming soon', message: 'Social sign-in will be available shortly.', variant: 'info' })">
              <UIcon name="i-simple-icons-facebook" class="lg-social__icon lg-social__icon--fb" /> Facebook
            </button>
          </div>

          <div class="lg-divider"><span>or sign in with email</span></div>

          <form class="lg-form" @submit.prevent="signIn">
            <label class="field" :class="fieldError(!emailValid)">
              <span class="field__label">Email</span>
              <div class="field__control">
                <UIcon name="i-heroicons-envelope" class="field__lead" />
                <input v-model="form.email" type="email" class="field__input field__input--icon" placeholder="you@example.com" autocomplete="email" />
              </div>
            </label>

            <label class="field" :class="fieldError(form.password.length < 1)">
              <span class="field__label">Password</span>
              <div class="field__control">
                <UIcon name="i-heroicons-lock-closed" class="field__lead" />
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="field__input field__input--icon field__input--trail"
                  placeholder="Your password"
                  autocomplete="current-password"
                />
                <button type="button" class="field__toggle" :aria-label="showPassword ? 'Hide password' : 'Show password'" @click="showPassword = !showPassword">
                  <UIcon :name="showPassword ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
                </button>
              </div>
            </label>

            <div class="lg-row">
              <label class="lg-remember">
                <input v-model="form.remember" type="checkbox" class="lg-remember__box" />
                <span>Remember me</span>
              </label>
              <NuxtLink to="/forgot-password" class="lg-link">
                Forgot password?
              </NuxtLink>
            </div>

            <button type="submit" class="lg-submit" :disabled="signing">
              <UIcon v-if="!signing" name="i-heroicons-arrow-right-on-rectangle" class="lg-submit__icon" />
              <UIcon v-else name="i-heroicons-arrow-path" class="lg-submit__icon lg-submit__icon--spin" />
              {{ signing ? 'Signing in…' : 'SIGN IN' }}
            </button>
          </form>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.lg { background: #F9FAFB; }
.lg__card { max-width: 1080px; margin: 0 auto; padding: 2rem 1.5rem 3.5rem; display: grid; grid-template-columns: 1fr 1fr; }
.lg__card > * { min-width: 0; }

/* Brand panel */
.lg-brand {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #E8202A 0%, #B3151D 100%);
  color: #fff; border-radius: 0.75rem 0 0 0.75rem;
  padding: 2.5rem 2.25rem; display: flex; flex-direction: column; justify-content: space-between;
}
.lg-brand::before, .lg-brand::after { content: ''; position: absolute; border-radius: 50%; background: rgba(255,255,255,0.08); pointer-events: none; }
.lg-brand::before { width: 16rem; height: 16rem; top: -5rem; right: -5rem; }
.lg-brand::after { width: 11rem; height: 11rem; bottom: -4rem; left: -3rem; }
.lg-brand__top { position: relative; z-index: 1; }
.lg-brand__logo { display: inline-flex; align-items: center; }
.lg-brand__logo-img { height: 2.6rem; width: auto; display: block; }
.lg-brand__headline { font-size: 1.65rem; font-weight: 800; line-height: 1.2; margin: 1.75rem 0 0.75rem; }
.lg-brand__sub { font-size: 0.875rem; line-height: 1.6; color: rgba(255,255,255,0.85); margin: 0; }

.lg-quote { position: relative; z-index: 1; background: rgba(255,255,255,0.1); border-radius: 0.75rem; padding: 1.25rem 1.25rem 1rem; margin: 2rem 0; }
.lg-quote__mark { width: 1.5rem; height: 1.5rem; opacity: 0.6; margin-bottom: 0.4rem; }
.lg-quote__text { font-size: 0.9rem; line-height: 1.55; margin: 0 0 0.6rem; font-style: italic; }
.lg-quote__by { font-size: 0.75rem; font-weight: 600; color: rgba(255,255,255,0.8); margin: 0; }

.lg-brand__foot { position: relative; z-index: 1; display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; color: rgba(255,255,255,0.75); margin: 0; }
.lg-brand__foot :deep(svg) { width: 0.95rem; height: 0.95rem; }

/* Form panel */
.lg-form-wrap { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-left: none; border-radius: 0 0.75rem 0.75rem 0; padding: 2.5rem 2.25rem; }
.lg-head__title { font-size: 1.5rem; font-weight: 800; color: #111827; margin: 0 0 0.35rem; }
.lg-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; }
.lg-link { color: var(--color-primary, #E8202A); font-weight: 600; text-decoration: none; }
.lg-link:hover { text-decoration: underline; }

.lg-social { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.lg-social__btn { display: flex; align-items: center; justify-content: center; gap: 0.5rem; border: 1px solid var(--color-border, #E5E7EB); background: #fff; border-radius: 0.5rem; padding: 0.65rem; font-size: 0.8125rem; font-weight: 600; color: #374151; cursor: pointer; transition: all 0.15s; }
.lg-social__btn:hover { border-color: #9CA3AF; background: #F9FAFB; }
.lg-social__icon { width: 1.1rem; height: 1.1rem; color: #EA4335; }
.lg-social__icon--fb { color: #1877F2; }

.lg-divider { display: flex; align-items: center; gap: 0.875rem; margin: 1.5rem 0; }
.lg-divider::before, .lg-divider::after { content: ''; flex: 1; height: 1px; background: var(--color-border, #E5E7EB); }
.lg-divider span { font-size: 0.6875rem; font-weight: 600; color: #9CA3AF; text-transform: uppercase; letter-spacing: 0.04em; }

.lg-form { display: flex; flex-direction: column; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__control { position: relative; display: flex; align-items: center; }
.field__lead { position: absolute; left: 0.7rem; width: 1.05rem; height: 1.05rem; color: #9CA3AF; pointer-events: none; }
.field__input { width: 100%; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 0.65rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; transition: border-color 0.15s, box-shadow 0.15s; }
.field__input--icon { padding-left: 2.35rem; }
.field__input--trail { padding-right: 2.6rem; }
.field__input:focus { border-color: var(--color-primary, #E8202A); box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.field--error .field__input { border-color: var(--color-primary, #E8202A); background: #FEF2F2; }
.field__toggle { position: absolute; right: 0.5rem; display: flex; background: none; border: none; cursor: pointer; color: #9CA3AF; padding: 0.35rem; }
.field__toggle:hover { color: #4B5563; }
.field__toggle :deep(svg) { width: 1.15rem; height: 1.15rem; }

.lg-row { display: flex; align-items: center; justify-content: space-between; font-size: 0.8125rem; }
.lg-remember { display: flex; align-items: center; gap: 0.45rem; color: #4B5563; cursor: pointer; }
.lg-remember__box { width: 1rem; height: 1rem; accent-color: var(--color-primary, #E8202A); }

.lg-submit { margin-top: 0.25rem; display: flex; align-items: center; justify-content: center; gap: 0.5rem; background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; letter-spacing: 0.02em; cursor: pointer; transition: background 0.15s; }
.lg-submit:hover:not(:disabled) { background: var(--color-primary-hover, #C8181F); }
.lg-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.lg-submit__icon { width: 1.1rem; height: 1.1rem; }
.lg-submit__icon--spin { animation: lg-spin 0.8s linear infinite; }
@keyframes lg-spin { to { transform: rotate(360deg); } }

@media (max-width: 820px) {
  .lg__card { grid-template-columns: 1fr; max-width: 440px; }
  .lg-brand { border-radius: 0.75rem 0.75rem 0 0; padding: 2rem; }
  .lg-brand__headline { font-size: 1.4rem; margin-top: 1.25rem; }
  .lg-quote { margin: 1.5rem 0; }
  .lg-form-wrap { border-left: 1px solid var(--color-border, #E5E7EB); border-top: none; border-radius: 0 0 0.75rem 0.75rem; padding: 2rem; }
}
@media (max-width: 460px) { .lg-social { grid-template-columns: 1fr; } }
</style>

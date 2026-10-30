<script setup lang="ts">
useHead({ title: 'Sign In — DOCH Xender' })

const { notify } = useNotify()
const { login } = useAuth()
const { $t } = useNuxtApp()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const submitted = ref(false)
const signing = ref(false)

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()))
const isValid = computed(() => emailValid.value && password.value.length >= 1)

async function signIn() {
  submitted.value = true
  if (!isValid.value) {
    notify({ title: 'Check your details', message: 'Enter a valid email and your password.', variant: 'error' })
    return
  }
  signing.value = true
  try {
    await login(email.value.trim(), password.value)
    notify({ title: 'Welcome back!', variant: 'success' })
    const redirect = (useRoute().query.redirect as string) || '/'
    await navigateTo(redirect)
  } catch (e: any) {
    const msg = e?.status === 401 || e?.status === 403
      ? 'Wrong email or password, or your account is not yet verified.'
      : e?.message || 'Could not sign in. Please try again.'
    notify({ title: 'Sign in failed', message: msg, variant: 'error' })
  } finally {
    signing.value = false
  }
}
</script>

<template>
  <section class="au">
    <div class="au__card">
      <aside class="au-brand">
        <div class="au-brand__top">
          <NuxtLink to="/" class="au-brand__logo">
            <img src="/images/logos/Logo-03.png" alt="DOCH Xender" class="au-brand__logo-img" />
          </NuxtLink>
          <h2 class="au-brand__headline">Welcome back.</h2>
          <p class="au-brand__sub">
            Sign in to send a parcel or track one that's already on its way.
          </p>
        </div>

        <div class="au-quote">
          <UIcon name="i-heroicons-truck" class="au-quote__mark" />
          <p class="au-quote__text">"I snapped my package, paid, and it reached my brother across town the same day."</p>
          <p class="au-quote__by">- Eric, Yaounde</p>
        </div>

        <p class="au-brand__foot">
          <UIcon name="i-heroicons-lock-closed" /> Encrypted, secure sign-in.
        </p>
      </aside>

      <div class="au-form-wrap">
        <header class="au-head">
          <h1 class="au-head__title">Sign in to your account</h1>
          <p class="au-head__sub">
            New here?
            <NuxtLink to="/signup" class="au-link">Create an account</NuxtLink>
          </p>
        </header>

        <form class="au-form" @submit.prevent="signIn">
          <label class="field" :class="{ 'field--error': submitted && !emailValid }">
            <span class="field__label">Email</span>
            <div class="field__control">
              <UIcon name="i-heroicons-envelope" class="field__lead" />
              <input v-model="email" type="email" class="field__input field__input--icon" placeholder="you@example.com" autocomplete="email" />
            </div>
          </label>

          <label class="field" :class="{ 'field--error': submitted && password.length < 1 }">
            <span class="field__label">Password</span>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input v-model="password" :type="showPassword ? 'text' : 'password'" class="field__input field__input--icon" placeholder="Your password" autocomplete="current-password" />
              <button type="button" class="field__toggle" @click="showPassword = !showPassword">
                <UIcon :name="showPassword ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
              </button>
            </div>
          </label>

          <div class="au-row">
            <NuxtLink to="/forgot-password" class="au-link">Forgot password?</NuxtLink>
          </div>

          <button type="submit" class="au-submit" :disabled="signing">
            <UIcon v-if="!signing" name="i-heroicons-arrow-right-on-rectangle" class="au-submit__icon" />
            <UIcon v-else name="i-heroicons-arrow-path" class="au-submit__icon au-submit__icon--spin" />
            {{ signing ? 'Signing in...' : 'Sign in' }}
          </button>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.au { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 2rem 1.5rem; }
.au__card { width: 100%; max-width: 1000px; display: grid; grid-template-columns: 1fr 1fr; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.2); border-radius: 0.85rem; }
.au__card > * { min-width: 0; }

.au-brand { position: relative; overflow: hidden; background: linear-gradient(150deg, #E8202A 0%, #B3151D 100%); color: #fff; border-radius: 0.85rem 0 0 0.85rem; padding: 2.5rem 2.25rem; display: flex; flex-direction: column; justify-content: space-between; }
.au-brand::before, .au-brand::after { content: ''; position: absolute; border-radius: 50%; background: rgba(255,255,255,0.08); pointer-events: none; }
.au-brand::before { width: 16rem; height: 16rem; top: -5rem; right: -5rem; }
.au-brand::after { width: 11rem; height: 11rem; bottom: -4rem; left: -3rem; }
.au-brand__top { position: relative; z-index: 1; }
.au-brand__logo { display: inline-flex; }
.au-brand__logo-img { height: 2.6rem; width: auto; display: block; }
.au-brand__headline { font-size: 1.65rem; font-weight: 800; line-height: 1.2; margin: 1.75rem 0 0.75rem; }
.au-brand__sub { font-size: 0.875rem; line-height: 1.6; color: rgba(255,255,255,0.85); margin: 0; }

.au-quote { position: relative; z-index: 1; background: rgba(255,255,255,0.1); border-radius: 0.75rem; padding: 1.25rem; margin: 2rem 0; }
.au-quote__mark { width: 1.5rem; height: 1.5rem; opacity: 0.7; margin-bottom: 0.4rem; }
.au-quote__text { font-size: 0.9rem; line-height: 1.55; margin: 0 0 0.6rem; font-style: italic; }
.au-quote__by { font-size: 0.75rem; font-weight: 600; color: rgba(255,255,255,0.8); margin: 0; }

.au-brand__foot { position: relative; z-index: 1; display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; color: rgba(255,255,255,0.75); margin: 0; }
.au-brand__foot :deep(svg) { width: 0.95rem; height: 0.95rem; }

.au-form-wrap { background: #fff; border: 1px solid #E5E7EB; border-left: none; border-radius: 0 0.85rem 0.85rem 0; padding: 2.5rem 2.25rem; display: flex; flex-direction: column; justify-content: center; }
.au-head__title { font-size: 1.5rem; font-weight: 800; color: #111827; margin: 0 0 0.35rem; }
.au-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.75rem; }
.au-link { color: #E8202A; font-weight: 600; text-decoration: none; background: none; border: none; cursor: pointer; padding: 0; font-size: inherit; }
.au-link:hover:not(:disabled) { text-decoration: underline; }
.au-link:disabled { color: #9CA3AF; cursor: not-allowed; }

.au-form { display: flex; flex-direction: column; gap: 1.1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__control { position: relative; display: flex; align-items: center; }
.field__lead { position: absolute; left: 0.7rem; width: 1.05rem; height: 1.05rem; color: #9CA3AF; pointer-events: none; }
.field__prefix { position: absolute; left: 0.75rem; font-size: 0.875rem; font-weight: 600; color: #6B7280; pointer-events: none; }
.field__input { width: 100%; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.7rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; transition: border-color 0.15s, box-shadow 0.15s; }
.field__input--icon { padding-left: 2.35rem; }
.field__input--prefix { padding-left: 3.1rem; }
.field__input:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.field--error .field__input { border-color: #E8202A; background: #FEF2F2; }
.field__hint { font-size: 0.72rem; color: #9CA3AF; margin: 0.1rem 0 0; }
.field__err { font-size: 0.72rem; color: #E8202A; font-weight: 600; margin: 0.1rem 0 0; }

.au-otp-head { display: flex; align-items: baseline; justify-content: space-between; gap: 0.5rem; }
.au-otp-head__text { font-size: 0.82rem; color: #4B5563; margin: 0; }
.au-otp-head__edit { font-size: 0.78rem; flex-shrink: 0; }

.au-submit { margin-top: 0.25rem; display: flex; align-items: center; justify-content: center; gap: 0.5rem; background: #E8202A; color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; letter-spacing: 0.02em; cursor: pointer; transition: background 0.15s; }
.au-submit:hover:not(:disabled) { background: #C8181F; }
.au-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.au-submit__icon { width: 1.1rem; height: 1.1rem; }
.au-submit__icon--spin { animation: au-spin 0.8s linear infinite; }
@keyframes au-spin { to { transform: rotate(360deg); } }

.au-resend { text-align: center; font-size: 0.8rem; color: #6B7280; margin: 0.25rem 0 0; }

@media (max-width: 820px) {
  .au__card { grid-template-columns: 1fr; max-width: 460px; }
  .au-brand { border-radius: 0.85rem 0.85rem 0 0; padding: 2rem; }
  .au-brand__headline { font-size: 1.4rem; margin-top: 1.25rem; }
  .au-quote { margin: 1.5rem 0; }
  .au-form-wrap { border-left: 1px solid #E5E7EB; border-top: none; border-radius: 0 0 0.85rem 0.85rem; padding: 2rem; }
}

.au-row { display: flex; justify-content: flex-end; margin: -0.25rem 0 0.25rem; }
.field__toggle { background: none; border: none; cursor: pointer; color: #9CA3AF; padding: 0 0.25rem; }
.field__toggle :deep(svg) { width: 1.1rem; height: 1.1rem; }
</style>

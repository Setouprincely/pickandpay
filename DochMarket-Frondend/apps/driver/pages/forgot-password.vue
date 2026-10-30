<script setup lang="ts">
definePageMeta({ layout: false })

const { forgotPassword, resetPassword } = useAuth()

const step = ref<'request' | 'reset'>('request')
const email = ref('')
const otp = ref('')
const newPassword = ref('')
const confirm = ref('')
const loading = ref(false)
const error = ref('')
const info = ref('')

async function requestCode() {
  error.value = ''
  info.value = ''
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    error.value = 'Enter a valid email.'
    return
  }
  loading.value = true
  try {
    await forgotPassword(email.value.trim())
  } catch {
    // Don't reveal whether the email exists.
  } finally {
    loading.value = false
  }
  info.value = 'If that email exists, a reset code has been sent.'
  step.value = 'reset'
}

async function submitReset() {
  error.value = ''
  if (!otp.value.trim() || newPassword.value.length < 6) {
    error.value = 'Enter your code and a password of at least 6 characters.'
    return
  }
  if (newPassword.value !== confirm.value) {
    error.value = 'Passwords do not match.'
    return
  }
  loading.value = true
  try {
    await resetPassword(email.value.trim(), otp.value.trim(), newPassword.value)
    info.value = 'Password updated. Redirecting you to sign in…'
    setTimeout(() => navigateTo('/login'), 1200)
  } catch (e: any) {
    error.value = e?.data?.message || 'That code is invalid or has expired.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="su-page">
    <div class="su-card su-card--single">
      <div class="su-form-wrap">
        <header class="su-head">
          <h1 class="su-head__title">Reset your password</h1>
          <p class="su-head__sub" v-if="step === 'request'">
            Enter your driver account email and we'll send you a reset code.
          </p>
          <p class="su-head__sub" v-else>
            Enter the code sent to <strong>{{ email }}</strong> and choose a new password.
          </p>
        </header>

        <form v-if="step === 'request'" class="su-form" @submit.prevent="requestCode">
          <label class="field field--wide">
            <span class="field__label">Email</span>
            <div class="field__control">
              <input v-model="email" type="email" class="field__input" placeholder="you@example.com" />
            </div>
          </label>
          <p v-if="error" class="su-error field--wide">{{ error }}</p>
          <button type="submit" class="su-submit field--wide" :disabled="loading">
            {{ loading ? 'Sending…' : 'Send reset code' }}
          </button>
        </form>

        <form v-else class="su-form" @submit.prevent="submitReset">
          <label class="field field--wide">
            <span class="field__label">Reset code</span>
            <div class="field__control">
              <input v-model="otp" type="text" class="field__input" placeholder="Enter code" />
            </div>
          </label>
          <label class="field field--wide">
            <span class="field__label">New password</span>
            <div class="field__control">
              <input v-model="newPassword" type="password" class="field__input" placeholder="••••••••" />
            </div>
          </label>
          <label class="field field--wide">
            <span class="field__label">Confirm password</span>
            <div class="field__control">
              <input v-model="confirm" type="password" class="field__input" placeholder="••••••••" />
            </div>
          </label>
          <p v-if="info" class="su-info field--wide">{{ info }}</p>
          <p v-if="error" class="su-error field--wide">{{ error }}</p>
          <button type="submit" class="su-submit field--wide" :disabled="loading">
            {{ loading ? 'Updating…' : 'Reset password' }}
          </button>
        </form>

        <p class="su-foot">
          <NuxtLink to="/login" class="su-link">Back to sign in</NuxtLink>
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.su-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 1.5rem; }
.su-card--single { width: 100%; max-width: 460px; border-radius: 0.85rem; overflow: hidden; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.2); background: #fff; }
.su-form-wrap { padding: 2.5rem 2.25rem; }
.su-head__title { font-size: 1.4rem; font-weight: 800; color: #111827; margin: 0 0 0.5rem; }
.su-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; line-height: 1.55; }
.su-form { display: flex; flex-direction: column; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field--wide { grid-column: 1 / -1; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__input { width: 100%; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.65rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; }
.field__input:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.su-error { font-size: 0.75rem; font-weight: 600; color: #E8202A; margin: -0.25rem 0 0; }
.su-info { font-size: 0.75rem; font-weight: 600; color: #16A34A; margin: -0.25rem 0 0; }
.su-submit { margin-top: 0.5rem; background: #E8202A; color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.su-submit:hover:not(:disabled) { background: #C8181F; }
.su-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.su-foot { text-align: center; font-size: 0.825rem; color: #6B7280; margin: 1.5rem 0 0; }
.su-link { color: #E8202A; font-weight: 600; text-decoration: none; }
.su-link:hover { text-decoration: underline; }
</style>

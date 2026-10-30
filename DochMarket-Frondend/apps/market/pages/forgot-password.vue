<script setup lang="ts">
definePageMeta({ layout: false })
useHead({ title: 'Reset Password — Doch Market' })

const { notify } = useNotify()
const { forgotPassword, resetPassword } = useAuth()

const step = ref<'request' | 'reset'>('request')
const email = ref('')
const otp = ref('')
const newPassword = ref('')
const confirm = ref('')
const loading = ref(false)

async function requestCode() {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    notify({ title: 'Enter a valid email', variant: 'error' })
    return
  }
  loading.value = true
  try {
    await forgotPassword(email.value.trim())
  } catch {
    // Don't reveal whether the email exists — same message either way.
  } finally {
    loading.value = false
  }
  notify({ title: 'Code sent', message: 'If that email exists, a reset code has been sent.', variant: 'info' })
  step.value = 'reset'
}

async function submitReset() {
  if (!otp.value.trim() || newPassword.value.length < 6) {
    notify({ title: 'Check the form', message: 'Enter your code and a password of at least 6 characters.', variant: 'error' })
    return
  }
  if (newPassword.value !== confirm.value) {
    notify({ title: 'Passwords do not match', variant: 'error' })
    return
  }
  loading.value = true
  try {
    await resetPassword(email.value.trim(), otp.value.trim(), newPassword.value)
    notify({ title: 'Password updated', message: 'You can now sign in with your new password.', variant: 'success' })
    setTimeout(() => navigateTo('/login'), 1200)
  } catch (e: any) {
    notify({ title: 'Could not reset password', message: e?.data?.message || 'That code is invalid or has expired.', variant: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="ov-page">
    <div class="ov-card">
      <img src="/images/logos/Logo-03.png" alt="Doch Market" class="ov-logo" />
      <h1 class="ov-title">Reset your password</h1>
      <p class="ov-sub" v-if="step === 'request'">
        Enter your account email and we'll send you a reset code.
      </p>
      <p class="ov-sub" v-else>
        Enter the code sent to <strong>{{ email }}</strong> and choose a new password.
      </p>

      <form v-if="step === 'request'" class="ov-form" @submit.prevent="requestCode">
        <input v-model="email" type="email" class="ov-text-input" placeholder="you@example.com" />
        <button type="submit" class="ov-submit" :disabled="loading">
          {{ loading ? 'Sending…' : 'Send reset code' }}
        </button>
      </form>

      <form v-else class="ov-form" @submit.prevent="submitReset">
        <input v-model="otp" type="text" class="ov-input" placeholder="Enter code" autocomplete="one-time-code" />
        <input v-model="newPassword" type="password" class="ov-text-input" placeholder="New password" />
        <input v-model="confirm" type="password" class="ov-text-input" placeholder="Confirm password" />
        <button type="submit" class="ov-submit" :disabled="loading">
          {{ loading ? 'Updating…' : 'Reset password' }}
        </button>
      </form>

      <NuxtLink to="/login" class="ov-back">Back to sign in</NuxtLink>
    </div>
    <MarketToaster />
  </div>
</template>

<style scoped>
.ov-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 2rem 1.5rem; }
.ov-card { width: 100%; max-width: 420px; background: #fff; border: 1px solid #E5E7EB; border-radius: 0.85rem; padding: 2.25rem 2rem; text-align: center; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.15); }
.ov-logo { height: 2.4rem; margin: 0 auto 1.25rem; display: block; }
.ov-title { font-size: 1.35rem; font-weight: 800; color: #111827; margin: 0 0 0.5rem; }
.ov-sub { font-size: 0.875rem; color: #6B7280; line-height: 1.55; margin: 0 0 1.5rem; }
.ov-form { display: flex; flex-direction: column; gap: 0.75rem; }
.ov-input { width: 100%; text-align: center; letter-spacing: 0.3em; font-size: 1.1rem; font-weight: 700; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.75rem; outline: none; }
.ov-text-input { width: 100%; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.75rem; outline: none; font-size: 0.9rem; }
.ov-input:focus, .ov-text-input:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.ov-submit { background: #E8202A; color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.ov-submit:hover:not(:disabled) { background: #C8181F; }
.ov-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.ov-back { display: inline-block; margin-top: 1.25rem; font-size: 0.8125rem; color: #6B7280; text-decoration: none; }
.ov-back:hover { text-decoration: underline; }
</style>

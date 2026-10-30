<script setup lang="ts">
useHead({ title: 'Reset Password — DOCH Xender' })

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
    notify({ title: 'Code sent', message: 'Check your inbox for a reset code.', variant: 'success' })
    step.value = 'reset'
  } catch (e: any) {
    // Don't reveal whether the email exists — same message either way.
    notify({ title: 'Code sent', message: 'If that email exists, a reset code has been sent.', variant: 'info' })
    step.value = 'reset'
  } finally {
    loading.value = false
  }
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
  <section class="au">
    <div class="au__card au__card--single">
      <div class="au-form-wrap">
        <header class="au-head">
          <h1 class="au-head__title">Reset your password</h1>
          <p class="au-head__sub" v-if="step === 'request'">
            Enter your account email and we'll send you a reset code.
          </p>
          <p class="au-head__sub" v-else>
            Enter the code sent to <strong>{{ email }}</strong> and choose a new password.
          </p>
        </header>

        <form v-if="step === 'request'" class="au-form" @submit.prevent="requestCode">
          <label class="field">
            <span class="field__label">Email</span>
            <div class="field__control">
              <input v-model="email" type="email" class="field__input" placeholder="you@example.com" />
            </div>
          </label>
          <button type="submit" class="au-submit" :disabled="loading">
            {{ loading ? 'Sending…' : 'Send reset code' }}
          </button>
        </form>

        <form v-else class="au-form" @submit.prevent="submitReset">
          <label class="field">
            <span class="field__label">Reset code</span>
            <div class="field__control">
              <input v-model="otp" type="text" class="field__input" placeholder="Enter code" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">New password</span>
            <div class="field__control">
              <input v-model="newPassword" type="password" class="field__input" placeholder="••••••••" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">Confirm password</span>
            <div class="field__control">
              <input v-model="confirm" type="password" class="field__input" placeholder="••••••••" />
            </div>
          </label>
          <button type="submit" class="au-submit" :disabled="loading">
            {{ loading ? 'Updating…' : 'Reset password' }}
          </button>
        </form>

        <p class="au-head__sub">
          <NuxtLink to="/login" class="au-link">Back to sign in</NuxtLink>
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.au { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 2rem 1.5rem; }
.au__card--single { width: 100%; max-width: 420px; border-radius: 0.85rem; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.2); background: #fff; }
.au-form-wrap { padding: 2.25rem 2rem; text-align: center; }
.au-head__title { font-size: 1.4rem; font-weight: 800; color: #111827; margin: 0 0 0.5rem; }
.au-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; line-height: 1.55; }
.au-link { color: #E8202A; font-weight: 600; text-decoration: none; }
.au-link:hover { text-decoration: underline; }
.au-form { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; text-align: left; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__input { width: 100%; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.75rem; outline: none; color: #111827; }
.field__input:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.au-submit { background: #E8202A; color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.au-submit:hover:not(:disabled) { background: #C8181F; }
.au-submit:disabled { opacity: 0.7; cursor: not-allowed; }
</style>

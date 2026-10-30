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
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">
        <img src="/images/logos/Logo-01.png" alt="Doch" class="logo-img" onerror="this.style.display='none'" />
      </div>
      <h2 class="login-title">Reset your password</h2>
      <p class="login-sub" v-if="step === 'request'">Enter your account email and we'll send you a reset code.</p>
      <p class="login-sub" v-else>Enter the code sent to <strong>{{ email }}</strong> and choose a new password.</p>

      <div v-if="step === 'request'" class="login-form">
        <div class="form-group">
          <label>Email</label>
          <UInput v-model="email" type="email" placeholder="you@example.com" size="md" :disabled="loading" />
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <UButton class="login-btn" :loading="loading" block @click="requestCode">Send reset code</UButton>
      </div>

      <div v-else class="login-form">
        <div class="form-group">
          <label>Reset code</label>
          <UInput v-model="otp" placeholder="Enter code" size="md" :disabled="loading" />
        </div>
        <div class="form-group">
          <label>New password</label>
          <UInput v-model="newPassword" type="password" placeholder="••••••••" size="md" :disabled="loading" />
        </div>
        <div class="form-group">
          <label>Confirm password</label>
          <UInput v-model="confirm" type="password" placeholder="••••••••" size="md" :disabled="loading" />
        </div>
        <p v-if="info" class="info-msg">{{ info }}</p>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <UButton class="login-btn" :loading="loading" block @click="submitReset">Reset password</UButton>
      </div>

      <p class="login-foot">
        <NuxtLink to="/login" class="login-link">Back to sign in</NuxtLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; }
.login-card { background: white; border-radius: 16px; padding: 40px; width: 100%; max-width: 420px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
.login-logo { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; gap: 8px; }
.logo-img { height: 48px; }
.login-title { font-size: 22px; font-weight: 700; color: #111827; text-align: center; margin: 0 0 4px; }
.login-sub { font-size: 14px; color: #6B7280; text-align: center; margin: 0 0 28px; line-height: 1.5; }
.login-form { display: flex; flex-direction: column; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; font-weight: 500; color: #6B7280; }
.login-btn { background: #EF2A39 !important; margin-top: 8px; }
.error-msg { color: #EF2A39; font-size: 13px; text-align: center; margin: 0; }
.info-msg { color: #16A34A; font-size: 13px; text-align: center; margin: 0; }
.login-foot { text-align: center; font-size: 13px; color: #6B7280; margin: 24px 0 0; }
.login-link { color: #EF2A39; font-weight: 600; text-decoration: none; }
.login-link:hover { text-decoration: underline; }
:deep(input) { color: #111827 !important; }
</style>

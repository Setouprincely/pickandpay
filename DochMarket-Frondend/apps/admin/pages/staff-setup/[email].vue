<script setup lang="ts">
definePageMeta({ layout: false })

const route = useRoute()
const { completeStaffSetup } = useAuth()

const email = decodeURIComponent(String(route.params.email || ''))
const otp = ref('')
const config = useRuntimeConfig()
const resending = ref(false)
const resendNote = ref('')

// Invitation codes expire; ask the backend to email a fresh one.
async function resendCode() {
  resending.value = true
  resendNote.value = ''
  try {
    const res: any = await $fetch(`/api/auth/staff-setup/resend`, { method: 'POST', query: { email } })
    resendNote.value = res?.message || 'A new code has been sent to your email.'
  } catch (e: any) {
    resendNote.value = e?.data?.message || 'Could not send a new code. Please try again in a minute.'
  } finally {
    resending.value = false
  }
}
const newPassword = ref('')
const confirm = ref('')
const loading = ref(false)
const error = ref('')
const success = ref(false)

const passwordsMatch = computed(() => confirm.value.length > 0 && newPassword.value === confirm.value)

async function submit() {
  error.value = ''
  if (!otp.value.trim() || newPassword.value.length < 6) {
    error.value = 'Enter your invite code and a password of at least 6 characters.'
    return
  }
  if (!passwordsMatch.value) {
    error.value = 'Passwords do not match.'
    return
  }
  loading.value = true
  try {
    await completeStaffSetup(email, otp.value.trim(), newPassword.value)
    success.value = true
    setTimeout(() => navigateTo('/login'), 1500)
  } catch (e: any) {
    error.value = e?.data?.message || 'Could not activate this account. The code may be invalid or expired.'
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
      <h2 class="login-title">Activate your account</h2>
      <p class="login-sub">
        You were invited to join the DOCH team as <strong>{{ email }}</strong>. Enter the code from your
        invite email and set a permanent password.
      </p>

      <div v-if="!success" class="login-form">
        <div class="form-group">
          <label>Invite code</label>
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
        <p v-if="error" class="error-msg">{{ error }}</p>
        <UButton class="login-btn" :loading="loading" block @click="submit">
          Activate account
        </UButton>
      </div>

      <div v-else class="setup-success">
        <p>✅ Account activated. Redirecting you to sign in…</p>
      </div>

      <p class="login-foot">
        Code expired or missing?
        <a href="#" class="login-link" @click.prevent="resendCode">{{ resending ? 'Sending…' : 'Send me a new code' }}</a>
        <span v-if="resendNote" style="display:block;margin-top:0.35rem">{{ resendNote }}</span>
      </p>
      <p class="login-foot">
        Already activated? <NuxtLink to="/login" class="login-link">Sign in</NuxtLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; }
.login-card { background: white; border-radius: 16px; padding: 40px; width: 100%; max-width: 440px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
.login-logo { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; gap: 8px; }
.logo-img { height: 48px; }
.login-title { font-size: 22px; font-weight: 700; color: #111827; text-align: center; margin: 0 0 4px; }
.login-sub { font-size: 14px; color: #6B7280; text-align: center; margin: 0 0 28px; line-height: 1.5; }
.login-form { display: flex; flex-direction: column; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; font-weight: 500; color: #6B7280; }
.login-btn { background: #EF2A39 !important; margin-top: 8px; }
.error-msg { color: #EF2A39; font-size: 13px; text-align: center; margin: 0; }
.login-foot { text-align: center; font-size: 13px; color: #6B7280; margin: 24px 0 0; }
.login-link { color: #EF2A39; font-weight: 600; text-decoration: none; }
.login-link:hover { text-decoration: underline; }
.setup-success { text-align: center; font-size: 14px; color: #111827; padding: 12px 0; }
:deep(input) { color: #111827 !important; }
</style>

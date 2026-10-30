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
  <div class="su-page">
    <div class="su-card su-card--single">
      <div class="su-form-wrap">
        <header class="su-head">
          <h1 class="su-head__title">Activate your driver account</h1>
          <p class="su-head__sub">
            You were invited to drive for DOCH as <strong>{{ email }}</strong>. Enter the invitation code from your email
            and set a permanent password to get started.
          </p>
        </header>

        <form v-if="!success" class="su-form" @submit.prevent="submit">
          <label class="field field--wide">
            <span class="field__label">Invite code</span>
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

          <p v-if="error" class="su-error field--wide">{{ error }}</p>

          <button type="submit" class="su-submit field--wide" :disabled="loading">
            {{ loading ? 'Activating…' : 'Activate account' }}
          </button>
        </form>

        <p v-else class="su-success">✅ Account activated. Redirecting you to sign in…</p>

        <p class="su-foot">
          Code expired or missing?
          <a href="#" class="su-link" @click.prevent="resendCode">{{ resending ? 'Sending…' : 'Send me a new code' }}</a>
          <span v-if="resendNote" style="display:block;margin-top:0.35rem">{{ resendNote }}</span>
        </p>
        <p class="su-foot">
          Already activated? <NuxtLink to="/login" class="su-link">Sign in</NuxtLink>
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
.su-submit { margin-top: 0.5rem; background: #E8202A; color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.su-submit:hover:not(:disabled) { background: #C8181F; }
.su-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.su-success { text-align: center; font-size: 0.9rem; color: #111827; padding: 1rem 0; }
.su-foot { text-align: center; font-size: 0.825rem; color: #6B7280; margin: 1.5rem 0 0; }
.su-link { color: #E8202A; font-weight: 600; text-decoration: none; }
.su-link:hover { text-decoration: underline; }
</style>

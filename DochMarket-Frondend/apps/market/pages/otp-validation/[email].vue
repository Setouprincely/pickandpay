<script setup lang="ts">
// Email verification step after signup. Matches the link the backend emails:
// {frontendBaseUrl}/otp-validation/{email}?otp=xxxxxx
definePageMeta({ layout: false })
useHead({ title: 'Verify Your Account — Doch Market' })

const route = useRoute()
const { notify } = useNotify()
const { verifyOtp, resendOtp } = useAuth()

const email = decodeURIComponent(String(route.params.email || ''))
const otp = ref(String(route.query.otp || ''))
const verifying = ref(false)
const resending = ref(false)
const verified = ref(false)

async function submit() {
  if (otp.value.trim().length < 4) {
    notify({ title: 'Enter the code', message: 'Please enter the code from your email.', variant: 'error' })
    return
  }
  verifying.value = true
  try {
    await verifyOtp(email, otp.value.trim())
    verified.value = true
    notify({ title: 'Account verified!', message: 'You can now sign in.', variant: 'success' })
    setTimeout(() => navigateTo('/login'), 1200)
  } catch (e: any) {
    const msg = e?.data?.message || 'That code is invalid or has expired.'
    notify({ title: 'Verification failed', message: msg, variant: 'error' })
  } finally {
    verifying.value = false
  }
}

async function resend() {
  resending.value = true
  try {
    await resendOtp(email)
    notify({ title: 'Code sent', message: 'Check your inbox for a new code.', variant: 'success' })
  } catch (e: any) {
    notify({ title: 'Could not resend', message: e?.data?.message || 'Please try again shortly.', variant: 'error' })
  } finally {
    resending.value = false
  }
}

// Auto-submit if the link already carried a valid-looking OTP.
onMounted(() => {
  if (otp.value.trim().length >= 4) submit()
})
</script>

<template>
  <div class="ov-page">
    <div class="ov-card">
      <img src="/images/logos/Logo-03.png" alt="Doch Market" class="ov-logo" />
      <h1 class="ov-title">Verify your account</h1>
      <p class="ov-sub">
        We sent a verification code to <strong>{{ email }}</strong>. Enter it below to
        activate your account.
      </p>

      <form class="ov-form" @submit.prevent="submit">
        <input
          v-model="otp"
          type="text"
          inputmode="numeric"
          maxlength="8"
          class="ov-input"
          placeholder="Enter code"
          autocomplete="one-time-code"
        />
        <button type="submit" class="ov-submit" :disabled="verifying || verified">
          {{ verified ? 'Verified ✓' : verifying ? 'Verifying…' : 'Verify account' }}
        </button>
      </form>

      <p class="ov-resend">
        Didn't get a code?
        <button type="button" class="ov-link" :disabled="resending" @click="resend">
          {{ resending ? 'Sending…' : 'Resend code' }}
        </button>
      </p>

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
.ov-input:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.ov-submit { background: #E8202A; color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.ov-submit:hover:not(:disabled) { background: #C8181F; }
.ov-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.ov-resend { font-size: 0.8125rem; color: #6B7280; margin: 1.25rem 0 0; }
.ov-link { background: none; border: none; color: #E8202A; font-weight: 600; cursor: pointer; padding: 0; font-size: inherit; }
.ov-link:disabled { opacity: 0.6; cursor: not-allowed; }
.ov-back { display: inline-block; margin-top: 1rem; font-size: 0.8125rem; color: #6B7280; text-decoration: none; }
.ov-back:hover { text-decoration: underline; }
</style>

<script setup lang="ts">
definePageMeta({ layout: false })

const route = useRoute()
const { verifyOtp, resendOtp } = useAuth()
const { notify } = useNotify()

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
    notify({ title: 'Verification failed', message: e?.data?.message || 'That code is invalid or has expired.', variant: 'error' })
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

onMounted(() => {
  if (otp.value.trim().length >= 4) submit()
})
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">
        <img src="/images/logos/Logo-01.png" alt="Doch" class="logo-img" onerror="this.style.display='none'" />
      </div>
      <h2 class="login-title">Verify your account</h2>
      <p class="login-sub">We sent a code to <strong>{{ email }}</strong>. Enter it below.</p>

      <div class="login-form">
        <div class="form-group">
          <label>Verification code</label>
          <UInput v-model="otp" placeholder="Enter code" size="md" :disabled="verifying || verified" />
        </div>
        <UButton class="login-btn" :loading="verifying" block @click="submit">
          {{ verified ? 'Verified ✓' : 'Verify account' }}
        </UButton>
        <UButton variant="ghost" block :loading="resending" @click="resend">
          Resend code
        </UButton>
      </div>

      <p class="login-foot">
        <NuxtLink to="/login" class="login-link">Back to sign in</NuxtLink>
      </p>
    </div>

    <DochLanguageToggle />
    <SellerToaster />
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; }
.login-card { background: white; border-radius: 16px; padding: 40px; width: 100%; max-width: 420px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); text-align: center; }
.login-logo { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; gap: 8px; }
.logo-img { height: 48px; }
.login-title { font-size: 22px; font-weight: 700; color: #111827; text-align: center; margin: 0 0 4px; }
.login-sub { font-size: 14px; color: #6B7280; text-align: center; margin: 0 0 28px; }
.login-form { display: flex; flex-direction: column; gap: 12px; }
.form-group { display: flex; flex-direction: column; gap: 6px; text-align: left; }
.form-group label { font-size: 13px; font-weight: 500; color: #6B7280; }
.login-btn { background: #EF2A39 !important; margin-top: 8px; }
.login-foot { text-align: center; font-size: 13px; color: #6B7280; margin: 24px 0 0; }
.login-link { color: #EF2A39; font-weight: 600; text-decoration: none; }
.login-link:hover { text-decoration: underline; }
:deep(input) { color: #111827 !important; }
</style>

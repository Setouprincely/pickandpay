<script setup lang="ts">
useHead({ title: 'Verify Your Account — DOCH Xender' })

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
  <section class="au">
    <div class="au__card au__card--single">
      <div class="au-form-wrap">
        <header class="au-head">
          <h1 class="au-head__title">Verify your account</h1>
          <p class="au-head__sub">
            We sent a code to <strong>{{ email }}</strong>. Enter it below to activate your account.
          </p>
        </header>

        <form class="au-form" @submit.prevent="submit">
          <label class="field">
            <span class="field__label">Verification code</span>
            <div class="field__control">
              <input
                v-model="otp"
                type="text"
                inputmode="numeric"
                maxlength="8"
                class="field__input"
                placeholder="Enter code"
                autocomplete="one-time-code"
              />
            </div>
          </label>

          <button type="submit" class="au-submit" :disabled="verifying || verified">
            {{ verified ? 'Verified ✓' : verifying ? 'Verifying…' : 'Verify account' }}
          </button>
        </form>

        <p class="au-resend">
          Didn't get a code?
          <button type="button" class="au-link-btn" :disabled="resending" @click="resend">
            {{ resending ? 'Sending…' : 'Resend code' }}
          </button>
        </p>

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
.au-form { display: flex; flex-direction: column; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; text-align: left; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__input { width: 100%; text-align: center; letter-spacing: 0.3em; font-weight: 700; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.75rem; outline: none; }
.field__input:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.au-submit { background: #E8202A; color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.au-submit:hover:not(:disabled) { background: #C8181F; }
.au-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.au-resend { font-size: 0.8125rem; color: #6B7280; margin: 1.25rem 0 0; }
.au-link-btn { background: none; border: none; color: #E8202A; font-weight: 600; cursor: pointer; padding: 0; font-size: inherit; }
.au-link-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>

<script setup lang="ts">
useHead({ title: 'Create Account — DOCH Xender' })

const { notify } = useNotify()
const { register } = useAuth()

const form = reactive({
  firstName: '', lastName: '', email: '', phone: '',
  password: '', confirm: '',
})
const showPassword = ref(false)
const submitted = ref(false)
const creating = ref(false)

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))
const phoneValid = computed(() => form.phone.replace(/\D/g, '').length >= 8)
const passwordsMatch = computed(() => form.confirm.length > 0 && form.password === form.confirm)
// Strong password: 8+ chars with upper, lower, number, and a special character.
const passwordStrong = computed(() =>
  form.password.length >= 8 &&
  /[A-Z]/.test(form.password) &&
  /[a-z]/.test(form.password) &&
  /\d/.test(form.password) &&
  /[^A-Za-z0-9]/.test(form.password)
)
const isValid = computed(() =>
  form.firstName.trim() !== '' && form.lastName.trim() !== '' &&
  emailValid.value && phoneValid.value &&
  passwordStrong.value && passwordsMatch.value
)

async function createAccount() {
  submitted.value = true
  if (!isValid.value) {
    notify({ title: 'Check your details', message: 'Please complete all fields correctly.', variant: 'error' })
    return
  }
  creating.value = true
  try {
    const res: any = await register({
      email: form.email.trim(),
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      userName: form.email.trim().split('@')[0] ?? '',
      password: form.password,
      phone: form.phone.trim(),
      doB: '1990-01-01',
    })
    if (res?.existingAccount && !res.verificationRequired) {
      notify({ title: 'Access added', message: res.message, variant: 'success' })
      await navigateTo('/login')
      return
    }
    notify({
      title: 'Account created',
      message: 'Enter the code we emailed you to verify your account.',
      variant: 'success',
    })
    await navigateTo(`/otp-validation/${encodeURIComponent(form.email.trim())}`)
  } catch (e: any) {
    const serverMsg = e?.data?.message || e?.data
    const msg = e?.status === 403 || e?.status === 500
      ? `Couldn't create your account - server returned ${e.status}.` + (serverMsg ? ' ' + serverMsg : '')
      : (serverMsg || e?.message || 'Could not create your account. Please try again.')
    notify({ title: 'Registration failed', message: msg, variant: 'error' })
  } finally {
    creating.value = false
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
          <h2 class="au-brand__headline">Send anything, anywhere.</h2>
          <p class="au-brand__sub">Create an account to send parcels across town and track every step.</p>
        </div>
        <p class="au-brand__foot">
          <UIcon name="i-heroicons-lock-closed" /> Encrypted, secure sign-up.
        </p>
      </aside>

      <div class="au-form-wrap">
        <header class="au-head">
          <h1 class="au-head__title">Create your account</h1>
          <p class="au-head__sub">
            Already have one?
            <NuxtLink to="/login" class="au-link">Sign in</NuxtLink>
          </p>
        </header>

        <form class="au-form" @submit.prevent="createAccount">
          <div class="field__row">
            <label class="field" :class="{ 'field--error': submitted && !form.firstName.trim() }">
              <span class="field__label">First name</span>
              <div class="field__control">
                <input v-model="form.firstName" type="text" class="field__input" placeholder="John" autocomplete="given-name" />
              </div>
            </label>
            <label class="field" :class="{ 'field--error': submitted && !form.lastName.trim() }">
              <span class="field__label">Last name</span>
              <div class="field__control">
                <input v-model="form.lastName" type="text" class="field__input" placeholder="Doe" autocomplete="family-name" />
              </div>
            </label>
          </div>

          <label class="field" :class="{ 'field--error': submitted && !emailValid }">
            <span class="field__label">Email</span>
            <div class="field__control">
              <UIcon name="i-heroicons-envelope" class="field__lead" />
              <input v-model="form.email" type="email" class="field__input field__input--icon" placeholder="you@example.com" autocomplete="email" />
            </div>
          </label>

          <label class="field" :class="{ 'field--error': submitted && !phoneValid }">
            <span class="field__label">Phone</span>
            <div class="field__control">
              <span class="field__prefix">+237</span>
              <input v-model="form.phone" type="tel" inputmode="numeric" class="field__input field__input--prefix" placeholder="6XX XXX XXX" autocomplete="tel" />
            </div>
          </label>

          <label class="field" :class="{ 'field--error': submitted && !passwordStrong }">
            <span class="field__label">Password</span>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input v-model="form.password" :type="showPassword ? 'text' : 'password'" class="field__input field__input--icon" placeholder="At least 8 characters" autocomplete="new-password" />
              <button type="button" class="field__toggle" @click="showPassword = !showPassword">
                <UIcon :name="showPassword ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
              </button>
            </div>
            <p class="field__rule" :class="{ 'field__rule--bad': submitted && !passwordStrong }">
              At least 8 characters with an uppercase letter, a lowercase letter, a number, and a special character (e.g. #$).
            </p>
          </label>

          <label class="field" :class="{ 'field--error': submitted && !passwordsMatch }">
            <span class="field__label">Confirm password</span>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input v-model="form.confirm" :type="showPassword ? 'text' : 'password'" class="field__input field__input--icon" placeholder="Re-enter password" autocomplete="new-password" />
            </div>
            <p v-if="submitted && form.confirm && !passwordsMatch" class="field__err">Passwords don't match.</p>
          </label>

          <button type="submit" class="au-submit" :disabled="creating">
            <UIcon v-if="!creating" name="i-heroicons-user-plus" class="au-submit__icon" />
            <UIcon v-else name="i-heroicons-arrow-path" class="au-submit__icon au-submit__icon--spin" />
            {{ creating ? 'Creating...' : 'Create account' }}
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

/* Brand panel */
.au-brand {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #E8202A 0%, #B3151D 100%);
  color: #fff; border-radius: 0.85rem 0 0 0.85rem;
  padding: 2.5rem 2.25rem; display: flex; flex-direction: column; justify-content: space-between;
}
.au-brand::before, .au-brand::after { content: ''; position: absolute; border-radius: 50%; background: rgba(255,255,255,0.08); pointer-events: none; }
.au-brand::before { width: 16rem; height: 16rem; top: -5rem; right: -5rem; }
.au-brand::after { width: 11rem; height: 11rem; bottom: -4rem; left: -3rem; }
.au-brand__top { position: relative; z-index: 1; }
.au-brand__logo { display: inline-flex; }
.au-brand__logo-img { height: 2.6rem; width: auto; display: block; }
.au-brand__headline { font-size: 1.65rem; font-weight: 800; line-height: 1.2; margin: 1.75rem 0 0.75rem; }
.au-brand__sub { font-size: 0.875rem; line-height: 1.6; color: rgba(255,255,255,0.85); margin: 0; }

.au-perks { position: relative; z-index: 1; list-style: none; padding: 0; margin: 2rem 0; display: flex; flex-direction: column; gap: 1rem; }
.au-perk { display: flex; align-items: center; gap: 0.875rem; font-size: 0.875rem; font-weight: 500; opacity: 0; transform: translateY(8px); animation: au-rise 0.5s ease forwards; }
.au-perk__icon { flex-shrink: 0; width: 2.25rem; height: 2.25rem; border-radius: 0.5rem; background: rgba(255,255,255,0.15); display: flex; align-items: center; justify-content: center; }
.au-perk__icon :deep(svg) { width: 1.2rem; height: 1.2rem; }
@keyframes au-rise { to { opacity: 1; transform: translateY(0); } }

.au-brand__foot { position: relative; z-index: 1; display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; color: rgba(255,255,255,0.75); margin: 0; }
.au-brand__foot :deep(svg) { width: 0.95rem; height: 0.95rem; }

/* Form panel */
.au-form-wrap { background: #fff; border: 1px solid #E5E7EB; border-left: none; border-radius: 0 0.85rem 0.85rem 0; padding: 2.5rem 2.25rem; display: flex; flex-direction: column; }
.au-head__title { font-size: 1.5rem; font-weight: 800; color: #111827; margin: 0 0 0.35rem; }
.au-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; }
.au-link { color: #E8202A; font-weight: 600; text-decoration: none; background: none; border: none; cursor: pointer; padding: 0; font-size: inherit; }
.au-link:hover:not(:disabled) { text-decoration: underline; }
.au-link:disabled { color: #9CA3AF; cursor: not-allowed; }

/* Step indicator */
.au-steps { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.75rem; }
.au-step { display: flex; align-items: center; gap: 0.45rem; font-size: 0.78rem; font-weight: 600; color: #9CA3AF; }
.au-step__dot { width: 1.5rem; height: 1.5rem; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: #F3F4F6; color: #9CA3AF; font-size: 0.75rem; }
.au-step--active .au-step__dot { background: #E8202A; color: #fff; }
.au-step--active { color: #111827; }
.au-step--done .au-step__dot { background: #16A34A; color: #fff; }
.au-step--done { color: #16A34A; }
.au-step__line { flex: 1; height: 1px; background: #E5E7EB; }

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
.field__rule { font-size: 0.7rem; line-height: 1.4; color: #9CA3AF; margin: 0.35rem 0 0; }
.field__rule--bad { color: #E8202A; font-weight: 600; }

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
.au-terms { font-size: 0.72rem; color: #9CA3AF; text-align: center; margin: 1.5rem 0 0; line-height: 1.5; }

@media (max-width: 820px) {
  .au__card { grid-template-columns: 1fr; max-width: 460px; }
  .au-brand { border-radius: 0.85rem 0.85rem 0 0; padding: 2rem; }
  .au-brand__headline { font-size: 1.4rem; margin-top: 1.25rem; }
  .au-perks { margin: 1.5rem 0; }
  .au-form-wrap { border-left: 1px solid #E5E7EB; border-top: none; border-radius: 0 0 0.85rem 0.85rem; padding: 2rem; }
}

.au-row { display: flex; justify-content: flex-end; margin: -0.25rem 0 0.25rem; }
.field__toggle { background: none; border: none; cursor: pointer; color: #9CA3AF; padding: 0 0.25rem; }
.field__toggle :deep(svg) { width: 1.1rem; height: 1.1rem; }
.field__row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
</style>

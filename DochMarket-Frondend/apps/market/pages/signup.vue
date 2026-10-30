<script setup lang="ts">
// Standalone signup — no storefront chrome, matching the DOCH Xender card design.
definePageMeta({ layout: false })
useHead({ title: 'Create Account — Doch Market' })

const { notify } = useNotify()
const { register } = useAuth()

const form = reactive({
  name: '',
  email: '',
  phone: '',
  password: '',
  confirm: '',
  agree: false,
})

const showPassword = ref(false)
const showConfirm = ref(false)
const submitted = ref(false)
const creating = ref(false)

// Why brands shop with us — animated benefits shown on the brand panel.
const perks = [
  { icon: 'i-heroicons-truck', text: 'Free delivery on orders over 50,000 XAF' },
  { icon: 'i-heroicons-shield-check', text: 'Secure Mobile Money & card payments' },
  { icon: 'i-heroicons-arrow-path', text: '7-day easy returns, no questions asked' },
  { icon: 'i-heroicons-sparkles', text: 'Members-only flash deals & early access' },
]

// Email looks valid, phone has enough digits — light client-side checks.
const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))
const phoneValid = computed(() => form.phone.replace(/\D/g, '').length >= 8)

// Password strength: scored 0–4 from length + character variety.
const strength = computed(() => {
  const p = form.password
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++
  return score
})
const strengthMeta = computed(() => {
  return (
    [
      { label: '', color: '' },
      { label: 'Weak', color: '#EF4444' },
      { label: 'Fair', color: '#F59E0B' },
      { label: 'Good', color: '#3B82F6' },
      { label: 'Strong', color: '#16A34A' },
    ] as const
  )[strength.value]
})

const passwordsMatch = computed(() => form.confirm.length > 0 && form.password === form.confirm)

// Strong password: 8+ chars with upper, lower, number, and a special character.
const passwordStrong = computed(() =>
  form.password.length >= 8 &&
  /[A-Z]/.test(form.password) &&
  /[a-z]/.test(form.password) &&
  /\d/.test(form.password) &&
  /[^A-Za-z0-9]/.test(form.password),
)

const isValid = computed(() =>
  form.name.trim() !== '' &&
  emailValid.value &&
  phoneValid.value &&
  passwordStrong.value &&
  passwordsMatch.value &&
  form.agree,
)

function fieldError(condition: boolean) {
  return { 'field--error': submitted.value && condition }
}

async function createAccount() {
  submitted.value = true
  if (!isValid.value) {
    notify({
      title: 'Check your details',
      message: 'Please complete all fields correctly before continuing.',
      variant: 'error',
    })
    return
  }

  creating.value = true
  try {
    // Real registration. The single "name" field is split into first/last; a
    // username is derived from the email. Backend role is ROLE_BUYER.
    const fullName = form.name.trim()
    const parts = fullName.split(/\s+/)
    const firstName = parts[0] || fullName
    const lastName = parts.slice(1).join(' ') || firstName
    const userName = form.email.trim().split('@')[0]

    const res: any = await register({
      email: form.email.trim(),
      firstName,
      lastName,
      userName,
      password: form.password,
      phone: form.phone.trim(),
      doB: '1990-01-01',
    })

    if (res?.existingAccount && !res.verificationRequired) {
      notify({ title: 'Access added', message: res.message, variant: 'success' })
      await navigateTo('/login')
      return
    }

    // Do NOT auto-login — the account must be verified by email first.
    notify({
      title: 'Account created',
      message: 'Enter the code we emailed you to verify your account.',
      variant: 'success',
    })
    await navigateTo(`/otp-validation/${encodeURIComponent(form.email.trim())}`)
  } catch (e) {
    const err = e as any
    const serverMsg = err?.data?.message || err?.data
    const msg = err?.status === 403 || err?.status === 500
      ? `Couldn't create your account — server rejected the request (${err.status}).` + (serverMsg ? ' ' + serverMsg : '')
      : (serverMsg || err?.message || 'Could not create your account. Please try again.')
    notify({ title: 'Registration failed', message: msg, variant: 'error' })
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="su-page">
    <section class="su">
    <div class="su__card">
      <!-- Brand panel -->
      <aside class="su-brand">
        <div class="su-brand__top">
          <NuxtLink to="/" class="su-brand__logo">
            <img src="/images/logos/Logo-03.png" alt="Doch Market" class="su-brand__logo-img" />
          </NuxtLink>
          <h2 class="su-brand__headline">Join thousands of happy shoppers.</h2>
          <p class="su-brand__sub">
            Create your free account and unlock a faster checkout, order tracking and rewards built just for you.
          </p>
        </div>

        <ul class="su-perks">
          <li v-for="(p, i) in perks" :key="p.text" class="su-perk" :style="{ animationDelay: `${i * 110}ms` }">
            <span class="su-perk__icon"><UIcon :name="p.icon" /></span>
            <span>{{ p.text }}</span>
          </li>
        </ul>

        <p class="su-brand__foot">
          <UIcon name="i-heroicons-lock-closed" /> Your data is encrypted and never shared.
        </p>
      </aside>

      <!-- Form panel -->
      <div class="su-form-wrap">
        <header class="su-head">
          <h1 class="su-head__title">Create your account</h1>
          <p class="su-head__sub">
            Already have one?
            <NuxtLink to="/login" class="su-link">Sign in</NuxtLink>
          </p>
        </header>

        <!-- Social sign-up (decorative for now) -->
        <div class="su-social">
          <button type="button" class="su-social__btn" @click="notify({ title: 'Coming soon', message: 'Social sign-up will be available shortly.', variant: 'info' })">
            <UIcon name="i-simple-icons-google" class="su-social__icon" /> Google
          </button>
          <button type="button" class="su-social__btn" @click="notify({ title: 'Coming soon', message: 'Social sign-up will be available shortly.', variant: 'info' })">
            <UIcon name="i-simple-icons-facebook" class="su-social__icon su-social__icon--fb" /> Facebook
          </button>
        </div>

        <div class="su-divider"><span>or sign up with email</span></div>

        <form class="su-form" @submit.prevent="createAccount">
          <label class="field field--wide" :class="fieldError(form.name.trim() === '')">
            <span class="field__label">Full name *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-user" class="field__lead" />
              <input v-model="form.name" type="text" class="field__input field__input--icon" placeholder="John Doe" autocomplete="name" />
            </div>
          </label>

          <label class="field" :class="fieldError(!emailValid)">
            <span class="field__label">Email *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-envelope" class="field__lead" />
              <input v-model="form.email" type="email" class="field__input field__input--icon" placeholder="you@example.com" autocomplete="email" />
            </div>
          </label>

          <label class="field" :class="fieldError(!phoneValid)">
            <span class="field__label">Phone *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-device-phone-mobile" class="field__lead" />
              <input v-model="form.phone" type="tel" class="field__input field__input--icon" placeholder="+237 6XX XXX XXX" autocomplete="tel" />
            </div>
          </label>

          <label class="field field--wide" :class="fieldError(!passwordStrong)">
            <span class="field__label">Password *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="field__input field__input--icon field__input--trail"
                placeholder="At least 8 characters"
                autocomplete="new-password"
              />
              <button type="button" class="field__toggle" :aria-label="showPassword ? 'Hide password' : 'Show password'" @click="showPassword = !showPassword">
                <UIcon :name="showPassword ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
              </button>
            </div>
            <!-- Strength meter -->
            <div v-if="form.password" class="su-strength">
              <div class="su-strength__bars">
                <span
                  v-for="n in 4" :key="n"
                  class="su-strength__bar"
                  :style="{ background: n <= strength ? strengthMeta.color : '#E5E7EB' }"
                />
              </div>
              <span class="su-strength__label" :style="{ color: strengthMeta.color }">{{ strengthMeta.label }}</span>
            </div>
            <p class="su-req" :class="{ 'su-req--bad': submitted && !passwordStrong }">
              At least 8 characters with an uppercase letter, a lowercase letter, a number, and a special character (e.g. #$).
            </p>
          </label>

          <label class="field field--wide" :class="fieldError(!passwordsMatch)">
            <span class="field__label">Confirm password *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input
                v-model="form.confirm"
                :type="showConfirm ? 'text' : 'password'"
                class="field__input field__input--icon field__input--trail"
                placeholder="Re-enter your password"
                autocomplete="new-password"
              />
              <button type="button" class="field__toggle" :aria-label="showConfirm ? 'Hide password' : 'Show password'" @click="showConfirm = !showConfirm">
                <UIcon :name="showConfirm ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
              </button>
            </div>
            <p v-if="form.confirm && !passwordsMatch" class="su-hint su-hint--bad">
              <UIcon name="i-heroicons-x-circle" /> Passwords don’t match
            </p>
            <p v-else-if="passwordsMatch" class="su-hint su-hint--ok">
              <UIcon name="i-heroicons-check-circle" /> Passwords match
            </p>
          </label>

          <label class="su-agree field--wide" :class="{ 'su-agree--error': submitted && !form.agree }">
            <input v-model="form.agree" type="checkbox" class="su-agree__box" />
            <span>
              I agree to the
              <NuxtLink to="/" class="su-link">Terms of Service</NuxtLink>
              and
              <NuxtLink to="/" class="su-link">Privacy Policy</NuxtLink>.
            </span>
          </label>

          <button type="submit" class="su-submit field--wide" :disabled="creating">
            <UIcon v-if="!creating" name="i-heroicons-user-plus" class="su-submit__icon" />
            <UIcon v-else name="i-heroicons-arrow-path" class="su-submit__icon su-submit__icon--spin" />
            {{ creating ? 'Creating account…' : 'CREATE ACCOUNT' }}
          </button>
        </form>
      </div>
    </div>
    </section>

    <!-- Toaster normally lives in the storefront layout; this page is layout-less. -->
    <MarketToaster />
  </div>
</template>

<style scoped>
.su { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 2rem 1.5rem; }
.su__card {
  width: 100%; max-width: 1000px; display: grid; grid-template-columns: 1fr 1fr;
  box-shadow: 0 20px 50px -20px rgba(0, 0, 0, 0.2); border-radius: 0.85rem;
}
.su__card > * { min-width: 0; }

/* ---- Brand panel ---- */
.su-brand {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #E8202A 0%, #B3151D 100%);
  color: #fff; border-radius: 0.85rem 0 0 0.85rem;
  padding: 2.5rem 2.25rem; display: flex; flex-direction: column; justify-content: space-between;
}
.su-brand::before,
.su-brand::after {
  content: ''; position: absolute; border-radius: 50%;
  background: rgba(255, 255, 255, 0.08); pointer-events: none;
}
.su-brand::before { width: 16rem; height: 16rem; top: -5rem; right: -5rem; }
.su-brand::after { width: 11rem; height: 11rem; bottom: -4rem; left: -3rem; }
.su-brand__top { position: relative; z-index: 1; }
.su-brand__logo { display: inline-flex; align-items: center; }
.su-brand__logo-img { height: 2.6rem; width: auto; display: block; }
.su-brand__headline { font-size: 1.65rem; font-weight: 800; line-height: 1.2; margin: 1.75rem 0 0.75rem; }
.su-brand__sub { font-size: 0.875rem; line-height: 1.6; color: rgba(255, 255, 255, 0.85); margin: 0; }

.su-perks { position: relative; z-index: 1; list-style: none; padding: 0; margin: 2rem 0; display: flex; flex-direction: column; gap: 1rem; }
.su-perk {
  display: flex; align-items: center; gap: 0.875rem; font-size: 0.875rem; font-weight: 500;
  opacity: 0; transform: translateY(8px); animation: su-rise 0.5s ease forwards;
}
.su-perk__icon {
  flex-shrink: 0; width: 2.25rem; height: 2.25rem; border-radius: 0.5rem;
  background: rgba(255, 255, 255, 0.15); display: flex; align-items: center; justify-content: center;
}
.su-perk__icon :deep(svg) { width: 1.2rem; height: 1.2rem; }
@keyframes su-rise { to { opacity: 1; transform: translateY(0); } }

.su-brand__foot { position: relative; z-index: 1; display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; color: rgba(255, 255, 255, 0.75); margin: 0; }
.su-brand__foot :deep(svg) { width: 0.95rem; height: 0.95rem; }

/* ---- Form panel ---- */
.su-form-wrap {
  background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-left: none;
  border-radius: 0 0.85rem 0.85rem 0; padding: 2.5rem 2.25rem;
}
.su-head__title { font-size: 1.5rem; font-weight: 800; color: #111827; margin: 0 0 0.35rem; }
.su-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; }
.su-link { color: var(--color-primary, #E8202A); font-weight: 600; text-decoration: none; }
.su-link:hover { text-decoration: underline; }

.su-social { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.su-social__btn {
  display: flex; align-items: center; justify-content: center; gap: 0.5rem;
  border: 1px solid var(--color-border, #E5E7EB); background: #fff; border-radius: 0.5rem;
  padding: 0.65rem; font-size: 0.8125rem; font-weight: 600; color: #374151; cursor: pointer; transition: all 0.15s;
}
.su-social__btn:hover { border-color: #9CA3AF; background: #F9FAFB; }
.su-social__icon { width: 1.1rem; height: 1.1rem; color: #EA4335; }
.su-social__icon--fb { color: #1877F2; }

.su-divider { display: flex; align-items: center; gap: 0.875rem; margin: 1.5rem 0; }
.su-divider::before, .su-divider::after { content: ''; flex: 1; height: 1px; background: var(--color-border, #E5E7EB); }
.su-divider span { font-size: 0.6875rem; font-weight: 600; color: #9CA3AF; text-transform: uppercase; letter-spacing: 0.04em; }

.su-form { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem 1.1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field--wide { grid-column: 1 / -1; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__control { position: relative; display: flex; align-items: center; }
.field__lead { position: absolute; left: 0.7rem; width: 1.05rem; height: 1.05rem; color: #9CA3AF; pointer-events: none; }
.field__input { width: 100%; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 0.65rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; transition: border-color 0.15s, box-shadow 0.15s; }
.field__input--icon { padding-left: 2.35rem; }
.field__input--trail { padding-right: 2.6rem; }
.field__input:focus { border-color: var(--color-primary, #E8202A); box-shadow: 0 0 0 3px rgba(232, 32, 42, 0.1); }
.field--error .field__input { border-color: var(--color-primary, #E8202A); background: #FEF2F2; }
.field__toggle { position: absolute; right: 0.5rem; display: flex; background: none; border: none; cursor: pointer; color: #9CA3AF; padding: 0.35rem; }
.field__toggle:hover { color: #4B5563; }
.field__toggle :deep(svg) { width: 1.15rem; height: 1.15rem; }

/* Strength meter */
.su-strength { display: flex; align-items: center; gap: 0.625rem; margin-top: 0.5rem; }
.su-strength__bars { display: flex; gap: 0.25rem; flex: 1; }
.su-strength__bar { flex: 1; height: 0.3rem; border-radius: 999px; transition: background 0.25s; }
.su-strength__label { font-size: 0.6875rem; font-weight: 700; min-width: 3rem; }

.su-hint { display: flex; align-items: center; gap: 0.3rem; font-size: 0.6875rem; font-weight: 600; margin: 0.1rem 0 0; }
.su-hint :deep(svg) { width: 0.9rem; height: 0.9rem; }
.su-hint--ok { color: #16A34A; }
.su-hint--bad { color: var(--color-primary, #E8202A); }
.su-req { font-size: 0.6875rem; line-height: 1.4; color: #9CA3AF; margin: 0.4rem 0 0; }
.su-req--bad { color: var(--color-primary, #E8202A); font-weight: 600; }

.su-agree { display: flex; align-items: flex-start; gap: 0.6rem; font-size: 0.8125rem; color: #4B5563; line-height: 1.5; cursor: pointer; }
.su-agree__box { margin-top: 0.15rem; width: 1rem; height: 1rem; accent-color: var(--color-primary, #E8202A); flex-shrink: 0; }
.su-agree--error { color: var(--color-primary, #E8202A); }

.su-submit {
  margin-top: 0.5rem; display: flex; align-items: center; justify-content: center; gap: 0.5rem;
  background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.5rem;
  padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; letter-spacing: 0.02em; cursor: pointer; transition: background 0.15s;
}
.su-submit:hover:not(:disabled) { background: var(--color-primary-hover, #C8181F); }
.su-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.su-submit__icon { width: 1.1rem; height: 1.1rem; }
.su-submit__icon--spin { animation: su-spin 0.8s linear infinite; }
@keyframes su-spin { to { transform: rotate(360deg); } }

@media (max-width: 820px) {
  .su__card { grid-template-columns: 1fr; max-width: 480px; }
  .su-brand { border-radius: 0.85rem 0.85rem 0 0; padding: 2rem; }
  .su-brand__headline { font-size: 1.4rem; margin-top: 1.25rem; }
  .su-perks { margin: 1.5rem 0; }
  .su-form-wrap { border-left: 1px solid var(--color-border, #E5E7EB); border-top: none; border-radius: 0 0 0.85rem 0.85rem; padding: 2rem; }
}
@media (max-width: 460px) {
  .su-form { grid-template-columns: 1fr; }
  .su-social { grid-template-columns: 1fr; }
}
</style>

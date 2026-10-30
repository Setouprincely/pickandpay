<script setup lang="ts">
definePageMeta({ layout: false })

const { $t } = useNuxtApp()
const { register } = useAuth()
const { notify } = useNotify()

const form = reactive({
  shopName: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  dob: '',
  city: '',
  address: '',
  password: '',
  confirm: '',
  agree: false,
})

const showPassword = ref(false)
const showConfirm = ref(false)
const submitted = ref(false)
const creating = ref(false)

const perks = computed(() => [
  { icon: 'i-heroicons-users', text: $t('seller.signup.perks.reach') },
  { icon: 'i-heroicons-chart-bar', text: $t('seller.signup.perks.tools') },
  { icon: 'i-heroicons-banknotes', text: $t('seller.signup.perks.payouts') },
  { icon: 'i-heroicons-sparkles', text: $t('seller.signup.perks.promos') },
])

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))
const phoneValid = computed(() => form.phone.replace(/\D/g, '').length >= 8)

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
  const metas = [
    { label: '', color: '' },
    { label: $t('seller.signup.weak'), color: '#EF4444' },
    { label: $t('seller.signup.fair'), color: '#F59E0B' },
    { label: $t('seller.signup.good'), color: '#3B82F6' },
    { label: $t('seller.signup.strong'), color: '#16A34A' },
  ] as const
  return metas[strength.value] ?? metas[0]
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
  form.shopName.trim() !== '' &&
  form.firstName.trim() !== '' &&
  form.lastName.trim() !== '' &&
  emailValid.value &&
  phoneValid.value &&
  form.dob !== '' &&
  form.city.trim() !== '' &&
  form.address.trim() !== '' &&
  form.password.length >= 8 &&
  passwordsMatch.value &&
  form.agree,
)

function fieldError(condition: boolean) {
  return { 'field--error': submitted.value && condition }
}

async function createAccount() {
  submitted.value = true
  if (!isValid.value) {
    notify({ title: $t('seller.signup.invalid'), variant: 'error' })
    return
  }
  creating.value = true
  try {
    const res: any = await register({
      email: form.email.trim(),
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      userName: form.email.split('@')[0].trim(),
      password: form.password,
      phone: form.phone.trim(),
      contactNumber: form.phone.trim(),
      dob: form.dob,
      storeName: form.shopName.trim(),
      city: form.city.trim(),
      address: form.address.trim(),
    })
    if (res?.existingAccount && !res.verificationRequired) {
      notify({ title: $t('seller.signup.created'), message: res.message, variant: 'success' })
      await navigateTo('/login')
      return
    }
    notify({ title: $t('seller.signup.created'), message: 'Enter the code we emailed you to verify your account.', variant: 'success' })
    await navigateTo(`/otp-validation/${encodeURIComponent(form.email.trim())}`)
  } catch (e: any) {
    // The backend returns a plain array of validation messages on 400
    // (not an { message } object), so surface those directly when present.
    const msg = Array.isArray(e?.data) ? e.data.join(' ') : (e?.data?.message || $t('seller.signup.error'))
    notify({ title: msg, variant: 'error' })
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="su-page">
    <div class="su-card">
      <!-- Brand panel -->
      <aside class="su-brand">
        <div class="su-brand__top">
          <NuxtLink to="/login" class="su-brand__logo">
            <img src="/images/logos/Logo-03.png" alt="Doch" class="su-brand__logo-img" onerror="this.style.display='none'" />
          </NuxtLink>
          <h2 class="su-brand__headline">{{ $t('seller.signup.brandHeadline') }}</h2>
          <p class="su-brand__sub">{{ $t('seller.signup.brandSub') }}</p>
        </div>

        <ul class="su-perks">
          <li v-for="(p, i) in perks" :key="p.text" class="su-perk" :style="{ animationDelay: `${i * 110}ms` }">
            <span class="su-perk__icon"><UIcon :name="p.icon" /></span>
            <span>{{ p.text }}</span>
          </li>
        </ul>

        <p class="su-brand__foot">
          <UIcon name="i-heroicons-lock-closed" /> {{ $t('seller.signup.secure') }}
        </p>
      </aside>

      <!-- Form panel -->
      <div class="su-form-wrap">
        <header class="su-head">
          <p class="su-head__welcome">{{ $t('seller.signup.welcome') }}</p>
          <h1 class="su-head__title">{{ $t('seller.signup.title') }}</h1>
          <p class="su-head__sub">{{ $t('seller.signup.sub') }}</p>
        </header>

        <form class="su-form" @submit.prevent="createAccount">
          <label class="field field--wide" :class="fieldError(form.shopName.trim() === '')">
            <span class="field__label">{{ $t('seller.signup.shopName') }} *</span>
            <div class="field__control">
              <UIcon name="i-lucide-store" class="field__lead" />
              <input v-model="form.shopName" type="text" class="field__input field__input--icon" placeholder="My Shop" autocomplete="organization" />
            </div>
          </label>

          <label class="field" :class="fieldError(form.firstName.trim() === '')">
            <span class="field__label">First name *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-user" class="field__lead" />
              <input v-model="form.firstName" type="text" class="field__input field__input--icon" placeholder="John" autocomplete="given-name" />
            </div>
          </label>

          <label class="field" :class="fieldError(form.lastName.trim() === '')">
            <span class="field__label">Last name *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-user" class="field__lead" />
              <input v-model="form.lastName" type="text" class="field__input field__input--icon" placeholder="Doe" autocomplete="family-name" />
            </div>
          </label>

          <label class="field" :class="fieldError(form.dob === '')">
            <span class="field__label">Date of birth *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-cake" class="field__lead" />
              <input v-model="form.dob" type="date" class="field__input field__input--icon" />
            </div>
          </label>

          <label class="field" :class="fieldError(form.city.trim() === '')">
            <span class="field__label">City *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-map-pin" class="field__lead" />
              <input v-model="form.city" type="text" class="field__input field__input--icon" placeholder="Douala" autocomplete="address-level2" />
            </div>
          </label>

          <label class="field field--wide" :class="fieldError(form.address.trim() === '')">
            <span class="field__label">Address *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-home" class="field__lead" />
              <input v-model="form.address" type="text" class="field__input field__input--icon" placeholder="Street, neighborhood" autocomplete="street-address" />
            </div>
          </label>

          <label class="field" :class="fieldError(!phoneValid)">
            <span class="field__label">{{ $t('seller.signup.phone') }} *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-device-phone-mobile" class="field__lead" />
              <input v-model="form.phone" type="tel" class="field__input field__input--icon" placeholder="+237 6XX XXX XXX" autocomplete="tel" />
            </div>
          </label>

          <label class="field field--wide" :class="fieldError(!emailValid)">
            <span class="field__label">{{ $t('seller.signup.email') }} *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-envelope" class="field__lead" />
              <input v-model="form.email" type="email" class="field__input field__input--icon" placeholder="you@example.com" autocomplete="email" />
            </div>
          </label>

          <label class="field field--wide" :class="fieldError(!passwordStrong)">
            <span class="field__label">{{ $t('seller.signup.password') }} *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="field__input field__input--icon field__input--trail"
                placeholder="••••••••"
                autocomplete="new-password"
              />
              <button type="button" class="field__toggle" @click="showPassword = !showPassword">
                <UIcon :name="showPassword ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
              </button>
            </div>
            <div v-if="form.password" class="su-strength">
              <div class="su-strength__bars">
                <span v-for="n in 4" :key="n" class="su-strength__bar" :style="{ background: n <= strength ? strengthMeta.color : '#E5E7EB' }" />
              </div>
              <span class="su-strength__label" :style="{ color: strengthMeta.color }">{{ strengthMeta.label }}</span>
            </div>
            <p class="su-req" :class="{ 'su-req--bad': submitted && !passwordStrong }">
              {{ $t('seller.signup.passwordRule') }}
            </p>
          </label>

          <label class="field field--wide" :class="fieldError(!passwordsMatch)">
            <span class="field__label">{{ $t('seller.signup.confirm') }} *</span>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input
                v-model="form.confirm"
                :type="showConfirm ? 'text' : 'password'"
                class="field__input field__input--icon field__input--trail"
                placeholder="••••••••"
                autocomplete="new-password"
              />
              <button type="button" class="field__toggle" @click="showConfirm = !showConfirm">
                <UIcon :name="showConfirm ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
              </button>
            </div>
            <p v-if="form.confirm && !passwordsMatch" class="su-hint su-hint--bad">
              <UIcon name="i-heroicons-x-circle" /> {{ $t('seller.signup.mismatch') }}
            </p>
            <p v-else-if="passwordsMatch" class="su-hint su-hint--ok">
              <UIcon name="i-heroicons-check-circle" /> {{ $t('seller.signup.match') }}
            </p>
          </label>

          <label class="su-agree field--wide" :class="{ 'su-agree--error': submitted && !form.agree }">
            <input v-model="form.agree" type="checkbox" class="su-agree__box" />
            <span>{{ $t('seller.signup.agree') }}</span>
          </label>

          <button type="submit" class="su-submit field--wide" :disabled="creating">
            <UIcon v-if="!creating" name="i-heroicons-user-plus" class="su-submit__icon" />
            <UIcon v-else name="i-heroicons-arrow-path" class="su-submit__icon su-submit__icon--spin" />
            {{ creating ? $t('seller.signup.creating') : $t('seller.signup.submit') }}
          </button>
        </form>

        <p class="su-foot">
          {{ $t('seller.signup.haveAccount') }}
          <NuxtLink to="/login" class="su-link">{{ $t('seller.signup.signIn') }}</NuxtLink>
        </p>
      </div>
    </div>

    <DochLanguageToggle />
    <SellerToaster />
  </div>
</template>

<style scoped>
.su-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 1.5rem; }
.su-card {
  width: 100%; max-width: 1000px; display: grid; grid-template-columns: 1fr 1fr; gap: 0;
  border-radius: 0.85rem; overflow: hidden; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.2);
}
.su-card > * { min-width: 0; }

/* Brand panel */
.su-brand {
  position: relative; overflow: hidden;
  background: linear-gradient(150deg, #E8202A 0%, #B3151D 100%);
  color: #fff; padding: 2.5rem 2.25rem; display: flex; flex-direction: column; justify-content: space-between;
}
.su-brand::before, .su-brand::after { content: ''; position: absolute; border-radius: 50%; background: rgba(255,255,255,0.08); pointer-events: none; }
.su-brand::before { width: 16rem; height: 16rem; top: -5rem; right: -5rem; }
.su-brand::after { width: 11rem; height: 11rem; bottom: -4rem; left: -3rem; }
.su-brand__top { position: relative; z-index: 1; }
.su-brand__logo-img { height: 2.6rem; width: auto; display: block; }
.su-brand__headline { font-size: 1.65rem; font-weight: 800; line-height: 1.2; margin: 1.75rem 0 0.75rem; }
.su-brand__sub { font-size: 0.875rem; line-height: 1.6; color: rgba(255,255,255,0.85); margin: 0; }

.su-perks { position: relative; z-index: 1; list-style: none; padding: 0; margin: 2rem 0; display: flex; flex-direction: column; gap: 1rem; }
.su-perk { display: flex; align-items: center; gap: 0.875rem; font-size: 0.875rem; font-weight: 500; opacity: 0; transform: translateY(8px); animation: su-rise 0.5s ease forwards; }
.su-perk__icon { flex-shrink: 0; width: 2.25rem; height: 2.25rem; border-radius: 0.5rem; background: rgba(255,255,255,0.15); display: flex; align-items: center; justify-content: center; }
.su-perk__icon :deep(svg) { width: 1.2rem; height: 1.2rem; }
@keyframes su-rise { to { opacity: 1; transform: translateY(0); } }
.su-brand__foot { position: relative; z-index: 1; display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; color: rgba(255,255,255,0.75); margin: 0; }
.su-brand__foot :deep(svg) { width: 0.95rem; height: 0.95rem; }

/* Form panel */
.su-form-wrap { background: #fff; padding: 2.5rem 2.25rem; }
.su-head__welcome { font-size: 0.8125rem; font-weight: 700; color: var(--color-primary, #E8202A); letter-spacing: 0.02em; margin: 0 0 0.25rem; }
.su-head__title { font-size: 1.4rem; font-weight: 800; color: #111827; margin: 0 0 0.35rem; }
.su-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; }
.su-link { color: var(--color-primary, #E8202A); font-weight: 600; text-decoration: none; }
.su-link:hover { text-decoration: underline; }

.su-form { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem 1.1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field--wide { grid-column: 1 / -1; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__control { position: relative; display: flex; align-items: center; }
.field__lead { position: absolute; left: 0.7rem; width: 1.05rem; height: 1.05rem; color: #9CA3AF; pointer-events: none; }
.field__input { width: 100%; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 0.65rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; transition: border-color 0.15s, box-shadow 0.15s; }
.field__input--icon { padding-left: 2.35rem; }
.field__input--trail { padding-right: 2.6rem; }
.field__input:focus { border-color: var(--color-primary, #E8202A); box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.field--error .field__input { border-color: var(--color-primary, #E8202A); background: #FEF2F2; }
.field__toggle { position: absolute; right: 0.5rem; display: flex; background: none; border: none; cursor: pointer; color: #9CA3AF; padding: 0.35rem; }
.field__toggle:hover { color: #4B5563; }
.field__toggle :deep(svg) { width: 1.15rem; height: 1.15rem; }

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

.su-submit { margin-top: 0.5rem; display: flex; align-items: center; justify-content: center; gap: 0.5rem; background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.5rem; padding: 0.85rem 1rem; font-size: 0.8125rem; font-weight: 700; letter-spacing: 0.02em; cursor: pointer; transition: background 0.15s; }
.su-submit:hover:not(:disabled) { background: var(--color-primary-hover, #C8181F); }
.su-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.su-submit__icon { width: 1.1rem; height: 1.1rem; }
.su-submit__icon--spin { animation: su-spin 0.8s linear infinite; }
@keyframes su-spin { to { transform: rotate(360deg); } }

.su-foot { text-align: center; font-size: 0.825rem; color: #6B7280; margin: 1.5rem 0 0; }

@media (max-width: 820px) {
  .su-card { grid-template-columns: 1fr; max-width: 460px; }
  .su-brand { padding: 2rem; }
  .su-brand__headline { font-size: 1.35rem; margin-top: 1.25rem; }
  .su-perks { margin: 1.5rem 0; }
  .su-form-wrap { padding: 2rem; }
}
@media (max-width: 460px) {
  .su-form { grid-template-columns: 1fr; }
}
</style>

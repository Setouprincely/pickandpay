<script setup lang="ts">
definePageMeta({ layout: false })

const { $t } = useNuxtApp()
const { signIn } = useAuth()

const form = reactive({ email: '', password: '' })
const showPassword = ref(false)
const submitted = ref(false)
const loading = ref(false)
const error = ref('')

const perks = computed(() => [
  { icon: 'i-heroicons-banknotes', text: $t('driver.login.perks.earn') },
  { icon: 'i-heroicons-map', text: $t('driver.login.perks.routes') },
  { icon: 'i-heroicons-clock', text: $t('driver.login.perks.flex') },
])

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()))

function fieldError(condition: boolean) {
  return { 'field--error': submitted.value && condition }
}

async function onSubmit() {
  submitted.value = true
  error.value = ''
  if (!form.email || !form.password) {
    error.value = $t('driver.login.errEmpty')
    return
  }
  loading.value = true
  try {
    await signIn(form.email.trim(), form.password)
    await navigateTo('/', { replace: true })
  } catch (e: any) {
    if (e?.message === 'Not a driver account') {
      error.value = 'This account does not have driver access'
    } else if (e?.data?.message) {
      error.value = e.data.message
    } else if (e?.status === undefined) {
      error.value = 'Could not reach the server. It may be waking up from idle — please try again in a moment.'
    } else {
      error.value = $t('driver.login.errFailed')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="su-page">
    <div class="su-card">
      <!-- Brand panel -->
      <aside class="su-brand">
        <div class="su-brand__top">
          <NuxtLink to="/" class="su-brand__logo">
            <img src="/images/logos/Logo-03.png" alt="Doch" class="su-brand__logo-img" onerror="this.style.display='none'" />
          </NuxtLink>
          <h2 class="su-brand__headline">{{ $t('driver.login.brandHeadline') }}</h2>
          <p class="su-brand__sub">{{ $t('driver.login.brandSub') }}</p>
        </div>

        <ul class="su-perks">
          <li v-for="(p, i) in perks" :key="p.text" class="su-perk" :style="{ animationDelay: `${i * 110}ms` }">
            <span class="su-perk__icon"><UIcon :name="p.icon" /></span>
            <span>{{ p.text }}</span>
          </li>
        </ul>

        <p class="su-brand__foot">
          <UIcon name="i-heroicons-lock-closed" /> {{ $t('driver.login.secure') }}
        </p>
      </aside>

      <!-- Form panel -->
      <div class="su-form-wrap">
        <header class="su-head">
          <p class="su-head__welcome">{{ $t('driver.login.welcome') }}</p>
          <h1 class="su-head__title">{{ $t('driver.login.title') }}</h1>
          <p class="su-head__sub">{{ $t('driver.login.sub') }}</p>
        </header>

        <form class="su-form" @submit.prevent="onSubmit">
          <label class="field field--wide" :class="fieldError(!emailValid)">
            <span class="field__label">{{ $t('driver.login.email') }}</span>
            <div class="field__control">
              <UIcon name="i-heroicons-envelope" class="field__lead" />
              <input v-model="form.email" type="email" class="field__input field__input--icon" placeholder="you@example.com" autocomplete="email" />
            </div>
          </label>

          <label class="field field--wide" :class="fieldError(!form.password)">
            <div class="field__label-row">
              <span class="field__label">{{ $t('driver.login.password') }}</span>
              <NuxtLink to="/forgot-password" class="su-link su-link--sm">{{ $t('driver.login.forgot') }}</NuxtLink>
            </div>
            <div class="field__control">
              <UIcon name="i-heroicons-lock-closed" class="field__lead" />
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="field__input field__input--icon field__input--trail"
                placeholder="••••••••"
                autocomplete="current-password"
              />
              <button type="button" class="field__toggle" @click="showPassword = !showPassword">
                <UIcon :name="showPassword ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'" />
              </button>
            </div>
          </label>

          <p v-if="error" class="su-error field--wide">{{ error }}</p>

          <button type="submit" class="su-submit field--wide" :disabled="loading">
            <UIcon v-if="!loading" name="i-heroicons-arrow-right-on-rectangle" class="su-submit__icon" />
            <UIcon v-else name="i-heroicons-arrow-path" class="su-submit__icon su-submit__icon--spin" />
            {{ loading ? $t('driver.login.signingIn') : $t('driver.login.submit') }}
          </button>
        </form>

        <p class="su-foot">
          {{ $t('driver.login.noAccount') }}
          <NuxtLink to="/signup" class="su-link">{{ $t('driver.login.createOne') }}</NuxtLink>
        </p>
      </div>
    </div>

    <DochLanguageToggle />
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
.su-brand__logo { display: inline-flex; align-items: center; }
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
.su-form-wrap { background: #fff; padding: 2.5rem 2.25rem; display: flex; flex-direction: column; justify-content: center; }
.su-head__welcome { font-size: 0.8125rem; font-weight: 700; color: var(--color-primary, #E8202A); letter-spacing: 0.02em; margin: 0 0 0.25rem; }
.su-head__title { font-size: 1.4rem; font-weight: 800; color: #111827; margin: 0 0 0.35rem; }
.su-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; }
.su-link { color: var(--color-primary, #E8202A); font-weight: 600; text-decoration: none; }
.su-link:hover { text-decoration: underline; }
.su-link--sm { font-size: 0.75rem; }

.su-form { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem 1.1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field--wide { grid-column: 1 / -1; }
.field__label-row { display: flex; align-items: center; justify-content: space-between; }
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

.su-error { font-size: 0.75rem; font-weight: 600; color: var(--color-primary, #E8202A); margin: -0.25rem 0 0; }

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

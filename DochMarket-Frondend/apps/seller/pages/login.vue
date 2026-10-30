<script setup lang="ts">
definePageMeta({ layout: false })
const { $t } = useNuxtApp()
const { login } = useAuth()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

const handleLogin = async () => {
  if (!email.value || !password.value) {
    error.value = $t('seller.login.fill')
    return
  }
  loading.value = true
  error.value = ''
  try {
    await login(email.value, password.value)
    navigateTo('/')
  } catch (e: any) {
    error.value = e?.data?.message || $t('seller.login.error')
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
      <h2 class="login-title">{{ $t('seller.login.title') }}</h2>
      <p class="login-sub">{{ $t('seller.login.sub') }}</p>

      <div class="login-form">
        <div class="form-group">
          <label>{{ $t('seller.login.email') }}</label>
          <UInput v-model="email" type="email" placeholder="seller@doch.com" size="md" :disabled="loading" />
        </div>
        <div class="form-group">
          <label>{{ $t('seller.login.password') }}</label>
          <UInput
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="••••••••"
            size="md"
            :disabled="loading"
            :ui="{ trailing: 'pe-1' }"
          >
            <template #trailing>
              <UButton
                color="neutral"
                variant="link"
                size="sm"
                :icon="showPassword ? 'i-heroicons-eye-slash' : 'i-heroicons-eye'"
                :aria-label="showPassword ? 'Hide password' : 'Show password'"
                tabindex="-1"
                @click="showPassword = !showPassword"
              />
            </template>
          </UInput>
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <UButton class="login-btn" :loading="loading" block @click="handleLogin">
          {{ $t('seller.login.signIn') }}
        </UButton>
        <p class="login-foot">
          <NuxtLink to="/forgot-password" class="login-link">{{ $t('seller.login.forgot') }}</NuxtLink>
        </p>
      </div>

      <p class="login-foot">
        {{ $t('seller.login.noAccount') }}
        <NuxtLink to="/signup" class="login-link">{{ $t('seller.login.createOne') }}</NuxtLink>
      </p>
    </div>

    <DochLanguageToggle />
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; }
.login-card { background: white; border-radius: 16px; padding: 40px; width: 100%; max-width: 420px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
.login-logo { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; gap: 8px; }
.logo-img { height: 48px; }
.login-title { font-size: 22px; font-weight: 700; color: #111827; text-align: center; margin: 0 0 4px; }
.login-sub { font-size: 14px; color: #6B7280; text-align: center; margin: 0 0 28px; }
.login-form { display: flex; flex-direction: column; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; font-weight: 500; color: #6B7280; }
.login-btn { background: #EF2A39 !important; margin-top: 8px; }
.error-msg { color: #EF2A39; font-size: 13px; text-align: center; margin: 0; }
.login-foot { text-align: center; font-size: 13px; color: #6B7280; margin: 24px 0 0; }
.login-link { color: #EF2A39; font-weight: 600; text-decoration: none; }
.login-link:hover { text-decoration: underline; }
:deep(input) { color: #111827 !important; }
</style>

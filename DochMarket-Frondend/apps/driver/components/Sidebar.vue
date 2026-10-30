<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const route = useRoute()
const { $t } = useNuxtApp()
const { signOut, token } = useAuth()
const { get } = useDriverApi()

const deliveriesRoutes = ['/deliveries', '/delivery-checklist']

const navItems = computed<NavigationMenuItem[]>(() => [
  { label: $t('driver.nav.dashboard'), icon: 'i-lucide-layout-dashboard', to: '/' },
  {
    label: $t('driver.nav.myDeliveries'),
    icon: 'i-lucide-package',
    to: '/deliveries',
    active: deliveriesRoutes.includes(route.path),
  },
  { label: $t('driver.nav.history'), icon: 'i-lucide-clock', to: '/history' },
  { label: $t('driver.nav.reportProblem'), icon: 'i-lucide-triangle-alert', to: '/report-problem' },
  { label: $t('driver.nav.settings'), icon: 'i-lucide-settings', to: '/settings' },
])

// Real daily performance from API
const dailyPerformance = ref<number | null>(null)
onMounted(async () => {
  try {
    const perf = await get<any>('/api/drivers/me/performance')
    // Use onTimeRate if available, otherwise compute from deliveries
    dailyPerformance.value = perf.onTimeRate ?? perf.averageRating ?? null
  } catch {
    dailyPerformance.value = null
  }
})

function onSignOut() {
  // signOut() handles navigation internally — don't double-navigate
  signOut()
}
</script>

<template>
  <div class="mx-5 my-8 border border-Accented border-t-4 border-2 border-b-3 rounded-2xl">
    <USidebar
      collapsible="none"
      :ui="{
        container: 'h-screen',
        inner: 'rounded-2xl bg-white flex flex-col divide-transparent border-r-0',
        body: 'flex-1 py-1',
      }"
    >
      <template #header>
        <div class="flex items-center gap-2 px-5 py-4">
          <img src="/images/logos/Logo-01-trim.png" alt="Doch" class="h-12 w-auto" />
        </div>
      </template>

      <template #default>
        <UNavigationMenu
          :items="navItems"
          orientation="vertical"
          :ui="{
            link: 'data-[active]:text-white data-[active]:bg-button-red flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-normal w-full text-[#404040] text-[15px]',
            linkLeadingIcon: 'data-[active]:text-white hover:text-white',
          }"
          class="px-2"
        />

        <div class="mx-2 mt-4 px-3 py-2.5 bg-green-50 rounded-xl border border-green-100">
          <p class="text-xs text-gray-500 font-medium">{{ $t('driver.sidebar.dailyPerformance') }}</p>
          <p class="text-lg font-bold text-green-600 mt-0.5">
            {{ dailyPerformance !== null ? `${dailyPerformance}%` : '—' }}
          </p>
        </div>
      </template>

      <template #footer>
        <div class="w-full">
          <USeparator class="w-full mb-7" />
          <div class="px-2">
            <UButton
              :label="$t('common.signOut')"
              icon="i-lucide-log-out"
              variant="ghost"
              class="w-full justify-start font-semibold text-button-red text-[15px]"
              @click="onSignOut"
            />
          </div>
        </div>
      </template>
    </USidebar>
  </div>
</template>
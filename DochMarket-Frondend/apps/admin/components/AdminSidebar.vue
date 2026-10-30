<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const { $t } = useNuxtApp()
const { logout } = useAuth()

const navItems = computed<NavigationMenuItem[]>(() => [
  { label: $t('nav.dashboard'), icon: 'i-lucide-layout-dashboard', to: '/' },
  { label: $t('nav.orders'), icon: 'i-lucide-shopping-bag', to: '/orders' },
  { label: $t('nav.dispatch'), icon: 'i-lucide-truck', to: '/dispatch' },
  { label: $t('nav.products'), icon: 'i-lucide-package-check', to: '/products' },
  { label: $t('nav.reports'), icon: 'i-lucide-chart-column', to: '/reports' },
  { label: $t('nav.users'), icon: 'i-lucide-users', to: '/users' },
  { label: $t('nav.vendorAgreement'), icon: 'i-lucide-handshake', to: '/vendor-agreement' },
  { label: $t('nav.audit'), icon: 'i-lucide-scroll-text', to: '/audit' },
  { label: $t('nav.settings'), icon: 'i-lucide-settings', to: '/settings' },
])

const handleSignOut = async () => {
  await logout()
}
</script>

<template>
  <div class="mx-5 my-8 border-Accented border-t-4 border-2 border-b-3 rounded-2xl">
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
            linkLeadingIcon: 'data-[active]:text-white hover:text-white'
          }"
          class="px-2"
        />
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
              @click="handleSignOut"
            />
          </div>
        </div>
      </template>
    </USidebar>
  </div>
</template>
<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const emit = defineEmits<{ navigate: [] }>()
const { $t } = useNuxtApp()
const { logout } = useAuth()

const navItems = computed<NavigationMenuItem[]>(() => [
  { label: $t('seller.nav.dashboard'), icon: 'i-lucide-layout-dashboard', to: '/', onSelect: () => emit('navigate') },
  { label: $t('seller.nav.orders'),    icon: 'i-lucide-shopping-bag',      to: '/orders', onSelect: () => emit('navigate') },
  { label: $t('seller.nav.products'),  icon: 'i-lucide-package',           to: '/products', onSelect: () => emit('navigate') },
  { label: $t('seller.nav.analytics'), icon: 'i-lucide-chart-column',      to: '/analytics', onSelect: () => emit('navigate') },
  { label: $t('seller.nav.settings'),  icon: 'i-lucide-settings',          to: '/settings', onSelect: () => emit('navigate') },
])
</script>

<template>
  <div class="mx-5 my-4 border border-Accented border-t-4 border-2 border-b-3 rounded-2xl">
    <USidebar
      collapsible="none"
      :ui="{
        container: 'h-screen',
        inner: 'rounded-2xl bg-white flex flex-col divide-transparent border-r-0',
        body: 'flex-1 py-3',
      }"
    >
      <template #header>
        <div class="flex items-center gap-2 px-5 py-6">
          <img src="/images/logos/Logo-01-trim.png" alt="Doch" class="h-12 w-auto" />
        </div>
      </template>

      <template #default>
        <UNavigationMenu
          :items="navItems"
          orientation="vertical"
          :ui="{
            list: 'space-y-2.5',
            link: 'data-[active]:text-white data-[active]:bg-button-red flex items-center gap-3 px-4 py-3.5 rounded-xl text-sm font-normal w-full text-[#404040] text-[15px]',
            linkLeadingIcon: 'data-[active]:text-white hover:text-white size-5',
          }"
          class="px-3"
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
              @click="logout"
            />
          </div>
        </div>
      </template>
    </USidebar>
  </div>
</template>

<script setup lang="ts">
import SellerTopbar from '~/apps/seller/components/SellerTopbar.vue'
import SellerSidebar from '~/apps/seller/components/SellerSidebar.vue'
import SellerToaster from '~/apps/seller/components/SellerToaster.vue'

const sidebarOpen = ref(false)
provide('openSidebar', () => { sidebarOpen.value = true })
</script>

<template>
  <div class="seller-shell">

    <Transition name="fade">
      <div v-if="sidebarOpen" class="mobile-overlay" @click="sidebarOpen = false" />
    </Transition>

    <aside class="seller-sidebar" :class="{ 'seller-sidebar--open': sidebarOpen }">
      <SellerSidebar @navigate="sidebarOpen = false" />
    </aside>

    <div class="seller-body">
      <div class="layout-topbar">
        <SellerTopbar />
      </div>

      <main class="seller-content">
        <slot />
      </main>
    </div>

    <SellerToaster />
    <DochLanguageToggle />

  </div>
</template>

<style scoped>
.seller-shell { display: flex; height: 100vh; overflow: hidden; }

.seller-body {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden; min-width: 0;
  background: var(--color-page-bg);
}

.layout-topbar { flex-shrink: 0; padding: 1rem 1rem 0; }
.seller-content { flex: 1; overflow-y: auto; }

@media (min-width: 1024px) {
  .seller-sidebar { position: static; transform: none !important; flex-shrink: 0; }
  .mobile-overlay { display: none !important; }
  .layout-topbar { padding: 1.5rem 1.5rem 0; }
}

@media (max-width: 1023px) {
  .seller-sidebar {
    position: fixed; top: 0; left: 0; height: 100%;
    z-index: 50; transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  .seller-sidebar--open { transform: translateX(0); }
  .mobile-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 40; }
}

.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to       { opacity: 0; }
</style>

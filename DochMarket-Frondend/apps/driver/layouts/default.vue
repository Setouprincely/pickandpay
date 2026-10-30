<script setup lang="ts">
import Topbar from '~/apps/driver/components/Topbar.vue'

const sidebarOpen = ref(false)
provide('openSidebar', () => { sidebarOpen.value = true })

// Re-hydrate the signed-in driver from storage after mount
const auth = useAuth() as any
onMounted(() => auth.restore())
</script>

<template>
  <div class="driver-shell">

    <Transition name="fade">
      <div v-if="sidebarOpen" class="mobile-overlay" @click="sidebarOpen = false" />
    </Transition>

    <aside class="driver-sidebar" :class="{ 'driver-sidebar--open': sidebarOpen }">
      <DriverSidebar />
    </aside>

    <div class="driver-body">
      <div class="layout-topbar">
        <Topbar />
      </div>

      <main class="driver-content">
        <slot />
      </main>
    </div>

    <!-- Floating EN / FR language switch (every driver page) -->
    <DochLanguageToggle />

  </div>
</template>

<style scoped>
.driver-shell { display: flex; height: 100vh; overflow: hidden; }

.driver-body {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden; min-width: 0;
  background: var(--color-page-bg);
}

.layout-topbar { flex-shrink: 0; padding: 1rem 1rem 0; }
.driver-content { flex: 1; overflow-y: auto; }

@media (min-width: 1024px) {
  .driver-sidebar { position: static; transform: none !important; flex-shrink: 0; }
  .mobile-overlay { display: none !important; }
  .layout-topbar  { padding: 1.5rem 1.5rem 0; }
}

@media (max-width: 1023px) {
  .driver-sidebar {
    position: fixed; top: 0; left: 0; height: 100%;
    z-index: 50; transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  .driver-sidebar--open { transform: translateX(0); }
  .mobile-overlay {
    position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 40;
  }
}

.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to       { opacity: 0; }
</style>

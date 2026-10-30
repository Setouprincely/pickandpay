<script setup lang="ts">
import Topbar from '~/components/dashboard/Topbar.vue'

const sidebarOpen = ref(false)
provide('openSidebar', () => { sidebarOpen.value = true })
</script>

<template>
  <div class="admin-shell">

    <Transition name="fade">
      <div v-if="sidebarOpen" class="mobile-overlay" @click="sidebarOpen = false" />
    </Transition>

    <aside class="admin-sidebar" :class="{ 'admin-sidebar--open': sidebarOpen }">
      <AdminSidebar />
    </aside>

    <div class="admin-body">
      <!-- Topbar shown on every page -->
      <div class="layout-topbar">
        <Topbar />
      </div>

      <!-- Scrollable page area -->
      <main class="admin-content">
        <slot />
      </main>
    </div>

    <!-- Floating EN / FR language switch (every admin page) -->
    <DochLanguageToggle />

  </div>
</template>

<style scoped>
.admin-shell  { display: flex; height: 100vh; overflow: hidden; }

.admin-body {
  flex: 1; display: flex; flex-direction: column;
  overflow: hidden; min-width: 0;
  background: var(--color-page-bg);
}

.layout-topbar { flex-shrink: 0; padding: 1rem 1rem 0; }

.admin-content { flex: 1; overflow-y: auto; }

@media (min-width: 1024px) {
  .admin-sidebar { position: static; transform: none !important; flex-shrink: 0; }
  .mobile-overlay { display: none !important; }
  .layout-topbar { padding: 1.5rem 1.5rem 0; }
}

@media (max-width: 1023px) {
  .admin-sidebar {
    position: fixed; top: 0; left: 0; height: 100%;
    z-index: 50; transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  .admin-sidebar--open { transform: translateX(0); }
  .mobile-overlay {
    position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 40;
  }
}

.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to       { opacity: 0; }
</style>

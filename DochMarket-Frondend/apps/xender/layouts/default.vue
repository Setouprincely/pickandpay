<script setup lang="ts">
// Authenticated Xender shell: top app bar + page content + toast stack.
// Auth pages (login/signup) and the public receiver tracker use the `blank`
// layout instead.
</script>

<template>
  <div class="xn-app">
    <XenderAppHeader />
    <main class="xn-app__main">
      <slot />
    </main>
    <XenderToaster />
    <DochLanguageToggle />
  </div>
</template>

<style scoped>
.xn-app { min-height: 100vh; display: flex; flex-direction: column; background: #F9FAFB; }
.xn-app__main { flex: 1; width: 100%; max-width: 1080px; margin: 0 auto; padding: 1.75rem 1.25rem 3rem; }

@media (max-width: 640px) {
  .xn-app__main { padding: 1rem 0.85rem 2rem; }
}
</style>

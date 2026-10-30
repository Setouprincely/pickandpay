<script setup lang="ts">
// Minimal full-screen layout (no app chrome) for sign-in / sign-up and the
// public receiver tracking page. Just mounts the toast stack.
</script>

<template>
  <div class="xn-blank">
    <slot />
    <XenderToaster />
    <DochLanguageToggle />
  </div>
</template>

<style scoped>
.xn-blank { min-height: 100vh; background: #F9FAFB; }
</style>

<script setup lang="ts">
// In-app toast stack for Xender. Mounted once in the default layout.
const { toasts, dismiss } = useNotify()
</script>

<template>
  <div class="xn-toaster">
    <TransitionGroup name="xn-toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="xn-toast"
        :class="`xn-toast--${t.variant}`"
        role="status"
        @click="dismiss(t.id)"
      >
        <UIcon :name="t.icon" class="xn-toast__icon" />
        <div class="xn-toast__body">
          <p class="xn-toast__title">{{ t.title }}</p>
          <p v-if="t.message" class="xn-toast__msg">{{ t.message }}</p>
        </div>
        <button class="xn-toast__close" aria-label="Dismiss" @click.stop="dismiss(t.id)">
          <UIcon name="i-heroicons-x-mark" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.xn-toaster {
  position: fixed; top: 1rem; right: 1rem; z-index: 100;
  display: flex; flex-direction: column; gap: 0.6rem; width: min(360px, calc(100vw - 2rem));
}
.xn-toast {
  display: flex; align-items: flex-start; gap: 0.7rem; cursor: pointer;
  background: #fff; border: 1px solid #E5E7EB; border-left-width: 4px;
  border-radius: 0.6rem; padding: 0.8rem 0.9rem; box-shadow: 0 10px 25px -10px rgba(0,0,0,0.25);
}
.xn-toast--success { border-left-color: #16A34A; }
.xn-toast--info    { border-left-color: #2563EB; }
.xn-toast--error   { border-left-color: #E8202A; }
.xn-toast__icon { flex-shrink: 0; width: 1.3rem; height: 1.3rem; margin-top: 0.05rem; }
.xn-toast--success .xn-toast__icon { color: #16A34A; }
.xn-toast--info    .xn-toast__icon { color: #2563EB; }
.xn-toast--error   .xn-toast__icon { color: #E8202A; }
.xn-toast__body { flex: 1; min-width: 0; }
.xn-toast__title { font-size: 0.85rem; font-weight: 700; color: #111827; margin: 0; }
.xn-toast__msg { font-size: 0.78rem; color: #6B7280; margin: 0.15rem 0 0; line-height: 1.45; }
.xn-toast__close { flex-shrink: 0; background: none; border: none; cursor: pointer; color: #9CA3AF; padding: 0.1rem; }
.xn-toast__close:hover { color: #4B5563; }
.xn-toast__close :deep(svg) { width: 1.05rem; height: 1.05rem; }

.xn-toast-enter-active, .xn-toast-leave-active { transition: all 0.3s ease; }
.xn-toast-enter-from { opacity: 0; transform: translateX(20px); }
.xn-toast-leave-to { opacity: 0; transform: translateX(20px); }
</style>

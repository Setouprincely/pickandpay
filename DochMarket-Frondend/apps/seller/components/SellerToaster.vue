<script setup lang="ts">
// Renders the stack of toast notifications (see useNotify). Mounted once in the layout.
const { toasts, dismiss } = useNotify()
</script>

<template>
  <Teleport to="body">
    <div class="toaster" aria-live="polite" aria-atomic="true">
      <TransitionGroup name="toast">
        <div
          v-for="t in toasts"
          :key="t.id"
          class="toast"
          :class="`toast--${t.variant}`"
          role="status"
        >
          <UIcon :name="t.icon" class="toast__icon" />
          <div class="toast__body">
            <p class="toast__title">{{ t.title }}</p>
            <p v-if="t.message" class="toast__msg">{{ t.message }}</p>
          </div>
          <button class="toast__close" aria-label="Dismiss" @click="dismiss(t.id)">
            <UIcon name="i-heroicons-x-mark" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toaster {
  position: fixed; top: 1.25rem; right: 1.25rem; z-index: 400;
  display: flex; flex-direction: column; gap: 0.625rem;
  width: 20rem; max-width: calc(100vw - 2rem); pointer-events: none;
}
.toast {
  pointer-events: auto;
  display: flex; align-items: flex-start; gap: 0.625rem;
  background: #fff; border-radius: 0.5rem; padding: 0.75rem 0.875rem;
  box-shadow: 0 12px 32px rgba(0,0,0,0.18); border-left: 4px solid #16A34A;
}
.toast--success { border-left-color: #16A34A; }
.toast--info { border-left-color: var(--color-primary, #E8202A); }
.toast--error { border-left-color: #DC2626; }

.toast__icon { width: 1.25rem; height: 1.25rem; flex-shrink: 0; margin-top: 0.05rem; }
.toast--success .toast__icon { color: #16A34A; }
.toast--info .toast__icon { color: var(--color-primary, #E8202A); }
.toast--error .toast__icon { color: #DC2626; }

.toast__body { flex: 1; min-width: 0; }
.toast__title { font-size: 0.8125rem; font-weight: 700; color: #111827; margin: 0; }
.toast__msg {
  font-size: 0.75rem; color: #6B7280; margin: 0.15rem 0 0; line-height: 1.35;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.toast__close {
  background: none; border: none; cursor: pointer; color: #9CA3AF; flex-shrink: 0; padding: 0.1rem;
  display: flex; align-items: center; justify-content: center;
}
.toast__close:hover { color: #111827; }
.toast__close :deep(svg) { width: 1rem; height: 1rem; }

.toast-enter-active, .toast-leave-active { transition: all 0.25s ease; }
.toast-enter-from { opacity: 0; transform: translateX(110%); }
.toast-leave-to { opacity: 0; transform: translateX(110%); }
.toast-move { transition: transform 0.25s ease; }

@media (max-width: 480px) {
  .toaster { top: 0.75rem; right: 0.75rem; left: 0.75rem; width: auto; }
}
</style>

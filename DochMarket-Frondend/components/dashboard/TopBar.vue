<script setup lang="ts">
const { $t } = useNuxtApp()
const { get } = useApi()

const search = ref('')
const openSidebar = inject<() => void>('openSidebar', () => {})

// ── Real notification count ── GET /api/notifications returns unread list
const notifications = ref(0)
// ── Real logged-in user ── GET /api/users/me
const fullName = ref('')

const initials = computed(() => {
  const n = fullName.value.trim()
  if (!n) return 'AD'
  const parts = n.split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || 'AD'
})

onMounted(async () => {
  try {
    const [notifs, me] = await Promise.all([
      get<any[]>('/api/notifications').catch(() => []),
      get<any>('/api/users/me').catch(() => null),
    ])
    notifications.value = Array.isArray(notifs) ? notifs.length : 0
    if (me) {
      fullName.value =
        me.fullName ??
        `${me.firstName ?? ''} ${me.lastName ?? ''}`.trim()
    }
  } catch {
    // leave defaults — topbar still renders
  }
})
</script>

<template>
  <div class="topbar">

    <!-- Left: hamburger (mobile/tablet) + dropdown + search -->
    <div class="topbar__left">
      <button class="topbar__hamburger" aria-label="Open sidebar" @click="openSidebar()">
        <UIcon name="i-heroicons-bars-3" class="topbar__ham-icon" />
      </button>

      <button class="topbar__dropdown" aria-label="Select product category">
        {{ $t('topbar.allProducts') }}
        <UIcon name="i-heroicons-chevron-down" class="topbar__chevron" />
      </button>

      <div class="topbar__divider topbar__divider--left" />

      <div class="topbar__search-wrap">
        <UIcon name="i-heroicons-magnifying-glass" class="topbar__search-icon" />
        <input
          v-model="search"
          type="text"
          :placeholder="$t('topbar.searchPlaceholder')"
          class="topbar__search-input"
          aria-label="Search products"
        />
      </div>
    </div>

    <!-- Right: bell + avatar -->
    <div class="topbar__right">
      <div class="topbar__bell-wrap">
        <button class="topbar__bell" aria-label="View notifications">
          <UIcon name="i-heroicons-bell" class="topbar__bell-icon" />
        </button>
        <span v-if="notifications > 0" class="topbar__badge">{{ notifications }}</span>
      </div>

      <div class="topbar__divider" />

      <button class="topbar__user" aria-label="User menu">
        <span class="topbar__avatar">{{ initials }}</span>
        <span class="topbar__username">{{ fullName || $t('topbar.role') }}</span>
        <UIcon name="i-heroicons-chevron-down" class="topbar__chevron" />
      </button>
    </div>

  </div>
</template>

<style scoped>
.topbar         { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem;
                  background: var(--color-surface); border: 1px solid var(--color-border);
                  border-radius: 0.75rem; padding: 0.5rem 0.75rem; min-width: 0; }

.topbar__left   { display: flex; align-items: center; gap: 0.5rem; flex: 1; min-width: 0; }
.topbar__right  { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }

.topbar__hamburger  { display: flex; align-items: center; justify-content: center;
                       background: none; border: none; cursor: pointer; padding: 0.375rem;
                       border-radius: 0.5rem; flex-shrink: 0; min-width: 2.5rem; min-height: 2.5rem; }
.topbar__hamburger:hover { background: var(--color-page-bg); }
.topbar__ham-icon   { width: 1.25rem; height: 1.25rem; color: var(--color-text-secondary); }

.topbar__dropdown   { display: none; align-items: center; gap: 0.375rem; font-size: 0.875rem;
                       font-weight: 500; color: var(--color-text-primary); background: none;
                       border: none; cursor: pointer; white-space: nowrap; flex-shrink: 0; }

.topbar__divider      { width: 1px; height: 20px; background: var(--color-border); flex-shrink: 0; }
.topbar__divider--left { display: none; }
.topbar__chevron      { width: 1rem; height: 1rem; color: var(--color-text-muted); }

.topbar__search-wrap  { flex: 1; display: flex; align-items: center; gap: 0.5rem; min-width: 0;
                         border: 1px solid var(--color-border); border-radius: 0.5rem;
                         padding: 0.375rem 0.75rem; background: #fff; }
.topbar__search-icon  { width: 1rem; height: 1rem; color: var(--color-text-muted); flex-shrink: 0; }
.topbar__search-input { flex: 1; border: none; outline: none; font-size: 0.875rem; min-width: 0;
                         color: var(--color-text-primary); background: transparent; }
.topbar__search-input::placeholder { color: var(--color-text-muted); }

.topbar__bell-wrap  { position: relative; }
.topbar__bell       { background: none; border: none; cursor: pointer; padding: 0.375rem;
                       border-radius: 0.5rem; display: flex; align-items: center; justify-content: center;
                       min-width: 2.5rem; min-height: 2.5rem; }
.topbar__bell:hover { background: var(--color-page-bg); }
.topbar__bell-icon  { width: 1.25rem; height: 1.25rem; color: var(--color-text-secondary); }
.topbar__badge      { position: absolute; top: -2px; right: -2px; min-width: 18px; height: 18px;
                       background: var(--color-primary); color: #fff; font-size: 10px; font-weight: 700;
                       border-radius: 999px; display: flex; align-items: center; justify-content: center; padding: 0 3px; }

.topbar__user         { display: flex; align-items: center; gap: 0.5rem; background: none; border: none;
                         cursor: pointer; padding: 0.25rem 0.5rem; border-radius: 0.5rem;
                         min-height: 2.5rem; }
.topbar__user:hover   { background: var(--color-page-bg); }
.topbar__avatar       { width: 2rem; height: 2rem; border-radius: 50%; background: var(--color-primary);
                         color: #ffffff; font-size: 0.6875rem; font-weight: 700;
                         display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.topbar__username     { display: none; font-size: 0.875rem; font-weight: 500; color: var(--color-text-primary);
                         max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

@media (min-width: 640px) {
  .topbar__username { display: inline; }
}

@media (min-width: 1024px) {
  .topbar__hamburger      { display: none; }
  .topbar__dropdown       { display: flex; }
  .topbar__divider--left  { display: block; }
  .topbar                 { padding: 0.625rem 1rem; gap: 1rem; }
  .topbar__left           { gap: 0.75rem; }
  .topbar__right          { gap: 0.75rem; }
}
</style>

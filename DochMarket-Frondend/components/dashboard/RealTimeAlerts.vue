<script setup lang="ts">
type Notification = {
  id: string
  message: string
  type: string
  sentAt: string
  read: boolean
}

const { get, patch } = useApi()
const notifications = ref<Notification[]>([])
const loading = ref(true)
const error = ref(false)

const timeAgo = (iso: string) => {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60_000)
  if (mins < 1) return 'Just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

const formatType = (type: string) =>
  type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())

// Fetch helper — reused by initial load AND the 20s poll.
async function fetchAlerts(initial = false) {
  if (initial) loading.value = true
  try {
    const res = await get<Notification[]>('/api/notifications')
    notifications.value = Array.isArray(res) ? res : []
    error.value = false
  } catch {
    if (initial) error.value = true   // only flip to error state on first load
  } finally {
    if (initial) loading.value = false
  }
}

// ── REAL-TIME: poll every 20s ──────────────────────────────────────────────
// The backend has no websocket/SSE for notifications, so we poll. The interval
// is cleared on unmount to avoid leaks / background requests.
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchAlerts(true)
  pollTimer = setInterval(() => fetchAlerts(false), 20_000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})

const dismissing = ref<string | null>(null)
const markRead = async (id: string) => {
  dismissing.value = id
  try {
    await patch(`/api/notifications/${id}/read`)
    // Optimistic remove, then refetch so the list reflects backend truth.
    notifications.value = notifications.value.filter(n => n.id !== id)
    await fetchAlerts(false)
  } catch {
    console.error('Failed to mark as read')
  } finally {
    dismissing.value = null
  }
}
</script>

<template>
  <div class="alerts-card">
    <div class="alerts-header">
      <span class="alerts-heading">
        <UIcon name="i-heroicons-exclamation-triangle" class="alerts-icon" />
        {{ $t('dashboard.alerts.title') }}
        <span v-if="notifications.length > 0" class="alerts-badge">{{ notifications.length }}</span>
      </span>
      <a href="#" class="alerts-link">{{ $t('dashboard.alerts.viewAll') }}</a>
    </div>

    <div v-if="loading" class="alert-state">Loading…</div>
    <div v-else-if="error" class="alert-state alert-state--error">Failed to load</div>
    <div v-else-if="notifications.length === 0" class="alert-state">✅ No new alerts</div>

    <template v-else>
      <div v-for="(notif, i) in notifications" :key="notif.id">
        <div class="alert-item">
          <div class="alert-row">
            <div class="alert-body">
              <p class="alert-title">{{ notif.message }}</p>
              <p class="alert-sub">{{ formatType(notif.type) }} · {{ timeAgo(notif.sentAt) }}</p>
            </div>
            <button
              class="alert-dismiss"
              :disabled="dismissing === notif.id"
              title="Mark as read"
              @click="markRead(notif.id)"
            >✕</button>
          </div>
        </div>
        <div v-if="i < notifications.length - 1" class="alerts-divider" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.alerts-card    { background: var(--color-alert-bg); border: 1px solid var(--color-alert-border); border-radius: 0.75rem; overflow: hidden; min-width: 0; }
.alerts-header  { display: flex; justify-content: space-between; align-items: center; padding: 0.75rem 1rem; }
.alerts-heading { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; font-weight: 700; color: var(--color-text-primary); }
.alerts-icon    { width: 1rem; height: 1rem; color: var(--color-alert-icon); flex-shrink: 0; }
.alerts-badge   { background: #EF4444; color: #fff; font-size: 0.6rem; font-weight: 700; border-radius: 999px; padding: 0.1rem 0.4rem; }
.alerts-link    { font-size: 0.75rem; color: var(--color-primary); text-decoration: none; font-weight: 600; padding: 0.5rem 0; min-height: 2.5rem; display: flex; align-items: center; }
.alerts-link:hover { color: var(--color-primary-hover); }
.alert-state    { padding: 1.5rem 1rem; font-size: 0.8125rem; color: var(--color-text-secondary); text-align: center; }
.alert-state--error { color: #DC2626; }
.alert-item     { padding: 0.625rem 1rem; }
.alert-row      { display: flex; align-items: flex-start; justify-content: space-between; gap: 0.5rem; }
.alert-body     { flex: 1; min-width: 0; }
.alert-title    { font-size: 0.8125rem; font-weight: 600; color: var(--color-alert-title); margin: 0 0 0.125rem; }
.alert-sub      { font-size: 0.75rem; color: var(--color-alert-sub); margin: 0; }
.alert-dismiss  { background: none; border: none; cursor: pointer; color: var(--color-text-secondary); font-size: 0.75rem; padding: 0.25rem 0.375rem; border-radius: 0.25rem; flex-shrink: 0; }
.alert-dismiss:hover:not(:disabled) { background: rgba(0,0,0,0.06); }
.alert-dismiss:disabled { opacity: 0.4; cursor: not-allowed; }
.alerts-divider { height: 1px; background: #FFFFFF; margin: 0 1rem; }
@media (min-width: 1024px) {
  .alerts-header { padding: 0.75rem 1.25rem; }
  .alert-item    { padding: 0.625rem 1.25rem; }
  .alerts-divider { margin: 0 1.25rem; }
  .alerts-heading { font-size: 0.8125rem; }
  .alerts-link   { font-size: 0.8125rem; }
}
</style>

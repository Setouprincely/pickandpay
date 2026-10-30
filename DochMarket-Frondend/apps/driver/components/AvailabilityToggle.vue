<script setup lang="ts">
// Availability toggle — PATCH /api/drivers/me/availability  { isAvailable: boolean }
// Reflects the real current state on load (read from GET /api/drivers/me/performance
// is NOT where availability lives; the driver profile carries it). We read it from
// GET /api/users/me if present, otherwise default to the last known value and let
// the toggle drive the backend. The PATCH body uses the exact DTO field `isAvailable`.
const { $t } = useNuxtApp()
const { get, patch } = useDriverApi()

const isAvailable = ref(false)
const loading = ref(true)
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    // The driver's own record carries availability. /api/users/me returns the
    // current user; the field may be `available` or `isAvailable` depending on
    // Jackson, so we read both.
    const me = await get<any>('/api/drivers/me/profile').catch(() => null)
    if (me) {
      isAvailable.value = me.available ?? me.isAvailable ?? false
    }
  } catch {
    // leave default false
  } finally {
    loading.value = false
  }
})

async function toggle() {
  const next = !isAvailable.value
  saving.value = true
  error.value = ''
  try {
    // PATCH /api/drivers/me/availability — body { isAvailable }
    await patch('/api/drivers/me/availability', { isAvailable: next })
    isAvailable.value = next
  } catch (e: any) {
    error.value = e?.data?.message ?? `Could not update availability (${e?.status ?? 'network error'})`
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="card">
    <p class="card-label">{{ $t('driver.settings.availability') ?? 'Availability' }}</p>
    <div class="avail-row">
      <div class="avail-info">
        <p class="avail-name">{{ isAvailable ? '🟢 Online — available for deliveries' : '⚪ Offline — dispatch cannot assign you' }}</p>
        <p class="avail-desc">{{ isAvailable ? 'Dispatch can assign you new jobs.' : 'Signing in does not put you online. Switch this on when you are ready to drive.' }}</p>
      </div>
      <label class="switch" :class="{ 'switch--off': saving }">
        <input type="checkbox" :checked="isAvailable" :disabled="loading || saving" @change="toggle" />
        <span class="track" />
      </label>
    </div>
    <p v-if="error" class="avail-err">{{ error }}</p>
  </section>
</template>

<style scoped>
.card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; margin-bottom: 1rem; }
.card-label { font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--color-text-primary); margin: 0 0 1rem; }
.avail-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
.avail-info { flex: 1; }
.avail-name { font-size: 0.875rem; font-weight: 600; color: var(--color-text-primary); margin: 0; }
.avail-desc { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0.25rem 0 0; }
.switch { position: relative; width: 44px; height: 24px; flex-shrink: 0; cursor: pointer; }
.switch input { display: none; }
.track { position: absolute; inset: 0; background: #D1D5DB; border-radius: 999px; transition: background 0.25s; }
.track::before { content: ''; position: absolute; width: 18px; height: 18px; left: 3px; top: 3px; background: #fff; border-radius: 50%; transition: transform 0.25s; box-shadow: 0 1px 3px rgba(0,0,0,0.2); }
.switch input:checked + .track { background: #16A34A; }
.switch input:checked + .track::before { transform: translateX(20px); }
.switch--off { opacity: 0.6; }
.avail-err { color: #DC2626; font-size: 0.75rem; margin: 0.75rem 0 0; }
</style>

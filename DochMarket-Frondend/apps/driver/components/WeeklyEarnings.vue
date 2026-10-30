<script setup lang="ts">
const { $t } = useNuxtApp()
const { get } = useDriverApi()

const loading = ref(true)
const bars = ref<{ day: string; amount: number }[]>([])

const dayKeys = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const dayKey: Record<string, string> = {
  Mon: 'mon', Tue: 'tue', Wed: 'wed',
  Thu: 'thu', Fri: 'fri', Sat: 'sat', Sun: 'sun',
}
const dayLabel = (d: string) => $t('driver.we.' + (dayKey[d] ?? d.toLowerCase()))

// REAL WalletTransactionDTO (confirmed from backend source):
//   { id, amount, type, description, date }
// type values: CREDIT_PENDING | RELEASE_TO_AVAILABLE | PAYOUT_DEBIT
// The previous code filtered type === 'CREDIT' (which never exists) so the chart
// was always zero. Earnings = the two credit types below. The date field is
// `date` (not `createdAt`).
const CREDIT_TYPES = ['CREDIT_PENDING', 'RELEASE_TO_AVAILABLE']

onMounted(async () => {
  try {
    const history = await get<any[]>('/api/wallets/me/history')
    const earnings: Record<string, number> = {}
    dayKeys.forEach(d => { earnings[d] = 0 })

    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
    for (const tx of (history ?? [])) {
      if (!CREDIT_TYPES.includes(tx.type)) continue
      const when = tx.date ?? tx.createdAt   // backend field is `date`
      if (!when) continue
      const dayName = dayNames[new Date(when).getDay()]
      if (dayName in earnings) {
        earnings[dayName] += (tx.amount ?? 0)
      }
    }

    bars.value = dayKeys.map(day => ({ day, amount: earnings[day] ?? 0 }))
  } catch {
    bars.value = dayKeys.map(day => ({ day, amount: 0 }))
  } finally {
    loading.value = false
  }
})

const max = computed(() => Math.max(...bars.value.map(b => b.amount), 1))
const height = (amount: number) => `${Math.round((amount / max.value) * 100)}%`
const fmt = (n: number) => `${n.toLocaleString('fr-FR')} XAF`
</script>

<template>
  <div class="we-card">
    <h2 class="we-title">{{ $t('driver.we.title') }}</h2>

    <div v-if="loading" class="we-state">Loading…</div>

    <div v-else class="we-chart">
      <div v-for="b in bars" :key="b.day" class="we-col">
        <span class="we-amount">{{ fmt(b.amount) }}</span>
        <div class="we-bar-wrap">
          <div class="we-bar" :style="{ height: height(b.amount) }" />
        </div>
        <span class="we-day">{{ dayLabel(b.day) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.we-card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.we-title { font-size: 0.9375rem; font-weight: 600; color: var(--color-text-primary); margin: 0 0 1.5rem; }
.we-state { padding: 2rem; text-align: center; font-size: 0.8125rem; color: var(--color-text-secondary); }
.we-chart { display: grid; grid-template-columns: repeat(7, 1fr); gap: 0.5rem; height: 260px; align-items: end; }
.we-col { display: flex; flex-direction: column; align-items: center; height: 100%; gap: 0.5rem; }
.we-amount { font-size: 0.6875rem; color: var(--color-text-muted); white-space: nowrap; }
.we-bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.we-bar { width: 100%; max-width: 70px; border-radius: 0.5rem 0.5rem 0 0; background: linear-gradient(to bottom, #34D399 0%, #059669 100%); min-height: 4px; }
.we-day { font-size: 0.75rem; color: var(--color-text-secondary); }
@media (max-width: 640px) { .we-amount { font-size: 0.5625rem; } .we-chart { gap: 0.25rem; } }
</style>

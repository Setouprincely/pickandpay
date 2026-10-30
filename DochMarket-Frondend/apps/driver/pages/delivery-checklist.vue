<script setup lang="ts">
const { $t } = useNuxtApp()

// ── Delivery checklist ── (key = i18n key under driver.checklist)
const checklist = reactive([
  { key: 'item1', checked: false },
  { key: 'item2', checked: false },
  { key: 'item3', checked: false },
  { key: 'item4', checked: false },
  { key: 'item5', checked: false },
  { key: 'item6', checked: false },
  { key: 'item7', checked: false },
])

// ── Failed-delivery reasons ──
const reasons = reactive([
  { key: 'reason1', checked: false },
  { key: 'reason2', checked: false },
  { key: 'reason3', checked: false },
  { key: 'reason4', checked: false },
  { key: 'reason5', checked: false },
])
const otherChecked = ref(false)
const otherText = ref('')
</script>

<template>
  <div class="checklist-page">

    <!-- Back to My Deliveries -->
    <NuxtLink to="/deliveries" class="back-link">
      <UIcon name="i-heroicons-arrow-left" class="back-link__icon" />
      {{ $t('driver.checklist.back') }}
    </NuxtLink>

    <!-- ── Delivery Checklist ── -->
    <section class="card">
      <div class="card-head">
        <h2 class="card-title">{{ $t('driver.checklist.title') }}</h2>
        <span class="status">
          {{ $t('driver.checklist.status') }}:
          <span class="status-badge"><span class="status-dot" /> {{ $t('driver.checklist.statusPending') }}</span>
        </span>
      </div>
      <p class="card-meta">{{ $t('driver.checklist.deliveryNumber') }} : xxxx xxxx</p>

      <hr class="divider" />

      <p class="group-label">{{ $t('driver.checklist.checklistItems') }}</p>
      <div class="chip-grid">
        <label v-for="(item, i) in checklist" :key="i" class="chip" :class="{ 'chip--on': item.checked }">
          <input v-model="item.checked" type="checkbox" class="chip-box" />
          <span class="chip-mark" />
          {{ $t('driver.checklist.' + item.key) }}
        </label>
      </div>
    </section>

    <!-- ── Failed Deliveries ── -->
    <section class="card">
      <h2 class="card-title">{{ $t('driver.checklist.failedTitle') }}</h2>
      <div class="card-meta-row">
        <span class="card-meta">{{ $t('driver.checklist.packageNumber') }} : xxxx xxxx</span>
        <span class="card-meta">{{ $t('driver.checklist.attemptDateTime') }} : 12/12/2025 | 10h30 (1)</span>
      </div>

      <p class="group-label">{{ $t('driver.checklist.reasonNonDelivery') }}</p>
      <div class="chip-grid">
        <label v-for="(r, i) in reasons" :key="i" class="chip" :class="{ 'chip--on': r.checked }">
          <input v-model="r.checked" type="checkbox" class="chip-box" />
          <span class="chip-mark" />
          {{ $t('driver.checklist.' + r.key) }}
        </label>

        <label class="chip" :class="{ 'chip--on': otherChecked }">
          <input v-model="otherChecked" type="checkbox" class="chip-box" />
          <span class="chip-mark" />
          {{ $t('driver.checklist.other') }} <span class="chip-hint">{{ $t('driver.checklist.textField') }}</span>
        </label>
      </div>

      <textarea
        v-if="otherChecked"
        v-model="otherText"
        class="other-input"
        rows="3"
        :placeholder="$t('driver.checklist.describeReason')"
      />
    </section>

  </div>
</template>

<style scoped>
.checklist-page { display: flex; flex-direction: column; gap: 1.25rem; padding: 1rem; min-width: 0; }

.back-link {
  display: inline-flex; align-items: center; gap: 0.5rem; align-self: flex-start;
  font-size: 0.875rem; font-weight: 500; color: var(--color-text-secondary);
  text-decoration: none; padding: 0.375rem 0.5rem; border-radius: 0.5rem; margin-bottom: -0.5rem;
}
.back-link:hover { color: var(--color-primary); background: var(--color-page-bg); }
.back-link__icon { width: 1.0625rem; height: 1.0625rem; }

.card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem 1.5rem; min-width: 0; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.card-title { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); margin: 0; }

.card-meta  { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0.375rem 0 0; }
.card-meta-row { display: flex; gap: 1.5rem; flex-wrap: wrap; margin-top: 0.375rem; }

.status      { font-size: 0.8125rem; color: var(--color-text-secondary); display: inline-flex; align-items: center; gap: 0.375rem; }
.status-badge{ display: inline-flex; align-items: center; gap: 0.375rem; background: #EFF6FF; color: #2563EB;
               font-weight: 600; padding: 0.15rem 0.6rem; border-radius: 999px; font-size: 0.75rem; }
.status-dot  { width: 7px; height: 7px; border-radius: 50%; background: #2563EB; }

.divider { border: none; border-top: 1px solid var(--color-border); margin: 1rem 0; }

.group-label { font-size: 0.75rem; font-weight: 600; color: #2563EB; margin: 0 0 0.75rem; }

/* Chips */
.chip-grid { display: flex; flex-wrap: wrap; gap: 0.625rem; }
.chip {
  display: inline-flex; align-items: center; gap: 0.5rem;
  border: 1px solid var(--color-border); border-radius: 0.5rem;
  padding: 0.5rem 0.75rem; font-size: 0.8125rem; color: var(--color-text-primary);
  cursor: pointer; transition: all 0.15s; background: #fff;
}
.chip:hover  { border-color: #C7CDD4; }
.chip--on    { border-color: var(--color-primary); background: #FEF2F2; }
.chip-box    { display: none; }
.chip-mark   { width: 15px; height: 15px; border: 1.5px solid #C7CDD4; border-radius: 4px; flex-shrink: 0; position: relative; }
.chip--on .chip-mark { background: var(--color-primary); border-color: var(--color-primary); }
.chip--on .chip-mark::after {
  content: ''; position: absolute; left: 4px; top: 1px;
  width: 4px; height: 8px; border: solid #fff; border-width: 0 2px 2px 0; transform: rotate(45deg);
}
.chip-hint { color: var(--color-text-muted); }

/* Other text field */
.other-input { width: 100%; margin-top: 0.875rem; border: 1px solid var(--color-border); border-radius: 0.5rem;
               padding: 0.625rem 0.75rem; font-size: 0.875rem; color: var(--color-text-primary);
               outline: none; resize: vertical; box-sizing: border-box; font-family: inherit; }
.other-input:focus { border-color: var(--color-primary); }

@media (min-width: 1024px) { .checklist-page { padding: 1.5rem; } }
</style>

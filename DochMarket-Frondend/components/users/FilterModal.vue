<script setup lang="ts">
const emit = defineEmits<{
  close: []
  apply: [filters: { role: string; status: string; dateFrom: string; dateTo: string }]
}>()

const { $t } = useNuxtApp()

const role   = ref('All')
const status = ref('All')
const dateFrom = ref('')
const dateTo   = ref('')

const roles    = ['All', 'Client', 'Vendor', 'Driver']
const statuses = ['All', 'Active', 'Inactive']

const roleLabel = (r: string) => r === 'All' ? $t('common.all') : $t('role.' + r.toLowerCase())
const statusLabel = (s: string) => s === 'All' ? $t('common.all') : $t('userStatus.' + s.toLowerCase())

function reset() {
  role.value     = 'All'
  status.value   = 'All'
  dateFrom.value = ''
  dateTo.value   = ''
}

function apply() {
  // Filtering happens client-side in UserTable (the backend has no filtered
  // /api/admin/users endpoint). We just hand the chosen filters to the parent.
  emit('apply', {
    role: role.value,
    status: status.value,
    dateFrom: dateFrom.value,
    dateTo: dateTo.value,
  })
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">

        <button class="modal-close" aria-label="Close" @click="$emit('close')">×</button>

        <div class="filter-group">
          <p class="filter-label">{{ $t('users.filterRole') }}</p>
          <div class="radio-row">
            <label v-for="r in roles" :key="r" class="radio-item">
              <input v-model="role" type="radio" :value="r" class="radio-input" />
              <span class="radio-custom" :class="{ 'radio-custom--active': role === r }" />
              {{ roleLabel(r) }}
            </label>
          </div>
        </div>

        <div class="filter-group">
          <p class="filter-label">{{ $t('users.filterStatus') }}</p>
          <div class="radio-row">
            <label v-for="s in statuses" :key="s" class="radio-item">
              <input v-model="status" type="radio" :value="s" class="radio-input" />
              <span class="radio-custom" :class="{ 'radio-custom--active': status === s }" />
              {{ statusLabel(s) }}
            </label>
          </div>
        </div>

        <div class="filter-group">
          <p class="filter-label">{{ $t('users.registrationDate') }}</p>
          <div class="date-row">
            <span class="date-prefix">{{ $t('common.from') }}</span>
            <input v-model="dateFrom" type="date" class="date-input" />
            <span class="date-prefix">{{ $t('common.to') }}</span>
            <input v-model="dateTo" type="date" class="date-input" />
          </div>
        </div>

        <div class="filter-actions">
          <button class="btn-primary" @click="apply">{{ $t('common.apply') }}</button>
          <button class="btn-secondary" @click="reset">{{ $t('common.reset') }}</button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card    { background: #fff; border-radius: 0.75rem; width: 100%; max-width: 400px; padding: 1.5rem; position: relative; box-shadow: 0 16px 48px rgba(0,0,0,0.18); }
.modal-close   { position: absolute; top: 0.75rem; right: 0.75rem; background: none; border: none; font-size: 1.25rem; cursor: pointer; color: var(--color-text-muted); padding: 0.25rem 0.375rem; border-radius: 4px; line-height: 1; }
.modal-close:hover { background: var(--color-page-bg); }
.filter-group  { margin-bottom: 1.125rem; }
.filter-label  { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--color-text-primary); margin: 0 0 0.625rem; }
.radio-row     { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.radio-item    { display: flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; color: var(--color-text-primary); cursor: pointer; }
.radio-input   { display: none; }
.radio-custom  { width: 14px; height: 14px; border-radius: 50%; border: 2px solid #D1D5DB; flex-shrink: 0; transition: all 0.15s; }
.radio-custom--active { border-color: var(--color-primary); background: var(--color-primary); }
.date-row      { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.date-prefix   { font-size: 0.875rem; color: var(--color-text-secondary); flex-shrink: 0; }
.date-input    { border: 1px solid var(--color-border); border-radius: 0.375rem; padding: 0.375rem 0.5rem; font-size: 0.8125rem; color: var(--color-text-primary); outline: none; flex: 1; min-width: 120px; }
.date-input:focus { border-color: var(--color-primary); }
.filter-actions { display: flex; gap: 0.75rem; margin-top: 1.25rem; }
.btn-primary    { flex: 1; background: var(--color-primary); color: #fff; border: none; border-radius: 0.5rem; padding: 0.625rem 1rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; }
.btn-primary:hover { background: var(--color-primary-hover); }
.btn-secondary  { flex: 1; background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.625rem 1rem; font-size: 0.875rem; cursor: pointer; }
.btn-secondary:hover { background: var(--color-page-bg); }
</style>

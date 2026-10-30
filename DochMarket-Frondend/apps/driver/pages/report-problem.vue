<script setup lang="ts">
const { $t } = useNuxtApp()
const { openNavigation } = useDriverNavigation()

const problemType = ref('Accident')
const description = ref('')

// Canonical (stable) values kept in English; only the display label is translated.
const problemTypes = [
  'Damaged package',
  'Accident',
  'Vehicle problem',
  'Customer problem',
  'Route problem',
  'Other',
]
const typeKey: Record<string, string> = {
  'Damaged package': 'type1',
  'Accident': 'type2',
  'Vehicle problem': 'type3',
  'Customer problem': 'type4',
  'Route problem': 'type5',
  'Other': 'type6',
}
const typeLabel = (t: string) => $t('driver.report.' + (typeKey[t] ?? t))

// ── Report persistence: POST /api/support/report, GET /api/support/reports/me ──
// Reports are stored in the database and alert DOCH admins.
type Report = { id: string; type: string; description: string; createdAt: string; status?: string }
const reports = ref<Report[]>([])
const { get, post } = useDriverApi()
const submitting = ref(false)

async function loadReports() {
  try {
    const list = await get<any[]>('/api/support/reports/me')
    reports.value = (list ?? []).map(r => ({ id: r.id, type: r.category, description: r.description, createdAt: r.createdAt, status: r.status }))
  } catch { reports.value = [] }
}
onMounted(loadReports)

// ── Submit ──
const formError = ref('')
const successMsg = ref('')

async function submit() {
  formError.value = ''
  successMsg.value = ''

  if (!problemType.value) { formError.value = $t('driver.report.errType') ?? 'Please choose a problem type.'; return }
  if (!description.value.trim()) { formError.value = $t('driver.report.errDesc') ?? 'Please describe the problem.'; return }

  submitting.value = true
  try {
    let deliveryId: string | undefined
    try {
      const active = await get<any[]>('/api/drivers/me/deliveries')
      deliveryId = active?.[0]?.deliveryId
    } catch { /* report without a delivery link */ }
    await post('/api/support/report', {
      category: problemType.value,
      subject: `Driver report: ${problemType.value}`,
      description: description.value.trim(),
      deliveryId,
    })
    successMsg.value = $t('driver.report.success') ?? 'Report submitted'
    description.value = ''
    await loadReports()
    setTimeout(() => { successMsg.value = '' }, 3000)
  } catch (e: any) {
    formError.value = e?.data?.message ?? `Could not send the report (${e?.status ?? 'network error'})`
  } finally {
    submitting.value = false
  }
}

// ── GPS navigation ──
// Navigates to the driver's first active delivery via GET /api/deliveries/{id}/route.
const gpsError = ref('')

async function onGps() {
  gpsError.value = ''
  // Find a delivery to navigate to (first non-delivered assignment).
  let deliveryId = ''
  try {
    const list = await get<any[]>('/api/drivers/me/deliveries')
    const active = (list ?? []).find((d: any) => d.status !== 'DELIVERED' && d.status !== 'CANCELLED') ?? (list ?? [])[0]
    deliveryId = active?.deliveryId ?? ''
  } catch {
    gpsError.value = $t('driver.map.routeUnavailable') ?? 'Route not available for this delivery'
    return
  }

  const res = await openNavigation(deliveryId)
  if (!res.ok) {
    gpsError.value = $t('driver.map.routeUnavailable') ?? 'Route not available for this delivery'
    setTimeout(() => { gpsError.value = '' }, 3000)
  }
}

const fmtDate = (iso: string) =>
  new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
</script>

<template>
  <div class="report-page">
    <section class="card">
      <h2 class="card-title">{{ $t('driver.report.title') }}</h2>

      <!-- Details -->
      <p class="group-label">{{ $t('driver.report.details') }}</p>
      <div class="detail-row">
        <span class="detail">{{ $t('driver.report.packageNumber') }} : xxxx xxxx</span>
        <span class="detail">{{ $t('driver.report.attemptDateTime') }} : 12/12/2025 | 10h30 (1)</span>
      </div>

      <hr class="divider" />

      <!-- Reason -->
      <p class="group-label">{{ $t('driver.report.reasonNonDelivery') }}</p>

      <div class="field">
        <label class="lbl">{{ $t('driver.report.problemType') }}</label>
        <select v-model="problemType" class="sel">
          <option v-for="t in problemTypes" :key="t" :value="t">{{ typeLabel(t) }}</option>
        </select>
      </div>

      <p class="group-label">{{ $t('driver.report.description') }}</p>
      <textarea v-model="description" class="desc" rows="3" :placeholder="$t('driver.report.describeProblem')" />

      <!-- Map -->
      <div class="map-card">
        <div class="map-header">
          <span class="map-title">{{ $t('driver.map.title') }}</span>
          <button class="gps-btn" @click="onGps">
            <UIcon name="i-heroicons-paper-airplane" class="gps-btn__icon" />
            {{ $t('driver.map.gps') }}
          </button>
        </div>

        <div class="map-canvas">
          <svg class="map-illustration" viewBox="0 0 400 280" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
            <rect width="400" height="280" fill="#E9EDF0" />
            <rect x="20" y="25" width="70" height="50" rx="8" fill="#D6E8D2" />
            <rect x="300" y="170" width="80" height="80" rx="8" fill="#D6E8D2" />
            <path d="M -10 80 C 80 110, 140 60, 210 100 S 340 170, 420 140 L 420 185 C 340 215, 260 150, 200 165 S 70 160, -10 125 Z"
                  fill="#BFE0EF" opacity="0.9" />
            <g stroke="#FFFFFF" stroke-linecap="round">
              <line x1="0" y1="55"  x2="400" y2="55"  stroke-width="8" />
              <line x1="0" y1="150" x2="400" y2="150" stroke-width="10" />
              <line x1="0" y1="220" x2="400" y2="220" stroke-width="7" />
              <line x1="90"  y1="0" x2="90"  y2="280" stroke-width="8" />
              <line x1="200" y1="0" x2="200" y2="280" stroke-width="10" />
              <line x1="310" y1="0" x2="310" y2="280" stroke-width="7" />
            </g>
            <g fill="#DEE3E7">
              <rect x="105" y="65" width="40" height="30" rx="3" />
              <rect x="215" y="70" width="45" height="25" rx="3" />
              <rect x="105" y="165" width="40" height="35" rx="3" />
            </g>
          </svg>

          <svg class="map-route" viewBox="0 0 100 100" preserveAspectRatio="none">
            <polyline points="22,80 22,55 55,55 55,30 78,30" fill="none" stroke="#2563EB" stroke-width="0.9" />
          </svg>

          <span class="dest-marker">
            <UIcon name="i-heroicons-map-pin-solid" class="dest-pin" />
          </span>

          <!-- Next destination + location actions -->
          <div class="next-card">
            <div class="next-row">
              <div>
                <p class="next-label">{{ $t('driver.route.nextDestination') }}</p>
                <p class="next-sub">12 Rue du Sentier, 75002 Paris</p>
              </div>
              <span class="next-time">10:30 - 11:30</span>
            </div>
            <button class="loc-btn" @click="onGps">{{ $t('driver.report.automaticGps') }}</button>
          </div>
        </div>

        <p v-if="gpsError" class="gps-error">{{ gpsError }}</p>
      </div>

      <!-- Submit -->
      <div class="actions">
        <button class="btn-submit" @click="submit">{{ $t('driver.report.submit') }}</button>
        <span v-if="successMsg" class="submit-ok">{{ successMsg }}</span>
        <span v-if="formError" class="submit-err">{{ formError }}</span>
      </div>
    </section>

    <!-- Previously submitted reports -->
    <section v-if="reports.length" class="card">
      <h2 class="card-title">{{ $t('driver.report.previous') ?? 'Submitted reports' }}</h2>
      <div class="rep-list">
        <div v-for="r in reports" :key="r.id" class="rep-item">
          <div class="rep-head">
            <span class="rep-type">{{ typeLabel(r.type) }}</span>
            <span class="rep-date">{{ fmtDate(r.createdAt) }}</span>
          </div>
          <p class="rep-desc">{{ r.description }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.report-page { padding: 1rem; min-width: 0; display: flex; flex-direction: column; gap: 1.25rem; }
.card        { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem 1.5rem; min-width: 0; }
.card-title  { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 1rem; }

.group-label { font-size: 0.75rem; font-weight: 600; color: #2563EB; margin: 0 0 0.625rem; }
.detail-row  { display: flex; gap: 1.5rem; flex-wrap: wrap; }
.detail      { font-size: 0.8125rem; color: var(--color-text-primary); }

.divider { border: none; border-top: 1px solid var(--color-border); margin: 1rem 0; }

.field { display: flex; flex-direction: column; gap: 0.3rem; margin-bottom: 1rem; }
.lbl   { font-size: 0.8125rem; color: var(--color-text-secondary); }
.sel   { border: 1px solid var(--color-border); border-radius: 0.375rem; padding: 0.5rem 0.75rem;
         font-size: 0.875rem; color: var(--color-text-primary); outline: none; background: #fff; width: 100%; appearance: none;
         background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%239CA3AF' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
         background-repeat: no-repeat; background-position: right 0.75rem center; cursor: pointer; }
.sel:focus { border-color: var(--color-primary); }

.desc { width: 100%; border: 1px solid var(--color-border); border-radius: 0.375rem; padding: 0.625rem 0.75rem;
        font-size: 0.875rem; color: var(--color-text-primary); outline: none; resize: vertical; box-sizing: border-box;
        font-family: inherit; background: #F9FAFB; margin-bottom: 1.25rem; }
.desc:focus { border-color: var(--color-primary); background: #fff; }
.desc::placeholder { color: var(--color-text-muted); }

/* Map */
.map-card   { border: 1px solid var(--color-border); border-radius: 0.625rem; padding: 1rem; }
.map-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 0.875rem; flex-wrap: wrap; }
.map-title  { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); }
.gps-btn       { display: flex; align-items: center; gap: 0.5rem; background: #2563EB; color: #fff;
                 border: none; border-radius: 0.5rem; padding: 0.5rem 1rem; font-size: 0.8125rem; font-weight: 600; cursor: pointer; }
.gps-btn:hover { background: #1D4ED8; }
.gps-btn__icon { width: 1rem; height: 1rem; }
.gps-error { margin: 0.75rem 0 0; font-size: 0.8125rem; color: #DC2626; }

.map-canvas { position: relative; height: 320px; border-radius: 0.5rem; overflow: hidden; }
.map-illustration { position: absolute; inset: 0; width: 100%; height: 100%; }
.map-route  { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 1; }
.dest-marker { position: absolute; left: 78%; top: 30%; transform: translate(-50%, -100%); z-index: 2; }
.dest-pin    { width: 2rem; height: 2rem; color: #16A34A; filter: drop-shadow(0 2px 3px rgba(0,0,0,0.3)); }

.next-card { position: absolute; left: 0.75rem; right: 0.75rem; bottom: 0.75rem; z-index: 3;
             background: #fff; border-radius: 0.625rem; padding: 0.875rem 1rem;
             box-shadow: 0 4px 16px rgba(0,0,0,0.12); display: flex; flex-direction: column; gap: 0.625rem; }
.next-row    { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
.next-label  { font-size: 0.875rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 0.125rem; }
.next-sub    { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0; }
.next-time   { font-size: 0.8125rem; color: var(--color-text-secondary); white-space: nowrap; flex-shrink: 0; }

.loc-btn { width: 100%; background: #2563EB; color: #fff; border: none; border-radius: 0.5rem;
           padding: 0.625rem 1rem; font-size: 0.8125rem; font-weight: 600; cursor: pointer; }
.loc-btn:hover { background: #1D4ED8; }

/* Submit */
.actions   { margin-top: 1.25rem; display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.btn-submit { background: var(--color-primary); color: #fff; border: none; border-radius: 0.5rem;
              padding: 0.75rem 2rem; font-size: 0.875rem; font-weight: 700; cursor: pointer; }
.btn-submit:hover { background: var(--color-primary-hover); }
.submit-ok  { font-size: 0.875rem; font-weight: 600; color: #16A34A; }
.submit-err { font-size: 0.875rem; color: #DC2626; }

/* Previous reports */
.rep-list { display: flex; flex-direction: column; gap: 0.75rem; }
.rep-item { border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.75rem 1rem; }
.rep-head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 0.25rem; }
.rep-type { font-size: 0.8125rem; font-weight: 700; color: var(--color-text-primary); }
.rep-date { font-size: 0.75rem; color: var(--color-text-muted); }
.rep-desc { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0; }

@media (max-width: 767px) { .detail-row { flex-direction: column; gap: 0.5rem; } }
@media (min-width: 1024px) { .report-page { padding: 1.5rem; } }
</style>

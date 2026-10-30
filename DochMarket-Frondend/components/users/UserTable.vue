<script setup lang="ts">
import UserDetailsModal from '~/components/users/UserDetailsModal.vue'

const { $t } = useNuxtApp()
const { get, patch, del } = useApi()

type Filters = { role: string; status: string; dateFrom: string; dateTo: string }
const props = withDefaults(defineProps<{ filters?: Filters }>(), {
  filters: () => ({ role: 'All', status: 'All', dateFrom: '', dateTo: '' }),
})

// Shared live search text written by the Topbar search box (see Topbar.vue).
// useState is global across components, so the topbar and this table share it
// without prop drilling through the layout.
const searchQuery = useState<string>('admin-user-search', () => '')

type ApiUser = {
  id: string
  firstName?: string
  lastName?: string
  email: string
  tel?: string
  phone?: string
  enabled?: boolean
  role?: string
  roles?: string[]
}

const users = ref<ApiUser[]>([])
const loading = ref(true)
const error = ref(false)

const actionError = ref('')

// Suspend / reactivate through the backend (PATCH /api/admin/users/{id}/status).
async function toggleSuspend(u: ApiUser) {
  closeMenu()
  actionError.value = ''
  try {
    await patch(`/api/admin/users/${u.id}/status?enabled=${!u.enabled}`, {})
    await load()
  } catch (e: any) {
    actionError.value = e?.data?.message || 'Could not change the account status.'
  }
}

// Soft delete (DELETE /api/admin/users/{id}): history is kept, the account disappears and can no longer sign in.
async function deleteUser(u: ApiUser) {
  closeMenu()
  actionError.value = ''
  if (!confirm(`Delete ${u.email}? They will no longer be able to sign in. Their past orders stay in the records.`)) return
  try {
    await del(`/api/admin/users/${u.id}`)
    await load()
  } catch (e: any) {
    actionError.value = e?.data?.message || 'Could not delete this user.'
  }
}
async function load() {
  loading.value = true
  error.value = false
  try {
    const res = await get<ApiUser[]>('/api/admin/users')
    users.value = Array.isArray(res) ? res : []
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)
defineExpose({ refresh: load })

const labelFor = (r: string) => {
  if (r.includes('ADMIN')) return 'Admin'
  if (r.includes('DELIVERY')) return 'Driver'
  if (r.includes('SELLER')) return 'Vendor'
  if (r.includes('BUYER')) return 'Client'
  return r.replace('ROLE_', '') || '—'
}
// One account can hold several roles (e.g. a vendor who also buys).
const roleLabels = (u: ApiUser) => [...new Set((u.roles?.length ? u.roles : [u.role ?? '']).map(labelFor))]
const roleClassFor = (label: string) =>
  ({ Admin: 'role--admin', Driver: 'role--driver', Vendor: 'role--vendor', Client: 'role--client' }[label] ?? 'role--client')

const statusLabel = (u: ApiUser) => (u.enabled ? 'Active' : 'Inactive')
const statusClass = (u: ApiUser) => {
  const s = statusLabel(u)
  return s === 'Active' ? 'status--active' : 'status--suspended'
}
const phone = (u: ApiUser) => u.tel ?? u.phone ?? '—'
const fullName = (u: ApiUser) => `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || '—'

const filtered = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  return users.value.filter((u) => {
    const f = props.filters
    if (f.role !== 'All' && !roleLabels(u).includes(f.role)) return false
    if (f.status !== 'All' && statusLabel(u) !== f.status) return false
    if (q) {
      const hay = `${fullName(u)} ${u.email}`.toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  })
})

const perPage = 10
const page = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / perPage)))
const paged = computed(() => filtered.value.slice((page.value - 1) * perPage, page.value * perPage))
watch(filtered, () => { page.value = 1 })

const openMenuId = ref<string | null>(null)
const toggleMenu = (id: string) => { openMenuId.value = openMenuId.value === id ? null : id }
const closeMenu = () => { openMenuId.value = null }

// View / Edit modal
const detailUserId = ref<string | null>(null)
const detailMode = ref<'view' | 'edit'>('view')
function openDetail(id: string, mode: 'view' | 'edit') {
  detailUserId.value = id
  detailMode.value = mode
  closeMenu()
}
function onDetailSaved() {
  load() // refetch so a role change is reflected from the backend, not just optimistic
}
</script>

<template>
  <div class="ut-card" @click="closeMenu">

    <div v-if="loading" class="ut-state">Loading users…</div>
    <div v-else-if="error" class="ut-state ut-state--error">Failed to load users</div>
    <div v-else-if="filtered.length === 0" class="ut-state">No users found</div>

    <template v-else>
      <div class="ut-scroll">
        <table class="ut-table">
          <thead>
            <tr>
              <th>{{ $t('users.colName') }}</th>
              <th>{{ $t('users.colEmail') }}</th>
              <th>{{ $t('users.colPhone') }}</th>
              <th>{{ $t('users.colRole') }}</th>
              <th>{{ $t('users.colStatus') }}</th>
              <th>{{ $t('users.colActions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in paged" :key="user.id" class="ut-row">
              <td class="ut-name">{{ fullName(user) }}</td>
              <td class="ut-email">{{ user.email }}</td>
              <td>{{ phone(user) }}</td>
              <td><span v-for="r in roleLabels(user)" :key="r" class="ut-role" :class="roleClassFor(r)" style="margin-right:0.25rem">{{ r }}</span></td>
              <td><span class="ut-status" :class="statusClass(user)">● {{ statusLabel(user) }}</span></td>
              <td class="ut-actions-cell">
                <div class="ut-menu-wrap">
                  <button class="ut-dots" aria-label="Row actions" @click.stop="toggleMenu(user.id)">⋮</button>
                  <div v-if="openMenuId === user.id" class="ut-dropdown" @click.stop>
                    <button class="ut-dropdown__item" @click="openDetail(user.id, 'view')">{{ $t('common.view') }}</button>
                    <button class="ut-dropdown__item" @click="openDetail(user.id, 'edit')">{{ $t('common.edit') }}</button>
                    <button class="ut-dropdown__item" @click="toggleSuspend(user)">
                      {{ user.enabled ? 'Suspend' : 'Activate' }}
                    </button>
                    <button class="ut-dropdown__item" style="color:#DC2626" @click="deleteUser(user)">Delete user</button>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="actionError" class="ut-state ut-state--error">{{ actionError }}</p>

      <div class="ut-pagination">
        <button class="pg-arrow" @click="page = Math.max(1, page - 1)">←</button>
        <button v-for="p in totalPages" :key="p" class="pg-btn" :class="{ 'pg-btn--active': page === p }" @click="page = p">
          {{ String(p).padStart(2, '0') }}
        </button>
        <button class="pg-arrow" @click="page = Math.min(totalPages, page + 1)">→</button>
      </div>
    </template>

    <UserDetailsModal
      v-if="detailUserId"
      :user-id="detailUserId"
      :mode="detailMode"
      @close="detailUserId = null"
      @saved="onDetailSaved"
    />
  </div>
</template>

<style scoped>
.ut-card   { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.ut-state  { padding: 2rem; text-align: center; font-size: 0.8125rem; color: var(--color-text-secondary); }
.ut-state--error { color: #DC2626; }
.ut-scroll { overflow-x: auto; }
.ut-table    { width: 100%; border-collapse: collapse; min-width: 560px; }
.ut-table th { font-size: 0.75rem; font-weight: 700; color: var(--color-text-muted); text-align: left; padding: 0.625rem 0.75rem; border-bottom: 1px solid var(--color-border); }
.ut-table td { font-size: 0.875rem; color: var(--color-text-primary); padding: 0.875rem 0.75rem; border-bottom: 1px solid var(--color-border); }
.ut-row:last-child td { border-bottom: none; }
.ut-name  { font-weight: 500; }
.ut-email { color: var(--color-text-secondary); }
.ut-role        { font-size: 0.75rem; font-weight: 600; padding: 0.2rem 0.625rem; border-radius: 999px; }
.role--client   { background: #DCFCE7; color: #16A34A; }
.role--driver   { background: #FEF9C3; color: #B45309; }
.role--vendor   { background: #FEE2E2; color: #DC2626; }
.role--admin    { background: #DBEAFE; color: #1D4ED8; }
.ut-status           { font-size: 0.8125rem; font-weight: 500; }
.status--active      { color: #16A34A; }
.status--pending     { background: #DBEAFE; color: #1D4ED8; padding: 0.2rem 0.625rem; border-radius: 999px; }
.status--suspended   { background: #FEE2E2; color: #DC2626; padding: 0.2rem 0.625rem; border-radius: 999px; }
.ut-actions-cell { position: relative; }
.ut-menu-wrap    { position: relative; display: inline-block; }
.ut-dots         { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: var(--color-text-muted); padding: 0.25rem 0.5rem; border-radius: 0.375rem; line-height: 1; }
.ut-dots:hover   { background: var(--color-page-bg); }
.ut-dropdown { position: absolute; right: 0; top: calc(100% + 4px); z-index: 20; background: #fff; border: 1px solid var(--color-border); border-radius: 0.5rem; box-shadow: 0 4px 16px rgba(0,0,0,0.1); min-width: 120px; padding: 0.25rem 0; }
.ut-dropdown__item { display: block; width: 100%; text-align: left; background: none; border: none; padding: 0.5rem 1rem; font-size: 0.875rem; color: var(--color-text-primary); cursor: pointer; }
.ut-dropdown__item:hover { background: var(--color-page-bg); }
.ut-pagination { display: flex; align-items: center; justify-content: center; gap: 0.375rem; margin-top: 1.25rem; }
.pg-btn        { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: none; font-size: 0.6875rem; background: transparent; color: var(--color-page-idle-text); cursor: pointer; }
.pg-btn--active{ background: var(--color-page-active-bg); color: #fff; font-weight: 700; }
.pg-arrow      { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: 1px solid var(--color-border); background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: 0.75rem; }
</style>

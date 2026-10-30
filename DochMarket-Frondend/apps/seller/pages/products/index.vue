<script setup lang="ts">
import type { SellerProduct, ProductStatus } from '~/apps/seller/types'

const { $t } = useNuxtApp()
const { products, loading, error, load, remove, removeMany, setStatus, setStock, activateMany } = useSellerProducts()
const { notify } = useNotify()
function goToNew() { void navigateTo('/products/new') }

const search = ref('')
const statusFilter = ref<'all' | ProductStatus>('all')
const selected = ref<Array<string | number>>([])

// Load real products on mount.
onMounted(load)

const statusItems = computed(() => ([
  { label: $t('seller.products.allStatus'), value: 'all' },
  { label: $t('seller.products.status.active'), value: 'active' },
  { label: $t('seller.products.status.draft'), value: 'draft' },
  { label: $t('seller.products.status.out_of_stock'), value: 'out_of_stock' },
]))

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return products.value.filter(p => {
    if (statusFilter.value !== 'all' && p.status !== statusFilter.value) return false
    if (!q) return true
    return p.title.toLowerCase().includes(q)
      || (p.sku || '').toLowerCase().includes(q)
      || (p.category || '').toLowerCase().includes(q)
  })
})

// Keep selection from referencing rows no longer visible.
watch(filtered, (list) => {
  const ids = new Set(list.map(p => p.id))
  selected.value = selected.value.filter(id => ids.has(id))
})

function onEdit(p: SellerProduct) { navigateTo(`/products/${p.id}`) }

const fail = (e: any) => notify({ title: e?.data?.message ?? `Request failed (${e?.status ?? 'network error'})`, variant: 'error' })

async function onDelete(p: SellerProduct) {
  if (!confirm($t('seller.products.confirmDelete'))) return
  try {
    await remove(p.id)
    notify({ title: $t('seller.products.toast.deleted'), variant: 'info' })
  } catch (e: any) { fail(e) }
}

async function onToggleStatus(p: SellerProduct) {
  const next: ProductStatus = p.status === 'draft' ? 'active' : 'draft'
  try {
    await setStatus(p.id, next)
    notify({ title: $t('seller.products.toast.statusUpdated') })
  } catch (e: any) { fail(e) }
}

async function onUpdateStock({ id, stock }: { id: string | number, stock: number }) {
  const res = await setStock(id, stock)
  if (!res.ok) {
    notify({ title: res.error ?? 'Update failed', variant: 'error' })
  } else {
    notify({ title: $t('seller.products.toast.stockUpdated') })
  }
}

async function bulkActivate() {
  try {
    await activateMany(selected.value)
    notify({ title: $t('seller.products.toast.activated') })
    selected.value = []
  } catch (e: any) { fail(e); await load() }
}

async function bulkDelete() {
  if (!confirm($t('seller.products.confirmDelete'))) return
  try {
    await removeMany(selected.value)
    notify({ title: $t('seller.products.toast.deleted'), variant: 'info' })
    selected.value = []
  } catch (e: any) { fail(e); await load() }
}

const pendingCount = computed(() => products.value.filter(p => p.publicationStatus === 'PENDING_REVIEW').length)
const rejected = computed(() => products.value.filter(p => p.publicationStatus === 'REJECTED'))
</script>

<template>
  <div class="products">
    <!-- Header -->
    <div class="products__head">
      <div>
        <h1 class="products__title">{{ $t('seller.products.title') }}</h1>
        <p class="products__sub">{{ $t('seller.products.subtitle') }}</p>
      </div>
      <UButton icon="i-lucide-plus" class="add-btn" @click="goToNew">
        {{ $t('seller.products.add') }}
      </UButton>
    </div>

    <!-- Error banner (surfaces backend 403/500 clearly) -->
    <div v-if="error" class="err-banner">{{ error }}</div>
    <div v-if="pendingCount" class="err-banner" style="background:#FFFBEB;color:#92400E;border-color:#F59E0B">
      {{ pendingCount }} product(s) awaiting DOCH review — they become visible to customers once approved.
    </div>
    <div v-for="p in rejected" :key="p.id" class="err-banner">
      “{{ p.title }}” was not approved: {{ p.rejectionReason }}. Edit and save it to resubmit.
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar__search">
        <UIcon name="i-heroicons-magnifying-glass" class="toolbar__search-icon" />
        <input v-model="search" type="text" :placeholder="$t('seller.products.search')" class="toolbar__search-input" />
      </div>
      <USelect v-model="statusFilter" :items="statusItems" value-key="value" size="md" class="toolbar__filter" />
    </div>

    <!-- Bulk bar -->
    <Transition name="slide">
      <div v-if="selected.length" class="bulkbar">
        <span class="bulkbar__count">{{ $t('seller.products.bulk.selected', { n: selected.length }) }}</span>
        <div class="bulkbar__actions">
          <UButton size="sm" variant="soft" color="success" icon="i-lucide-eye" @click="bulkActivate">
            {{ $t('seller.products.bulk.activate') }}
          </UButton>
          <UButton size="sm" variant="soft" color="error" icon="i-lucide-trash-2" @click="bulkDelete">
            {{ $t('seller.products.bulk.delete') }}
          </UButton>
        </div>
      </div>
    </Transition>

    <!-- Table / empty -->
    <div class="card">
      <div v-if="loading" class="no-results">Loading…</div>
      <SellerProductTable
        v-else-if="filtered.length"
        v-model:selected="selected"
        :products="filtered"
        @edit="onEdit"
        @delete="onDelete"
        @toggle-status="onToggleStatus"
        @update-stock="onUpdateStock"
      />
      <SellerEmptyState
        v-else-if="!products.length"
        :title="$t('seller.products.empty.title')"
        :sub="$t('seller.products.empty.sub')"
      >
        <UButton icon="i-lucide-plus" class="add-btn" @click="goToNew">
          {{ $t('seller.products.add') }}
        </UButton>
      </SellerEmptyState>
      <p v-else class="no-results">{{ $t('seller.products.noResults') }}</p>
    </div>
  </div>
</template>

<style scoped>
.products { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; min-width: 0; }

.products__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.products__title { font-size: 1.25rem; font-weight: 800; color: var(--color-text-primary); margin: 0; }
.products__sub { font-size: 0.825rem; color: var(--color-text-secondary); margin: 0.2rem 0 0; }
.add-btn { background: var(--color-button-red) !important; flex-shrink: 0; }

.err-banner { background: #FFF1F2; border: 1px solid #FCA5A5; color: #DC2626; border-radius: 0.625rem; padding: 0.6rem 0.9rem; font-size: 0.825rem; }

.toolbar { display: flex; gap: 0.625rem; flex-wrap: wrap; }
.toolbar__search {
  flex: 1; min-width: 12rem; display: flex; align-items: center; gap: 0.5rem;
  background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.625rem; padding: 0.5rem 0.75rem;
}
.toolbar__search-icon { width: 1rem; height: 1rem; color: var(--color-text-muted); flex-shrink: 0; }
.toolbar__search-input { flex: 1; border: none; outline: none; background: transparent; font-size: 0.875rem; color: var(--color-text-primary); min-width: 0; }
.toolbar__search-input::placeholder { color: var(--color-text-muted); }
.toolbar__filter { min-width: 11rem; }

.bulkbar {
  display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; flex-wrap: wrap;
  background: #FEF2F2; border: 1px solid #FECACA; border-radius: 0.625rem; padding: 0.6rem 0.9rem;
}
.bulkbar__count { font-size: 0.8rem; font-weight: 600; color: #B91C1C; }
.bulkbar__actions { display: flex; gap: 0.5rem; }

.card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; overflow: hidden; }
.no-results { text-align: center; padding: 2.5rem 1rem; color: var(--color-text-secondary); font-size: 0.875rem; margin: 0; }

.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-0.3rem); }

@media (min-width: 1024px) {
  .products { padding: 1.5rem; gap: 1.25rem; }
}
</style>

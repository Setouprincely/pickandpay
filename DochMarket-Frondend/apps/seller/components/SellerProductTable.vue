<script setup lang="ts">
import type { SellerProduct } from '~/apps/seller/types'

const props = defineProps<{
  products: SellerProduct[]
  selected: Array<string | number>
}>()

const emit = defineEmits<{
  'update:selected': [Array<string | number>]
  edit: [SellerProduct]
  delete: [SellerProduct]
  'toggle-status': [SellerProduct]
  'update-stock': [{ id: string | number, stock: number }]
}>()

const { $t } = useNuxtApp()
const { format } = useMoney()

// ── bulk selection ──
const allSelected = computed(() =>
  props.products.length > 0 && props.products.every(p => props.selected.includes(p.id))
)
function toggleAll() {
  emit('update:selected', allSelected.value ? [] : props.products.map(p => p.id))
}
function toggleOne(id: string | number) {
  emit('update:selected', props.selected.includes(id)
    ? props.selected.filter(x => x !== id)
    : [...props.selected, id])
}

// ── inline stock editing ──
const editingStock = ref<string | number | null>(null)
const stockDraft = ref(0)

function startStock(p: SellerProduct) {
  editingStock.value = p.id
  stockDraft.value = p.stockCount
}
function commitStock(p: SellerProduct) {
  if (editingStock.value === null) return
  if (stockDraft.value !== p.stockCount) {
    emit('update-stock', { id: p.id, stock: stockDraft.value })
  }
  editingStock.value = null
}
</script>

<template>
  <div class="table-wrap">
    <table class="ptable">
      <thead>
        <tr>
          <th class="ptable__check">
            <UCheckbox :model-value="allSelected" @update:model-value="toggleAll" />
          </th>
          <th class="ptable__product">{{ $t('seller.products.col.product') }}</th>
          <th class="ptable__sku">{{ $t('seller.products.col.sku') }}</th>
          <th class="ptable__price">{{ $t('seller.products.col.price') }}</th>
          <th class="ptable__stock">{{ $t('seller.products.col.stock') }}</th>
          <th class="ptable__status">{{ $t('seller.products.col.status') }}</th>
          <th class="ptable__sold">{{ $t('seller.products.col.sold') }}</th>
          <th class="ptable__actions" />
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in products" :key="p.id" :class="{ 'is-selected': selected.includes(p.id) }">
          <td class="ptable__check">
            <UCheckbox :model-value="selected.includes(p.id)" @update:model-value="toggleOne(p.id)" />
          </td>

          <td class="ptable__product">
            <div class="prod">
              <div class="prod__img">
                <img v-if="p.image" :src="p.image" :alt="p.title" onerror="this.style.opacity='0.15'" />
                <UIcon v-else name="i-lucide-image" class="prod__placeholder" />
              </div>
              <div class="prod__meta">
                <button class="prod__title" @click="emit('edit', p)">{{ p.title }}</button>
                <span class="prod__cat">{{ p.category }}</span>
              </div>
            </div>
          </td>

          <td class="ptable__sku"><span class="mono">{{ p.sku || '—' }}</span></td>

          <td class="ptable__price">
            <span class="price">{{ format(p.price) }}</span>
            <span v-if="p.oldPrice" class="price-old">{{ format(p.oldPrice) }}</span>
          </td>

          <td class="ptable__stock">
            <input
              v-if="editingStock === p.id"
              v-model.number="stockDraft"
              type="number"
              min="0"
              class="stock-input"
              @blur="commitStock(p)"
              @keydown.enter="commitStock(p)"
            />
            <button
              v-else
              class="stock-pill"
              :class="{ 'stock-pill--low': p.stockCount <= 5, 'stock-pill--out': p.stockCount === 0 }"
              @click="startStock(p)"
            >
              {{ p.stockCount }}
            </button>
          </td>

          <td class="ptable__status"><SellerStatusBadge :status="p.status" /></td>

          <td class="ptable__sold">{{ p.sold }}</td>

          <td class="ptable__actions">
            <div class="row-actions">
              <button class="icon-btn" :title="$t('common.edit')" @click="emit('edit', p)">
                <UIcon name="i-lucide-pencil" />
              </button>
              <button
                class="icon-btn"
                :title="p.status === 'active' ? 'Unpublish' : 'Activate'"
                @click="emit('toggle-status', p)"
              >
                <UIcon :name="p.status === 'active' ? 'i-lucide-eye-off' : 'i-lucide-eye'" />
              </button>
              <button class="icon-btn icon-btn--danger" :title="$t('common.delete')" @click="emit('delete', p)">
                <UIcon name="i-lucide-trash-2" />
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrap { overflow-x: auto; }
.ptable { width: 100%; border-collapse: collapse; min-width: 720px; }
.ptable thead th {
  text-align: left; font-size: 0.7rem; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.03em; color: var(--color-text-muted); padding: 0.75rem 0.75rem;
  border-bottom: 1px solid var(--color-border); white-space: nowrap;
}
.ptable tbody td { padding: 0.75rem 0.75rem; border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.ptable tbody tr:hover { background: var(--color-page-bg); }
.ptable tbody tr.is-selected { background: #FEF2F2; }

.ptable__check { width: 2.5rem; }
.ptable__sold, .ptable__stock { text-align: left; }
.ptable__actions { width: 7rem; }

.prod { display: flex; align-items: center; gap: 0.625rem; min-width: 0; }
.prod__img {
  width: 2.5rem; height: 2.5rem; border-radius: 0.5rem; overflow: hidden; flex-shrink: 0;
  background: var(--color-page-bg); display: flex; align-items: center; justify-content: center;
}
.prod__img img { width: 100%; height: 100%; object-fit: cover; }
.prod__placeholder { width: 1.1rem; height: 1.1rem; color: var(--color-text-muted); }
.prod__meta { display: flex; flex-direction: column; min-width: 0; gap: 0.1rem; }
.prod__title {
  font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); text-align: left;
  background: none; border: none; cursor: pointer; padding: 0; max-width: 16rem;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.prod__title:hover { color: var(--color-primary); }
.prod__cat { font-size: 0.7rem; color: var(--color-text-muted); }

.mono { font-size: 0.75rem; color: var(--color-text-secondary); font-variant-numeric: tabular-nums; }

.price { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); display: block; }
.price-old { font-size: 0.7rem; color: var(--color-text-muted); text-decoration: line-through; }

.stock-pill {
  font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary);
  background: var(--color-page-bg); border: 1px solid transparent; border-radius: 0.4rem;
  padding: 0.2rem 0.6rem; cursor: pointer; min-width: 2.5rem;
}
.stock-pill:hover { border-color: var(--color-border); }
.stock-pill--low { color: #B45309; background: #FEF3C7; }
.stock-pill--out { color: #B91C1C; background: #FEE2E2; }
.stock-input {
  width: 4rem; font-size: 0.8125rem; padding: 0.2rem 0.4rem; border: 1px solid var(--color-primary);
  border-radius: 0.4rem; outline: none; color: var(--color-text-primary);
}

.row-actions { display: flex; gap: 0.25rem; }
.icon-btn {
  width: 1.85rem; height: 1.85rem; border-radius: 0.45rem; border: none; cursor: pointer;
  background: transparent; color: var(--color-text-secondary);
  display: flex; align-items: center; justify-content: center;
}
.icon-btn:hover { background: var(--color-page-bg); color: var(--color-text-primary); }
.icon-btn--danger:hover { background: #FEE2E2; color: #DC2626; }
.icon-btn :deep(svg) { width: 1rem; height: 1rem; }
</style>

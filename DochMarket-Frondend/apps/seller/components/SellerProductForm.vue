<script setup lang="ts">
import type { SellerProduct, ProductDraft, ProductStatus } from '~/apps/seller/types'

const props = defineProps<{
  /** Existing product when editing; undefined when creating. */
  product?: SellerProduct
  submitting?: boolean
}>()

const emit = defineEmits<{
  submit: [ProductDraft]
  cancel: []
}>()

const { $t } = useNuxtApp()

const { get } = useApi()
const categoryOptions = ref<{ label: string; value: string }[]>([])

onMounted(async () => {
  try {
    const cats = await get<any[]>('/api/category')
    categoryOptions.value = (cats ?? []).map((c: any) => ({ label: c.name, value: c.name }))
  } catch {
    categoryOptions.value = []
  }
})

const { notify } = useNotify()

// Only fields that are stored by the backend (ProductRequest) are collected.
const form = reactive({
  title: props.product?.title ?? '',
  description: props.product?.description ?? '',
  category: props.product?.category ?? '',
  sku: props.product?.sku ?? '',
  price: props.product?.price ?? 0,
  stockCount: props.product?.stockCount ?? 0,
  weightKg: props.product?.weightKg ?? (undefined as number | undefined),
  lengthCm: props.product?.lengthCm ?? (undefined as number | undefined),
  widthCm: props.product?.widthCm ?? (undefined as number | undefined),
  heightCm: props.product?.heightCm ?? (undefined as number | undefined),
  status: (props.product?.status === 'draft' ? 'draft' : 'active') as ProductStatus,
})

// Image is only editable when creating — the backend has no endpoint to
// change a product's image after creation, so we don't pretend it does.
const imageFile = ref<File | null>(null)

const statusOptions = computed(() => ([
  { label: $t('seller.products.status.active'), value: 'active' },
  { label: 'Hidden (deactivated)', value: 'draft' },
]))

const error = ref('')

function onSubmit() {
  error.value = ''
  if (!form.title.trim() || !form.price || Number(form.price) <= 0) {
    error.value = $t('seller.editor.required')
  } else if (!form.category) {
    error.value = 'Please choose a category.'
  } else if (!form.weightKg || Number(form.weightKg) <= 0) {
    error.value = 'Weight (kg) is required — it decides which DOCH vehicle can carry the package.'
  } else if (!props.product && !imageFile.value) {
    error.value = 'Please add a product photo.'
  }
  if (error.value) {
    notify({ title: error.value, variant: 'error' })
    return
  }

  const draft: ProductDraft = {
    id: props.product?.id,
    title: form.title.trim(),
    description: form.description.trim() || undefined,
    category: form.category.trim(),
    sku: form.sku.trim() || undefined,
    price: Number(form.price),
    stockCount: Math.max(0, Math.floor(Number(form.stockCount) || 0)),
    status: form.status,
    weightKg: Number(form.weightKg),
    lengthCm: form.lengthCm ? Number(form.lengthCm) : undefined,
    widthCm: form.widthCm ? Number(form.widthCm) : undefined,
    heightCm: form.heightCm ? Number(form.heightCm) : undefined,
    image: props.product?.image,
    imageFile: imageFile.value,
  }
  emit('submit', draft)
}
</script>

<template>
  <form class="form" @submit.prevent="onSubmit">
    <div class="form__grid">
      <!-- Main column -->
      <div class="form__main">
        <section class="card">
          <h3 class="card__title">{{ $t('seller.editor.section.basics') }}</h3>
          <div class="field">
            <label>{{ $t('seller.editor.field.title') }} *</label>
            <UInput v-model="form.title" :placeholder="$t('seller.editor.placeholder.title')" size="md" />
          </div>
          <div class="field">
            <label>{{ $t('seller.editor.field.description') }}</label>
            <UTextarea v-model="form.description" :placeholder="$t('seller.editor.placeholder.description')" :rows="4" size="md" />
          </div>
          <div class="field">
            <label>{{ $t('seller.editor.field.category') }} *</label>
            <USelect v-model="form.category" :items="categoryOptions" value-key="value" :placeholder="$t('seller.editor.placeholder.category')" size="md" />
          </div>
        </section>

        <section class="card">
          <h3 class="card__title">{{ $t('seller.editor.section.media') }}</h3>
          <SellerImageUploader v-if="!product" v-model="imageFile" />
          <div v-else class="media-locked">
            <img v-if="product.image" :src="product.image" alt="Current product" class="media-locked__img" />
            <p class="media-locked__note">The photo can't be changed after a product is created yet — contact support if this needs updating.</p>
          </div>
        </section>

        <section class="card">
          <h3 class="card__title">Shipping</h3>
          <div class="field">
            <label>Weight per unit (kg) *</label>
            <UInput v-model.number="form.weightKg" type="number" min="0.01" step="0.01" size="md" placeholder="2.5" />
          </div>
          <div class="field-row">
            <div class="field">
              <label>Length (cm)</label>
              <UInput v-model.number="form.lengthCm" type="number" min="0" size="md" />
            </div>
            <div class="field">
              <label>Width (cm)</label>
              <UInput v-model.number="form.widthCm" type="number" min="0" size="md" />
            </div>
            <div class="field">
              <label>Height (cm)</label>
              <UInput v-model.number="form.heightCm" type="number" min="0" size="md" />
            </div>
          </div>
          <p class="media-locked__note">DOCH uses weight and size to pick the vehicle (bicycle, Suzuki Carry or Toyota HiAce).</p>
        </section>
      </div>

      <!-- Sidebar column -->
      <div class="form__side">
        <section class="card">
          <h3 class="card__title">{{ $t('seller.editor.section.pricing') }}</h3>
          <div class="field">
            <label>{{ $t('seller.editor.field.price') }} (XAF) *</label>
            <UInput v-model.number="form.price" type="number" min="1" size="md" />
          </div>
        </section>

        <section class="card">
          <h3 class="card__title">{{ $t('seller.editor.section.inventory') }}</h3>
          <div class="field">
            <label>{{ $t('seller.editor.field.sku') }}</label>
            <UInput v-model="form.sku" :placeholder="$t('seller.editor.placeholder.sku')" size="md" />
          </div>
          <div class="field">
            <label>{{ $t('seller.editor.field.stock') }}</label>
            <UInput v-model.number="form.stockCount" type="number" min="0" size="md" />
          </div>
          <div class="field">
            <label>Visibility</label>
            <USelect v-model="form.status" :items="statusOptions" value-key="value" size="md" />
          </div>
          <p v-if="product?.publicationStatus" class="media-locked__note">
            Moderation: <strong>{{ product.publicationStatus.replace('_', ' ').toLowerCase() }}</strong>
            <span v-if="product.rejectionReason"> — {{ product.rejectionReason }} (saving resubmits it for review)</span>
          </p>
        </section>
      </div>
    </div>

    <p v-if="error" class="form__error">{{ error }}</p>

    <div class="form__actions">
      <UButton variant="ghost" color="neutral" class="cancel-btn" :disabled="submitting" @click="emit('cancel')">
        {{ $t('seller.editor.cancel') }}
      </UButton>
      <UButton type="submit" class="submit-btn" :loading="submitting">
        {{ product ? $t('seller.editor.save') : $t('seller.editor.create') }}
      </UButton>
    </div>
  </form>
</template>

<style scoped>
.form { display: flex; flex-direction: column; gap: 1rem; }
.form__grid { display: grid; grid-template-columns: 1fr; gap: 1rem; }
.form__main, .form__side { display: flex; flex-direction: column; gap: 1rem; min-width: 0; }

.card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; }
.card__title { font-size: 0.85rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 1rem; }
.cancel-btn { color: var(--color-text-primary) !important; }

.field { display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 0.9rem; }
.field:last-child { margin-bottom: 0; }
.field label { font-size: 0.75rem; font-weight: 500; color: var(--color-text-secondary); }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.field :deep(input), .field :deep(textarea), .field :deep(button[role="combobox"]) { color: var(--color-text-primary) !important; }
.field :deep(.u-input), .field :deep(.u-select) { width: 100%; }

.form__error { color: #EF2A39; font-size: 0.8rem; margin: 0; }
.media-locked { display: flex; flex-direction: column; gap: 0.6rem; }
.media-locked__img { width: 100%; max-width: 220px; aspect-ratio: 1; object-fit: cover; border-radius: 0.625rem; border: 1px solid var(--color-border); }
.media-locked__note { font-size: 0.75rem; color: var(--color-text-muted); margin: 0; }
.form__actions { display: flex; justify-content: flex-end; gap: 0.5rem; }
.submit-btn { background: var(--color-button-red) !important; }

@media (min-width: 1024px) {
  .form__grid { grid-template-columns: minmax(0, 1fr) 20rem; }
}
</style>

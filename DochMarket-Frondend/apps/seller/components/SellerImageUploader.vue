<script setup lang="ts">
// Real single-file picker. The backend's only working image path is
// POST /api/product/productWithImage — one multipart image alongside the
// product fields, created together in one request. There's no separate
// "upload an image, get an ID back" endpoint that actually works (the one
// that exists, /api/product/image/upload, is an unfinished stub that just
// returns a fixed string and never stores anything) and no image-update path
// on PUT /api/product/{id} either. So: one image, chosen at creation time,
// shown as a local preview until the product is actually submitted.
const props = defineProps<{ modelValue: File | null; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [File | null] }>()

const { $t } = useNuxtApp()
const previewUrl = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

function pick() {
  if (props.disabled) return
  fileInput.value?.click()
}

function onChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
  emit('update:modelValue', file)
}

function remove() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  emit('update:modelValue', null)
  if (fileInput.value) fileInput.value.value = ''
}

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <div class="uploader">
    <div v-if="previewUrl" class="uploader__preview">
      <img :src="previewUrl" alt="Product" class="uploader__img" />
      <button type="button" class="uploader__remove" title="Remove" :disabled="disabled" @click="remove">
        <UIcon name="i-lucide-trash-2" />
      </button>
    </div>

    <button v-else type="button" class="uploader__empty" :disabled="disabled" @click="pick">
      <UIcon name="i-lucide-image-plus" class="uploader__empty-icon" />
      <span>{{ $t('seller.editor.addImage') }}</span>
    </button>

    <input ref="fileInput" type="file" accept="image/*" class="uploader__input" @change="onChange" />
    <p class="uploader__hint">{{ $t('seller.editor.imageHint') }}</p>
  </div>
</template>

<style scoped>
.uploader { display: flex; flex-direction: column; gap: 0.5rem; }
.uploader__input { display: none; }
.uploader__preview {
  position: relative; width: 100%; max-width: 220px; aspect-ratio: 1; border-radius: 0.625rem;
  overflow: hidden; border: 1px solid var(--color-border);
}
.uploader__img { width: 100%; height: 100%; object-fit: cover; }
.uploader__remove {
  position: absolute; top: 0.4rem; right: 0.4rem; width: 1.75rem; height: 1.75rem; border-radius: 0.4rem;
  border: none; cursor: pointer; background: rgba(0,0,0,0.55); color: #fff;
  display: flex; align-items: center; justify-content: center;
}
.uploader__remove:hover { background: #DC2626; }
.uploader__remove :deep(svg) { width: 0.9rem; height: 0.9rem; }
.uploader__empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.4rem;
  width: 100%; max-width: 220px; aspect-ratio: 1; border-radius: 0.625rem;
  border: 1.5px dashed var(--color-border); background: var(--color-page-bg);
  color: var(--color-text-secondary); font-size: 0.8rem; font-weight: 600; cursor: pointer;
}
.uploader__empty:hover { border-color: var(--color-primary); color: var(--color-primary); }
.uploader__empty-icon { width: 1.75rem; height: 1.75rem; }
.uploader__hint { font-size: 0.7rem; color: var(--color-text-muted); margin: 0; }
</style>

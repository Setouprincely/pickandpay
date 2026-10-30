<script setup lang="ts">
// Completing a delivery is multipart: the backend wants BOTH an OTP and a
// proof-of-delivery photo (POST /api/deliveries/{id}/complete — otp as form
// param, image as file part). So this modal now collects both and emits them
// to the parent, which performs the real upload.
const emit = defineEmits<{ close: []; confirm: [payload: { otp: string; image: File }] }>()

const { $t } = useNuxtApp()

const code = ref('')
const imageFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const err = ref('')

function onFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  imageFile.value = f
  previewUrl.value = URL.createObjectURL(f)
}

function onConfirm() {
  err.value = ''
  if (!code.value.trim()) { err.value = 'Enter the OTP.'; return }
  if (!imageFile.value) { err.value = 'A proof-of-delivery photo is required.'; return }
  emit('confirm', { otp: code.value.trim(), image: imageFile.value })
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">

        <div class="modal-icon">
          <UIcon name="i-heroicons-shield-check" class="modal-icon__svg" />
        </div>

        <h2 class="modal-title">{{ $t('driver.confirm.title') }}</h2>
        <p class="modal-sub">{{ $t('driver.confirm.sub') }}</p>

        <input
          v-model="code"
          type="text"
          inputmode="numeric"
          class="otp-input"
          :placeholder="$t('driver.confirm.placeholder')"
          aria-label="OTP code"
          @keyup.enter="onConfirm"
        />

        <!-- Proof-of-delivery photo (mandatory, @RequestPart image) -->
        <label class="pod-label">
          <UIcon name="i-heroicons-camera" class="pod-icon" />
          <span>{{ imageFile ? imageFile.name : 'Add delivery photo' }}</span>
          <input type="file" accept="image/*" capture="environment" class="pod-input" @change="onFileChange" />
        </label>

        <img v-if="previewUrl" :src="previewUrl" alt="proof" class="pod-preview" />

        <p v-if="err" class="pod-err">{{ err }}</p>

        <div class="modal-actions">
          <button class="btn-cancel" @click="$emit('close')">{{ $t('common.cancel') }}</button>
          <button class="btn-confirm" :disabled="!code.trim() || !imageFile" @click="onConfirm">
            {{ $t('driver.confirm.validate') }}
          </button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 100;
                 display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card {
  background: #fff; border-radius: 0.875rem; width: 100%; max-width: 380px;
  padding: 1.75rem 1.5rem; text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  display: flex; flex-direction: column; align-items: center;
}
.modal-icon { width: 3rem; height: 3rem; border-radius: 50%; background: #EFF6FF;
              display: flex; align-items: center; justify-content: center; }
.modal-icon__svg { width: 1.5rem; height: 1.5rem; color: #60A5FA; }
.modal-title { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); margin: 1rem 0 0.375rem; }
.modal-sub   { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0 0 1.25rem; line-height: 1.45; max-width: 280px; }
.otp-input  { width: 100%; border: 1px solid var(--color-border); border-radius: 0.5rem;
              padding: 0.75rem 0.875rem; font-size: 0.9375rem; color: var(--color-text-primary);
              outline: none; text-align: center; letter-spacing: 0.15em; box-sizing: border-box; }
.otp-input:focus { border-color: var(--color-primary); }
.otp-input::placeholder { color: var(--color-text-muted); letter-spacing: normal; }
.pod-label { display: flex; align-items: center; justify-content: center; gap: 0.5rem;
             width: 100%; margin-top: 0.75rem; padding: 0.625rem 0.875rem; cursor: pointer;
             border: 1px dashed var(--color-border); border-radius: 0.5rem;
             font-size: 0.8125rem; color: var(--color-text-secondary); box-sizing: border-box; }
.pod-label:hover { border-color: var(--color-primary); color: var(--color-primary); }
.pod-icon { width: 1.1rem; height: 1.1rem; flex-shrink: 0; }
.pod-input { display: none; }
.pod-preview { margin-top: 0.75rem; width: 100%; max-height: 140px; object-fit: cover; border-radius: 0.5rem; }
.pod-err { color: #DC2626; font-size: 0.75rem; margin: 0.625rem 0 0; }
.modal-actions { display: flex; gap: 0.75rem; width: 100%; margin-top: 1.25rem; }
.btn-cancel {
  flex: 1; background: #fff; color: var(--color-text-secondary);
  border: 1px solid var(--color-border); border-radius: 0.5rem;
  padding: 0.625rem 1rem; font-size: 0.875rem; cursor: pointer;
}
.btn-cancel:hover { background: var(--color-page-bg); }
.btn-confirm {
  flex: 1; background: var(--color-primary); color: #fff;
  border: none; border-radius: 0.5rem;
  padding: 0.625rem 1rem; font-size: 0.875rem; font-weight: 600; cursor: pointer;
  transition: background 0.15s;
}
.btn-confirm:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-confirm:disabled { background: #D1D5DB; color: #fff; cursor: not-allowed; }
</style>

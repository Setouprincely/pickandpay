<script setup lang="ts">
// Six-box OTP entry. v-model is the joined string ("" → "123456"). Handles
// auto-advance, backspace-to-previous, and pasting a full code.
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [string]; complete: [string] }>()

const LEN = 6
const boxes = ref<HTMLInputElement[]>([])

// Local per-box characters, kept in sync with the incoming model value.
const digits = ref<string[]>(Array.from({ length: LEN }, (_, i) => props.modelValue[i] ?? ''))

watch(() => props.modelValue, (v) => {
  for (let i = 0; i < LEN; i++) digits.value[i] = v[i] ?? ''
})

function setRef(el: any, i: number) {
  if (el) boxes.value[i] = el
}

function emitValue() {
  const joined = digits.value.join('')
  emit('update:modelValue', joined)
  if (joined.length === LEN) emit('complete', joined)
}

function onInput(i: number) {
  digits.value[i] = (digits.value[i] || '').replace(/\D/g, '').slice(-1)
  if (digits.value[i] && i < LEN - 1) boxes.value[i + 1]?.focus()
  emitValue()
}

function onKeydown(e: KeyboardEvent, i: number) {
  if (e.key === 'Backspace' && !digits.value[i] && i > 0) {
    boxes.value[i - 1]?.focus()
  }
}

function onPaste(e: ClipboardEvent) {
  const text = (e.clipboardData?.getData('text') || '').replace(/\D/g, '').slice(0, LEN)
  if (!text) return
  e.preventDefault()
  for (let i = 0; i < LEN; i++) digits.value[i] = text[i] ?? ''
  boxes.value[Math.min(text.length, LEN - 1)]?.focus()
  emitValue()
}

// Production auto-fill: read the incoming SMS code via the Web OTP API
// (Chrome / Android, HTTPS only). The SMS must end with a line like
// "@your-domain #123456". Where unsupported it simply never resolves — harmless.
let otpAbort: AbortController | null = null
onMounted(() => {
  if (!import.meta.client || !('OTPCredential' in window)) return
  otpAbort = new AbortController()
  ;(navigator.credentials as any)
    .get({ otp: { transport: ['sms'] }, signal: otpAbort.signal })
    .then((cred: any) => {
      const code: string | undefined = cred?.code
      if (!code) return
      for (let i = 0; i < LEN; i++) digits.value[i] = code[i] ?? ''
      emitValue()
    })
    .catch(() => { /* aborted or unsupported */ })
})
onBeforeUnmount(() => otpAbort?.abort())

defineExpose({
  focus: () => boxes.value[0]?.focus(),
})
</script>

<template>
  <div class="xn-otp">
    <input
      v-for="i in LEN"
      :key="i"
      :ref="el => setRef(el, i - 1)"
      v-model="digits[i - 1]"
      class="xn-otp__box"
      inputmode="numeric"
      maxlength="1"
      autocomplete="one-time-code"
      @input="onInput(i - 1)"
      @keydown="onKeydown($event, i - 1)"
      @paste="onPaste"
    />
  </div>
</template>

<style scoped>
.xn-otp { display: flex; gap: 0.5rem; justify-content: space-between; }
.xn-otp__box {
  width: 100%; aspect-ratio: 1 / 1; max-width: 3.25rem;
  text-align: center; font-size: 1.35rem; font-weight: 700; color: #111827;
  border: 1px solid #E5E7EB; border-radius: 0.6rem; outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.xn-otp__box:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.12); }
</style>

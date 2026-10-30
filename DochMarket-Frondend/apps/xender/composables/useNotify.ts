// Lightweight, self-contained toast notifications for the Xender app. Rendered
// by <XenderToaster> (mounted in the default layout). Kept in-app to match the
// hand-rolled UI pattern used across the DOCH apps rather than pulling in <UApp>.

export type ToastVariant = 'success' | 'info' | 'error'

export interface Toast {
  id: number
  title: string
  message?: string
  icon: string
  variant: ToastVariant
}

interface NotifyOptions {
  title: string
  message?: string
  icon?: string
  variant?: ToastVariant
  /** Auto-dismiss delay in ms. Pass 0 to keep it until dismissed. */
  duration?: number
}

const DEFAULT_ICON: Record<ToastVariant, string> = {
  success: 'i-heroicons-check-circle',
  info: 'i-heroicons-information-circle',
  error: 'i-heroicons-exclamation-circle',
}

let nextId = 0

export function useNotify() {
  const toasts = useState<Toast[]>('xender-toasts', () => [])

  function dismiss(id: number) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  function notify(opts: NotifyOptions) {
    const variant = opts.variant ?? 'success'
    const toast: Toast = {
      id: ++nextId,
      title: opts.title,
      message: opts.message,
      icon: opts.icon ?? DEFAULT_ICON[variant],
      variant,
    }
    toasts.value = [...toasts.value, toast]

    const duration = opts.duration ?? 3500
    if (import.meta.client && duration > 0) {
      setTimeout(() => dismiss(toast.id), duration)
    }
    return toast.id
  }

  return { toasts, notify, dismiss }
}

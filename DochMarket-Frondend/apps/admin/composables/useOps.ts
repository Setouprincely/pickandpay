// Shared formatting / status helpers for the admin operations pages.
export function useOps() {
  const money = (v?: number | null) =>
    v == null ? '—' : `${Math.round(v).toLocaleString('fr-FR')} XAF`

  const dt = (v?: string | Date | null) => {
    if (!v) return '—'
    const d = new Date(v)
    return isNaN(d.getTime()) ? String(v) : d.toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' })
  }

  const label = (s?: string | null) => (s ? s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase()) : '—')

  const statusClass = (s?: string | null) => {
    switch (s) {
      case 'DELIVERED': case 'COMPLETED': case 'PUBLISHED': return 'bg-green-100 text-green-800'
      case 'PROCESSING': case 'DRIVER_ASSIGNED': case 'PICKED_UP': case 'IN_TRANSIT': case 'SHIPPED': case 'AT_HUB':
        return 'bg-blue-100 text-blue-800'
      case 'PENDING': case 'PENDING_PAYMENT': case 'PENDING_PREPARATION': case 'PENDING_ASSIGNMENT': case 'PENDING_REVIEW':
        return 'bg-amber-100 text-amber-800'
      case 'FAILED': case 'PAYMENT_FAILED': case 'CANCELLED': case 'RETURNED': case 'REJECTED': case 'REFUNDED':
        return 'bg-red-100 text-red-800'
      default: return 'bg-gray-100 text-gray-700'
    }
  }

  const errorMessage = (e: any, fallback = 'Request failed') =>
    e?.data?.message || (typeof e?.data === 'string' ? e.data : '') || (e?.status ? `${fallback} (${e.status})` : `${fallback} (network error)`)

  const vehicleLabel = (c?: string | null) =>
    ({ BICYCLE: 'Bicycle', K_TRUCK: 'Suzuki Carry / K-Truck', VAN: 'Toyota HiAce' } as Record<string, string>)[c ?? ''] ?? '—'

  return { money, dt, label, statusClass, errorMessage, vehicleLabel }
}

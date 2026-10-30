// Shipment state for Xender — real backend.
// create():  POST /api/deliveries (multipart: `request` JSON part + `images` files)
// load():    GET  /api/deliveries  → user's delivery history
// get(id):   GET  /api/deliveries/{id}
// initiatePayment(): POST /api/payments/initiate (xenderRequestId = deliveryId)
// paymentStatus():   GET  /api/payments/{paymentId}/status
// route(id): GET  /api/deliveries/{id}/route  (coords for navigation)
// The fake driver simulation has been removed — the real driver app advances status.
import { useApi } from '../composables/useApi'
import type { Quote, TransportType } from '../composables/usePricing'

export type ShipmentStatus =
  | 'finding_driver'
  | 'driver_assigned'
  | 'picked_up'
  | 'in_transit'
  | 'arrived'
  | 'delivered'
  | 'failed'

export interface ShipmentEndpoint {
  id: string
  name: string
  area: string
  region?: string
  city?: string
  subdivision?: string
  landmark?: string
  lat?: number
  lng?: number
}

export interface Driver {
  name: string
  vehicle: string
  plate: string
  rating: number
  phone: string
}

export interface ShipmentEvent {
  status: ShipmentStatus
  at: number
}

export interface Shipment {
  id: string
  /** Public tracking number (DCH-...). */
  ref: string
  xenderRequestId?: string
  paymentStatus?: string
  failureReason?: string
  photo: string | null
  description: string
  weightKg: number
  pickup: ShipmentEndpoint
  destination: ShipmentEndpoint
  distanceKm: number
  etaMin: number
  receiver: { name: string; phone: string }
  price: Quote
  payment: { method: 'mtn' | 'orange'; phone: string }
  otp: string
  status: ShipmentStatus
  driver: Driver | null
  events: ShipmentEvent[]
  createdAt: number
  paymentId?: string
  paymentUrl?: string
}

export const STATUS_ORDER: ShipmentStatus[] = [
  'finding_driver', 'driver_assigned', 'picked_up', 'in_transit', 'arrived', 'delivered',
]

export const STATUS_META: Record<ShipmentStatus, { label: string; icon: string; color: string }> = {
  finding_driver:  { label: 'Finding a driver',        icon: 'i-heroicons-magnifying-glass',  color: '#D97706' },
  driver_assigned: { label: 'Driver assigned',         icon: 'i-heroicons-user-circle',       color: '#2563EB' },
  picked_up:       { label: 'Picked up',               icon: 'i-heroicons-archive-box',       color: '#4F46E5' },
  in_transit:      { label: 'In transit',              icon: 'i-heroicons-truck',             color: '#4F46E5' },
  arrived:         { label: 'Arrived — awaiting code', icon: 'i-heroicons-map-pin',           color: '#D97706' },
  delivered:       { label: 'Delivered',               icon: 'i-heroicons-check-badge',       color: '#16A34A' },
  failed:          { label: 'Failed',                  icon: 'i-heroicons-x-circle',          color: '#DC2626' },
}

// Map backend DeliveryStatus enum → the UI ShipmentStatus.
function mapStatus(backend: string): ShipmentStatus {
  switch (backend) {
    case 'PENDING_PAYMENT':
    case 'PENDING_PREPARATION':
    case 'PENDING_ASSIGNMENT': return 'finding_driver'
    case 'DRIVER_ASSIGNED':    return 'driver_assigned'
    case 'PICKED_UP':          return 'picked_up'
    case 'IN_TRANSIT':
    case 'AT_HUB':             return 'in_transit'
    case 'DELIVERED':          return 'delivered'
    case 'FAILED':
    case 'CANCELLED':
    case 'RETURNED':           return 'failed'
    default:                   return 'finding_driver'
  }
}

export function useShipments() {
  const { get, post } = useApi()

  const list = useState<Shipment[]>('xender-shipments', () => [])
  const loading = useState<boolean>('xender-shipments-loading', () => false)
  const error = useState<string>('xender-shipments-error', () => '')


  // Map a backend DeliveryResponseDTO into the UI Shipment shape (only real fields).
  function mapDelivery(d: any): Shipment {
    const status = mapStatus(d.status ?? 'PENDING_PAYMENT')
    const events: ShipmentEvent[] = (d.events ?? [])
      .filter((e: any) => e.toStatus)
      .map((e: any) => ({ status: mapStatus(e.toStatus), at: new Date(e.occurredAt).getTime() }))
    return {
      id: d.deliveryId,
      ref: d.trackingNumber ?? d.deliveryId,
      xenderRequestId: d.paymentDetails?.xenderRequestId,
      paymentStatus: d.paymentDetails?.status,
      failureReason: d.failureReason ?? d.paymentDetails?.failureReason,
      photo: (d.parcelImageUrls && d.parcelImageUrls[0]) || null,
      description: d.deliveryNote ?? '',
      weightKg: d.packageWeightKg ?? 0,
      pickup: { id: 'p', name: d.pickupLocationAddress ?? '—', area: '', lat: num(d.pickupLocationCoords?.latitude), lng: num(d.pickupLocationCoords?.longitude) },
      destination: { id: 'd', name: d.receiverAddress ?? '—', area: '', lat: num(d.receiverLocationCoords?.latitude), lng: num(d.receiverLocationCoords?.longitude) },
      distanceKm: 0,
      etaMin: d.estimatedDeliveryTime ? Math.max(0, Math.round((new Date(d.estimatedDeliveryTime).getTime() - Date.now()) / 60000)) : 0,
      receiver: { name: d.receiverName ?? '', phone: d.receiverPhone ?? '' },
      price: { zone: '', km: 0, total: d.price ?? 0 } as any,
      payment: { method: d.paymentDetails?.paymentMethod === 'ORANGE_MONEY' ? 'orange' : 'mtn', phone: '' },
      otp: d.deliveryOtp ?? '',
      status,
      driver: d.driverName ? {
        name: d.driverName,
        vehicle: d.vehicle ? `${d.vehicle.name}` : '',
        plate: d.vehicle?.plateNumber ?? '',
        rating: 0,
        phone: d.driverPhone ?? '',
      } : null,
      events,
      createdAt: d.createdAt ? new Date(d.createdAt).getTime() : 0,
      paymentId: d.paymentDetails?.paymentId,
      paymentUrl: d.paymentDetails?.paymentUrl,
    }
  }
  async function load() {
    loading.value = true
    error.value = ''
    try {
      const res = await get<any[]>('/api/deliveries')
      list.value = (res ?? []).map(mapDelivery)
    } catch (e: any) {
      error.value = e?.status === 403 || e?.status === 500
        ? `Couldn't load deliveries — server returned ${e.status}. This may be the known backend authorization issue.`
        : `Couldn't load deliveries (${e?.status ?? 'network error'}).`
    } finally {
      loading.value = false
    }
  }

  function get_(id: string) { return list.value.find(s => s.id === id) }

  async function fetchOne(id: string): Promise<Shipment | null> {
    try {
      const d = await get<any>(`/api/deliveries/${id}`)
      return d ? mapDelivery(d) : null
    } catch {
      return null
    }
  }

  interface CreateInput {
    file: File | null
    description: string
    weightKg: number
    transportType: TransportType
    pickup: ShipmentEndpoint
    destination: ShipmentEndpoint
    distanceKm: number
    etaMin: number
    receiver: { name: string; phone: string }
    price: Quote
    payment: { method: 'mtn' | 'orange'; phone: string }
  }

  // Real create — multipart with a `request` JSON part + `images` file part(s).
  async function create(input: CreateInput): Promise<{ ok: boolean; shipment?: Shipment; error?: string }> {
    error.value = ''
    try {
      if (!input.file) {
        return { ok: false, error: 'A package photo is required to create a delivery.' }
      }
      // DochXenderRequestDTO (the `request` part). Price is always computed by the backend.
      const requestDto = {
        pickupLocationAddress: fullAddress(input.pickup),
        receiverAddress: fullAddress(input.destination),
        deliveryNote: [input.description, input.receiver.name && `Receiver: ${input.receiver.name} ${input.receiver.phone}`].filter(Boolean).join(' — '),
        paymentMethod: input.payment.method === 'mtn' ? 'MTN_MOMO' : 'ORANGE_MONEY',
        paymentPhone: input.payment.phone.replace(/\D/g, '').replace(/^237/, '') || undefined,
        packageWeightKg: input.weightKg > 0 ? input.weightKg : undefined,
        transportType: input.transportType,
      }

      const fd = new FormData()
      // The JSON part must be sent as a Blob with JSON content-type so Spring
      // binds @RequestPart("request") to the DTO.
      fd.append('request', new Blob([JSON.stringify(requestDto)], { type: 'application/json' }))
      fd.append('images', input.file)

      // The backend creates the package and immediately starts its payment (prompt sent to paymentPhone).
      const created = await postMultipart<any>('/api/deliveries', fd)
      const shipment = mapDelivery(created)
      shipment.distanceKm = input.distanceKm
      list.value = [shipment, ...list.value]
      return { ok: true, shipment }
    } catch (e: any) {
      const msg = e?.data?.message || `Could not create delivery (${e?.status ?? 'network error'}).`
      error.value = msg
      return { ok: false, error: msg }
    }
  }

  // Retry payment for an unpaid request (the amount is taken from the request on the server).
  async function initiatePayment(xenderRequestId: string, method: 'mtn' | 'orange', phone: string) {
    try {
      const res = await post<any>('/api/payments/initiate', {
        paymentMethod: method === 'mtn' ? 'MTN_MOMO' : 'ORANGE_MONEY',
        xenderRequestId,
        phone: phone.replace(/\D/g, '').replace(/^237/, '') || undefined,
      })
      return { ok: res.status !== 'FAILED', paymentId: res.paymentId, paymentUrl: res.paymentUrl, status: res.status, error: res.message }
    } catch (e: any) {
      return { ok: false, error: e?.data?.message || `Could not start payment (${e?.status ?? 'network error'}).` }
    }
  }
  async function paymentStatus(paymentId: string) {
    try { return await get<any>(`/api/payments/${paymentId}/status`) } catch { return null }
  }

  // Navigation coords — GET /api/deliveries/{id}/route returns a delivery with
  // receiverLocationCoords (LocationDTO: latitude/longitude as strings).
  async function openNavigation(id: string): Promise<{ ok: boolean; reason?: string }> {
    try {
      const route = await get<any>(`/api/deliveries/${id}/route`)
      const c = route?.receiverLocationCoords ?? route?.pickupLocationCoords
      const lat = c?.latitude, lng = c?.longitude
      if (!lat || !lng || isNaN(Number(lat)) || isNaN(Number(lng))) {
        return { ok: false, reason: 'Route not available for this delivery' }
      }
      const url = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`
      if (typeof window !== 'undefined') window.open(url, '_blank')
      return { ok: true }
    } catch {
      return { ok: false, reason: 'Route not available for this delivery' }
    }
  }

  const active = computed(() => list.value.filter(s => s.status !== 'delivered'))
  const past = computed(() => list.value.filter(s => s.status === 'delivered'))

  return {
    list, active, past, loading, error,
    load, get: get_, fetchOne, create,
    initiatePayment, paymentStatus, openNavigation,
  }
}

function fullAddress(e: ShipmentEndpoint): string {
  return [e.name, e.area, e.region, 'Cameroon'].filter((x, i, a) => x && a.indexOf(x) === i).join(', ')
}

function num(v: any): number | undefined {
  if (v === null || v === undefined || v === '') return undefined
  const n = Number(v)
  return isNaN(n) ? undefined : n
}

// Replace the broken stub at the bottom of shipments.ts with this:
async function postMultipart<T>(endpoint: string, fd: FormData): Promise<T> {
  const { token } = useAuth()
  const config = useRuntimeConfig()
  const t = token.value || (typeof window !== 'undefined' ? localStorage.getItem('xender-token') : null)
  return await $fetch<T>(`${config.public.apiBase}${endpoint}`, {
    method: 'POST',
    headers: t ? { Authorization: `Bearer ${t}` } : {},
    body: fd,
  })

}


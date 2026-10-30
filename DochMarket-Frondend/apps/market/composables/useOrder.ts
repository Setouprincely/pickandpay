import type { CartItem } from '~/apps/market/composables/useCart'

// Order flow — real backend.
//   preview()      POST /api/orders/preview           real per-seller delivery quote
//   place()        POST /api/orders                    creates the order + starts payment
//   history()      GET  /api/orders/me
//   get()          GET  /api/orders/{idOrReference}
//   pay()          POST /api/payments/initiate          retry a failed / unfinished payment
//   paymentStatus  GET  /api/payments/{paymentId}/status
//   cancel()       POST /api/orders/{id}/cancel
//   review()       POST /api/orders/{id}/review

export interface PlacedOrder {
  reference: string
  orderId: string
  placedAt: number
  items: CartItem[]
  subtotal: number
  shipping: number
  total: number
  customer: {
    name: string; email: string; phone: string
    address: string; city: string; region: string
  }
  payment: string
  paymentUrl?: string
  paymentId?: string
  paymentStatus?: string
}

const errText = (e: any, what: string) => e?.data?.message || `${what} (${e?.status ?? 'network error'})`

export function useOrder() {
  const api = useApi()
  const lastOrder = useState<PlacedOrder | null>('market-last-order', () => null)

  async function preview(items: CartItem[], deliveryAddress: string) {
    return await api.post<any>('/api/orders/preview', {
      items: items.map(i => ({ productId: String(i.product.id), quantity: i.qty })),
      deliveryAddress,
      transportType: 'SINGLE_PARCEL',
    })
  }

  async function place(opts: {
    items: CartItem[]
    deliveryAddress: string
    contactPhone?: string
    paymentMethod: 'MTN_MOMO' | 'ORANGE_MONEY'
    /** When set, a mobile money prompt is sent to this number instead of a payment page. */
    paymentPhone?: string
    customer: PlacedOrder['customer']
  }): Promise<{ ok: boolean; order?: PlacedOrder; error?: string }> {
    try {
      const res = await api.post<any>('/api/orders', {
        items: opts.items.map(i => ({ productId: String(i.product.id), quantity: i.qty })),
        deliveryAddress: opts.deliveryAddress,
        contactPhone: opts.contactPhone,
        paymentMethod: opts.paymentMethod,
        paymentPhone: opts.paymentPhone || undefined,
        transportType: 'SINGLE_PARCEL',
      })
      const order: PlacedOrder = {
        reference: res.reference,
        orderId: res.orderId,
        placedAt: Date.now(),
        items: opts.items,
        subtotal: res.productTotal,
        shipping: res.deliveryFee,
        total: res.totalAmount,
        customer: opts.customer,
        payment: opts.paymentMethod,
        paymentUrl: res.paymentDetails?.paymentUrl,
        paymentId: res.paymentDetails?.paymentId,
        paymentStatus: res.paymentDetails?.status,
      }
      lastOrder.value = order
      return { ok: true, order }
    } catch (e: any) {
      return { ok: false, error: errText(e, "Couldn't place the order") }
    }
  }

  const history = () => api.get<any[]>('/api/orders/me')
  const get = (idOrReference: string) => api.get<any>(`/api/orders/${encodeURIComponent(idOrReference)}`)

  async function pay(orderId: string, paymentMethod: string, phone?: string) {
    return await api.post<any>('/api/payments/initiate', { orderId, paymentMethod, phone: phone || undefined })
  }

  async function paymentStatus(paymentId: string) {
    return await api.get<any>(`/api/payments/${paymentId}/status`)
  }

  const cancel = (orderId: string, reason?: string) => api.post<any>(`/api/orders/${orderId}/cancel`, { reason })
  const review = (orderId: string, body: { rating: number; deliveryRating?: number; comment?: string }) =>
    api.post<any>(`/api/orders/${orderId}/review`, body)

  return { lastOrder, place, preview, history, get, pay, paymentStatus, cancel, review, errText }
}

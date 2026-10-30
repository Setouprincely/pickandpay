// Pricing for Xender. The price shown and charged always comes from the backend:
// POST /api/deliveries/preview (XenderPricePreviewResponse: price, distanceKm, estimatedDurationMinutes, priceBreakdown).

export type TransportType = 'SINGLE_PARCEL' | 'FULL_TRUCK'

export interface Quote {
  zone: string
  km: number
  total: number
  etaMin?: number
}

export function usePricing() {
  const { post } = useApi()

  /** Straight-line distance (km), only used to warn about identical pickup / drop-off spots. */
  function distanceKm(a: { lat: number; lng: number }, b: { lat: number; lng: number }): number {
    const R = 6371
    const dLat = toRad(b.lat - a.lat)
    const dLng = toRad(b.lng - a.lng)
    const h = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2
    return 2 * R * Math.asin(Math.sqrt(h))
  }

  async function previewQuote(pickupLocationAddress: string, receiverAddress: string, transportType: TransportType = 'SINGLE_PARCEL'): Promise<Quote> {
    const res = await post<any>('/api/deliveries/preview', { pickupLocationAddress, receiverAddress, transportType })
    return {
      zone: res.priceBreakdown ?? '',
      km: Math.round((res.distanceKm ?? 0) * 10) / 10,
      total: res.price ?? 0,
      etaMin: res.estimatedDurationMinutes != null ? Math.round(res.estimatedDurationMinutes) : undefined,
    }
  }

  return { distanceKm, previewQuote }
}

function toRad(deg: number): number {
  return (deg * Math.PI) / 180
}
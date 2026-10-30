// useDriverNavigation — opens Google Maps directions to a delivery's real
// destination coordinates.
//
// GET /api/deliveries/{id}/route returns a DeliveryResponseDTO (confirmed from
// backend source). The destination coordinates live in:
//   receiverLocationCoords: LocationDTO { latitude: string, longitude: string }
// (pickupLocationCoords holds the pickup point). LocationDTO fields are STRINGS.
//
// On success: opens https://www.google.com/maps/dir/?api=1&destination=LAT,LNG
// On failure / no coords: returns a clear flag so the caller can show

import { useDriverApi } from "./useDriverApi";

// "Route not available for this delivery" instead of a broken map.
export function useDriverNavigation() {
  const { get } = useDriverApi()

  async function openNavigation(deliveryId: string): Promise<{ ok: boolean; reason?: string }> {
    if (!deliveryId) return { ok: false, reason: 'No delivery selected' }

    let route: any
    try {
      route = await get<any>(`/api/deliveries/${deliveryId}/route`)
    } catch (e: any) {
      // Surface the same auth-bug signal if it appears on this endpoint too.
      if (e?.status === 403 || e?.status === 500) {
        return { ok: false, reason: 'Route not available for this delivery' }
      }
      return { ok: false, reason: 'Route not available for this delivery' }
    }

    const coords = route?.receiverLocationCoords ?? route?.pickupLocationCoords
    const lat = coords?.latitude
    const lng = coords?.longitude

    // LocationDTO values are strings; guard against empty/invalid.
    if (!lat || !lng || isNaN(Number(lat)) || isNaN(Number(lng))) {
      return { ok: false, reason: 'Route not available for this delivery' }
    }

    const url = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`
    if (import.meta.client) window.open(url, '_blank')
    return { ok: true }
  }

  return { openNavigation }
}

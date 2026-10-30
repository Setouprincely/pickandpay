import type { SeriesPoint, CategorySales } from '~/apps/seller/types'
import { useSellerProducts } from './useSellerProducts'

// Seller analytics computed only from real data:
//   - paid orders containing the seller's items  GET /api/orders/seller/me
//   - wallet balances                             GET /api/wallets/me
//   - own catalogue                               GET /api/product/mine (via useSellerProducts)
export function useSellerAnalytics() {
  const { products } = useSellerProducts()
  const { get } = useApi()

  const wallet = useState<any | null>('seller-wallet', () => null)
  const sellerOrders = useState<any[]>('seller-orders', () => [])
  const analyticsError = useState<string>('seller-analytics-error', () => '')

  async function loadAnalytics() {
    analyticsError.value = ''
    try {
      const [w, o] = await Promise.all([get<any>('/api/wallets/me'), get<any[]>('/api/orders/seller/me')])
      wallet.value = w
      sellerOrders.value = o ?? []
    } catch (e: any) {
      analyticsError.value = e?.data?.message ?? `Could not load sales data (${e?.status ?? 'network error'})`
    }
  }

  // Orders that were not cancelled or returned count as sales.
  const salesOrders = computed(() => sellerOrders.value.filter(o => !['CANCELLED', 'RETURNED', 'REFUNDED'].includes(o.status)))

  // Last 12 calendar months, oldest first.
  const months = computed(() => {
    const now = new Date()
    return Array.from({ length: 12 }, (_, i) => {
      const d = new Date(now.getFullYear(), now.getMonth() - 11 + i, 1)
      return { key: `${d.getFullYear()}-${d.getMonth()}`, label: d.toLocaleDateString('en-GB', { month: 'short' }) }
    })
  })
  const monthKey = (iso: string) => { const d = new Date(iso); return `${d.getFullYear()}-${d.getMonth()}` }

  const revenueSeries = computed<SeriesPoint[]>(() => months.value.map(m => ({
    label: m.label,
    value: salesOrders.value.filter(o => monthKey(o.createdAt) === m.key).reduce((s, o) => s + (o.productTotal ?? 0), 0),
  })))

  const unitsSeries = computed<SeriesPoint[]>(() => months.value.map(m => ({
    label: m.label,
    value: salesOrders.value.filter(o => monthKey(o.createdAt) === m.key)
      .reduce((s, o) => s + (o.items ?? []).reduce((u: number, i: any) => u + i.quantity, 0), 0),
  })))

  /** Gross product sales (before the platform commission). */
  const totalRevenue = computed(() => salesOrders.value.reduce((s, o) => s + (o.productTotal ?? 0), 0))
  const totalOrders = computed(() => salesOrders.value.length)
  const totalUnitsSold = computed(() =>
    salesOrders.value.reduce((s, o) => s + (o.items ?? []).reduce((u: number, i: any) => u + i.quantity, 0), 0))
  const avgOrderValue = computed(() => totalOrders.value ? Math.round(totalRevenue.value / totalOrders.value) : 0)

  const avgPrice = computed(() => {
    if (!products.value.length) return 0
    return Math.round(products.value.reduce((s, p) => s + p.price, 0) / products.value.length)
  })

  const liveProducts = computed(() => products.value.filter(p => p.visibleToCustomers).length)

  const lowStock = computed(() =>
    products.value.filter(p => p.status !== 'draft' && p.stockCount <= 5).sort((a, b) => a.stockCount - b.stockCount))

  const topProducts = computed(() => [...products.value].sort((a, b) => b.sold - a.sold).slice(0, 5))

  const salesByCategory = computed<CategorySales[]>(() => {
    const byProduct = new Map(products.value.map(p => [String(p.id), p]))
    const map = new Map<string, CategorySales>()
    for (const o of salesOrders.value) {
      for (const i of o.items ?? []) {
        const key = byProduct.get(String(i.productId))?.category || 'Other'
        const row = map.get(key) ?? { category: key, units: 0, revenue: 0 }
        row.units += i.quantity
        row.revenue += i.total_price
        map.set(key, row)
      }
    }
    return [...map.values()].sort((a, b) => b.revenue - a.revenue)
  })

  return {
    loadAnalytics, wallet, sellerOrders, analyticsError,
    avgPrice, revenueSeries, unitsSeries,
    totalUnitsSold, totalRevenue, totalOrders, avgOrderValue,
    liveProducts, lowStock, topProducts, salesByCategory,
  }
}
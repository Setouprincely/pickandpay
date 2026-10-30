import type { Product } from '~/apps/market/types'

// Storefront product source — now backed by the real catalogue API.
// Maps the backend ProductResponse → the market Product shape, including the
// image-ID URL pattern (GET /api/product/image/download/{imageId}).
//
// ProductResponse (confirmed from backend source):
//   { id, name, imagerIds[], images[], description, price, currency,
//     stockQuantity, weight, SKU, quantitySold, categories[] }

export function mapProduct(p: any, imageUrl: (id?: string | null) => string): Product {
  const firstImageId = p.imagerIds && p.imagerIds.length ? p.imagerIds[0] : null
  const gallery = (p.imagerIds || []).map((id: string) => imageUrl(id))
  const inStock = (p.stockQuantity ?? 0) > 0
  return {
    id: p.id,
    title: p.name ?? '',
    price: p.price ?? 0,
    image: firstImageId ? imageUrl(firstImageId) : '/images/market/products/placeholder.jpg',
    gallery: gallery.length ? gallery : undefined,
    category: (p.categories && p.categories[0]?.name) || undefined,
    sku: p.SKU ?? String(p.id),
    stock: inStock ? 'In Stock' : 'Out of Stock',
    availability: inStock ? 'In Stock' : 'Out of Stock',
    soldOut: !inStock,
    reviews: undefined,
    rating: undefined,
    detailed: true,
  } as Product
}

// Full catalogue from GET /api/product. Shared across shop/search/detail/related.
export function useCatalog() {
  const { get, imageUrl } = useApi()

  const products = useState<Product[]>('market-catalog', () => [])
  const pending = useState<boolean>('market-catalog-pending', () => false)
  const error = useState<string>('market-catalog-error', () => '')

  async function load() {
    if (products.value.length) return // already loaded this session
    pending.value = true
    error.value = ''
    try {
      const list = await get<any[]>('/api/product')
      products.value = (list ?? []).map(p => mapProduct(p, imageUrl))
    } catch (e: any) {
      error.value = `Couldn't load products (${e?.status ?? 'network error'}).`
      products.value = []
    } finally {
      pending.value = false
    }
  }

  const categories = computed<string[]>(() =>
    [...new Set(products.value.map(p => p.category).filter(Boolean) as string[])])

  const brands = computed<string[]>(() =>
    [...new Set(products.value.map(p => p.brand).filter(Boolean) as string[])])

  return { products, categories, brands, pending, error, load }
}

// Kept for backward compatibility with components that asked for a "kind"
// (bestDeals / featured). Both now derive from the real catalogue: best deals =
// any with an oldPrice or simply the first slice; featured = a slice. We expose
// the real list and let the component slice/sort as before.
export function useProducts(_kind?: string) {
  const { products, pending, error, load } = useCatalog()
  onMounted(load)
  return { products, pending, error: ref<unknown>(error.value) }
}

// Single product by id — fetches the specific product (so deep links work even
// before the full catalogue loads).
export function useProduct(id: MaybeRefOrGetter<Product['id']>) {
  const { get, imageUrl } = useApi()
  const product = useState<Product | null>(`market-product-${toValue(id)}`, () => null)

  async function loadOne() {
    try {
      const p = await get<any>(`/api/product/${toValue(id)}`)
      product.value = p ? mapProduct(p, imageUrl) : null
    } catch {
      product.value = null
    }
  }
  onMounted(loadOne)
  watch(() => toValue(id), loadOne)

  return product
}

// Real categories from GET /api/category — { id, name }[].
export function useCategories() {
  const { get } = useApi()
  const categories = useState<any[]>('market-categories', () => [])

  async function load() {
    if (categories.value.length) return
    try {
      categories.value = await get<any[]>('/api/category')
    } catch {
      categories.value = []
    }
  }
  onMounted(load)
  return { categories, load }
}

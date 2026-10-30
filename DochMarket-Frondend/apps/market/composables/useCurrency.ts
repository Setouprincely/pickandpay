// ──────────────────────────────────────────────────────────────────────────
//  CURRENCY  —  base prices are stored in XAF (FCFA). This converts & formats
//  them into the shopper's selected currency using live exchange rates.
//  Rates are loaded once (see apps/market/layouts/default.vue) from a free,
//  no-key API: https://open.er-api.com/v6/latest/XAF
// ──────────────────────────────────────────────────────────────────────────

export const CURRENCIES = ['XAF', 'USD', 'EUR'] as const
export type CurrencyCode = typeof CURRENCIES[number]

export function useCurrency() {
  // Shared, app-wide state.
  const currency = useState<CurrencyCode>('market-currency', () => 'XAF')
  const rates = useState<Record<string, number>>('market-rates', () => ({ XAF: 1 }))
  const ratesLoaded = useState<boolean>('market-rates-loaded', () => false)

  function setCurrency(code: CurrencyCode) {
    currency.value = code
  }

  /** Group digits with commas, e.g. 1000000 -> "1,000,000". */
  function withCommas(value: number, fractionDigits = 0): string {
    return value.toLocaleString('en-US', {
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    })
  }

  /** Format an amount given in XAF into the selected currency. */
  function format(amountXaf: number | undefined | null): string {
    if (amountXaf == null) return ''
    const rate = rates.value[currency.value] ?? 1
    const value = amountXaf * rate
    switch (currency.value) {
      case 'USD': return `$${withCommas(value, 2)}`
      case 'EUR': return `€${withCommas(value, 2)}`
      default:    return `${withCommas(Math.round(value))} FCFA` // XAF
    }
  }

  return { currency, currencies: CURRENCIES, rates, ratesLoaded, setCurrency, format }
}

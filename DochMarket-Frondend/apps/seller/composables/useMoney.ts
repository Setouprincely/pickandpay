// Formats XAF (FCFA) amounts with thin-space grouping, matching the storefront.
// e.g. 456789 -> "456 789 XAF".
export function useMoney() {
  const format = (amount: number, withSuffix = true): string => {
    const grouped = Math.round(amount || 0)
      .toString()
      .replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
    return withSuffix ? `${grouped} XAF` : grouped
  }

  /** Compact form for KPI cards: 1 234 567 -> "1.2M XAF". */
  const compact = (amount: number): string => {
    const n = Math.round(amount || 0)
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M XAF`
    if (n >= 10_000) return `${(n / 1000).toFixed(0)}K XAF`
    return format(n)
  }

  return { format, compact }
}

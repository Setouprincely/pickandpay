// Small formatting helpers shared across the Xender workflow.
export function useFormat() {
  /** 12500 → "12 500 XAF" (French grouping, no decimals — XAF has no minor unit). */
  function formatXAF(n: number): string {
    return `${Math.round(n).toLocaleString('fr-FR')} XAF`
  }

  /** Minutes → "25 min" or "1 h 10 min". */
  function formatDuration(min: number): string {
    const m = Math.round(min)
    if (m < 60) return `${m} min`
    const h = Math.floor(m / 60)
    const rem = m % 60
    return rem ? `${h} h ${rem} min` : `${h} h`
  }

  /** Mask a phone for display: "+237 6 99 11 22 33". Best-effort, keeps the raw if odd. */
  function formatPhone(phone: string): string {
    const d = phone.replace(/\D/g, '').replace(/^237/, '')
    if (d.length !== 9) return phone
    return `+237 ${d[0]} ${d.slice(1, 3)} ${d.slice(3, 5)} ${d.slice(5, 7)} ${d.slice(7, 9)}`
  }

  /** Relative-ish timestamp, e.g. "2 min ago", "just now". */
  function timeAgo(ts: number): string {
    const s = Math.floor((Date.now() - ts) / 1000)
    if (s < 60) return 'just now'
    const m = Math.floor(s / 60)
    if (m < 60) return `${m} min ago`
    const h = Math.floor(m / 60)
    if (h < 24) return `${h} h ago`
    return new Date(ts).toLocaleDateString('fr-FR')
  }

  return { formatXAF, formatDuration, formatPhone, timeAgo }
}

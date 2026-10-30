// ──────────────────────────────────────────────────────────────────────────
//  TOP ANNOUNCEMENT BAR  (the "Black Friday" bar)
//
//  This is the ONLY file you edit to put the bar up or take it down for a sale.
//  No need to touch any component.
//
//  • To turn it OFF:           set `enabled: false`
//  • To turn it ON right now:   set `enabled: true` and leave startsAt/endsAt null
//  • To auto-show on sale days: set `enabled: true` and fill startsAt + endsAt
//                               (the bar shows itself only inside that date range)
//  • To change the message:     edit badge / badgeRest / lead / highlight / trail
//  • To change the button:      edit `cta`
// ──────────────────────────────────────────────────────────────────────────

export interface AnnouncementConfig {
  /** Master switch. `false` hides the bar everywhere. */
  enabled: boolean

  /** Optional auto-schedule (YYYY-MM-DD). When BOTH are set, the bar only appears
   *  between these dates (inclusive) — so it disappears by itself after the sale.
   *  Leave both `null` to show whenever `enabled` is true. */
  startsAt: string | null
  endsAt: string | null

  /** Content — all optional pieces of the headline. */
  badge: string       // text inside the red pill, e.g. "Black"
  badgeRest: string   // text right after the pill, e.g. "Friday"
  lead: string        // e.g. "Up to"
  highlight: string   // emphasised red value, e.g. "59%"
  trail: string       // e.g. "OFF"

  /** Call-to-action button. */
  cta: { label: string; to: string }
}

export const announcement: AnnouncementConfig = {
  enabled: true,

  // Example for a real Black Friday window:
  //   startsAt: '2026-11-27',
  //   endsAt:   '2026-11-30',
  startsAt: null,
  endsAt: null,

  badge: 'Black',
  badgeRest: 'Friday',
  lead: 'Up to',
  highlight: '59%',
  trail: 'OFF',

  cta: { label: 'SHOP NOW', to: '#' },
}

import { announcement, type AnnouncementConfig } from '~/apps/market/config/announcement'

/**
 * Drives the top announcement ("Black Friday") bar.
 * Developer-controlled only: visibility = master switch ✕ inside optional date window.
 * Edit visibility/content in `apps/market/config/announcement.ts`.
 */

// the true or false helps enable or disable the announcement bar, while the date window allows you to set a specific time frame for when the announcement should be visible. This way, you can prepare announcements in advance and have them automatically appear and disappear based on the specified dates.

export function useAnnouncement() {
  const inWindow = computed(() => {
    if (!announcement.startsAt || !announcement.endsAt) return false
    const now = Date.now()
    const start = Date.parse(`${announcement.startsAt}T00:00:00`)
    const end = Date.parse(`${announcement.endsAt}T23:59:59`)
    return now >= start && now <= end
  })

  const visible = computed(() => announcement.enabled && inWindow.value)

  return {
    config: announcement as AnnouncementConfig,
    visible,
  }
}

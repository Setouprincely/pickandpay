import { messages, DEFAULT_LOCALE, type Locale } from '~/i18n/messages'

// Provides a lightweight, dependency-free translation layer:
//   $t('a.b.c')            -> translated string for the current locale
//   $t('x', { name: 'Y' }) -> with {name} interpolation
//   $locale                -> reactive current locale ('en' | 'fr')
//   $setLocale(l) / $toggleLocale()
// Locale is persisted in a cookie so it survives reloads and SSR.
export default defineNuxtPlugin(() => {
  const cookie = useCookie<Locale>('doch-locale', {
    default: () => DEFAULT_LOCALE,
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax',
    path: '/',
  })

  const locale = useState<Locale>('doch-locale', () => cookie.value ?? DEFAULT_LOCALE)
  watch(locale, (v) => { cookie.value = v })

  function lookup(dict: Record<string, any>, key: string): unknown {
    return key.split('.').reduce<any>((o, k) => (o == null ? undefined : o[k]), dict)
  }

  function t(key: string, params?: Record<string, string | number>): string {
    const raw = lookup(messages[locale.value], key) ?? lookup(messages[DEFAULT_LOCALE], key)
    let str = typeof raw === 'string' ? raw : key
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        str = str.replace(new RegExp(`\\{${k}\\}`, 'g'), String(v))
      }
    }
    return str
  }

  return {
    provide: {
      t,
      locale,
      setLocale: (l: Locale) => { locale.value = l },
      toggleLocale: () => { locale.value = locale.value === 'en' ? 'fr' : 'en' },
    },
  }
})

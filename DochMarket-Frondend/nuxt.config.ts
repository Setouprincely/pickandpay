// https://nuxt.com/docs/api/configuration/nuxt-config
// Minimal ambient declaration to satisfy TypeScript when Node types are not
// installed (prevents "Cannot find name 'process'" errors in editors).
declare const process: { env: Record<string, string | undefined> };
const APP = process.env.APP || 'admin'

// The admin dashboard and the seller analytics page render charts, so chart.js
// is loaded for those two apps — every other app skips registering it.
const usesCharts = ['admin', 'seller'].includes(APP)

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: false },

  // CRITICAL: every app (admin/driver/market/seller/xender) shares this one
  // nuxt.config.ts and switches its pages/layouts/middleware/components by
  // the APP env var — but Nuxt's build output directory defaults to the same
  // ".nuxt/" path regardless of APP. Running two apps' dev servers at the
  // same time (e.g. `dev:driver` and `dev:seller` in separate terminals)
  // means both processes write into that same folder and corrupt each
  // other's compiled output — the server ends up rendering one app's markup
  // while the client JS bundle is for a different app. Namespacing buildDir
  // per app makes every app fully isolated, so running several at once is
  // safe.
  buildDir: `.nuxt/${APP}`,

  // Pins Nuxt back to root-relative path resolution (Nuxt 4 defaults to an
  // "app/" srcDir that this project, with its custom apps/<name>/ layout,
  // doesn't use).
  srcDir: '.',

  dir: {
    pages: `apps/${APP}/pages`,
    layouts: `apps/${APP}/layouts`,
    middleware: `apps/${APP}/middleware`,
  },

  modules: [
    '@nuxt/ui',
  ],

  css: [
    '~/assets/styles/main.css'
  ],

  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'https://dochbackend.onrender.com',
    }
  },

  components: [
    { path: `~/apps/${APP}/components`, prefix: APP === 'admin' ? 'Admin' : APP.charAt(0).toUpperCase() + APP.slice(1) },
    { path: '~/components/dashboard', prefix: 'Dashboard' },
    { path: '~/components/ui', prefix: 'Doch' },
    { path: '~/components/shared', prefix: 'Doch' },
  ],

  plugins: [
    '~/plugins/i18n.ts',
    ...(usesCharts ? ['~/plugins/chartjs.client.ts'] : []),
  ],

  // Pre-bundle chart.js up front (admin only) so Vite doesn't transform it
  // on first navigation to the dashboard.
  vite: {
    // Vite's dependency cache is separate from Nuxt's buildDir.
    cacheDir: `node_modules/.cache/vite/${APP}`,
    optimizeDeps: {
      include: usesCharts ? ['chart.js', 'vue-chartjs'] : [],
    },
    server: {
      // Per-app HMR port + ignore sibling apps' build dirs so concurrent dev servers don't clash.
      hmr: { port: 24678 + Math.max(0, ['admin', 'driver', 'market', 'seller', 'xender'].indexOf(APP)) },
      watch: { ignored: ['**/.nuxt/**'] },
    },
  },

  imports: {
    dirs: [
      `apps/${APP}/stores`,
      `apps/${APP}/composables`,
    ]
  },

  routeRules: {
    '/**': { middleware: ['auth'] },
    '/login': { middleware: [] },
    '/signup': { middleware: [] },
    '/otp-validation/**': { middleware: [] },
    '/staff-setup/**': { middleware: [] },
    '/forgot-password': { middleware: [] }
  },
  nitro: {
    preset: 'aws-amplify'
  },

  app: {
    head: {
      link: [
        { rel: 'icon', type: 'image/png', href: '/favicons/FAVICON-01.png' }
      ]
    }
  }
})
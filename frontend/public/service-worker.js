// Minimal PWA service worker: caches only the static app shell (HTML/CSS/JS)
// for offline app-load resilience. It deliberately never touches /api/ or
// /actuator/ requests -- those always go straight to the network, so session
// data, KYC documents, and any other authenticated API response are never
// cached here, per the build brief's explicit requirement.

const CACHE_NAME = 'supplybase-partners-shell-v1'
const SHELL_PATHS = ['/', '/index.html', '/manifest.json']

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_PATHS)))
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))),
    ),
  )
  self.clients.claim()
})

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url)

  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/actuator/')) {
    return // never intercept -- always hit the network directly
  }
  if (event.request.method !== 'GET' || url.origin !== self.location.origin) {
    return
  }

  event.respondWith(
    caches.match(event.request).then((cached) => {
      const network = fetch(event.request)
        .then((response) => {
          if (response.ok) {
            const clone = response.clone()
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone))
          }
          return response
        })
        .catch(() => cached)
      return cached || network
    }),
  )
})

package dev.optosync.test

@Suppress("UNUSED_EXPRESSION")
fun main() {
    js("navigator.serviceWorker.register('/service-worker.js')")
    js("window.addEventListener('online', () => navigator.serviceWorker.controller && navigator.serviceWorker.controller.postMessage({ type: 'drain' }))")
}

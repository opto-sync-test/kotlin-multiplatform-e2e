# Kotlin Multiplatform + OptoSync E2E

This fixture compiles one OptoSync background protocol across Kotlin/JVM,
Kotlin/JS, Linux, macOS, and iOS targets with Kotlin 2.4.10. It also compiles
the official pinned JVM `OptoSyncClient` source into the desktop target.

The common worker creates immutable per-lane snapshots, preserves sequence
order, returns authoritative payloads from the transport, and retries a whole
snapshot after a disconnected attempt. The JVM scheduler drains lanes on a
real thread pool. Its test uses a barrier to prove the desktop and mobile lanes
overlap, fails the first call for both, and verifies that each retry receives
the exact same immutable batch.

The Kotlin/JS entry point registers `web/service-worker.js`. That worker owns a
durable IndexedDB queue, uses Background Sync and Periodic Background Sync when
the browser exposes them, drains lanes concurrently, and retains failed rows
for the next wake. It posts authoritative responses back to controlled windows.

## Targets validated

- JVM desktop execution and concurrency test;
- JavaScript IR production Webpack bundle and service-worker syntax;
- Linux x64 Kotlin/Native compilation;
- macOS x64 and arm64 Kotlin/Native compilation;
- iOS device arm64 and simulator arm64 compilation.

## Immutable OptoSync boundary

`vendor/opto-sync-clients` and its nested `syncer.c` gitlink are declared in
`opto-sync-pin.json`. CI checks both commits before compiling. The JVM source
set consumes the official Kotlin client directly from that pinned submodule,
so a mutable package registry cannot silently change this fixture.

## Run locally

```sh
git submodule update --init --recursive
gradle desktopTest
gradle jsBrowserProductionWebpack
gradle compileKotlinMacosArm64 compileKotlinIosSimulatorArm64
```

GitHub Actions repeats the Linux/JVM/web build on Ubuntu and compiles the Apple
mobile and desktop targets on macOS.

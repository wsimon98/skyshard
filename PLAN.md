# SkyShard — Project Plan

SkyShard is a single-APK Android parallel-app launcher. Install it on a stock
phone, point it at any APK you have, and SkyShard runs that app inside an
isolated "shard" — separate accounts, separate storage, separate notifications,
no second SIM required. Multiple shards per app are supported. Nothing leaves
the phone. There is no backend, no account system, no telemetry.

This document is the long-form plan: how it works, why those choices, what
ships today, and where the project is going.

## 0. Hard constraints

- **No server.** The APK runs entirely on-device. The host app declares
  `QUERY_ALL_PACKAGES` (so it can see installed apps you might want to clone)
  and storage access — nothing else. No phone-home.
- **No tracking, no analytics, no accounts.** Period.
- **Single, signed APK install** — sideload-friendly, no Play Store required.
- **Stock Android.** No root required for the common case, no custom ROM, no
  bootloader unlock. Root is recommended for the deepest compatibility with
  Google-services-locked apps, but not required for the core sandbox.
- **Open source.** MIT for the SkyShard host code. The vendored engine is
  Apache 2.0 (see `NOTICE`).

## 1. Architecture in one paragraph

SkyShard is two layers: a host UI written in Kotlin/Java, and an in-process
**virtualization engine** that runs cloned apps inside the host's own process.
When you tap "Add" on an installed app, the engine extracts that APK, parses
its manifest, registers a virtual `PackageInfo` against its own private
PackageManager, redirects every cloned-app file path under
`/data/data/com.american2day.skyshard/shards/<shard-id>/`, and proxies Binder
calls through a stub host activity so each cloned app sees a complete (but
fake) Android environment. Result: each shard behaves like a separate device
to the cloned app — separate login session, separate notifications, separate
crash domain.

## 2. Engine choice

Building a virtualization engine from scratch is months of work. Forking an
abandoned one (asLody/VirtualApp 2017, VirtualXposed 2022) means inheriting
Android-version drift the moment you ship. The pragmatic move is to vendor an
**actively-maintained** Apache-licensed FOSS engine and treat it as our
runtime.

SkyShard v0.1 vendors the **NewBlackbox** engine
(<https://github.com/ALEX5402/NewBlackbox>), Apache 2.0, last updated 2025,
explicit Android 14 and 15 support, with a public API
(`BlackBoxCore`, `BActivityManager`, etc.) intended for embedding. The
upstream credits — VirtualApp, VirtualAPK, Dobby, xDL, BlackReflection,
FreeReflection — are the foundational projects this lineage rests on.

The host UI module (`:app`) is a re-branded fork of the engine's reference UI
so we ship something working on day one rather than rewriting from scratch. The
engine modules (`:Bcore`, `:black-reflection`, `:compiler`) are vendored
verbatim.

## 3. v0.1 — what ships now

- Multi-clone sandboxing: install any APK into SkyShard once, then add it as
  many separate shards as you want. Each shard has its own data tree and login
  state.
- Side-by-side launcher. Installed apps on one screen, your shards on another.
- Per-shard actions: launch, force-stop, clear data, uninstall, create home
  screen shortcut.
- Optional GMS environment for apps that hard-require Google Play Services
  (driven by the engine's GMS Manager).
- Optional Xposed-style hook layer (engine ships LSPosed-compatible support);
  hidden behind an advanced toggle.
- Optional fake-location overlay (engine has built-in GPS mocking).
- App label, launcher icon, theme, applicationId, output filename, signing
  config all carry the SkyShard brand. The cloned-app side is unaware of
  SkyShard; it sees a normal Android environment.

## 4. v0.2 — quality of life

- First-run welcome flow that explains what a shard is and asks for the right
  permissions in plain language.
- Per-shard freeze (engine API `BlackBoxCore.suspendShard`).
- Per-shard notification badge color, so cloned-Instagram notifications
  visually differ from the original on the lock screen.
- Disguise / hide the SkyShard launcher icon for users who want it offscreen.
- JSON backup/restore: export the list of shards (which APK, per-shard
  settings) to a single file.

## 5. v1.0 — depth and breadth

- Test matrix: WhatsApp, Instagram, Telegram, Signal, Discord, three
  representative banking apps. Each one explicitly listed as "supported,"
  "limited," or "blocked by Play Integrity" inside the app, with a one-line
  reason.
- Updates to the underlying engine pulled in on a release cadence rather than
  ad-hoc — we treat the vendored engine as a tracked dependency.
- Optional second engine: Android's Work Profile, for users who prefer a
  single OS-isolated clone over the in-process sandbox. The two engines coexist
  behind a `ShardEngine` switch.

## 6. Out of scope

- Bypassing root checks, SafetyNet, Play Integrity, or DRM. Some banking and
  streaming apps will refuse to run cloned, and we will not fight that —
  they'll be labelled blocked inside the app.
- iOS. iOS does not allow this category of app to exist at all.
- Anything that requires payment, account creation, or a server.

## 7. Build & release

- Native Android with Java + Kotlin host code, Gradle 8.13, AGP 8.13.2,
  JDK 21, `compileSdk=35`, `minSdk=21`, `targetSdk=28` (intentionally low to
  keep cloned apps out of strict-mode jail; the engine documents why).
- NDK 28.2.13676358 is pinned in `Bcore/build.gradle` and
  `app/build.gradle`. Native libraries are built for `arm64-v8a` and
  `armeabi-v7a`.
- One signing key shared with the rest of the `american2day` apps so users
  can sideload alongside App 563 and Claw Image without OS warnings.
- Output: `app/build/outputs/apk/release/SkyShard_<ver>_universal-release.apk`,
  copied to `/var/www/html/skyshard/skyshard.apk` on `elite` by
  `/root/deploy_skyshard_apk.sh`.
- Versioned filename pattern: `skyshard-v<X.Y.Z>+<code>-<slug>.apk`, with a
  stable `skyshard.apk` symlink always pointing at the latest.
- Stable URL: `http://10.0.0.103/skyshard/` (LAN), `http://100.82.34.43/skyshard/`
  (Tailscale).

## 8. Why "SkyShard"

Each cloned instance is a shard — a small, isolated piece of the original app
with its own memory and identity, sharing the device but not the data. The
`sky-` prefix lines up with the existing `skypoint/` workspace on this
server and keeps the naming consistent without colliding with any known
parallel-app product on the Play Store or in the FOSS world.

## 9. Licensing

The host UI code SkyShard authors is MIT licensed. The vendored engine
(`Bcore`, `black-reflection`, `compiler`) is Apache 2.0. `NOTICE` at the
repo root explains the bundling and lists the upstream credits the engine
itself acknowledges.

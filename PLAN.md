# SkyShard — Project Plan

SkyShard is a single-APK Android parallel-app launcher. Install it on a stock
phone, point it at any app you already have, and SkyShard runs an isolated
"shard" of that app — separate accounts, separate storage, separate notifications,
no second SIM required. Nothing leaves the phone. There is no backend, no
account system, no telemetry.

This document is the long-form plan: how it works, why those choices, what
ships in v0.1, and how we get to N-copy virtualization later.

## 0. Hard constraints

- **No server.** Not a soft preference. The APK runs entirely on-device. No
  network permission in the manifest for v0.1.
- **No tracking, no analytics, no accounts.** Period.
- **Single, signed APK install** — sideload-friendly, no Play Store required.
- **Stock Android.** No root, no custom ROM, no bootloader unlock.
- **Open source.** MIT licensed. Anyone can fork, rebuild, audit.

## 1. The two viable cloning strategies

Every existing parallel-app product is one of these two engines, or a hybrid:

### A. Work Profile (Android Managed Profile)

Uses Android's built-in `DevicePolicyManager` to create a separate work profile
on the device. Apps installed *into* the work profile have their own data
sandbox, their own notifications (with a briefcase badge), and the OS isolates
their storage from the personal profile. Shelter and Island both work this way.

- **Pros:** 100% stable across Android versions. Backed by official API. No
  hooking. No SELinux fights. Works on Android 5 through 15+. Banking apps are
  more likely to tolerate it because they see a real Android profile, not a
  sandbox.
- **Cons:** **One** clone per app, max. The work profile is a singleton — you
  can't have three WhatsAppes. Some OEMs disable or restrict it.

### B. In-process virtualization (VirtualApp style)

The host app loads the target app's APK inside its own process, intercepts
Binder calls, and redirects file I/O to a per-shard subdirectory. asLody's
VirtualApp, VirtualXposed, DroidPlugin all work this way.

- **Pros:** Unlimited clones (N copies of WhatsApp). No special device setup.
- **Cons:** Brittle. Every Android release breaks something. The public
  VirtualApp repo froze in 2017; current forks lag behind Android by 1–2 major
  versions. Doesn't pass Play Integrity on the cloned app. Hard for one
  developer to maintain alone.

### Why SkyShard does both, in order

Strategy A ships **today** with a small, readable Java codebase. It covers
the most common ask (two Instagrams, two WhatsApps) and is permanent — it does
not bit-rot.

Strategy B is the v1.x roadmap. We design v0.1 around a `ShardEngine`
interface so the work-profile engine and a future sandbox engine swap in and
out without rewriting the UI or storage layer.

## 2. Architecture (v0.1 → v1.x)

```
+--------------------------------------------------------+
|  UI layer (Activity, RecyclerView, settings)           |
|  - Lists installed apps                                |
|  - Shows current shards                                |
|  - Triggers clone / freeze / wipe actions              |
+--------------------------------------------------------+
                       |   via
                       v
+--------------------------------------------------------+
|  ShardEngine (interface)                               |
|  - List<Shard> listShards()                            |
|  - void createShard(AppInfo, ShardOptions)             |
|  - void launchShard(Shard)                             |
|  - void freezeShard(Shard) / unfreeze                  |
|  - void wipeShard(Shard) / wipeAll                     |
+-------------------+--------------+---------------------+
                    |              |
        WorkProfileShardEngine   SandboxShardEngine
        (v0.1, ships now)        (v1.x, future)
        Uses DevicePolicyManager Uses a forked / in-house
        + DeviceAdminReceiver    virtualization runtime
```

### Modules

- `MainActivity` — single-screen launcher. Top half: installed-app picker. Bottom half: existing shards.
- `ShardEngine` — interface every engine implements.
- `WorkProfileShardEngine` — v0.1 default engine. Provisions a managed profile if absent, enables apps in that profile.
- `ShardAdminReceiver` — extends `DeviceAdminReceiver` — required by the platform for any DPC app.
- `AppRepo` — wraps `PackageManager` to list user-installed apps with icons & labels.
- `Prefs` — tiny `SharedPreferences` wrapper. No DB.

No internet, no Firebase, no analytics SDK, no crash reporter. The APK should be < 2 MB.

## 3. v0.1 feature list (ships in the first APK on `/skyshard/`)

1. Launch screen lists all third-party (non-system) apps on the device.
2. Tap an app → "Add to SkyShard space". First time, this triggers the OS
   "set up work profile?" flow. After that, subsequent clones drop straight in.
3. "Shards" tab lists every app already cloned into the SkyShard space, with a
   one-tap launcher.
4. "Wipe SkyShard space" in settings — removes the entire managed profile.
5. About screen: version, build date, source URL on GitHub.
6. No `INTERNET` permission. No `READ_EXTERNAL_STORAGE`. Manifest is minimal.

## 4. v0.2 — quality of life

- Per-shard freeze (`setApplicationHidden` via DPC): pause the shard without uninstalling.
- Per-shard notification badge color so cloned-Instagram notifications visually differ from the original.
- Disguise / hide SkyShard's own launcher icon (for users who don't want a SkyShard icon on their home screen).
- Backup / restore: export the shard list (which apps are cloned, plus per-shard preferences) to a single JSON file.

## 5. v1.0 — multi-copy via SandboxShardEngine

This is the work that takes months. Outline only:

- Fork a viable virtualization engine (candidates: `va-exposed`, `BlackDex` reader + custom IPC layer, or write Binder hooks from scratch using LSPlant/Pine).
- Embed the engine as a JNI library inside SkyShard.
- Loader: extract the target APK, parse manifest, register fake `PackageInfo` with the engine's virtual `PackageManager`.
- I/O redirection: hook `open()`, `stat()`, content resolver calls — every file path the cloned app sees gets rewritten under `/data/data/com.american2day.skyshard/shards/<shard-id>/`.
- Binder proxy: each cloned activity runs under a stub host activity (`P0`, `P1`, … pre-declared in our manifest). Same trick VirtualApp uses.
- Test matrix: WhatsApp, Instagram, Telegram, Signal, three banking apps. Anything Play-Integrity-locked is explicitly out-of-scope and labeled as such in-app.

## 6. Build & release

- Native Android (Java), Gradle 8.9, AGP 8.7, JDK 21, `compileSdk=35`, `minSdk=24`, `targetSdk=35`.
- One signing key shared with the rest of the `american2day` apps so users can sideload alongside App 563 and Claw Image without OS warnings.
- Output: `app-release.apk`, copied to `/var/www/html/skyshard/skyshard.apk` on `elite` after every build.
- Versioned filename pattern: `skyshard-v<X.Y.Z>+<code>-<slug>.apk`, with a stable `skyshard.apk` symlink to the latest.
- Stable URL: `http://10.0.0.103/skyshard/` (LAN), `http://100.82.34.43/skyshard/` (Tailscale).

## 7. Why "SkyShard"

Each cloned instance is a shard — a small, isolated piece of the original app
with its own memory and identity, sharing the device but not the data. The
`sky-` prefix lines up with the existing `/skypoint/` workspace on this server
and keeps the naming consistent without colliding with any known Android cloner
on the Play Store or in the FOSS world.

## 8. Out of scope (intentionally)

- Bypassing root checks, SafetyNet, Play Integrity, or DRM. Not a goal.
- Anything that requires payment, account creation, or a server.
- A web UI. Browsers are not a parallel-app surface.
- iOS. iOS does not allow this category of app to exist at all.

## 9. Milestones

| Version | Engine                 | Capability                                     |
|---------|------------------------|------------------------------------------------|
| 0.1     | WorkProfileShardEngine | One clone per app via work profile, wipe all   |
| 0.2     | WorkProfileShardEngine | Freeze, hide launcher icon, JSON backup        |
| 0.3     | WorkProfileShardEngine | Per-shard preferences, badge color, polish     |
| 1.0     | SandboxShardEngine     | Unlimited clones for the curated test matrix   |
| 1.x     | SandboxShardEngine     | Expand the supported app matrix, Android N+1   |

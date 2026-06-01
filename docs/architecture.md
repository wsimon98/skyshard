# Architecture notes

Companion to `PLAN.md`. The plan covers what we ship and why; this file
covers how the code is laid out and what to expect when reading it.

## Module map

```
android/
  ├─ settings.gradle              # Module declarations + Maven repos
  ├─ build.gradle                 # Root build, version catalog, ext{} block
  ├─ gradle.properties            # AndroidX, R8, annotation processor flags
  ├─ key.properties               # Signing config (gitignored)
  ├─ local.properties             # SDK path (gitignored)
  ├─ Bcore/                       # Vendored: virtualization engine
  ├─ black-reflection/            # Vendored: hidden-API reflection layer
  ├─ compiler/                    # Vendored: annotation processor used by Bcore
  └─ app/                         # SkyShard host UI (re-branded fork)
      ├─ build.gradle             # applicationId = com.american2day.skyshard
      └─ src/main/
          ├─ AndroidManifest.xml  # Theme.SkyShard, signing, queries
          ├─ java/top/niunaijun/blackboxa/...   # Host UI (Kotlin)
          └─ res/                 # SkyShard branded resources
```

The `top.niunaijun.blackboxa` Java package name is preserved because users
never see it — only `applicationId` and `app_name` matter for end-user
identity, and both are SkyShard.

## What we modified vs. inherited

**Modified (host UI / branding):**

- `app/build.gradle` — applicationId, output filename pattern, signing
  config, NDK version pin.
- `app/src/main/res/values/strings.xml` (+ `values-zh-rCN`, `values-zh-rTW`)
  — `app_name` and any string that referenced "BlackBox" by name.
- `app/src/main/res/values/themes.xml` — theme renamed `Theme.BlackBox`
  → `Theme.SkyShard`.
- `app/src/main/res/values/colors.xml` — palette replaced with the SkyShard
  sky/teal pair.
- `app/src/main/res/mipmap-*/ic_launcher*.png` — replaced with the
  SkyShard mark (vector-rasterized at every density).
- `app/src/main/AndroidManifest.xml` — `android:theme` updated to the
  renamed theme.

**Inherited verbatim (engine):**

- `Bcore/` — virtualization runtime: `BlackBoxCore`, `BActivityManager`,
  `BPackageManager`, the native `cpp/` hooks built via `Android.mk`.
- `black-reflection/` — hidden-API reflection wrapper used by Bcore.
- `compiler/` — annotation processor that generates the reflection stubs.

## Engine entry points (the parts SkyShard's UI actually calls)

- `BlackBoxCore.get()` — singleton entry point. Held by the host `App`
  class for the lifetime of the process.
- `BlackBoxCore.get().installPackageAsUser(apkFilePath, userId)` — adds an
  APK to a shard space.
- `BlackBoxCore.get().uninstallPackageAsUser(pkgName, userId)` — removes
  a shard.
- `BlackBoxCore.get().launchApk(pkgName, userId)` — start a shard.
- `BlackBoxCore.get().getInstalledApps(userId)` — list shards in a given
  user space.

`userId` is the engine's per-space identifier; multiple userIds give you
multiple copies of the same package. That's how "second Instagram" and
"third Instagram" coexist.

## Permissions

The release manifest declares:

- `QUERY_ALL_PACKAGES` — needed to list apps the user might want to clone.
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (the latter capped at
  API 29 by `maxSdkVersion`) — needed to ingest APKs from disk.

It does **not** declare `INTERNET`. SkyShard itself cannot phone home.
Apps running inside a shard inherit whatever network permission they have
in their own manifest; SkyShard does not grant them anything extra.

## Build matrix

| Item            | Value                                |
|-----------------|--------------------------------------|
| `compileSdk`    | 35                                   |
| `targetSdk`     | 28 (intentional — see PLAN)          |
| `minSdk`        | 21                                   |
| Language        | Java 21 source + Kotlin 1.9.23       |
| Gradle          | 8.13                                 |
| AGP             | 8.13.2                               |
| NDK             | 28.2.13676358                        |
| ABIs            | arm64-v8a, armeabi-v7a, universal    |
| Signing         | Shared release key (sideload family) |

## Why `targetSdk=28`

Android tightens what apps in a sandbox can do at every release. The
engine's redirection layer relies on file-path interception and dynamic
class loading that progressively get harder above API 28. Keeping the
host targetSdk low keeps cloned apps in a more permissive runtime jail.
Cloned apps themselves still target whatever SDK their own manifest says.

## Upgrade path

When the upstream engine ships an Android-16 update we will:

1. `git diff` the upstream tree vs. our vendored copy to confirm we have
   no engine modifications they'd clobber (currently zero deltas).
2. Replace `Bcore/`, `black-reflection/`, `compiler/` from upstream.
3. Re-run the branding patch on `app/` if the upstream UI moved any
   strings or theme names.
4. Build, sign, deploy.

That diff/replace is fast; the branding patch is the only thing that has
to be hand-maintained.

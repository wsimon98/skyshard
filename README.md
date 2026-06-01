# SkyShard

A single-APK Android parallel-app launcher. Install it on a stock phone,
point it at any APK, and run as many isolated copies of that app as you
want — separate accounts, separate storage, separate notifications. Each
copy is a "shard."

No server. No account. No telemetry. No phone-home. Sideload only.

## Status

**v0.1** — first working build. Runs in-process virtualization via a
vendored, Apache-licensed FOSS engine. See [`PLAN.md`](PLAN.md) for the
long form, and [`NOTICE`](NOTICE) for the bundled-engine attribution.

## Install

Download the latest signed APK from the project's distribution server:

- LAN: <http://10.0.0.103/skyshard/>
- Tailscale: <http://100.82.34.43/skyshard/>

Or build it yourself (see below).

## Build

Requirements: JDK 21, Android SDK with build-tools 36, NDK 28.2.13676358,
and a signing key (defaults to the shared `american2day` release key —
see `android/key.properties`).

```bash
cd android
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/SkyShard_<ver>_universal-release.apk
```

## Architecture in one paragraph

`:app` is the SkyShard host — a Kotlin/Java Android app whose UI lets you
add installed apps to the "SkyShard space" and launch them as shards. It
depends on `:Bcore`, the vendored virtualization engine, which runs each
cloned app in-process inside its own redirected file system and Binder
proxy. The engine handles APK parsing, fake PackageManager, activity
proxying, and per-shard storage isolation. SkyShard owns the brand and
the UX; the engine owns the runtime.

See [`PLAN.md`](PLAN.md) for the long form and
[`docs/architecture.md`](docs/architecture.md) for module-level detail.

## License

The host UI code in this repository is MIT licensed. The vendored engine
modules (`android/Bcore/`, `android/black-reflection/`, `android/compiler/`)
are Apache 2.0. See [`LICENSE`](LICENSE), [`android/LICENSE`](android/LICENSE),
and [`NOTICE`](NOTICE).

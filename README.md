# SkyShard

An open-source Android parallel-app launcher. Run a second copy of any app
on a single phone — second WhatsApp, second Instagram, second Signal — with
fully isolated storage and notifications. Sideload-only, no server, no
account, no telemetry, no internet permission. The whole APK lives on
your phone.

## Status

**v0.1** — engine: Android Work Profile (one clone per app). See
[`PLAN.md`](PLAN.md) for the architecture and the road to multi-clone
virtualization in v1.x.

## Install

Get the latest signed APK from the project's distribution server:

- LAN: <http://10.0.0.103/skyshard/>
- Tailscale: <http://100.82.34.43/skyshard/>

Or build it yourself (see below).

## Build

Requirements: JDK 21, Android SDK with build-tools 36, a signing key.

```bash
cd android
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Architecture in one paragraph

`MainActivity` shows a list of installed apps and a list of existing
"shards". Both lists are backed by `AppRepo` (wraps `PackageManager`).
Cloning an app calls into `ShardEngine`, which in v0.1 is
`WorkProfileShardEngine` — it provisions an Android managed profile via
`DevicePolicyManager` and installs the chosen app's package into that
profile. Cloned apps then live in OS-isolated storage with a separate
identity. v1.x adds a second engine that does in-process sandboxing for
unlimited clones per app.

See [`PLAN.md`](PLAN.md) for the long form.

## License

MIT. See [`LICENSE`](LICENSE).

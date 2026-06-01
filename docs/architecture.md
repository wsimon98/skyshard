# Architecture notes

This is a deeper companion to `PLAN.md`. The plan covers what we ship and why;
this file covers how the code is laid out and what to expect when reading it.

## Layering

```
+------------------ MainActivity ---------------------+
| - Wires UI to ShardEngine via two RecyclerViews     |
| - Owns the provisioning request flow                |
+--------------------------+--------------------------+
                           |
+--------------------------v--------------------------+
| ShardEngine (interface)                             |
|   listShards / createShard / launchShard / wipeAll  |
+--------------------------+--------------------------+
                           |
+--------------------------v--------------------------+
| WorkProfileShardEngine (v0.1)                       |
|   Wraps DevicePolicyManager + ShardAdminReceiver    |
+-----------------------------------------------------+

AppRepo:          PackageManager + LAUNCHER category scan
AppListAdapter:   RecyclerView.Adapter rendering Row(packageName,label,icon,action)
Shard:            data class for one cloned app
AppInfo:          data class for one installed app
ShardException:   recoverable engine errors
```

## State

There is no database and no preferences file. The "list of shards" is derived
on demand from the OS — whichever apps SkyShard's `DeviceAdminReceiver` is
authorized to see inside the managed profile. This is intentional: state lives
where the truth is, and "what's cloned" cannot drift out of sync with the OS.

When the sandbox engine arrives in v1.x, *that* engine will own its own
per-shard directory tree and a small manifest under
`/data/data/com.american2day.skyshard/shards/`. The `ShardEngine` interface
hides that difference from the UI.

## Permissions

The release manifest declares:

- `BIND_DEVICE_ADMIN` (held by `ShardAdminReceiver`).

That's it. No `INTERNET`, no `READ_EXTERNAL_STORAGE`, no `POST_NOTIFICATIONS`.
The v0.1 binary has nothing to phone home with even if you wanted it to.

## Why one big Activity instead of Fragments / Compose

v0.1 is one screen. Adding Fragments or Compose for one screen would be more
plumbing than the feature is worth. When the surface area grows (settings,
per-shard detail, backup/restore) we'll split it.

## Build matrix

| Item              | Value                                |
|-------------------|--------------------------------------|
| `compileSdk`      | 35                                   |
| `targetSdk`       | 35                                   |
| `minSdk`          | 24                                   |
| Language          | Java 17 source                       |
| Gradle            | 8.9                                  |
| AGP               | 8.7.0                                |
| Signing           | Shared release key (sideload family) |

## Roadmap for the sandbox engine

The sandbox engine is the interesting work. Three orderings under
consideration:

1. **Fork an existing engine** (preferred): start from a current VirtualApp
   fork that already targets Android 14, vendor it as a subproject under
   `engine/sandbox/`, and write thin JNI glue. Fastest path; largest
   maintenance burden because we inherit someone else's hooks.
2. **Build from scratch using LSPlant**: write our own Binder + I/O hooks on
   top of LSPlant's ART hooking primitives. Slowest path; cleanest result.
3. **Hybrid**: ship a curated subset of the VirtualApp engine for the file-IO
   and PackageManager redirection, and write our own thin process/intent
   layer. Pragmatic; risks the worst of both maintenance burdens.

The decision blocker is whether any current VirtualApp fork passes a real
test matrix on Android 14 and 15. That investigation precedes v1.0.

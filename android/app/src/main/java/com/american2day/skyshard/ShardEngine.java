package com.american2day.skyshard;

import android.app.Activity;
import java.util.List;

/**
 * SkyShard's engine boundary. v0.1 ships WorkProfileShardEngine; v1.x will add
 * SandboxShardEngine for unlimited in-process clones. The UI layer must not
 * know which engine is wired in.
 */
public interface ShardEngine {

    /** True if this engine can be used on the current device right now. */
    boolean isAvailable();

    /** Human readable reason isAvailable() returned false. */
    String unavailabilityReason();

    /** True once the shard space has been provisioned (work profile created, etc). */
    boolean isProvisioned();

    /**
     * Begin provisioning. Engine may need to call startActivityForResult
     * on the host activity; the result is delivered back to the activity.
     */
    void requestProvisioning(Activity host, int requestCode);

    /** Returns the apps currently cloned into the shard space. */
    List<Shard> listShards();

    /** Add the given app to the shard space. */
    void createShard(Activity host, AppInfo app) throws ShardException;

    /** Launch a cloned app. */
    void launchShard(Activity host, Shard shard) throws ShardException;

    /** Wipe the entire shard space (removes every shard). */
    void wipeAll(Activity host);
}

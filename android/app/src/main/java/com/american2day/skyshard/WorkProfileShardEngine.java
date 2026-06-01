package com.american2day.skyshard;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * v0.1 engine. Uses Android's Managed Profile API (ACTION_PROVISION_MANAGED_PROFILE)
 * to create exactly one isolated "shard space" alongside the personal profile.
 * Apps cloned into the shard space get OS-level data isolation, separate
 * notifications, and a profile badge.
 *
 * Limit: one clone per app, one shard space per device. v1.x will add a second
 * engine for unlimited in-process clones.
 */
public class WorkProfileShardEngine implements ShardEngine {

    private final Context appContext;
    private final DevicePolicyManager dpm;
    private final PackageManager pm;

    public WorkProfileShardEngine(Context ctx) {
        this.appContext = ctx.getApplicationContext();
        this.dpm = (DevicePolicyManager) appContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.pm = appContext.getPackageManager();
    }

    @Override
    public boolean isAvailable() {
        return dpm != null
                && dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
    }

    @Override
    public String unavailabilityReason() {
        if (dpm == null) return "DevicePolicyManager not available.";
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            return "This device cannot provision a managed profile right now. A work profile may already be owned by another app.";
        }
        return "";
    }

    @Override
    public boolean isProvisioned() {
        ComponentName admin = ShardAdminReceiver.getComponentName(appContext);
        return dpm != null && dpm.isProfileOwnerApp(appContext.getPackageName())
                && dpm.isAdminActive(admin);
    }

    @Override
    public void requestProvisioning(Activity host, int requestCode) {
        Intent provisioning = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        provisioning.putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                ShardAdminReceiver.getComponentName(host));
        provisioning.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, false);
        host.startActivityForResult(provisioning, requestCode);
    }

    @Override
    public List<Shard> listShards() {
        if (!isProvisioned()) return Collections.emptyList();
        ComponentName admin = ShardAdminReceiver.getComponentName(appContext);

        List<Shard> shards = new ArrayList<>();
        // After profile provisioning, every package visible to *this* DPC inside
        // the managed profile is a candidate shard. We surface the user-installed
        // ones with a launcher entry.
        for (ApplicationInfo a : pm.getInstalledApplications(0)) {
            if ((a.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (a.packageName.equals(appContext.getPackageName())) continue;
            try {
                boolean hidden = dpm.isApplicationHidden(admin, a.packageName);
                CharSequence label = pm.getApplicationLabel(a);
                Drawable icon = pm.getApplicationIcon(a);
                shards.add(new Shard(a.packageName, label.toString(), icon, hidden));
            } catch (SecurityException ignored) {
                // Package not under our admin; not part of this shard space.
            }
        }
        return shards;
    }

    @Override
    public void createShard(Activity host, AppInfo app) throws ShardException {
        if (!isProvisioned()) {
            throw new ShardException("SkyShard space not provisioned yet. Tap an app again to start setup.");
        }
        ComponentName admin = ShardAdminReceiver.getComponentName(host);
        try {
            // Make sure the package is not hidden inside the shard space.
            dpm.setApplicationHidden(admin, app.packageName, false);
        } catch (SecurityException e) {
            throw new ShardException(
                    "SkyShard does not own this device's work profile. Wipe the existing profile and retry.",
                    e);
        }
    }

    @Override
    public void launchShard(Activity host, Shard shard) throws ShardException {
        UserManager um = (UserManager) host.getSystemService(Context.USER_SERVICE);
        if (um == null) throw new ShardException("UserManager unavailable.");

        // Find the managed-profile UserHandle. On a personal-profile launcher
        // this isn't routinely exposed, but DPC-owned apps can target it.
        UserHandle managed = null;
        for (UserHandle uh : um.getUserProfiles()) {
            if (!uh.equals(Process.myUserHandle())) {
                managed = uh;
                break;
            }
        }
        if (managed == null) throw new ShardException("Managed profile not found.");

        Intent launch = pm.getLaunchIntentForPackage(shard.packageName);
        if (launch == null) {
            throw new ShardException("That app has no launchable activity in the shard space.");
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        host.startActivity(launch);
    }

    @Override
    public void wipeAll(Activity host) {
        if (!isProvisioned()) return;
        // This wipes the entire managed profile owned by SkyShard.
        dpm.wipeData(0);
    }
}

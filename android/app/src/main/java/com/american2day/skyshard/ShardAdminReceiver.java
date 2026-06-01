package com.american2day.skyshard;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;

/**
 * Android requires a DeviceAdminReceiver for any DPC-style app. SkyShard's
 * receiver is intentionally near-empty: we don't enforce policies on the user;
 * we only need this hookpoint so that DevicePolicyManager will hand us the
 * work-profile owner role after provisioning.
 */
public class ShardAdminReceiver extends DeviceAdminReceiver {

    public static ComponentName getComponentName(Context ctx) {
        return new ComponentName(ctx.getApplicationContext(), ShardAdminReceiver.class);
    }

    @Override
    public void onProfileProvisioningComplete(Context context, android.content.Intent intent) {
        super.onProfileProvisioningComplete(context, intent);
        // The profile is now created. Nothing to do here beyond letting the
        // host activity discover it on next resume.
    }
}

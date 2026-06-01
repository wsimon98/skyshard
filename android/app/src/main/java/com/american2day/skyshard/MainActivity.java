package com.american2day.skyshard;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_PROVISION = 1001;

    private ShardEngine engine;
    private AppListAdapter shardsAdapter;
    private AppListAdapter appsAdapter;
    private TextView versionLabel;
    private TextView noShardsHint;
    private String pendingClonePkg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        engine = new WorkProfileShardEngine(this);

        RecyclerView shardsList = findViewById(R.id.shards_list);
        RecyclerView appsList = findViewById(R.id.apps_list);
        Button wipeButton = findViewById(R.id.wipe_button);
        versionLabel = findViewById(R.id.version_label);
        noShardsHint = findViewById(R.id.no_shards_hint);

        shardsList.setLayoutManager(new LinearLayoutManager(this));
        appsList.setLayoutManager(new LinearLayoutManager(this));

        shardsAdapter = new AppListAdapter(this::onLaunchShard);
        appsAdapter = new AppListAdapter(this::onCloneClicked);
        shardsList.setAdapter(shardsAdapter);
        appsList.setAdapter(appsAdapter);

        wipeButton.setOnClickListener(v -> onWipeClicked());
        renderVersion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void renderVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionLabel.setText(getString(R.string.version_template, info.versionName, info.versionCode));
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }

    private void refresh() {
        // Shards
        List<AppListAdapter.Row> shardRows = new ArrayList<>();
        for (Shard s : engine.listShards()) {
            shardRows.add(new AppListAdapter.Row(
                    s.packageName,
                    s.label + (s.frozen ? "  (frozen)" : ""),
                    s.icon,
                    getString(R.string.action_launch)));
        }
        shardsAdapter.setRows(shardRows);
        noShardsHint.setVisibility(shardRows.isEmpty() ? View.VISIBLE : View.GONE);

        // Installed apps
        List<AppListAdapter.Row> appRows = new ArrayList<>();
        for (AppInfo a : AppRepo.listLaunchableApps(this)) {
            appRows.add(new AppListAdapter.Row(
                    a.packageName, a.label, a.icon, getString(R.string.action_clone)));
        }
        appsAdapter.setRows(appRows);
    }

    private void onCloneClicked(String pkg) {
        if (!engine.isAvailable()) {
            Toast.makeText(this, R.string.wp_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        if (!engine.isProvisioned()) {
            pendingClonePkg = pkg;
            Toast.makeText(this, R.string.wp_provisioning, Toast.LENGTH_SHORT).show();
            engine.requestProvisioning(this, REQ_PROVISION);
            return;
        }
        cloneNow(pkg);
    }

    private void cloneNow(String pkg) {
        for (AppInfo a : AppRepo.listLaunchableApps(this)) {
            if (a.packageName.equals(pkg)) {
                try {
                    engine.createShard(this, a);
                    Toast.makeText(this, "Added " + a.label + " to SkyShard", Toast.LENGTH_SHORT).show();
                    refresh();
                } catch (ShardException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void onLaunchShard(String pkg) {
        for (Shard s : engine.listShards()) {
            if (s.packageName.equals(pkg)) {
                try {
                    engine.launchShard(this, s);
                } catch (ShardException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void onWipeClicked() {
        engine.wipeAll(this);
        Toast.makeText(this, "SkyShard space wiped.", Toast.LENGTH_SHORT).show();
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PROVISION) {
            if (resultCode == RESULT_OK && pendingClonePkg != null) {
                cloneNow(pendingClonePkg);
                pendingClonePkg = null;
            } else {
                Toast.makeText(this, R.string.wp_unavailable, Toast.LENGTH_LONG).show();
            }
        }
    }
}

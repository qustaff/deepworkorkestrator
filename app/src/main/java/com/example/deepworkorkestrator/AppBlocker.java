package com.example.deepworkorkestrator;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import android.app.ActivityManager;
import android.content.ComponentName;

public class AppBlocker {
    private Context context;
    private SharedPreferences prefs;
    private Set<String> blockedApps;
    private boolean isBlocking = false;

    public AppBlocker(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("deepwork", Context.MODE_PRIVATE);
        loadBlockedApps();
    }

    private void loadBlockedApps() {
        String blockedAppsStr = prefs.getString("blocked_apps", "");
        blockedApps = new HashSet<>(Arrays.asList(blockedAppsStr.split(",")));
        blockedApps.remove(""); // Remove empty entries
    }

    public boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void startBlocking() {
        isBlocking = true;
        startBlockingService();
    }

    public void stopBlocking() {
        isBlocking = false;
        stopBlockingService();
    }

    private void startBlockingService() {
        Intent intent = new Intent(context, BlockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(new Intent(context, BlockerService.class));
    }

    private void stopBlockingService() {
        context.stopService(new Intent(context, BlockerService.class));
    }

    public boolean isAppBlocked(String packageName) {
        return isBlocking && blockedApps.contains(packageName);
    }

    public String getForegroundApp() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 1000, time);
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                return mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        return "";
    }
} 
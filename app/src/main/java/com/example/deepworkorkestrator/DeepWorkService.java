package com.example.deepworkorkestrator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeepWorkService extends Service {

    private static final String CHANNEL_ID = "deepwork_channel";
    private static final int NOTIFICATION_ID = 1;

    private Handler handler;
    private Runnable appCheckRunnable;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceWithNotification();
        setDoNotDisturb();
        // Dodajemy małe opóźnienie przed rozpoczęciem blokowania, żeby Spotify zdążył się uruchomić
        handler = new Handler();
        handler.postDelayed(() -> startAppBlocker(), 3000); // 3 sekundy opóźnienia
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetDoNotDisturb();
        stopAppBlocker();
        stopForeground(true);
    }

    private void startForegroundServiceWithNotification() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Deep Work")
                .setContentText("Tryb Deep Work jest aktywny")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Deep Work Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void setDoNotDisturb() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        }
        // else: nie rób nic, nie otwieraj ustawień!
    }

    private void resetDoNotDisturb() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
        // else: NIC nie rób, nie otwieraj ustawień
    }

    private void startAppBlocker() {
        if (handler == null) handler = new Handler();
        isRunning = true;
        appCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    checkAndBlockApps();
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(appCheckRunnable);
    }

    private void stopAppBlocker() {
        isRunning = false;
        if (handler != null && appCheckRunnable != null) {
            handler.removeCallbacks(appCheckRunnable);
        }
    }

    // Pobierz listę pakietów do zablokowania z ustawień (SharedPreferences)
    private Set<String> getBlockedPackages() {
        Set<String> blockedPackages = getSharedPreferences("deepwork", MODE_PRIVATE)
                .getStringSet("blocked_packages", new HashSet<>());

        // Parsowanie z pola tekstowego "blocked_apps" jeśli "blocked_packages" jest puste
        if (blockedPackages == null || blockedPackages.isEmpty()) {
            String blockedAppsString = getSharedPreferences("deepwork", MODE_PRIVATE)
                    .getString("blocked_apps", "");
            if (!blockedAppsString.isEmpty()) {
                blockedPackages = new HashSet<>();
                String[] apps = blockedAppsString.split(",");
                for (String app : apps) {
                    blockedPackages.add(app.trim());
                }
            }
        }

        return blockedPackages != null ? blockedPackages : new HashSet<>();
    }

    private void checkAndBlockApps() {
        Set<String> blockedPackages = getBlockedPackages();
        if (blockedPackages.isEmpty()) return;

        String topPackage = getTopPackageName();

        // NIE blokuj Spotify ani naszej własnej aplikacji
        if (topPackage.equals("com.spotify.music") ||
                topPackage.equals(getPackageName()) ||
                topPackage.equals("com.example.deepworkorkestrator")) {
            return;
        }

        if (blockedPackages.contains(topPackage)) {
            Intent lockIntent = new Intent(this, BlockActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(lockIntent);
        }
    }

    private String getTopPackageName() {
        long end = System.currentTimeMillis();
        long begin = end - 10000;
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm != null) {
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end);
            if (stats != null && !stats.isEmpty()) {
                UsageStats recentStat = null;
                for (UsageStats usageStats : stats) {
                    if (recentStat == null || usageStats.getLastTimeUsed() > recentStat.getLastTimeUsed()) {
                        recentStat = usageStats;
                    }
                }
                if (recentStat != null) {
                    return recentStat.getPackageName();
                }
            }
        }
        return "";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
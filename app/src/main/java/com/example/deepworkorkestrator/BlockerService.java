package com.example.deepworkorkestrator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.content.Context;
import android.util.Log;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.os.Process;
import android.provider.Settings;
import android.content.pm.PackageManager;

public class BlockerService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "DeepWorkBlocker";
    private Handler handler;
    private AppBlocker appBlocker;
    private static final long CHECK_INTERVAL_MS = 100; // Check more frequently
    private static final String TAG = "BlockerService";
    private ActivityManager activityManager;
    private PackageManager packageManager;

    private final Runnable checkAppsRunnable = new Runnable() {
        @Override
        public void run() {
            String currentApp = appBlocker.getForegroundApp();
            Log.d(TAG, "Current app: " + currentApp);
            
            if (!currentApp.isEmpty() && appBlocker.isAppBlocked(currentApp)) {
                Log.d(TAG, "Blocking app: " + currentApp);
                
                // Launch blocking activity
                Intent blockIntent = new Intent(BlockerService.this, BlockActivity.class);
                blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                blockIntent.putExtra("blocked_package", currentApp);
                startActivity(blockIntent);

                // Try to bring our app to front
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        appBlocker = new AppBlocker(this);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        packageManager = getPackageManager();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        handler.post(checkAppsRunnable);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkAppsRunnable);
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Deep Work Blocker",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Monitoruje i blokuje aplikacje podczas Deep Work");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Deep Work aktywne")
                .setContentText("Blokowanie aplikacji jest aktywne")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        return builder.build();
    }
} 
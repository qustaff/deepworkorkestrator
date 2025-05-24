package com.example.deepworkorkestrator;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.content.SharedPreferences;
import android.app.ActivityManager;
import android.content.Context;
import java.util.HashSet;
import java.util.Set;

public class AppBlockerAccessibilityService extends AccessibilityService {
    private static final String TAG = "AppBlockerService";
    private SharedPreferences prefs;
    private Set<String> blockedApps;
    private Handler handler;
    private ActivityManager activityManager;
    private static final long CHECK_INTERVAL_MS = 100;
    private boolean isServiceRunning = false;

    private final Runnable checkBlockedAppsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isServiceRunning) return;
            
            try {
                if (prefs.getBoolean("is_blocking", false)) {
                    String currentPackage = getCurrentPackage();
                    if (currentPackage != null && blockedApps.contains(currentPackage)) {
                        launchBlockActivity(currentPackage);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in checkBlockedAppsRunnable", e);
            }
            
            if (isServiceRunning) {
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
            activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            handler = new Handler(Looper.getMainLooper());
            loadBlockedApps();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        try {
            prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
            activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            handler = new Handler(Looper.getMainLooper());
            loadBlockedApps();
            isServiceRunning = true;
            startBlockingCheck();
            Log.d(TAG, "Accessibility Service connected");
        } catch (Exception e) {
            Log.e(TAG, "Error in onServiceConnected", e);
        }
    }

    private void loadBlockedApps() {
        try {
            String blockedAppsStr = prefs.getString("blocked_apps", "");
            blockedApps = new HashSet<>();
            if (!blockedAppsStr.isEmpty()) {
                String[] apps = blockedAppsStr.split(",");
                for (String app : apps) {
                    blockedApps.add(app);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading blocked apps", e);
            blockedApps = new HashSet<>();
        }
    }

    private String getCurrentPackage() {
        try {
            ActivityManager.RunningTaskInfo taskInfo = activityManager.getRunningTasks(1).get(0);
            return taskInfo.topActivity.getPackageName();
        } catch (Exception e) {
            Log.e(TAG, "Error getting current package", e);
            return null;
        }
    }

    private void launchBlockActivity(String packageName) {
        try {
            Log.d(TAG, "Blocking app: " + packageName);
            Intent blockIntent = new Intent(this, BlockActivity.class);
            blockIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK 
                | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            );
            blockIntent.putExtra("blocked_package", packageName);
            startActivity(blockIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching block activity", e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isServiceRunning) return;
        
        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
                Log.d(TAG, "Current package: " + packageName);

                boolean isBlocking = prefs.getBoolean("is_blocking", false);
                if (isBlocking && !packageName.isEmpty() && blockedApps.contains(packageName)) {
                    launchBlockActivity(packageName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onAccessibilityEvent", e);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
        isServiceRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null) {
                String action = intent.getAction();
                if ("START_BLOCKING".equals(action)) {
                    loadBlockedApps();
                    isServiceRunning = true;
                    startBlockingCheck();
                } else if ("STOP_BLOCKING".equals(action)) {
                    isServiceRunning = false;
                    stopBlockingCheck();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
        }
        return START_STICKY;
    }

    private void startBlockingCheck() {
        try {
            handler.removeCallbacks(checkBlockedAppsRunnable);
            handler.post(checkBlockedAppsRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error starting blocking check", e);
        }
    }

    private void stopBlockingCheck() {
        try {
            handler.removeCallbacks(checkBlockedAppsRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping blocking check", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        stopBlockingCheck();
    }
} 
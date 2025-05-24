package com.example.deepworkorkestrator;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AppBlockerAccessibilityService extends AccessibilityService {
    private static final String TAG = "AppBlockerService";
    private SharedPreferences prefs;
    private Set<String> blockedApps;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
        loadBlockedApps();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
        loadBlockedApps();
        Log.d(TAG, "Accessibility Service connected");
    }

    private void loadBlockedApps() {
        String blockedAppsStr = prefs.getString("blocked_apps", "");
        blockedApps = new HashSet<>();
        if (!blockedAppsStr.isEmpty()) {
            String[] apps = blockedAppsStr.split(",");
            for (String app : apps) {
                blockedApps.add(app);
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            Log.d(TAG, "Window state changed - Current package: " + packageName);

            // Sprawdzamy flagę blokowania z SharedPreferences
            boolean isBlocking = prefs.getBoolean("is_blocking", false);
            Log.d(TAG, "Is blocking active: " + isBlocking + ", Package: " + packageName + ", Is blocked: " + blockedApps.contains(packageName));

            if (isBlocking && !packageName.isEmpty() && blockedApps.contains(packageName)) {
                Log.d(TAG, "Attempting to block app: " + packageName);
                
                // Launch blocking activity
                Intent blockIntent = new Intent(this, BlockActivity.class);
                blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                blockIntent.putExtra("blocked_package", packageName);
                startActivity(blockIntent);
                Log.d(TAG, "Block activity launched for: " + packageName);
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_BLOCKING".equals(action)) {
                loadBlockedApps();
            } else if ("STOP_BLOCKING".equals(action)) {
                // Implementacja zatrzymania blokowania
            }
        }
        return START_STICKY;
    }
} 
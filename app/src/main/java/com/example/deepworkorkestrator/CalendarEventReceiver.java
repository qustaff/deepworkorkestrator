package com.example.deepworkorkestrator;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.provider.CalendarContract;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;

import java.util.List;

public class CalendarEventReceiver extends BroadcastReceiver {
    private static final String TAG = "CalendarEventReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "=== CalendarEventReceiver onReceive ===");
        Log.d(TAG, "Intent: " + (intent != null ? intent.getAction() : "null"));
        
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Intent or action is null");
            return;
        }

        // Sprawdź uprawnienia do kalendarza
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No calendar permission");
                return;
            }
        }

        // Sprawdź czy to jest akcja, którą chcemy obsłużyć
        boolean isRelevantAction = intent.getAction().equals("android.intent.action.PROVIDER_CHANGED") ||
                                 intent.getAction().equals("android.intent.action.EVENT_REMINDER") ||
                                 intent.getAction().equals("android.intent.action.TIME_TICK") ||
                                 intent.getAction().equals("android.intent.action.TIME_SET") ||
                                 intent.getAction().equals("android.intent.action.DATE_CHANGED") ||
                                 intent.getAction().equals("android.intent.action.BOOT_COMPLETED");

        if (!isRelevantAction) {
            Log.d(TAG, "Not a relevant action: " + intent.getAction());
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences("DeepWorkSettings", Context.MODE_PRIVATE);
        boolean autoStartFromCalendar = prefs.getBoolean("autoStartFromCalendar", false);
        String eventTitle = prefs.getString("calendarEventTitle", "Głęboka praca");
        
        Log.d(TAG, "Auto start from calendar: " + autoStartFromCalendar);
        Log.d(TAG, "Looking for events with title: " + eventTitle);
        
        if (!autoStartFromCalendar) {
            Log.d(TAG, "Auto start from calendar is disabled");
            return;
        }

        try {
            // Check current events first to see if any have ended
            List<CalendarSettingsActivity.CalendarEvent> currentEvents = 
                CalendarSettingsActivity.getCurrentDeepWorkEvents(context);
            
            if (currentEvents.isEmpty()) {
                // No current events, check if we need to stop blocking
                SharedPreferences deepWorkPrefs = context.getSharedPreferences("deepwork", Context.MODE_PRIVATE);
                if (deepWorkPrefs.getBoolean("is_blocking", false)) {
                    Log.d(TAG, "No current events found, stopping Deep Work mode");
                    stopDeepWorkMode(context);
                }
            }

            // Then check for upcoming events
            List<CalendarSettingsActivity.CalendarEvent> events = 
                CalendarSettingsActivity.getUpcomingDeepWorkEvents(context);

            Log.d(TAG, "Found " + events.size() + " upcoming events");

            if (!events.isEmpty()) {
                CalendarSettingsActivity.CalendarEvent nextEvent = events.get(0);
                long currentTime = System.currentTimeMillis();
                
                Log.d(TAG, "Next event details:");
                Log.d(TAG, "Title: " + nextEvent.title);
                Log.d(TAG, "Start time: " + new java.util.Date(nextEvent.startTime));
                Log.d(TAG, "End time: " + new java.util.Date(nextEvent.endTime));
                Log.d(TAG, "Current time: " + new java.util.Date(currentTime));
                
                // Sprawdź czy wydarzenie już się rozpoczęło lub rozpocznie się w ciągu minuty
                if (nextEvent.startTime <= currentTime + 60000 && nextEvent.startTime > currentTime - 60000) {
                    Log.d(TAG, "Event is starting now or within the next minute");
                    startDeepWorkMode(context, nextEvent);
                } else {
                    Log.d(TAG, "Event is not starting soon enough. Time difference: " + 
                              (nextEvent.startTime - currentTime) / 1000 + " seconds");
                }
            } else {
                Log.d(TAG, "No upcoming events found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing calendar events", e);
        }
    }

    private void startDeepWorkMode(Context context, CalendarSettingsActivity.CalendarEvent event) {
        Log.d(TAG, "=== Starting Deep Work mode ===");
        
        try {
            // Set Deep Work as active in preferences
            SharedPreferences prefs = context.getSharedPreferences("deepwork", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_blocking", true).apply();
            Log.d(TAG, "Deep Work mode set in preferences");

            // Enable Do Not Disturb mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_NONE
                    );
                    Log.d(TAG, "Do Not Disturb mode enabled");
                } else {
                    Log.e(TAG, "Could not enable Do Not Disturb mode - no permission");
                }
            }

            // Start Deep Work service
            Intent serviceIntent = new Intent(context, DeepWorkService.class);
            serviceIntent.setAction("com.example.deepworkorkestrator.START_DEEP_WORK");
            serviceIntent.putExtra("end_time", event.endTime);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Deep Work service started");

            // Start app blocker with a small delay to allow Spotify to launch first
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent blockerIntent = new Intent(context, AppBlockerAccessibilityService.class);
                    blockerIntent.setAction("START_BLOCKING");
                    context.startService(blockerIntent);
                    Log.d(TAG, "App blocker started");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting app blocker", e);
                }
            }, 3000);

            // Launch Spotify playlist
            String playlistId = prefs.getString("spotify_playlist_id", "");
            if (!playlistId.isEmpty()) {
                Intent spotifyIntent = new Intent(Intent.ACTION_VIEW);
                spotifyIntent.setData(android.net.Uri.parse("spotify:playlist:" + playlistId));
                spotifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(spotifyIntent);
                Log.d(TAG, "Spotify playlist launched");
            } else {
                Log.e(TAG, "No Spotify playlist ID configured");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in startDeepWorkMode", e);
        }
    }

    private void stopDeepWorkMode(Context context) {
        Log.d(TAG, "=== Stopping Deep Work mode ===");
        
        try {
            // Set Deep Work as inactive in preferences
            SharedPreferences prefs = context.getSharedPreferences("deepwork", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_blocking", false).apply();
            Log.d(TAG, "Deep Work mode disabled in preferences");

            // Disable Do Not Disturb mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    );
                    Log.d(TAG, "Do Not Disturb mode disabled");
                }
            }

            // Stop app blocker
            Intent blockerIntent = new Intent(context, AppBlockerAccessibilityService.class);
            blockerIntent.setAction("STOP_BLOCKING");
            context.startService(blockerIntent);
            Log.d(TAG, "App blocker stopped");

        } catch (Exception e) {
            Log.e(TAG, "Error in stopDeepWorkMode", e);
        }
    }
} 
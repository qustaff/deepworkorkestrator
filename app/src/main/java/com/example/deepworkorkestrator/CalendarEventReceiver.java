package com.example.deepworkorkestrator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.provider.CalendarContract;

import java.util.List;

public class CalendarEventReceiver extends BroadcastReceiver {
    private static final String TAG = "CalendarEventReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent: " + (intent != null ? intent.getAction() : "null"));
        
        if (intent != null && 
            (intent.getAction() != null && 
             (intent.getAction().equals("android.intent.action.PROVIDER_CHANGED") ||
              intent.getAction().equals("android.intent.action.EVENT_REMINDER") ||
              intent.getAction().equals("android.intent.action.TIME_TICK")))) {
            
            SharedPreferences prefs = context.getSharedPreferences("DeepWorkSettings", Context.MODE_PRIVATE);
            boolean autoStartFromCalendar = prefs.getBoolean("autoStartFromCalendar", false);
            
            Log.d(TAG, "Auto start from calendar: " + autoStartFromCalendar);
            
            if (!autoStartFromCalendar) {
                return;
            }

            List<CalendarSettingsActivity.CalendarEvent> events = 
                CalendarSettingsActivity.getUpcomingDeepWorkEvents(context);

            Log.d(TAG, "Found " + events.size() + " upcoming events");

            if (!events.isEmpty()) {
                CalendarSettingsActivity.CalendarEvent nextEvent = events.get(0);
                long currentTime = System.currentTimeMillis();
                
                Log.d(TAG, "Next event: " + nextEvent.title + 
                          " Start: " + new java.util.Date(nextEvent.startTime) + 
                          " Current: " + new java.util.Date(currentTime));
                
                // Check if the event is starting within the next minute
                if (nextEvent.startTime - currentTime <= 60000 && nextEvent.startTime > currentTime) {
                    Log.d(TAG, "Starting Deep Work mode for event: " + nextEvent.title);
                    startDeepWorkMode(context, nextEvent);
                }
            }
        }
    }

    private void startDeepWorkMode(Context context, CalendarSettingsActivity.CalendarEvent event) {
        Intent serviceIntent = new Intent(context, DeepWorkService.class);
        serviceIntent.setAction("com.example.deepworkorkestrator.START_DEEP_WORK");
        serviceIntent.putExtra("end_time", event.endTime);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
} 
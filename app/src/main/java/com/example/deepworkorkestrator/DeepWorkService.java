package com.example.deepworkorkestrator;

import android.annotation.SuppressLint;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DeepWorkService extends Service {
    private SharedPreferences prefs;
    private NotificationManager notifManager;
    private AudioManager audioManager;
    private List<String> blockedApps;
    private String playlistUri;
    private Timer timer;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
        notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Pobierz URI playlisty z ustawień użytkownika
        playlistUri = prefs.getString("playlist_uri", "");
        // Pobierz listę blokowanych aplikacji z ustawień (rozdzielone przecinkami)
        String blockedRaw = prefs.getString("blocked_apps", "");
        blockedApps = Arrays.asList(blockedRaw.split(","));

        // Notyfikacja foreground service (wymagane od Android 8+)
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Deep Work aktywny")
                .setContentText("Tryb głębokiej pracy jest włączony")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        startForeground(1, notification);

        startScheduler();
    }

    private void startScheduler() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkCalendarAndStart();
            }
        }, 0, 60 * 1000); // Sprawdza co minutę
    }

    // Tu dodaj logikę sprawdzania kalendarza i aktywowania trybu deep work
    private void checkCalendarAndStart() {
        // TODO: Pobierz wydarzenia z kalendarza przez ContentResolver
        // Jeśli trwa blok deep work (np. tytuł zawiera słowo kluczowe i aktualny czas się mieści)
        // to wywołaj activateDeepWork()
        // Po zakończeniu bloku wywołaj deactivateDeepWork()
    }

    private void activateDeepWork() {
        // Wyciszenie powiadomień i aktywacja trybu DND
        notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);

        // Odtwarzanie playlisty Spotify, jeśli URI nie jest puste
        if (playlistUri != null && !playlistUri.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(playlistUri));
            intent.setPackage("com.spotify.music");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        // TODO: Monitorowanie uruchamianych aplikacji i blokowanie wybranych (UsageStatsManager + BlockActivity)
    }

    private void deactivateDeepWork() {
        // Przywracanie normalnych powiadomień
        notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);

        // TODO: Zatrzymać blokadę aplikacji, zapisać podsumowanie sesji itp.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        deactivateDeepWork();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
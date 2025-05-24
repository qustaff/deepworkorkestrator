package com.example.deepworkorkestrator;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button startButton;
    private Button stopButton;
    private ImageButton settingsButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        statusText = findViewById(R.id.statusText);
        settingsButton = findViewById(R.id.settingsButton);

        startButton.setOnClickListener(v -> {
            statusText.setText("Deep Work: aktywne");

            // Uruchomienie Spotify z playlistą
            launchSpotifyPlaylist();

            // Uruchomienie DeepWorkService (blokowanie aplikacji, tryb DND)
            Intent serviceIntent = new Intent(this, DeepWorkService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        });

        stopButton.setOnClickListener(v -> {
            statusText.setText("Deep Work: wyłączone");

            // Zatrzymanie DeepWorkService
            Intent serviceIntent = new Intent(this, DeepWorkService.class);
            stopService(serviceIntent);
        });

        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
    }

    private void launchSpotifyPlaylist() {
        // Pobierz URI playlisty z ustawień
        String playlistUri = getSharedPreferences("deepwork", MODE_PRIVATE)
                .getString("playlist_uri", "spotify:playlist:3KtdhyYc64U9hxIq0QCm3v");

        // Sprawdź czy Spotify jest zainstalowany
        if (!isSpotifyInstalled()) {
            Toast.makeText(this, "Spotify nie jest zainstalowany", Toast.LENGTH_SHORT).show();
            return;
        }

        // Metoda 1: Próba bezpośredniego uruchomienia przez URI
        try {
            Intent spotifyIntent = new Intent(Intent.ACTION_VIEW);
            spotifyIntent.setData(Uri.parse(playlistUri));
            spotifyIntent.setPackage("com.spotify.music");

            // Sprawdź czy intent może być obsłużony
            if (canHandleIntent(spotifyIntent)) {
                startActivity(spotifyIntent);
                Toast.makeText(this, "Uruchamianie Spotify...", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Metoda 2: Uruchomienie Spotify bez konkretnej playlisty
        try {
            Intent spotifyIntent = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if (spotifyIntent != null) {
                startActivity(spotifyIntent);
                Toast.makeText(this, "Spotify uruchomiony - otwórz playlistę ręcznie", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Metoda 3: Fallback - otwórz Spotify przez przeglądarkę
        try {
            String webUrl = playlistUri.replace("spotify:", "https://open.spotify.com/");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            startActivity(browserIntent);
            Toast.makeText(this, "Otwieranie Spotify w przeglądarce...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Nie udało się uruchomić Spotify", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean isSpotifyInstalled() {
        try {
            getPackageManager().getPackageInfo("com.spotify.music", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean canHandleIntent(Intent intent) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        return !activities.isEmpty();
    }
}
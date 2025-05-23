package com.example.deepworkorkestrator;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button startButton;
    private Button stopButton;
    private Button settingsButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Upewnij się, że masz taki layout z odpowiednimi ID!

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        statusText = findViewById(R.id.statusText);
        settingsButton = findViewById(R.id.settingsButton);

        startButton.setOnClickListener(v -> {
            statusText.setText("Deep Work: aktywne");

            // Odpalenie playlisty Spotify
            Intent spotifyIntent = new Intent(Intent.ACTION_VIEW);
            spotifyIntent.setData(Uri.parse("spotify:playlist:3KtdhyYc64U9hxIq0QCm3v"));
            spotifyIntent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + getPackageName()));
            spotifyIntent.setPackage("com.spotify.music");
            try {
                startActivity(spotifyIntent);
            } catch (Exception e) {
                // Spotify nie jest zainstalowany, można wrzucić Toast lub inne info
                e.printStackTrace();
            }

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
}
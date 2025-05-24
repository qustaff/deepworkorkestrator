package com.example.deepworkorkestrator;

import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private MaterialButton startButton;
    private MaterialButton stopButton;
    private MaterialButton dndPermissionButton;
    private ImageButton settingsButton;
    private TextView statusText;
    private NotificationManager notificationManager;
    private boolean isDeepWorkActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void initializeViews() {
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        dndPermissionButton = findViewById(R.id.dndPermissionButton);
        statusText = findViewById(R.id.statusText);
        settingsButton = findViewById(R.id.settingsButton);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void setupClickListeners() {
        startButton.setOnClickListener(v -> startDeepWork());
        stopButton.setOnClickListener(v -> stopDeepWork());
        dndPermissionButton.setOnClickListener(v -> requestDoNotDisturbPermission());
        settingsButton.setOnClickListener(v -> openSettings());
    }

    private void startDeepWork() {
        if (!hasDoNotDisturbPermission()) {
            showToast("Najpierw przyznaj uprawnienie 'Nie przeszkadzać'");
            return;
        }

        if (isDeepWorkActive) {
            showToast("Deep Work jest już aktywne");
            return;
        }

        try {
            enableDoNotDisturbMode();
            isDeepWorkActive = true;
            updateStatusText("Deep Work: aktywne");
            launchSpotifyPlaylist();
            startDeepWorkService();
            showToast("Deep Work rozpoczęte! Tryb 'Nie przeszkadzać' włączony");
        } catch (Exception e) {
            handleError("Błąd podczas uruchamiania Deep Work", e);
            isDeepWorkActive = false;
            updateStatusText("Błąd uruchamiania");
        }

        updateUI();
    }

    private void stopDeepWork() {
        try {
            disableDoNotDisturbMode();
            isDeepWorkActive = false;
            updateStatusText("Deep Work: wyłączone");
            stopDeepWorkService();
            showToast("Deep Work zatrzymane. Tryb 'Nie przeszkadzać' wyłączony");
        } catch (Exception e) {
            handleError("Błąd podczas zatrzymywania Deep Work", e);
        }

        updateUI();
    }

    private void launchSpotifyPlaylist() {
        Intent spotifyIntent = new Intent(Intent.ACTION_VIEW);
        spotifyIntent.setData(Uri.parse("spotify:playlist:3KtdhyYc64U9hxIq0QCm3v"));
        spotifyIntent.putExtra(Intent.EXTRA_REFERRER,
                Uri.parse("android-app://" + getPackageName()));
        spotifyIntent.setPackage("com.spotify.music");

        try {
            startActivity(spotifyIntent);
        } catch (Exception e) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse("https://open.spotify.com/playlist/3KtdhyYc64U9hxIq0QCm3v"));
                startActivity(browserIntent);
            } catch (Exception browserException) {
                showToast("Nie można otworzyć Spotify");
            }
        }
    }

    private void startDeepWorkService() {
        Intent serviceIntent = new Intent(this, DeepWorkService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopDeepWorkService() {
        Intent serviceIntent = new Intent(this, DeepWorkService.class);
        stopService(serviceIntent);
    }

    private boolean hasDoNotDisturbPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }

    private void requestDoNotDisturbPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                    showToast("Znajdź 'Deep Work Orkestrator' i włącz dostęp");
                } catch (Exception e) {
                    showToast("Nie można otworzyć ustawień uprawnień");
                }
            } else {
                showToast("Uprawnienie już przyznane");
            }
        } else {
            showToast("Uprawnienie nie jest wymagane na tym urządzeniu");
        }
    }

    private void enableDoNotDisturbMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_NONE
                );
            } else {
                showToast("Brak uprawnień do ustawienia trybu 'Nie przeszkadzać'");
            }
        }
    }

    private void disableDoNotDisturbMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_ALL
                );
            }
        }
    }

    private void openSettings() {
        try {
            startActivity(new Intent(this, SettingsActivity.class));
        } catch (Exception e) {
            showToast("Nie można otworzyć ustawień");
        }
    }

    private void updateUI() {
        updateDndPermissionButton();
        updateButtonStates();

        if (!isDeepWorkActive) {
            updateStatusText("Oczekiwanie");
        }
    }

    private void updateDndPermissionButton() {
        if (hasDoNotDisturbPermission()) {
            dndPermissionButton.setVisibility(View.GONE);
        } else {
            dndPermissionButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateButtonStates() {
        startButton.setEnabled(!isDeepWorkActive);
        stopButton.setEnabled(isDeepWorkActive);
        startButton.setAlpha(isDeepWorkActive ? 0.5f : 1.0f);
        stopButton.setAlpha(isDeepWorkActive ? 1.0f : 0.5f);
    }

    private void updateStatusText(String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(String message, Exception e) {
        e.printStackTrace();
        showToast(message);
    }

    private boolean isServiceRunning() {
        return isDeepWorkActive;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

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
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;
import android.util.Log;
import android.app.AlertDialog;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MaterialButton startButton;
    private MaterialButton stopButton;
    private MaterialButton dndPermissionButton;
    private MaterialButton accessibilityButton;
    private ImageButton settingsButton;
    private TextView statusText;
    private NotificationManager notificationManager;
    private boolean isDeepWorkActive = false;
    private AppBlocker appBlocker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        appBlocker = new AppBlocker(this);
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
        accessibilityButton = findViewById(R.id.accessibilityButton);
        statusText = findViewById(R.id.statusText);
        settingsButton = findViewById(R.id.settingsButton);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void setupClickListeners() {
        startButton.setOnClickListener(v -> startDeepWork());
        stopButton.setOnClickListener(v -> stopDeepWork());
        dndPermissionButton.setOnClickListener(v -> requestDoNotDisturbPermission());
        accessibilityButton.setOnClickListener(v -> requestAccessibilityPermission());
        settingsButton.setOnClickListener(v -> openSettings());
    }

    private void startDeepWork() {
        if (!hasDoNotDisturbPermission()) {
            showToast("Najpierw przyznaj uprawnienie 'Nie przeszkadzać'");
            return;
        }

        if (!isAccessibilityServiceEnabled()) {
            showToast("Wymagane uprawnienie do monitorowania aplikacji");
            requestAccessibilityPermission();
            return;
        }

        if (isDeepWorkActive) {
            showToast("Deep Work jest już aktywne");
            return;
        }

        try {
            getSharedPreferences("deepwork", MODE_PRIVATE)
                .edit().putBoolean("is_blocking", true).apply();
            enableDoNotDisturbMode();
            isDeepWorkActive = true;
            updateStatusText("Deep Work: aktywne");
            launchSpotifyPlaylist();
            startDeepWorkService();
            startAppBlocker();
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
            getSharedPreferences("deepwork", MODE_PRIVATE)
                .edit().putBoolean("is_blocking", false).apply();
            disableDoNotDisturbMode();
            isDeepWorkActive = false;
            updateStatusText("Deep Work: wyłączone");
            stopDeepWorkService();
            stopAppBlocker();
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

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            Log.e("MainActivity", "Error checking accessibility settings", e);
        }

        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (services != null) {
                return services.contains(getPackageName() + "/" + AppBlockerAccessibilityService.class.getCanonicalName());
            }
        }
        return false;
    }

    private void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        showToast("Znajdź 'Deep Work Orkestrator' i włącz dostęp");
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

    private void startAppBlocker() {
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog();
            return;
        }

        Intent intent = new Intent(this, AppBlockerAccessibilityService.class);
        intent.setAction("START_BLOCKING");
        startService(intent);

        SharedPreferences prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
        prefs.edit().putBoolean("is_blocking", true).apply();

        Toast.makeText(this, "Blokowanie aplikacji włączone", Toast.LENGTH_SHORT).show();
    }

    private void stopAppBlocker() {
        Intent intent = new Intent(this, AppBlockerAccessibilityService.class);
        intent.setAction("STOP_BLOCKING");
        startService(intent);

        SharedPreferences prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
        prefs.edit().putBoolean("is_blocking", false).apply();

        Toast.makeText(this, "Blokowanie aplikacji wyłączone", Toast.LENGTH_SHORT).show();
    }

    private void showAccessibilityServiceDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Wymagane uprawnienia")
            .setMessage("Aby blokować aplikacje, musisz włączyć usługę dostępności dla DeepWorkOrkestrator. Czy chcesz przejść do ustawień?")
            .setPositiveButton("Tak", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            })
            .setNegativeButton("Nie", null)
            .show();
    }

    private void updateUI() {
        updateDndPermissionButton();
        updateAccessibilityButton();
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

    private void updateAccessibilityButton() {
        if (isAccessibilityServiceEnabled()) {
            accessibilityButton.setVisibility(View.GONE);
        } else {
            accessibilityButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateButtonStates() {
        // Przycisk Start - widoczny zawsze, ale wyłączony gdy Deep Work jest aktywne
        startButton.setEnabled(!isDeepWorkActive);
        startButton.setAlpha(isDeepWorkActive ? 0.5f : 1.0f);

        // Przycisk Stop - widoczny tylko gdy Deep Work jest aktywne
        if (isDeepWorkActive) {
            stopButton.setVisibility(View.VISIBLE);
        } else {
            stopButton.setVisibility(View.GONE);
        }
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
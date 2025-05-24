package com.example.deepworkorkestrator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import java.util.Map;

public class BlockActivity extends Activity {
    private static final String TAG = "BlockActivity";
    private EditText passwordInput;
    private Button unlockButton;
    private Button exitButton;
    private TextView blockedAppText;
    private SharedPreferences prefs;
    private String blockedPackage;
    private Handler handler;
    private static final long CHECK_INTERVAL_MS = 100;

    private final Runnable checkForegroundRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing()) {
                // Keep the activity on top
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                
                // Hide system UI
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "BlockActivity created");
        
        setContentView(R.layout.activity_block);

        // Ustaw flagi okna
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );

        // Ukryj system UI
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
        
        passwordInput = findViewById(R.id.passwordInput);
        unlockButton = findViewById(R.id.unlockButton);
        exitButton = findViewById(R.id.exitButton);
        blockedAppText = findViewById(R.id.blockedAppText);
        handler = new Handler(Looper.getMainLooper());

        // Pobierz nazwę zablokowanej aplikacji
        blockedPackage = getIntent().getStringExtra("blocked_package");
        if (blockedPackage != null) {
            String appName = getAppNameFromPackage(blockedPackage);
            blockedAppText.setText("Zablokowana aplikacja: " + appName);
        }

        // Ustaw listenery dla przycisków
        unlockButton.setOnClickListener(v -> checkPassword());
        exitButton.setOnClickListener(v -> exitBlockedApp());

        // Rozpocznij okresowe sprawdzanie, czy aktywność jest na wierzchu
        startTopCheck();
    }

    private String getAppNameFromPackage(String packageName) {
        for (Map.Entry<String, String> entry : SocialMediaApps.APPS.entrySet()) {
            if (entry.getValue().equals(packageName)) {
                return entry.getKey();
            }
        }
        return packageName;
    }

    @Override
    public void onBackPressed() {
        // Prevent going back
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(checkForegroundRunnable);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void checkPassword() {
        String enteredPassword = passwordInput.getText().toString();
        String savedPassword = prefs.getString("lock_password", "");
        
        if (savedPassword.isEmpty()) {
            Toast.makeText(this, "Hasło nie zostało ustawione w ustawieniach", Toast.LENGTH_LONG).show();
            return;
        }

        if (PasswordHelper.verify(enteredPassword, savedPassword)) {
            // Wyłącz blokowanie przed zamknięciem aktywności
            prefs.edit().putBoolean("is_blocking", false).apply();
            finish();
        } else {
            Toast.makeText(this, "Nieprawidłowe hasło", Toast.LENGTH_SHORT).show();
            passwordInput.setText("");
        }
    }

    private void exitBlockedApp() {
        // Przejdź do ekranu głównego
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        finish();
    }

    private void startTopCheck() {
        handler.post(checkForegroundRunnable);
    }
}
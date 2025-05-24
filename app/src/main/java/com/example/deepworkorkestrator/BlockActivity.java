package com.example.deepworkorkestrator;

import android.app.Activity;
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
import android.app.ActivityManager;
import android.content.Context;
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
    private ActivityManager activityManager;

    private final Runnable checkForegroundRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing()) {
                // Keep the activity on top
                getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                );
                
                // Hide system UI
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );

                // Force activity to front using ActivityManager
                try {
                    activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
                } catch (Exception e) {
                    Log.e(TAG, "Error moving task to front", e);
                }
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "BlockActivity created");
        
        setContentView(R.layout.activity_block);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // Set window flags
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );

        // Hide system UI
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

        // Get blocked app name
        blockedPackage = getIntent().getStringExtra("blocked_package");
        if (blockedPackage != null) {
            String appName = getAppNameFromPackage(blockedPackage);
            blockedAppText.setText("Zablokowana aplikacja: " + appName);
        }

        // Set unlock button listener
        unlockButton.setOnClickListener(v -> checkPassword());

        // Set exit button listener
        exitButton.setOnClickListener(v -> {
            // Wyłącz blokowanie przed zamknięciem aktywności
            prefs.edit().putBoolean("is_blocking", false).apply();
            finish();
        });

        // Start periodic foreground check
        startTopCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force activity to front when resumed
        try {
            activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
        } catch (Exception e) {
            Log.e(TAG, "Error moving task to front in onResume", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Prevent activity from being paused
        try {
            activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
        } catch (Exception e) {
            Log.e(TAG, "Error moving task to front in onPause", e);
        }
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
            // Force activity to front when window gains focus
            try {
                activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
            } catch (Exception e) {
                Log.e(TAG, "Error moving task to front in onWindowFocusChanged", e);
            }
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
            // Disable blocking before finishing activity
            prefs.edit().putBoolean("is_blocking", false).apply();
            finish();
        } else {
            Toast.makeText(this, "Nieprawidłowe hasło", Toast.LENGTH_SHORT).show();
            passwordInput.setText("");
        }
    }

    private void startTopCheck() {
        handler.post(checkForegroundRunnable);
    }
}
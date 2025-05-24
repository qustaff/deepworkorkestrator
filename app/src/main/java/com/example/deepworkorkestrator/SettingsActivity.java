package com.example.deepworkorkestrator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public class SettingsActivity extends Activity {
    private LinearLayout appsContainer;
    private EditText passwordEdit;
    private Button saveBtn;
    private SharedPreferences prefs;
    private Set<String> selectedApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
        selectedApps = new HashSet<>();

        appsContainer = findViewById(R.id.appsContainer);
        passwordEdit = findViewById(R.id.passwordEdit);
        saveBtn = findViewById(R.id.saveBtn);

        // Load saved settings
        String savedApps = prefs.getString("blocked_apps", "");
        if (!savedApps.isEmpty()) {
            String[] apps = savedApps.split(",");
            for (String app : apps) {
                selectedApps.add(app);
            }
        }

        // Create checkboxes for each social media app
        for (Map.Entry<String, String> entry : SocialMediaApps.APPS.entrySet()) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(entry.getKey());
            checkBox.setTag(entry.getValue());
            checkBox.setChecked(selectedApps.contains(entry.getValue()));
            
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String packageName = (String) buttonView.getTag();
                if (isChecked) {
                    selectedApps.add(packageName);
                } else {
                    selectedApps.remove(packageName);
                }
            });

            appsContainer.addView(checkBox);
        }

        saveBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            
            // Save selected apps
            String blockedApps = String.join(",", selectedApps);
            editor.putString("blocked_apps", blockedApps);

            // Save password if changed
            if (!passwordEdit.getText().toString().isEmpty()) {
                String encoded = PasswordHelper.encode(passwordEdit.getText().toString());
                editor.putString("lock_password", encoded);
            }

            editor.apply();
            Toast.makeText(this, "Zapisano ustawienia", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
package com.example.deepworkorkestrator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.content.SharedPreferences;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private EditText keywordsEdit, playlistEdit, appsEdit, passwordEdit;
    private Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        keywordsEdit = findViewById(R.id.keywordsEdit);
        playlistEdit = findViewById(R.id.playlistEdit);
        appsEdit = findViewById(R.id.appsEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        saveBtn = findViewById(R.id.saveBtn);

        SharedPreferences prefs = getSharedPreferences("deepwork", MODE_PRIVATE);

        // Load saved settings
        keywordsEdit.setText(prefs.getString("calendar_keywords", ""));
        playlistEdit.setText(prefs.getString("playlist_uri", ""));
        appsEdit.setText(prefs.getString("blocked_apps", ""));
        passwordEdit.setText("");

        saveBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("calendar_keywords", keywordsEdit.getText().toString());
            editor.putString("playlist_uri", playlistEdit.getText().toString());
            editor.putString("blocked_apps", appsEdit.getText().toString());
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
package com.example.deepworkorkestrator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statusText;
    private Button startButton, stopButton, settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        settingsButton = findViewById(R.id.settingsButton);

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeepWorkService.class);
            startService(intent);
            statusText.setText("Deep Work: aktywne");
        });

        stopButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeepWorkService.class);
            stopService(intent);
            statusText.setText("Deep Work: wyłączone");
        });

        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
    }
}
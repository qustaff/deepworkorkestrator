package com.example.deepworkorkestrator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;

public class BlockActivity extends Activity {
    private EditText passwordInput;
    private Button unlockBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);

        passwordInput = findViewById(R.id.passwordInput);
        unlockBtn = findViewById(R.id.unlockBtn);

        unlockBtn.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("deepwork", MODE_PRIVATE);
            String stored = prefs.getString("lock_password", "");
            if (PasswordHelper.check(passwordInput.getText().toString(), stored)) {
                finish(); // Odblokuj dostęp
            } else {
                Toast.makeText(this, "Błędne hasło!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Zablokuj wyjście
    }
}
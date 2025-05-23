package com.example.deepworkorkestrator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class BlockActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Ta aplikacja jest zablokowana podczas Deep Work!");
        tv.setTextSize(20);
        tv.setPadding(40, 200, 40, 40);
        setContentView(tv);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finish();
    }
}
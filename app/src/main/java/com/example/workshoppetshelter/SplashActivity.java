// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\SplashActivity.java
package com.example.workshoppetshelter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Level 1 UX: branded entry screen shown for 1500 ms before the login screen.
 * Navigation is unchanged: the splash simply replaces MainActivity as the LAUNCHER target.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openMain = () -> {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // Never keep the splash on the back stack.
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // postDelayed schedules on the main looper; it never blocks the UI thread.
        handler.postDelayed(openMain, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        // Defensive: if the activity is destroyed early, do not leak a pending navigation.
        handler.removeCallbacks(openMain);
        super.onDestroy();
    }
}

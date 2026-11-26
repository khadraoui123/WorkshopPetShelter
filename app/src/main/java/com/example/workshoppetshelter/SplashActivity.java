// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\SplashActivity.java
package com.example.workshoppetshelter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Level 1 UX: branded entry screen shown for 1500 ms.
 * Session management: after the delay, Splash is the single routing decision point.
 * It checks SessionManager and opens the right dashboard, or the login screen.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SessionManager sessionManager;

    private final Runnable openNextScreen = () -> {
        Intent intent;

        if (sessionManager.isLoggedIn()) {
            long userId = sessionManager.getUserId();
            String userName = sessionManager.getUserName();
            String role = sessionManager.getRole();

            // Deny by default: only the two known roles route to a dashboard.
            if ("ADMIN".equals(role)) {
                intent = new Intent(this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(this, UserDashboardActivity.class);
            }

            // Same extras as MainActivity, so dashboards need no special session code.
            intent.putExtra("user_id", userId);
            intent.putExtra("user_name", userName);
            intent.putExtra("user_role", role);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish(); // Never keep the splash on the back stack.
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);

        // postDelayed schedules on the main looper; it never blocks the UI thread.
        handler.postDelayed(openNextScreen, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        // Defensive: if the activity is destroyed early, do not leak a pending navigation.
        handler.removeCallbacks(openNextScreen);
        super.onDestroy();
    }
}

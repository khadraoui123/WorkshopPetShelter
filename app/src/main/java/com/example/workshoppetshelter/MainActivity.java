// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\MainActivity.java
package com.example.workshoppetshelter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IS_DARK = "is_dark_mode";

    private MaterialButton themeToggleButton;

    // Phase 4 fields. db is created once in onCreate, not on every login attempt.
    private UserDbHelper db;
    private EditText etEmail;
    private EditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoRegister;
    private TextView tvForgotPassword;
    private TextView tvAbout;
    private MaterialButton btnLanguage;

    // Session management: separate from theme prefs.
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applySavedLocale(this);

        // PHASE 1 LOGIC: MUST stay BEFORE super.onCreate(). Do not move this block.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark;

        if (prefs.contains(KEY_IS_DARK)) {
            isDark = prefs.getBoolean(KEY_IS_DARK, false);
            AppCompatDelegate.setDefaultNightMode(
                    isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            isDark = isSystemInDarkMode();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        themeToggleButton = findViewById(R.id.buttonToggleTheme);
        updateToggleButtonText(isDark);

        btnLanguage = findViewById(R.id.btnLanguage);
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        themeToggleButton.setOnClickListener(v -> {
            boolean newDark = !isDarkModeActive();
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_IS_DARK, newDark)
                    .apply();
            AppCompatDelegate.setDefaultNightMode(
                    newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // PHASE 4: all findViewById calls happen AFTER setContentView.
        db = new UserDbHelper(this);
        sessionManager = new SessionManager(this);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvAbout = findViewById(R.id.tvAbout);

        // Level 1 UX: eye icon reveals/hides the password. No validation logic touched.
        setupPasswordVisibilityToggle(etPassword);

        btnLogin.setOnClickListener(v -> login());

        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Forgot password: explicit internal navigation to the reset screen.
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ResetPasswordActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // About: explicit internal navigation; the system intents live in AboutActivity.
        tvAbout.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    /** In-app language switcher: French (default), Arabic (RTL), English. */
    private void showLanguageDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.language_dialog_title)
                .setMessage(R.string.language_dialog_message)
                .setItems(new CharSequence[]{
                        getString(R.string.language_fr),
                        getString(R.string.language_ar),
                        getString(R.string.language_en)
                }, (dialog, which) -> {
                    String language = which == 0 ? "fr" : which == 1 ? "ar" : "en";
                    LocaleHelper.setLocale(this, language);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean isSystemInDarkMode() {
        int uiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean isDarkModeActive() {
        int mode = AppCompatDelegate.getDefaultNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        }
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        }
        return isSystemInDarkMode();
    }

    private void updateToggleButtonText(boolean isDark) {
        themeToggleButton.setText(isDark ? R.string.theme_dark : R.string.theme_light);
    }

    /**
     * Level 1 UX: toggles the password TransformationMethod when the end drawable is tapped.
     * Uses getCompoundDrawablesRelative() so "end" is correct in LTR and RTL layouts.
     */
    private void setupPasswordVisibilityToggle(EditText field) {
        field.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }

            Drawable[] drawables = field.getCompoundDrawablesRelative();
            if (drawables.length < 3 || drawables[2] == null) {
                return false;
            }

            int iconWidth = drawables[2].getBounds().width();
            if (iconWidth <= 0) {
                return false;
            }

            int rightLimit = field.getWidth() - field.getPaddingEnd();
            int leftLimit = rightLimit - iconWidth;
            boolean insideIcon = event.getX() >= leftLimit && event.getX() <= rightLimit
                    && event.getY() >= 0 && event.getY() <= field.getHeight();
            if (!insideIcon) {
                return false; // Let the EditText keep normal focus/cursor behaviour.
            }

            if (field.getTransformationMethod() instanceof PasswordTransformationMethod) {
                field.setTransformationMethod(null);
            } else {
                field.setTransformationMethod(new PasswordTransformationMethod());
            }
            field.setSelection(field.getText().length());
            return true; // Consumed: do not also move the cursor.
        });
    }

    /** Level 1 UX: dismiss the keyboard using whatever view currently has focus. */
    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Level 1 UX: one coloured Toast style for the whole screen. */
    @SuppressWarnings("deprecation")
    private void showToast(String msg, boolean isSuccess) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        View v = toast.getView();
        if (v != null) {
            v.setBackgroundResource(R.drawable.toast_bg);
            TextView text = v.findViewById(android.R.id.message);
            if (text != null) {
                text.setTextColor(isSuccess
                        ? Color.parseColor("#4CAF50")
                        : Color.parseColor("#F44336"));
            }
        }
        toast.show();
    }

    /** Validates input, delegates to the Phase 3 database layer, then routes by role. */
    private void login() {
        String email = etEmail.getText().toString().trim();
        // Password is deliberately NOT trimmed: spaces can be part of a password.
        String password = etPassword.getText().toString();

        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                || password.isEmpty()) {
            showToast(getString(R.string.error_check_email_password), false);
            return;
        }

        // Keyboard only closes once the form is valid; errors keep focus for correction.
        hideKeyboard();

        UserProfile user = db.login(email, password);
        if (user == null) {
            // Generic message: do not reveal whether the email exists.
            showToast(getString(R.string.error_invalid_credentials), false);
            return;
        }

        // Remember Me: save ONLY after successful authentication.
        sessionManager.saveSession(user.getId(), user.getFullName(), user.getRole().name());

        Intent intent;
        if (user.getRole() == Role.ADMIN) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, UserDashboardActivity.class);
        }

        intent.putExtra("user_id", user.getId());
        intent.putExtra("user_name", user.getFullName());
        intent.putExtra("user_role", user.getRole().name());

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish(); // The login screen must not stay on the back stack.
    }
}

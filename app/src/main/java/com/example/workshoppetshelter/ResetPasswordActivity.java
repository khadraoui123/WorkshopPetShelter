// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\ResetPasswordActivity.java
package com.example.workshoppetshelter;

import android.content.Context;
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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * Forgot-password screen (educational workshop variant: no emailed reset token).
 *
 * Separation of concerns:
 *   - This Activity owns UI validation, visibility toggles and user feedback.
 *   - UserDbHelper.resetPassword() owns the salt regeneration, PBKDF2 hashing and the
 *     parameterized UPDATE. No SQL and no crypto live in this class.
 *
 * On success the screen simply finishes(), returning to the login form where the user
 * signs in with the new password. It never auto-logs-in and never writes a session.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private UserDbHelper db;
    private EditText etResetEmail;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private MaterialButton btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Helper created once, not on every click.
        db = new UserDbHelper(this);

        etResetEmail = findViewById(R.id.etResetEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnReset = findViewById(R.id.btnReset);

        // Same UX as RegisterActivity: an eye icon toggles BOTH secret fields.
        setupPasswordVisibilityToggle(etNewPassword);
        setupPasswordVisibilityToggle(etConfirmPassword);

        btnReset.setOnClickListener(v -> resetPassword());
    }

    /**
     * Level 1 UX: toggles the password TransformationMethod when the end drawable is tapped.
     * getCompoundDrawablesRelative() keeps index 2 correct for RTL layouts too.
     */
    private void setupPasswordVisibilityToggle(EditText field) {
        field.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }

            Drawable[] drawables = field.getCompoundDrawablesRelative();
            if (drawables.length < 3 || drawables[2] == null) {
                return false; // Defensive: no icon, no tap target.
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

    /** Validates the form, delegates the crypto + SQL to the DB layer, then closes on success. */
    private void resetPassword() {
        String email = etResetEmail.getText().toString().trim();
        // Password is deliberately NOT trimmed: spaces are valid characters in a secret.
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Vérifiez l'adresse e-mail.", false);
            return;
        }

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            showToast("Le mot de passe doit contenir au moins 8 caractères.", false);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showToast("Les mots de passe ne correspondent pas.", false);
            return;
        }

        // Keyboard only closes once the form is valid; errors keep focus for correction.
        hideKeyboard();

        boolean success = db.resetPassword(email, newPassword);
        if (!success) {
            // Workshop UX: explicit failure is more informative for the demo.
            // A production app would show the SAME message for every outcome.
            showToast("Cet e-mail n'existe pas.", false);
            return;
        }

        showToast("Mot de passe réinitialisé avec succès !", true);
        finish(); // Return to MainActivity; the user signs in with the new password.
    }
}

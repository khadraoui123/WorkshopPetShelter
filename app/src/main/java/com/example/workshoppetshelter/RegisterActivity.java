// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\RegisterActivity.java
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
 * Phase 5: real registration screen.
 *
 * Separation of concerns:
 *   - This Activity owns UI validation and user feedback (format, emptiness, length).
 *   - UserDbHelper owns data integrity (UNIQUE email, Role.USER, PBKDF2 salt/hash).
 * No Intent to MainActivity is used: the new user has NOT logged in yet.
 *
 * Level 1 UX ONLY: password visibility toggle, coloured Toasts, keyboard dismissal.
 */
public class RegisterActivity extends AppCompatActivity {

    private UserDbHelper db;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Context is valid here; the helper is created once, not on every click.
        db = new UserDbHelper(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);

        setupPasswordVisibilityToggle(etPassword);

        btnRegister.setOnClickListener(v -> register());
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
                return false;
            }

            if (field.getTransformationMethod() instanceof PasswordTransformationMethod) {
                field.setTransformationMethod(null);
            } else {
                field.setTransformationMethod(new PasswordTransformationMethod());
            }
            field.setSelection(field.getText().length());
            return true;
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

    /** Validates the form, delegates persistence to the DB, then closes this screen on success. */
    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        // Password is deliberately NOT trimmed: spaces are valid characters in a secret.
        String password = etPassword.getText().toString();

        if (name.isEmpty()) {
            showToast("Veuillez saisir votre nom.", false);
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Vérifiez l'adresse e-mail.", false);
            return;
        }

        if (password.length() < 8) {
            showToast("Le mot de passe doit contenir au moins 8 caractères.", false);
            return;
        }

        // Keyboard only closes once the form is valid; errors keep focus for correction.
        hideKeyboard();

        boolean success = db.register(name, email, password);
        if (!success) {
            // The only remaining reason for failure is the UNIQUE email constraint.
            showToast("Cet e-mail est déjà utilisé.", false);
            return;
        }

        showToast("Compte créé avec succès !", true);
        // Return to the previous screen (login). Never auto-login after registration.
        finish();
    }
}

// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\RegisterActivity.java
package com.example.workshoppetshelter;

import android.content.Context;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
 * Phase 8 polish ONLY: hideKeyboard() helper; validation and DB logic unchanged.
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

        btnRegister.setOnClickListener(v -> register());
    }

    /** Phase 8 polish: dismiss the keyboard using the currently focused field's window token. */
    private void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Validates the form, delegates persistence to the DB, then closes this screen on success. */
    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        // Password is deliberately NOT trimmed: spaces are valid characters in a secret.
        String password = etPassword.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre nom.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matches(email)) {
            Toast.makeText(this, "Vérifiez l'adresse e-mail.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(this,
                    "Le mot de passe doit contenir au moins 8 caractères.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Phase 8: keyboard only closes once the form is valid; errors keep focus for correction.
        hideKeyboard(etPassword);

        boolean success = db.register(name, email, password);
        if (!success) {
            // The only remaining reason for failure is the UNIQUE email constraint.
            Toast.makeText(this, "Cet e-mail est déjà utilisé.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Compte créé avec succès !", Toast.LENGTH_SHORT).show();
        // Return to the previous screen (login). Never auto-login after registration.
        finish();
    }
}

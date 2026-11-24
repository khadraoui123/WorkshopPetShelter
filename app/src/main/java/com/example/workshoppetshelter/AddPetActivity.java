// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\AddPetActivity.java
package com.example.workshoppetshelter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/** Phase 7: admin-only form that inserts a new pet through the Phase 3 helper. */
public class AddPetActivity extends AppCompatActivity {

    private UserDbHelper db;
    private EditText etName;
    private EditText etSpecies;
    private EditText etAge;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Phase 8.5 defense in depth: check the role BEFORE inflating any UI.
         * "ADMIN" is Role.ADMIN.name(). Deny by default: null/"" is rejected too.
         */
        String userRole = getIntent().getStringExtra("user_role");
        if (!"ADMIN".equals(userRole)) {
            showToast("Accès refusé. Réservé aux administrateurs.", false);
            finish();
            return;
        }

        setContentView(R.layout.activity_add_pet);

        db = new UserDbHelper(this);

        etName = findViewById(R.id.etPetName);
        etSpecies = findViewById(R.id.etPetSpecies);
        etAge = findViewById(R.id.etPetAge);
        btnSave = findViewById(R.id.btnSavePet);

        btnSave.setOnClickListener(v -> savePet());
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

    private void savePet() {
        String name = etName.getText().toString().trim();
        String species = etSpecies.getText().toString().trim();
        String ageText = etAge.getText().toString().trim();

        if (name.isEmpty() || species.isEmpty() || ageText.isEmpty()) {
            showToast("Veuillez remplir tous les champs.", false);
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            showToast("Âge invalide.", false);
            return;
        }

        if (age < 0) {
            showToast("Âge invalide.", false);
            return;
        }

        // Keyboard only closes once the form is valid; errors keep focus for correction.
        hideKeyboard();

        try {
            long id = db.addPet(name, species, age);
            if (id == -1L) {
                showToast("Erreur lors de l'ajout.", false);
                return;
            }
        } catch (Exception e) {
            showToast("Erreur lors de l'ajout.", false);
            return;
        }

        showToast("Animal ajouté !", true);
        finish();
    }
}

// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\AddPetActivity.java
package com.example.workshoppetshelter;

import android.os.Bundle;
import android.widget.EditText;
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
            Toast.makeText(this, "Accès refusé. Réservé aux administrateurs.", Toast.LENGTH_SHORT).show();
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

    private void savePet() {
        String name = etName.getText().toString().trim();
        String species = etSpecies.getText().toString().trim();
        String ageText = etAge.getText().toString().trim();

        if (name.isEmpty() || species.isEmpty() || ageText.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Âge invalide.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (age < 0) {
            Toast.makeText(this, "Âge invalide.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long id = db.addPet(name, species, age);
            if (id == -1L) {
                Toast.makeText(this, "Erreur lors de l'ajout.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de l'ajout.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Animal ajouté !", Toast.LENGTH_SHORT).show();
        finish();
    }
}

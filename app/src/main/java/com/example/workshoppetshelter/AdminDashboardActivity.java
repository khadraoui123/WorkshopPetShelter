// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\AdminDashboardActivity.java
package com.example.workshoppetshelter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Phase 7: manager dashboard. Pet rows open details in admin mode; the add button
 * opens the real AddPetActivity.
 * Phase 8: empty-state messages are toggled beside each list.
 * Phase 8.5: every outgoing navigation carries user_role = "ADMIN".
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private UserDbHelper db;
    private TextView tvGreeting;
    private ListView lvAllPets;
    private ListView lvUsers;
    private TextView tvEmptyPets;
    private TextView tvEmptyUsers;
    private MaterialButton btnAddPet;
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        String userName = getIntent().getStringExtra("user_name");
        // user_id is received for consistency with the login extras.
        getIntent().getLongExtra("user_id", -1L);

        db = new UserDbHelper(this);

        tvGreeting = findViewById(R.id.tvGreeting);
        lvAllPets = findViewById(R.id.lvAllPets);
        lvUsers = findViewById(R.id.lvUsers);
        tvEmptyPets = findViewById(R.id.tvEmptyPets);
        tvEmptyUsers = findViewById(R.id.tvEmptyUsers);
        btnAddPet = findViewById(R.id.btnAddPet);
        btnLogout = findViewById(R.id.btnLogout);

        tvGreeting.setText("Bonjour " + (userName == null ? "" : userName) + " — Gestionnaire");

        lvAllPets.setOnItemClickListener((parent, view, position, id) -> {
            Pet pet = (Pet) parent.getItemAtPosition(position);
            Intent intent = new Intent(this, PetDetailsActivity.class);
            intent.putExtra("pet_id", pet.getId());
            intent.putExtra("user_role", Role.ADMIN.name());
            startActivity(intent);
        });

        btnAddPet.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPetActivity.class);
            intent.putExtra("user_role", Role.ADMIN.name());
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload on every resume: returning from details/add must show fresh DB data.
        List<Pet> pets = db.getAllPets();
        List<UserProfile> users = db.getAllUsers();
        lvAllPets.setAdapter(new PetAdapter(this, pets));
        lvUsers.setAdapter(new UserAdapter(this, users));

        tvEmptyPets.setVisibility(pets.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyUsers.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
    }
}

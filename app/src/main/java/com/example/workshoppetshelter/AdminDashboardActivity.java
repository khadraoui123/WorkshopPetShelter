// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\AdminDashboardActivity.java
package com.example.workshoppetshelter;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7: manager dashboard. Pet rows open details in admin mode; the add button
 * opens the real AddPetActivity.
 * Phase 8: empty-state messages are toggled beside each list.
 * Phase 8.5: every outgoing navigation carries user_role = "ADMIN".
 * Level 2: only the pets list is filtered; the users list is never touched.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private UserDbHelper db;
    private TextView tvGreeting;
    private ListView lvAllPets;
    private ListView lvUsers;
    private TextView tvEmptyPets;
    private TextView tvEmptyUsers;
    private EditText etSearchPets;
    private MaterialButton btnAddPet;
    private MaterialButton btnLogout;

    private PetAdapter petAdapter;

    // Keeps the Level 1 empty state correct while the built-in filter runs asynchronously.
    private final DataSetObserver petDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            updatePetEmptyState();
        }

        @Override
        public void onInvalidated() {
            updatePetEmptyState();
        }
    };

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
        etSearchPets = findViewById(R.id.etSearchPets);
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

        setupSearch();

        btnAddPet.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPetActivity.class);
            intent.putExtra("user_role", Role.ADMIN.name());
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> finish());
    }

    /** Level 2: ArrayAdapter's Filterable implementation does all the work in memory. */
    private void setupSearch() {
        etSearchPets.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before the text changes.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filtering happens in afterTextChanged, where the Editable is final.
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (petAdapter == null) {
                    return;
                }
                petAdapter.getFilter().filter(s.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload on every resume: returning from details/add must show fresh DB data.
        List<Pet> pets = db.getAllPets();
        if (pets == null) {
            pets = new ArrayList<>();
        }

        List<UserProfile> users = db.getAllUsers();
        if (users == null) {
            users = new ArrayList<>();
        }

        /*
         * Give the adapter a COPY of the loaded data. ArrayAdapter.getFilter() saves its own
         * original-values snapshot, so our copy keeps the source list reusable and reversible.
         */
        if (petAdapter != null) {
            petAdapter.unregisterDataSetObserver(petDataSetObserver);
        }
        petAdapter = new PetAdapter(this, new ArrayList<>(pets));
        petAdapter.registerDataSetObserver(petDataSetObserver);

        lvAllPets.setAdapter(petAdapter);
        lvUsers.setAdapter(new UserAdapter(this, users));

        // Re-apply a query that survived a rotation or a return from another screen.
        String query = etSearchPets.getText() == null
                ? ""
                : etSearchPets.getText().toString();
        if (!query.isEmpty()) {
            petAdapter.getFilter().filter(query);
        }

        updatePetEmptyState();
        tvEmptyUsers.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updatePetEmptyState() {
        if (tvEmptyPets == null || petAdapter == null) {
            return;
        }
        tvEmptyPets.setVisibility(petAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
    }
}

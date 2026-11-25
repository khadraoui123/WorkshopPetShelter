// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\AdminDashboardActivity.java
package com.example.workshoppetshelter;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7: manager dashboard. Pet rows open details in admin mode; the FAB
 * opens the real AddPetActivity.
 * Phase 8: empty-state messages are toggled beside each list.
 * Phase 8.5: every outgoing navigation carries user_role = "ADMIN".
 * Level 2: only the pets list is filtered; the users list is never touched.
 * Level 3: third section = adoption history; all lists fade in after data load.
 * Session: corrupted intents are rejected; logout clears session and returns to login.
 * Material 3: MaterialToolbar + FloatingActionButton replace the custom header and button.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private UserDbHelper db;
    private SessionManager sessionManager;
    private TextView tvGreeting;
    private ListView lvAllPets;
    private ListView lvUsers;
    private ListView lvAdoptedPets;
    private TextView tvEmptyPets;
    private TextView tvEmptyUsers;
    private TextView tvEmptyAdopted;
    private EditText etSearchPets;
    private FloatingActionButton fabAddPet;
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
        LocaleHelper.applySavedLocale(this);
        super.onCreate(savedInstanceState);

        /*
         * Session integrity: -1 means the intent is missing/corrupted. Bounce to login
         * BEFORE inflating the dashboard, so no private data is rendered.
         */
        long incomingUserId = getIntent().getLongExtra("user_id", -1L);
        if (incomingUserId == -1L) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_dashboard);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);

        String userName = getIntent().getStringExtra("user_name");

        db = new UserDbHelper(this);

        tvGreeting = findViewById(R.id.tvGreeting);
        lvAllPets = findViewById(R.id.lvAllPets);
        lvUsers = findViewById(R.id.lvUsers);
        lvAdoptedPets = findViewById(R.id.lvAdoptedPets);
        tvEmptyPets = findViewById(R.id.tvEmptyPets);
        tvEmptyUsers = findViewById(R.id.tvEmptyUsers);
        tvEmptyAdopted = findViewById(R.id.tvEmptyAdopted);
        etSearchPets = findViewById(R.id.etSearchPets);
        fabAddPet = findViewById(R.id.fabAddPet);
        btnLogout = findViewById(R.id.btnLogout);

        tvGreeting.setText(userName == null || userName.isEmpty()
                ? getString(R.string.greeting_admin_empty)
                : getString(R.string.greeting_admin, userName));

        lvAllPets.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));
        lvAdoptedPets.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));

        setupSearch();

        fabAddPet.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPetActivity.class);
            intent.putExtra("user_role", Role.ADMIN.name());
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    /** Session: confirmed logout clears the session and returns to the login screen. */
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    sessionManager.logout();
                    // Explicit navigation is mandatory: finish() alone would let Splash
                    // read a stale/cleared session and skip the login screen.
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openPetDetails(Pet pet) {
        Intent intent = new Intent(this, PetDetailsActivity.class);
        intent.putExtra("pet_id", pet.getId());
        intent.putExtra("user_role", Role.ADMIN.name());
        startActivity(intent);
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

        List<Pet> adopted = db.getAdoptedPets();
        if (adopted == null) {
            adopted = new ArrayList<>();
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

        // Fresh adapter + fresh list on every resume: no duplicate rows in the history.
        lvAdoptedPets.setAdapter(new PetAdapter(this, adopted));

        // Re-apply a query that survived a rotation or a return from another screen.
        String query = etSearchPets.getText() == null
                ? ""
                : etSearchPets.getText().toString();
        if (!query.isEmpty()) {
            petAdapter.getFilter().filter(query);
        }

        updatePetEmptyState();
        tvEmptyUsers.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyAdopted.setVisibility(adopted.isEmpty() ? View.VISIBLE : View.GONE);

        // Level 3: data is loaded and views are measured; now start the system fade-in.
        startFadeIn(lvAllPets);
        startFadeIn(lvUsers);
        startFadeIn(lvAdoptedPets);
    }

    /** A fresh Animation instance per view avoids sharing mutable start-time state. */
    private void startFadeIn(View view) {
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        view.startAnimation(fadeIn);
    }

    private void updatePetEmptyState() {
        if (tvEmptyPets == null || petAdapter == null) {
            return;
        }
        tvEmptyPets.setVisibility(petAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
    }
}

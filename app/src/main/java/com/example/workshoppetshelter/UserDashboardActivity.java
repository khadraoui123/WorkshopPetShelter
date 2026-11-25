// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserDashboardActivity.java
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

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 7: adopter dashboard. Pet rows open the real details screen.
 * Phase 8: empty-state messages are toggled beside each list.
 * Phase 8.5: every outgoing navigation carries user_role = "USER" plus user_id.
 * Level 2: PetAdapter's built-in ArrayAdapter filter searches name + species.
 * Level 3: "Mon Profil" routes to ProfileActivity; lists fade in after data load.
 * Session: corrupted intents are rejected; logout clears session and returns to login.
 */
public class UserDashboardActivity extends AppCompatActivity {

    private UserDbHelper db;
    private SessionManager sessionManager;
    private long userId;
    private TextView tvGreeting;
    private ListView lvAvailablePets;
    private ListView lvMyRequests;
    private TextView tvEmptyAvailable;
    private TextView tvEmptyRequests;
    private EditText etSearchAvailable;
    private MaterialButton btnLogout;
    private MaterialButton btnProfile;

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

        setContentView(R.layout.activity_user_dashboard);

        userId = incomingUserId;
        sessionManager = new SessionManager(this);

        String userName = getIntent().getStringExtra("user_name");

        db = new UserDbHelper(this);

        /*
         * Level 3: the current login screen passes id/name/role but not the email.
         * Read the extra when present (forward-compatible), otherwise resolve it with
         * the DB helper's existing public method. No new SQL method is introduced.
         */
        String emailExtra = getIntent().getStringExtra("user_email");
        final String userEmail = (emailExtra == null || emailExtra.isEmpty())
                ? resolveEmailForUser(userId)
                : emailExtra;

        tvGreeting = findViewById(R.id.tvGreeting);
        lvAvailablePets = findViewById(R.id.lvAvailablePets);
        lvMyRequests = findViewById(R.id.lvMyRequests);
        tvEmptyAvailable = findViewById(R.id.tvEmptyAvailable);
        tvEmptyRequests = findViewById(R.id.tvEmptyRequests);
        etSearchAvailable = findViewById(R.id.etSearchAvailable);
        btnLogout = findViewById(R.id.btnLogout);
        btnProfile = findViewById(R.id.btnProfile);

        tvGreeting.setText("Bonjour " + (userName == null ? "" : userName) + " 🐾");

        lvAvailablePets.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));
        lvMyRequests.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));

        setupSearch();

        btnProfile.setOnClickListener(v -> openProfile(userName, userEmail));
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    /** Session: confirmed logout clears the session and returns to the login screen. */
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    sessionManager.logout();
                    // Explicit navigation is mandatory: finish() alone would let Splash
                    // read a stale/cleared session and skip the login screen.
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    /** Reuses the existing public user list; no SQL is added for this Level 3 route. */
    private String resolveEmailForUser(long id) {
        if (id < 0) {
            return null;
        }
        for (UserProfile user : db.getAllUsers()) {
            if (user.getId() == id) {
                return user.getEmail();
            }
        }
        return null;
    }

    /** Level 3: explicit extras requested by ProfileActivity. */
    private void openProfile(String userName, String userEmail) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_name", userName);
        intent.putExtra("user_email", userEmail);
        startActivity(intent);
    }

    /** Level 2: ArrayAdapter's Filterable implementation does all the work in memory. */
    private void setupSearch() {
        etSearchAvailable.addTextChangedListener(new TextWatcher() {
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

    private void openPetDetails(Pet pet) {
        Intent intent = new Intent(this, PetDetailsActivity.class);
        intent.putExtra("pet_id", pet.getId());
        intent.putExtra("user_id", userId);
        intent.putExtra("user_role", Role.USER.name());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload on every resume: returning from PetDetails must show fresh DB data.
        List<Pet> available = db.getAvailablePets();
        if (available == null) {
            available = new ArrayList<>();
        }

        List<Pet> requests = db.getPetsRequestedBy(userId);
        if (requests == null) {
            requests = new ArrayList<>();
        }

        /*
         * Give the adapter a COPY of the loaded data. ArrayAdapter.getFilter() saves its own
         * original-values snapshot, so our copy keeps the source list reusable and reversible.
         */
        if (petAdapter != null) {
            petAdapter.unregisterDataSetObserver(petDataSetObserver);
        }
        petAdapter = new PetAdapter(this, new ArrayList<>(available));
        petAdapter.registerDataSetObserver(petDataSetObserver);

        lvAvailablePets.setAdapter(petAdapter);
        lvMyRequests.setAdapter(new PetAdapter(this, requests));

        // Re-apply a query that survived a rotation or a return from another screen.
        String query = etSearchAvailable.getText() == null
                ? ""
                : etSearchAvailable.getText().toString();
        if (!query.isEmpty()) {
            petAdapter.getFilter().filter(query);
        }

        updatePetEmptyState();
        tvEmptyRequests.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);

        // Level 3: data is loaded and views are measured; now start the system fade-in.
        startFadeIn(lvAvailablePets);
        startFadeIn(lvMyRequests);
    }

    /** A fresh Animation instance per view avoids sharing mutable start-time state. */
    private void startFadeIn(View view) {
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        view.startAnimation(fadeIn);
    }

    private void updatePetEmptyState() {
        if (tvEmptyAvailable == null || petAdapter == null) {
            return;
        }
        tvEmptyAvailable.setVisibility(
                petAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
    }
}

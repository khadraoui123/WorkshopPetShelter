// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserDashboardActivity.java
package com.example.workshoppetshelter;

import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
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
 * Material 3: a MaterialToolbar replaces the old custom section header.
 * Phase B: first-load ProgressBar, MaterialCardView rows, and preserved search state.
 * Smart Features: Match Me suggestion + favorites wishlist, both refreshed on resume.
 */
public class UserDashboardActivity extends AppCompatActivity {

    private static final long LOAD_DELAY_MS = 800L;

    private UserDbHelper db;
    private SessionManager sessionManager;
    private long userId;
    private String userName;
    private TextView tvGreeting;
    private ListView lvAvailablePets;
    private ListView lvMyRequests;
    private ListView lvFavorites;
    private TextView tvEmptyAvailable;
    private TextView tvEmptyRequests;
    private TextView tvEmptyFavorites;
    private EditText etSearchAvailable;
    private ProgressBar progressBar;
    private MaterialButton btnLogout;
    private MaterialButton btnProfile;
    private MaterialButton btnMatchMe;

    private PetAdapter petAdapter;
    private boolean isFirstLoad = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }

        setContentView(R.layout.activity_user_dashboard);

        // A rotation restores the UI state; do not replay the simulated first-load delay.
        if (savedInstanceState != null) {
            isFirstLoad = false;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        userId = incomingUserId;
        sessionManager = new SessionManager(this);
        userName = getIntent().getStringExtra("user_name");

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
        lvFavorites = findViewById(R.id.lvFavorites);
        tvEmptyAvailable = findViewById(R.id.tvEmptyAvailable);
        tvEmptyRequests = findViewById(R.id.tvEmptyRequests);
        tvEmptyFavorites = findViewById(R.id.tvEmptyFavorites);
        etSearchAvailable = findViewById(R.id.etSearchAvailable);
        progressBar = findViewById(R.id.progressBar);
        btnLogout = findViewById(R.id.btnLogout);
        btnProfile = findViewById(R.id.btnProfile);
        btnMatchMe = findViewById(R.id.btnMatchMe);

        tvGreeting.setText(userName == null || userName.isEmpty()
                ? getString(R.string.greeting_user_empty)
                : getString(R.string.greeting_user, userName));

        lvAvailablePets.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));
        lvMyRequests.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));
        lvFavorites.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));

        btnMatchMe.setOnClickListener(v -> showRandomPetSuggestion());
        setupSearch();

        btnProfile.setOnClickListener(v -> openProfile(userName, userEmail));
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
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
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
    private void openProfile(String name, String userEmail) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_name", name);
        intent.putExtra("user_email", userEmail);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

    /** Smart Features: SQLite draws one AVAILABLE pet; Java only presents the result. */
    private void showRandomPetSuggestion() {
        Pet suggested = db.getRandomAvailablePet();
        if (suggested == null) {
            showToast(getString(R.string.empty_available_pets), false);
            return;
        }

        String name = suggested.getName() == null || suggested.getName().trim().isEmpty()
                ? getString(R.string.empty_value)
                : suggested.getName();
        String species = PetLabels.labelForSpecies(this, suggested.getSpecies());

        new AlertDialog.Builder(this)
                .setTitle(R.string.match_suggestion_title)
                .setMessage(getString(R.string.match_suggestion_message, name, species))
                .setPositiveButton(R.string.view_details, (dialog, which) ->
                        openPetDetails(suggested))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openPetDetails(Pet pet) {
        if (pet == null) {
            return;
        }
        Intent intent = new Intent(this, PetDetailsActivity.class);
        intent.putExtra("pet_id", pet.getId());
        intent.putExtra("user_id", userId);
        intent.putExtra("user_name", userName);
        intent.putExtra("user_role", Role.USER.name());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Capture the query BEFORE any data work so filtering survives every resume.
        final String query = currentSearchQuery();

        if (isFirstLoad) {
            progressBar.setVisibility(View.VISIBLE);
            setListsVisibility(View.GONE);

            // Main-looper postDelayed keeps the UI thread non-blocking (no Thread.sleep).
            mainHandler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                loadDashboardData(query);
                progressBar.setVisibility(View.GONE);
                setListsVisibility(View.VISIBLE);
                isFirstLoad = false;
                fadeInLists();
            }, LOAD_DELAY_MS);
            return;
        }

        // Returning from PetDetails: fresh data, no spinner flash, same search filter.
        loadDashboardData(query);
        fadeInLists();
    }

    /** Reads the search field defensively; EditText text can be null during restoration. */
    private String currentSearchQuery() {
        return etSearchAvailable.getText() == null
                ? ""
                : etSearchAvailable.getText().toString();
    }

    /** Single source of truth for loading adapters and re-applying the saved filter. */
    private void loadDashboardData(String query) {
        // Reload on every resume: returning from PetDetails must show fresh DB data.
        List<Pet> available = db.getAvailablePets();
        if (available == null) {
            available = new ArrayList<>();
        }

        List<Pet> requests = db.getPetsRequestedBy(userId);
        if (requests == null) {
            requests = new ArrayList<>();
        }

        // Smart Features: favorites are read on every resume, so removing one in details
        // is reflected immediately when the user returns to the dashboard.
        List<Pet> favorites = db.getFavoritePetsByUser(userId);
        if (favorites == null) {
            favorites = new ArrayList<>();
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
        lvFavorites.setAdapter(new PetAdapter(this, new ArrayList<>(favorites)));

        // Re-apply a query that survived a rotation or a return from another screen.
        if (!query.isEmpty()) {
            petAdapter.getFilter().filter(query);
        }

        updatePetEmptyState();
        tvEmptyRequests.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyFavorites.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setListsVisibility(int visibility) {
        lvAvailablePets.setVisibility(visibility);
        lvMyRequests.setVisibility(visibility);
        lvFavorites.setVisibility(visibility);
    }

    private void fadeInLists() {
        startFadeIn(lvAvailablePets);
        startFadeIn(lvMyRequests);
        startFadeIn(lvFavorites);
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

    /** Same coloured Toast style as the other screens. */
    @SuppressWarnings("deprecation")
    private void showToast(String msg, boolean isSuccess) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        View view = toast.getView();
        if (view != null) {
            view.setBackgroundResource(R.drawable.toast_bg);
            TextView text = view.findViewById(android.R.id.message);
            if (text != null) {
                text.setTextColor(isSuccess
                        ? Color.parseColor("#4CAF50")
                        : Color.parseColor("#F44336"));
            }
        }
        toast.show();
    }

    @Override
    protected void onDestroy() {
        // No delayed callback may touch a destroyed Activity.
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserDashboardActivity.java
package com.example.workshoppetshelter;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Phase 7: adopter dashboard. Pet rows open the real details screen.
 * Phase 8: empty-state messages are toggled beside each list.
 * Phase 8.5: every outgoing navigation carries user_role = "USER" plus user_id.
 * Level 2 UX: in-memory, case-insensitive filtering of the AVAILABLE pet list only.
 */
public class UserDashboardActivity extends AppCompatActivity {

    private UserDbHelper db;
    private long userId;
    private TextView tvGreeting;
    private ListView lvAvailablePets;
    private ListView lvMyRequests;
    private TextView tvEmptyAvailable;
    private TextView tvEmptyRequests;
    private EditText etSearch;
    private MaterialButton btnLogout;

    // Keep the unfiltered source separate from the adapter: filtering must be reversible.
    private List<Pet> allAvailablePets = new ArrayList<>();
    private PetAdapter petAdapter;
    private Filter petFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        userId = getIntent().getLongExtra("user_id", -1L);
        String userName = getIntent().getStringExtra("user_name");

        db = new UserDbHelper(this);

        tvGreeting = findViewById(R.id.tvGreeting);
        lvAvailablePets = findViewById(R.id.lvAvailablePets);
        lvMyRequests = findViewById(R.id.lvMyRequests);
        tvEmptyAvailable = findViewById(R.id.tvEmptyAvailable);
        tvEmptyRequests = findViewById(R.id.tvEmptyRequests);
        etSearch = findViewById(R.id.etSearch);
        btnLogout = findViewById(R.id.btnLogout);

        tvGreeting.setText("Bonjour " + (userName == null ? "" : userName) + " 🐾");

        lvAvailablePets.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));
        lvMyRequests.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));

        setupSearch();

        btnLogout.setOnClickListener(v -> finish());
    }

    /** Level 2 UX: filter as the user types; no SQL query is executed. */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before the text changes.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (petAdapter == null) {
                    return;
                }
                petAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Filtering already happened in onTextChanged.
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
        List<Pet> loadedAvailable = db.getAvailablePets();
        allAvailablePets = loadedAvailable == null
                ? new ArrayList<>()
                : new ArrayList<>(loadedAvailable);

        List<Pet> requests = db.getPetsRequestedBy(userId);
        if (requests == null) {
            requests = new ArrayList<>();
        }

        /*
         * The adapter receives a COPY. The filter's publishResults() will clear/add items,
         * so sharing the same list would destroy our unfiltered source list.
         */
        petAdapter = new PetAdapter(this, new ArrayList<>(allAvailablePets)) {
            @Override
            public Filter getFilter() {
                return petFilter;
            }
        };
        petFilter = new PetListFilter(petAdapter, allAvailablePets, tvEmptyAvailable);

        lvAvailablePets.setAdapter(petAdapter);
        lvMyRequests.setAdapter(new PetAdapter(this, requests));

        String query = etSearch.getText() == null ? "" : etSearch.getText().toString();
        if (query.trim().isEmpty()) {
            tvEmptyAvailable.setVisibility(
                    allAvailablePets.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            // Re-apply a previous search after returning from another screen.
            petAdapter.getFilter().filter(query);
        }
        tvEmptyRequests.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Case-insensitive in-memory filter over name and species.
     * Pet does not override toString(), so ArrayAdapter's default Filter cannot be relied on.
     */
    private static class PetListFilter extends Filter {

        private final PetAdapter adapter;
        private final List<Pet> allPets;
        private final TextView emptyView;

        PetListFilter(PetAdapter adapter, List<Pet> allPets, TextView emptyView) {
            this.adapter = adapter;
            this.allPets = allPets;
            this.emptyView = emptyView;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String query = constraint == null
                    ? ""
                    : constraint.toString().trim().toLowerCase(Locale.ROOT);

            List<Pet> filtered = new ArrayList<>();
            if (query.isEmpty()) {
                filtered.addAll(allPets);
            } else {
                for (Pet pet : allPets) {
                    if (pet == null) {
                        continue;
                    }
                    String name = pet.getName() == null
                            ? ""
                            : pet.getName().toLowerCase(Locale.ROOT);
                    String species = pet.getSpecies() == null
                            ? ""
                            : pet.getSpecies().toLowerCase(Locale.ROOT);
                    if (name.contains(query) || species.contains(query)) {
                        filtered.add(pet);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filtered;
            results.count = filtered.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            List<Pet> values;
            if (results != null && results.values instanceof List<?>) {
                values = (List<Pet>) results.values;
            } else {
                values = new ArrayList<>(allPets);
            }

            adapter.clear();
            adapter.addAll(values);
            adapter.notifyDataSetChanged();

            // Level 1 empty state follows the FILTERED result count, not the original list.
            if (emptyView != null) {
                emptyView.setVisibility(values.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    }
}

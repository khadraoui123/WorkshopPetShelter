// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserDashboardActivity.java
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
 * Phase 7: adopter dashboard. Pet rows open the real details screen.
 * Phase 8: empty-state messages are toggled beside each list.
 * Phase 8.5: every outgoing navigation carries user_role = "USER" plus user_id.
 */
public class UserDashboardActivity extends AppCompatActivity {

    private UserDbHelper db;
    private long userId;
    private TextView tvGreeting;
    private ListView lvAvailablePets;
    private ListView lvMyRequests;
    private TextView tvEmptyAvailable;
    private TextView tvEmptyRequests;
    private MaterialButton btnLogout;

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
        btnLogout = findViewById(R.id.btnLogout);

        tvGreeting.setText("Bonjour " + (userName == null ? "" : userName) + " 🐾");

        lvAvailablePets.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));
        lvMyRequests.setOnItemClickListener((parent, view, position, id) ->
                openPetDetails((Pet) parent.getItemAtPosition(position)));

        btnLogout.setOnClickListener(v -> finish());
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
        List<Pet> requests = db.getPetsRequestedBy(userId);
        lvAvailablePets.setAdapter(new PetAdapter(this, available));
        lvMyRequests.setAdapter(new PetAdapter(this, requests));

        tvEmptyAvailable.setVisibility(available.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyRequests.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
    }
}

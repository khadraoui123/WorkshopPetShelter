// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\ProfileActivity.java
package com.example.workshoppetshelter;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Level 3: read-only profile for the adopter (USER) dashboard.
 *
 * The screen receives user_id, user_name and user_email from UserDashboardActivity.
 * It is internal-only (exported="false" in the manifest) and is never reachable from
 * the admin dashboard, so its role is necessarily Role.USER. The adopted count is
 * recomputed in onResume(), exactly like the dashboard lists, so returning to this
 * screen always shows the current database value.
 */
public class ProfileActivity extends AppCompatActivity {

    private UserDbHelper db;
    private long userId;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private TextView tvAdoptedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new UserDbHelper(this);

        userId = getIntent().getLongExtra("user_id", -1L);
        String userName = getIntent().getStringExtra("user_name");
        String userEmail = getIntent().getStringExtra("user_email");

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvAdoptedCount = findViewById(R.id.tvAdoptedCount);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        tvProfileName.setText("Nom : " + (userName == null || userName.isEmpty()
                ? "—" : userName));
        tvProfileEmail.setText("Email : " + (userEmail == null || userEmail.isEmpty()
                ? "—" : userEmail));
        // This Activity is USER-only; no extra intent field is needed for the role.
        tvProfileRole.setText("Rôle : " + Role.USER.name());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // COUNT(*) runs in SQLite; we never fetch every pet just to count them in Java.
        int adoptedCount = db.countAdoptedPetsByUser(userId);
        tvAdoptedCount.setText("Animaux adoptés : " + adoptedCount);
    }
}

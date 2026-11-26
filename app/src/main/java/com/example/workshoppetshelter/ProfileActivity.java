// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\ProfileActivity.java
package com.example.workshoppetshelter;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Level 3: profile for the adopter (USER) dashboard.
 * Phase C: the name can be edited; the database AND the session are updated together.
 * Bug-fix polish: Material 3 cards, an initials avatar and a compact icon edit action.
 *
 * The screen receives user_id, user_name and user_email from UserDashboardActivity.
 * It is internal-only (exported="false" in the manifest) and is never reachable from
 * the admin dashboard, so its role is necessarily Role.USER. The adopted count is
 * recomputed in onResume(), exactly like the dashboard lists, so returning to this
 * screen always shows the current database value.
 */
public class ProfileActivity extends AppCompatActivity {

    private UserDbHelper db;
    private SessionManager sessionManager;
    private long userId;
    private String currentName;
    private TextView tvAvatarInitial;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private TextView tvAdoptedCount;
    private MaterialButton btnEditName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new UserDbHelper(this);
        sessionManager = new SessionManager(this);

        userId = getIntent().getLongExtra("user_id", -1L);
        String userName = getIntent().getStringExtra("user_name");
        String userEmail = getIntent().getStringExtra("user_email");

        // Keep one source of truth for the dialog's pre-filled value.
        currentName = (userName == null || userName.isEmpty())
                ? sessionManager.getUserName()
                : userName;
        if (currentName == null) {
            currentName = "";
        }

        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvAdoptedCount = findViewById(R.id.tvAdoptedCount);
        btnEditName = findViewById(R.id.btnEditName);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnEditName.setOnClickListener(v -> showEditNameDialog());

        updateAvatarInitial(currentName);
        tvProfileName.setText(getString(R.string.profile_name_format,
                currentName.isEmpty() ? getString(R.string.empty_value) : currentName));
        tvProfileEmail.setText(getString(R.string.profile_email_format,
                userEmail == null || userEmail.isEmpty()
                        ? getString(R.string.empty_value)
                        : userEmail));
        // This Activity is USER-only; no extra intent field is needed for the role.
        tvProfileRole.setText(getString(R.string.profile_role_format,
                getString(R.string.role_user)));
    }

    /**
     * One character, uppercased with the device locale. Arabic works because the
     * character itself is preserved; only Latin case conversion changes anything.
     */
    private void updateAvatarInitial(String name) {
        String initial = name == null || name.trim().isEmpty()
                ? "?"
                : name.trim().substring(0, 1).toUpperCase(Locale.getDefault());
        tvAvatarInitial.setText(initial);
    }

    /**
     * Phase C: edit the name in-place. Validation happens BEFORE any write, and the three
     * states (database, TextView, SharedPreferences session) are updated in one callback.
     */
    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        input.setSingleLine(true);
        input.setText(currentName);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_name_title)
                .setView(input)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, R.string.error_name_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newName.equals(currentName)) {
                        return; // No database write for an unchanged value.
                    }

                    if (!db.updateUserName(userId, newName)) {
                        Toast.makeText(this, R.string.error_update,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentName = newName;
                    tvProfileName.setText(getString(R.string.profile_name_format, newName));
                    updateAvatarInitial(newName);

                    // Keep the session in sync: SplashActivity reads the NAME from here, so a
                    // stale session would route back to the dashboard with the old greeting.
                    // The stored role is preserved instead of being overwritten blindly.
                    String storedRole = sessionManager.getRole();
                    String role = storedRole == null ? Role.USER.name() : storedRole;
                    sessionManager.saveSession(userId, newName, role);

                    Toast.makeText(this, R.string.name_updated,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // COUNT(*) runs in SQLite; we never fetch every pet just to count them in Java.
        int adoptedCount = db.countAdoptedPetsByUser(userId);
        tvAdoptedCount.setText(getString(R.string.profile_adopted_format, adoptedCount));
    }
}

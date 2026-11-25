// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\PetDetailsActivity.java
package com.example.workshoppetshelter;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * Phase 7: pet detail + state-dependent actions.
 *
 * State machine enforced here and in UserDbHelper:
 *   AVAILABLE --(user requests)--> PENDING --(admin approves)--> ADOPTED
 *                                     |
 *                                     +--(admin rejects)--> AVAILABLE
 *
 * Phase 8: confirmation dialog, single-shot buttons, emoji contentDescription.
 * Phase 8.5: role is a String carried in "user_role" ("ADMIN" == Role.ADMIN.name()).
 *            Admin-only delete, strict button projection, deny-by-default role check.
 * Level 1 UX: coloured Toasts only; no state or navigation logic changed.
 * Level 2: MaterialToolbar back and native ACTION_SEND sharing.
 * i18n: every user-visible value is read from resources through PetLabels / getString.
 * Material 3: the toolbar title is replaced dynamically with the pet's name.
 */
public class PetDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_USER_ROLE = "user_role";

    private UserDbHelper db;
    private long petId;
    private long userId;
    private String userRole;
    private boolean isAdmin;
    private Pet pet;

    private TextView tvEmoji;
    private TextView tvInfo;
    private TextView tvStatus;
    private TextView tvRequestedBy;
    private MaterialButton btnShare;
    private MaterialButton btnRequestAdoption;
    private MaterialButton btnApprove;
    private MaterialButton btnReject;
    private MaterialButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        petId = getIntent().getLongExtra("pet_id", -1L);
        userId = getIntent().getLongExtra("user_id", -1L);
        userRole = getIntent().getStringExtra(EXTRA_USER_ROLE);
        // Deny by default: null, "", "USER" or any unknown token gets USER permissions only.
        isAdmin = "ADMIN".equals(userRole);

        db = new UserDbHelper(this);

        tvEmoji = findViewById(R.id.tvDetailsEmoji);
        tvInfo = findViewById(R.id.tvDetailsInfo);
        tvStatus = findViewById(R.id.tvDetailsStatus);
        tvRequestedBy = findViewById(R.id.tvDetailsRequestedBy);
        btnShare = findViewById(R.id.btnShare);
        btnRequestAdoption = findViewById(R.id.btnRequestAdoption);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnDelete = findViewById(R.id.btnDelete);

        btnShare.setOnClickListener(v -> sharePet());

        pet = db.getPetById(petId);
        if (pet == null) {
            showToast(getString(R.string.error_pet_not_found), false);
            finish();
            return;
        }

        renderPet(pet);
        applyVisibilityRules(pet);
        wireActions();
    }

    private void renderPet(Pet pet) {
        // Dynamic title: the toolbar identifies the exact animal, not a generic label.
        String title = pet.getName() == null || pet.getName().trim().isEmpty()
                ? getString(R.string.animal)
                : pet.getName();
        getSupportActionBar().setTitle(title);

        String species = pet.getSpecies();
        tvEmoji.setText(PetLabels.emojiForSpecies(this, species));
        // TalkBack announces a localized species label, not an emoji name.
        tvEmoji.setContentDescription(PetLabels.labelForSpecies(this, species));

        String speciesLabel = species == null || species.trim().isEmpty()
                ? getString(R.string.empty_value)
                : PetLabels.labelForSpecies(this, species);
        tvInfo.setText(getString(R.string.pet_info_short_format,
                speciesLabel, pet.getAge(), getString(R.string.years)));

        String status = pet.getStatus();
        tvStatus.setText(PetLabels.labelForStatus(this, status));
        tvStatus.setTextColor(Color.parseColor(PetLabels.colorForStatus(status)));
    }

    /**
     * Buttons are a direct projection of (role, status): no invalid action is ever offered.
     * ADMIN: delete always; approve/reject only on PENDING; never request adoption.
     * USER:  request adoption only on AVAILABLE; no admin action, ever.
     */
    private void applyVisibilityRules(Pet pet) {
        btnRequestAdoption.setVisibility(View.GONE);
        btnApprove.setVisibility(View.GONE);
        btnReject.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);
        tvRequestedBy.setVisibility(View.GONE);

        String status = pet.getStatus();

        if (isAdmin) {
            btnDelete.setVisibility(View.VISIBLE);
            if ("PENDING".equals(status)) {
                btnApprove.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
            }
        } else if ("AVAILABLE".equals(status)) {
            btnRequestAdoption.setVisibility(View.VISIBLE);
        }

        if ("PENDING".equals(status)) {
            String requester = pet.getRequestedBy();
            tvRequestedBy.setText(getString(R.string.requested_by_colon,
                    requester == null || requester.isEmpty()
                            ? getString(R.string.empty_value)
                            : requester));
            tvRequestedBy.setVisibility(View.VISIBLE);
        }
    }

    private void wireActions() {
        btnRequestAdoption.setOnClickListener(v -> {
            // Single-shot guard: a second tap cannot reach the DB write.
            btnRequestAdoption.setEnabled(false);
            boolean ok = db.requestAdoption(petId, userId);
            showToast(getString(ok
                    ? R.string.success_request_sent
                    : R.string.error_pet_unavailable), ok);
            finish();
        });

        btnApprove.setOnClickListener(v -> {
            btnApprove.setEnabled(false);
            boolean ok = db.approveAdoption(petId);
            showToast(getString(ok
                    ? R.string.success_adoption_approved
                    : R.string.error_pet_unavailable), ok);
            finish();
        });

        /*
         * Reject is irreversible, so we ask first. The button is disabled inside the
         * CONFIRMED action's listener (its first line), not when the dialog opens, so
         * "Cancel" truly does nothing and leaves the screen usable. finish() follows.
         */
        btnReject.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm)
                        .setMessage(R.string.confirm_reject)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            btnReject.setEnabled(false);
                            boolean ok = db.rejectAdoption(petId);
                            showToast(getString(ok
                                    ? R.string.success_request_rejected
                                    : R.string.error_pet_unavailable), ok);
                            finish();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());

        /*
         * Delete is admin-only and destructive: confirmation is mandatory, and the
         * button is disabled as the first line of the confirmed action's listener.
         */
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm)
                        .setMessage(R.string.confirm_delete_pet)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            btnDelete.setEnabled(false);
                            boolean ok = db.deletePet(petId);
                            showToast(getString(ok
                                    ? R.string.success_pet_deleted
                                    : R.string.error_pet_not_found), ok);
                            finish();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());
    }

    /** Level 2: native Android share sheet; visible to Admin and User alike. */
    private void sharePet() {
        if (pet == null) {
            showToast(getString(R.string.error_pet_not_found), false);
            return;
        }

        String name = pet.getName() == null
                ? getString(R.string.animal)
                : pet.getName();
        String species = pet.getSpecies() == null || pet.getSpecies().trim().isEmpty()
                ? getString(R.string.empty_value)
                : PetLabels.labelForSpecies(this, pet.getSpecies());
        String shareText = getString(R.string.share_text_format,
                name, species, pet.getAge(), getString(R.string.years));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.error_no_share_app), false);
        }
    }

    /** Level 1 UX: one coloured Toast style for the whole screen. */
    @SuppressWarnings("deprecation")
    private void showToast(String msg, boolean isSuccess) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        View v = toast.getView();
        if (v != null) {
            v.setBackgroundResource(R.drawable.toast_bg);
            TextView text = v.findViewById(android.R.id.message);
            if (text != null) {
                text.setTextColor(isSuccess
                        ? Color.parseColor("#4CAF50")
                        : Color.parseColor("#F44336"));
            }
        }
        toast.show();
    }
}

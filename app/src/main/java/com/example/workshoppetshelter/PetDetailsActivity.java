// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\PetDetailsActivity.java
package com.example.workshoppetshelter;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

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
 */
public class PetDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_USER_ROLE = "user_role";

    private UserDbHelper db;
    private long petId;
    private long userId;
    private String userRole;
    private boolean isAdmin;

    private TextView tvEmoji;
    private TextView tvTitle;
    private TextView tvInfo;
    private TextView tvStatus;
    private TextView tvRequestedBy;
    private MaterialButton btnRequestAdoption;
    private MaterialButton btnApprove;
    private MaterialButton btnReject;
    private MaterialButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_details);

        petId = getIntent().getLongExtra("pet_id", -1L);
        userId = getIntent().getLongExtra("user_id", -1L);
        userRole = getIntent().getStringExtra(EXTRA_USER_ROLE);
        // Deny by default: null, "", "USER" or any unknown token gets USER permissions only.
        isAdmin = "ADMIN".equals(userRole);

        db = new UserDbHelper(this);

        tvEmoji = findViewById(R.id.tvDetailsEmoji);
        tvTitle = findViewById(R.id.tvDetailsTitle);
        tvInfo = findViewById(R.id.tvDetailsInfo);
        tvStatus = findViewById(R.id.tvDetailsStatus);
        tvRequestedBy = findViewById(R.id.tvDetailsRequestedBy);
        btnRequestAdoption = findViewById(R.id.btnRequestAdoption);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnDelete = findViewById(R.id.btnDelete);

        Pet pet = db.getPetById(petId);
        if (pet == null) {
            showToast("Animal introuvable.", false);
            finish();
            return;
        }

        renderPet(pet);
        applyVisibilityRules(pet);
        wireActions();
    }

    private void renderPet(Pet pet) {
        String species = pet.getSpecies();
        tvEmoji.setText(emojiForSpecies(species));
        // TalkBack should announce "Chien", not "dog face emoji".
        tvEmoji.setContentDescription(labelForSpecies(species));

        tvTitle.setText(pet.getName());
        tvInfo.setText((species == null ? "" : species) + " • " + pet.getAge() + " ans");

        String status = pet.getStatus();
        tvStatus.setText(labelForStatus(status));
        tvStatus.setTextColor(Color.parseColor(colorForStatus(status)));
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
            tvRequestedBy.setText("Demandé par : "
                    + (requester == null || requester.isEmpty() ? "—" : requester));
            tvRequestedBy.setVisibility(View.VISIBLE);
        }
    }

    private void wireActions() {
        btnRequestAdoption.setOnClickListener(v -> {
            // Single-shot guard: a second tap cannot reach the DB write.
            btnRequestAdoption.setEnabled(false);
            boolean ok = db.requestAdoption(petId, userId);
            showToast(ok ? "Demande envoyée !" : "Cet animal n'est plus disponible.", ok);
            finish();
        });

        btnApprove.setOnClickListener(v -> {
            btnApprove.setEnabled(false);
            boolean ok = db.approveAdoption(petId);
            showToast(ok ? "Adoption approuvée !" : "Cet animal n'est plus disponible.", ok);
            finish();
        });

        /*
         * Reject is irreversible, so we ask first. The button is disabled inside the
         * CONFIRMED action's listener (its first line), not when the dialog opens, so
         * "Annuler" truly does nothing and leaves the screen usable. finish() follows.
         */
        btnReject.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Confirmer")
                        .setMessage("Voulez-vous vraiment refuser cette demande ?")
                        .setPositiveButton("Oui", (dialog, which) -> {
                            btnReject.setEnabled(false);
                            boolean ok = db.rejectAdoption(petId);
                            showToast(ok ? "Demande refusée." : "Cet animal n'est plus disponible.", ok);
                            finish();
                        })
                        .setNegativeButton("Annuler", null)
                        .show());

        /*
         * Delete is admin-only and destructive: confirmation is mandatory, and the
         * button is disabled as the first line of the confirmed action's listener.
         */
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Confirmer")
                        .setMessage("Voulez-vous vraiment supprimer cet animal ?")
                        .setPositiveButton("Oui", (dialog, which) -> {
                            btnDelete.setEnabled(false);
                            boolean ok = db.deletePet(petId);
                            showToast(ok ? "Animal supprimé." : "Animal introuvable.", ok);
                            finish();
                        })
                        .setNegativeButton("Annuler", null)
                        .show());
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

    private String labelForStatus(String status) {
        if ("PENDING".equals(status)) {
            return "En attente";
        }
        if ("ADOPTED".equals(status)) {
            return "Adopté";
        }
        return "Disponible";
    }

    private String colorForStatus(String status) {
        if ("PENDING".equals(status)) {
            return "#FF9800";
        }
        if ("ADOPTED".equals(status)) {
            return "#9E9E9E";
        }
        return "#4CAF50";
    }

    private String emojiForSpecies(String species) {
        if (species == null) {
            return "🐾";
        }
        switch (species.toLowerCase(Locale.ROOT)) {
            case "chien":
                return "🐕";
            case "chat":
                return "🐈";
            case "lapin":
                return "🐇";
            default:
                return "🐾";
        }
    }

    /** Accessibility label for the species emoji, independent of the glyph shown. */
    private String labelForSpecies(String species) {
        if (species == null) {
            return "Animal";
        }
        switch (species.toLowerCase(Locale.ROOT)) {
            case "chien":
                return "Chien";
            case "chat":
                return "Chat";
            case "lapin":
                return "Lapin";
            default:
                return "Animal";
        }
    }
}

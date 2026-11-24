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
 * Phase 8 polish ONLY: confirmation dialog, single-shot buttons, emoji contentDescription.
 * No SQL, crypto, or navigation logic was changed.
 */
public class PetDetailsActivity extends AppCompatActivity {

    private UserDbHelper db;
    private long petId;
    private long userId;
    private boolean isAdmin;

    private TextView tvEmoji;
    private TextView tvTitle;
    private TextView tvInfo;
    private TextView tvStatus;
    private TextView tvRequestedBy;
    private MaterialButton btnRequestAdoption;
    private MaterialButton btnApprove;
    private MaterialButton btnReject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_details);

        petId = getIntent().getLongExtra("pet_id", -1L);
        userId = getIntent().getLongExtra("user_id", -1L);
        isAdmin = getIntent().getBooleanExtra("is_admin", false);

        db = new UserDbHelper(this);

        tvEmoji = findViewById(R.id.tvDetailsEmoji);
        tvTitle = findViewById(R.id.tvDetailsTitle);
        tvInfo = findViewById(R.id.tvDetailsInfo);
        tvStatus = findViewById(R.id.tvDetailsStatus);
        tvRequestedBy = findViewById(R.id.tvDetailsRequestedBy);
        btnRequestAdoption = findViewById(R.id.btnRequestAdoption);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);

        Pet pet = db.getPetById(petId);
        if (pet == null) {
            Toast.makeText(this, "Animal introuvable.", Toast.LENGTH_SHORT).show();
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
        // Phase 8: TalkBack should announce "Chien", not "dog face emoji".
        tvEmoji.setContentDescription(labelForSpecies(species));

        tvTitle.setText(pet.getName());
        tvInfo.setText((species == null ? "" : species) + " • " + pet.getAge() + " ans");

        String status = pet.getStatus();
        tvStatus.setText(labelForStatus(status));
        tvStatus.setTextColor(Color.parseColor(colorForStatus(status)));
    }

    /** Buttons are a direct projection of (isAdmin, status): no invalid action is offered. */
    private void applyVisibilityRules(Pet pet) {
        btnRequestAdoption.setVisibility(View.GONE);
        btnApprove.setVisibility(View.GONE);
        btnReject.setVisibility(View.GONE);
        tvRequestedBy.setVisibility(View.GONE);

        String status = pet.getStatus();

        if (!isAdmin && "AVAILABLE".equals(status)) {
            btnRequestAdoption.setVisibility(View.VISIBLE);
        }
        if (isAdmin && "PENDING".equals(status)) {
            btnApprove.setVisibility(View.VISIBLE);
            btnReject.setVisibility(View.VISIBLE);
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
            btnRequestAdoption.setEnabled(false);
            boolean ok = db.requestAdoption(petId, userId);
            Toast.makeText(this,
                    ok ? "Demande envoyée !" : "Cet animal n'est plus disponible.",
                    Toast.LENGTH_SHORT).show();
            finish();
        });

        btnApprove.setOnClickListener(v -> {
            btnApprove.setEnabled(false);
            boolean ok = db.approveAdoption(petId);
            Toast.makeText(this,
                    ok ? "Adoption approuvée !" : "Cet animal n'est plus disponible.",
                    Toast.LENGTH_SHORT).show();
            finish();
        });

        /*
         * Reject is irreversible, so we ask first. The button is disabled when the CONFIRMED
         * action is dispatched (not when the dialog opens) so that "Annuler" truly does nothing
         * and leaves the screen usable. No setEnabled(true) exists: finish() follows success.
         */
        btnReject.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Confirmer")
                        .setMessage("Voulez-vous vraiment refuser cette demande ?")
                        .setPositiveButton("Oui", (dialog, which) -> {
                            btnReject.setEnabled(false);
                            boolean ok = db.rejectAdoption(petId);
                            Toast.makeText(this,
                                    ok ? "Demande refusée." : "Cet animal n'est plus disponible.",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .setNegativeButton("Annuler", null)
                        .show());
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

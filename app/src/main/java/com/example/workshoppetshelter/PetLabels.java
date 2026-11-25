// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\PetLabels.java
package com.example.workshoppetshelter;

import android.content.Context;

import java.util.Locale;

/**
 * Single source of truth for user-facing pet labels.
 *
 * The database intentionally stores French seed tokens ("Chien", "Chat", "Lapin",
 * "Perroquet"). They are DATA, not UI text, so we map them to localized resources
 * here instead of changing the schema or the seed script.
 */
final class PetLabels {

    private PetLabels() {
        // Utility class: no instances.
    }

    static String emojiForSpecies(Context context, String species) {
        switch (normalized(species)) {
            case "chien":
            case "dog":
            case "كلب":
            case "الكلب":
                return context.getString(R.string.emoji_dog);
            case "chat":
            case "cat":
            case "قط":
            case "القط":
                return context.getString(R.string.emoji_cat);
            case "lapin":
            case "rabbit":
            case "أرنب":
            case "الأرنب":
                return context.getString(R.string.emoji_rabbit);
            case "perroquet":
            case "parrot":
            case "ببغاء":
            case "الببغاء":
                return context.getString(R.string.emoji_parrot);
            default:
                return context.getString(R.string.pet_emoji);
        }
    }

    /** Accessibility label and displayed species label, both localized. */
    static String labelForSpecies(Context context, String species) {
        switch (normalized(species)) {
            case "chien":
            case "dog":
            case "كلب":
            case "الكلب":
                return context.getString(R.string.species_dog);
            case "chat":
            case "cat":
            case "قط":
            case "القط":
                return context.getString(R.string.species_cat);
            case "lapin":
            case "rabbit":
            case "أرنب":
            case "الأرنب":
                return context.getString(R.string.species_rabbit);
            case "perroquet":
            case "parrot":
            case "ببغاء":
            case "الببغاء":
                return context.getString(R.string.species_parrot);
            default:
                return species == null || species.trim().isEmpty()
                        ? context.getString(R.string.animal)
                        : species;
        }
    }

    static String labelForStatus(Context context, String status) {
        if ("AVAILABLE".equals(status)) {
            return context.getString(R.string.status_available);
        }
        if ("PENDING".equals(status)) {
            return context.getString(R.string.status_pending);
        }
        if ("ADOPTED".equals(status)) {
            return context.getString(R.string.status_adopted);
        }
        return context.getString(R.string.status_unknown);
    }

    static String colorForStatus(String status) {
        if ("PENDING".equals(status)) {
            return "#FF9800";
        }
        if ("ADOPTED".equals(status)) {
            return "#9E9E9E";
        }
        return "#4CAF50"; // AVAILABLE and any unknown value default to green.
    }

    static String infoForPet(Context context, Pet pet) {
        String name = pet.getName() == null || pet.getName().trim().isEmpty()
                ? context.getString(R.string.empty_value)
                : pet.getName();
        String species = labelForSpecies(context, pet.getSpecies());
        String info = context.getString(R.string.pet_info_format,
                name, species, pet.getAge(), context.getString(R.string.years));

        String requestedBy = pet.getRequestedBy();
        if (requestedBy != null && !requestedBy.isEmpty()) {
            info += context.getString(R.string.requested_by_format, requestedBy);
        }
        return info;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

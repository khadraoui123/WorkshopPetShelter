// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\PetAdapter.java
package com.example.workshoppetshelter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Custom ArrayAdapter: binds one Pet to one row_pet.xml view. */
public class PetAdapter extends ArrayAdapter<Pet> {

    public PetAdapter(Context context, List<Pet> pets) {
        // 0 = no built-in Android resource id; we inflate our own row layout in getView().
        super(context, 0, pets);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_pet, parent, false);
            holder = new ViewHolder();
            holder.emoji = convertView.findViewById(R.id.tvPetEmoji);
            holder.info = convertView.findViewById(R.id.tvPetInfo);
            holder.status = convertView.findViewById(R.id.tvPetStatus);
            convertView.setTag(holder);
        } else {
            // Reuse: avoid inflating XML and finding views again while scrolling.
            holder = (ViewHolder) convertView.getTag();
        }

        Pet pet = getItem(position);
        if (pet == null) {
            return convertView;
        }

        holder.emoji.setText(emojiForSpecies(pet.getSpecies()));
        // Phase 8: accessibility label must match the actual species, even on recycled rows.
        holder.emoji.setContentDescription(labelForSpecies(pet.getSpecies()));
        holder.info.setText(infoForPet(pet));
        holder.status.setText(labelForStatus(pet.getStatus()));
        holder.status.setTextColor(Color.parseColor(colorForStatus(pet.getStatus())));

        return convertView;
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

    /** Accessibility label for the species emoji. */
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

    private String infoForPet(Pet pet) {
        String info = pet.getName()
                + " • " + pet.getSpecies()
                + " • " + pet.getAge() + " ans";
        String requestedBy = pet.getRequestedBy();
        if (requestedBy != null && !requestedBy.isEmpty()) {
            info += "\nDemandé par " + requestedBy;
        }
        return info;
    }

    private String labelForStatus(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "AVAILABLE":
                return "Disponible";
            case "PENDING":
                return "En attente";
            case "ADOPTED":
                return "Adopté";
            default:
                return status;
        }
    }

    private String colorForStatus(String status) {
        if ("PENDING".equals(status)) {
            return "#FF9800";
        }
        if ("ADOPTED".equals(status)) {
            return "#9E9E9E";
        }
        return "#4CAF50"; // AVAILABLE and any unknown value default to green.
    }

    static class ViewHolder {
        TextView emoji;
        TextView info;
        TextView status;
    }
}

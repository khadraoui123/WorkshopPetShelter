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

        Context context = getContext();
        // Emoji glyphs, accessibility labels, info line and status all come from resources.
        holder.emoji.setText(PetLabels.emojiForSpecies(context, pet.getSpecies()));
        holder.emoji.setContentDescription(
                PetLabels.labelForSpecies(context, pet.getSpecies()));
        holder.info.setText(PetLabels.infoForPet(context, pet));
        holder.status.setText(PetLabels.labelForStatus(context, pet.getStatus()));
        holder.status.setTextColor(
                Color.parseColor(PetLabels.colorForStatus(pet.getStatus())));

        return convertView;
    }

    static class ViewHolder {
        TextView emoji;
        TextView info;
        TextView status;
    }
}

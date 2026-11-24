// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserAdapter.java
package com.example.workshoppetshelter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/** Custom ArrayAdapter: binds one UserProfile to one row_user.xml view. */
public class UserAdapter extends ArrayAdapter<UserProfile> {

    public UserAdapter(Context context, List<UserProfile> users) {
        // 0 = no built-in Android resource id; we inflate our own row layout in getView().
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_user, parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.tvUserName);
            holder.email = convertView.findViewById(R.id.tvUserEmail);
            holder.role = convertView.findViewById(R.id.tvUserRole);
            convertView.setTag(holder);
        } else {
            // Reuse: avoid inflating XML and finding views again while scrolling.
            holder = (ViewHolder) convertView.getTag();
        }

        UserProfile user = getItem(position);
        if (user == null) {
            return convertView;
        }

        holder.name.setText(user.getFullName());
        holder.email.setText(user.getEmail());

        Role role = user.getRole();
        holder.role.setText(role == null ? "" : role.name());
        holder.role.setTextColor(Color.parseColor(
                role == Role.ADMIN ? "#D32F2F" : "#1976D2"));

        return convertView;
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        TextView role;
    }
}

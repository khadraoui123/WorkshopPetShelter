// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserAdapter.java
package com.example.workshoppetshelter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
            holder.initial = convertView.findViewById(R.id.tvInitial);
            holder.name = convertView.findViewById(R.id.tvUserName);
            holder.email = convertView.findViewById(R.id.tvUserEmail);
            holder.role = convertView.findViewById(R.id.tvUserRole);
            convertView.setTag(holder);
        } else {
            // Reuse: avoid inflating XML and finding views again while scrolling.
            holder = (ViewHolder) convertView.getTag();
        }

        // The row root is clickable for its ripple. Forward its click to the ListView so
        // the existing setOnItemClickListener() keeps working (AOSP skips performItemClick
        // when the child declares android:focusable="true").
        bindRowClick(convertView, parent, position);

        UserProfile user = getItem(position);
        if (user == null) {
            holder.initial.setText("?");
            holder.name.setText("");
            holder.email.setText("");
            holder.role.setText("");
            return convertView;
        }

        String name = user.getFullName();
        String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
        holder.initial.setText(initial);

        holder.name.setText(name);
        holder.email.setText(user.getEmail());

        Role role = user.getRole();
        holder.role.setText(role == null
                ? getContext().getString(R.string.status_unknown)
                : getContext().getString(role == Role.ADMIN
                        ? R.string.role_admin
                        : R.string.role_user));
        holder.role.setTextColor(Color.parseColor(
                role == Role.ADMIN ? "#D32F2F" : "#1976D2"));

        return convertView;
    }

    /** Keeps MaterialCardView ripple + ListView item clicks working together. */
    private void bindRowClick(View row, ViewGroup parent, int position) {
        row.setOnClickListener(v -> {
            if (v.isEnabled() && parent instanceof AdapterView<?>) {
                ((AdapterView<?>) parent).performItemClick(v, position, getItemId(position));
            }
        });
        row.setEnabled(true);
    }

    static class ViewHolder {
        TextView initial;
        TextView name;
        TextView email;
        TextView role;
    }
}

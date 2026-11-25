// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\SessionManager.java
package com.example.workshoppetshelter;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Session management (Remember Me).
 *
 * This class owns ONE dedicated SharedPreferences file: "session_prefs".
 * It never touches "theme_prefs", so logging out cannot reset the user's
 * dark/light mode choice (Single Responsibility Principle).
 *
 * Authentication still happens in UserDbHelper.login(). This class only remembers
 * the result of a successful authentication so SplashActivity can route directly
 * to the right dashboard on the next app launch.
 */
public class SessionManager {

    private static final String PREFS_NAME = "session_prefs";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        // Application context avoids leaking an Activity through this long-lived helper.
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Persists a successful login. apply() writes asynchronously but safely. */
    public void saveSession(long userId, String userName, String role) {
        prefs.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName == null ? "" : userName)
                .putString(KEY_USER_ROLE, role)
                .apply();
    }

    /** @return the stored user id, or -1 when no session exists. */
    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    /** @return the stored display name, or null when it was never saved. */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    /** @return "ADMIN", "USER", or null when the session is absent/corrupted. */
    public String getRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }

    /**
     * A session is valid only when BOTH an id and a known role exist.
     * Unknown tokens are treated as logged out (deny by default).
     */
    public boolean isLoggedIn() {
        String role = getRole();
        return getUserId() != -1L
                && ("ADMIN".equals(role) || "USER".equals(role));
    }

    /** Clears ONLY this session file; theme_prefs is a separate file and is untouched. */
    public void logout() {
        prefs.edit().clear().apply();
    }
}

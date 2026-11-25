// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\LocaleHelper.java
package com.example.workshoppetshelter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * In-app language management for French (default), Arabic (RTL) and English.
 *
 * The chosen code is stored in its own SharedPreferences file so it never
 * interferes with "theme_prefs" (dark/light) or "session_prefs" (Remember Me).
 * Every Activity calls {@link #applySavedLocale(Context)} before super.onCreate()
 * so the language also survives a process restart.
 */
public final class LocaleHelper {

    public static final String PREFS_NAME = "language_prefs";
    public static final String KEY_LANGUAGE = "app_language";
    public static final String DEFAULT_LANGUAGE = "fr";

    private LocaleHelper() {
        // Utility class: no instances.
    }

    /** Restores the persisted language at the very start of every Activity. */
    public static void applySavedLocale(Context context) {
        applyLocale(context, getSavedLanguage(context));
    }

    /** @return one of "fr", "ar", "en"; unknown/corrupt values fall back to French. */
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return normalize(prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE));
    }

    /** Persists the choice, applies it immediately, then rebuilds the current UI. */
    public static void setLocale(Activity activity, String languageCode) {
        String language = normalize(languageCode);
        saveLanguage(activity, language);
        applyLocale(activity, language);
        activity.recreate();
    }

    private static void saveLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, normalize(languageCode)).apply();
    }

    /**
     * Applies the locale to the process and to the current Resources.
     * We copy the existing Configuration instead of using new Configuration()
     * so font scale and night mode settings are preserved.
     */
    @SuppressWarnings("deprecation")
    public static void applyLocale(Context context, String languageCode) {
        String language = normalize(languageCode);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        context.getResources().updateConfiguration(
                configuration, context.getResources().getDisplayMetrics());

        Context appContext = context.getApplicationContext();
        if (appContext != null && appContext != context) {
            Configuration appConfiguration =
                    new Configuration(appContext.getResources().getConfiguration());
            appConfiguration.setLocale(locale);
            appConfiguration.setLayoutDirection(locale);
            appContext.getResources().updateConfiguration(
                    appConfiguration, appContext.getResources().getDisplayMetrics());
        }
    }

    private static String normalize(String languageCode) {
        if (languageCode == null) {
            return DEFAULT_LANGUAGE;
        }
        String code = languageCode.trim().toLowerCase(Locale.ROOT);
        if ("ar".equals(code)) {
            return "ar";
        }
        if ("en".equals(code)) {
            return "en";
        }
        return DEFAULT_LANGUAGE;
    }
}

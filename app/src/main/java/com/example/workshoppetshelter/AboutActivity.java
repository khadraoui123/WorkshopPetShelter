// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\AboutActivity.java
package com.example.workshoppetshelter;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * About screen: branding, dynamic version, and two SYSTEM collaborations.
 *
 * This Activity never implements dialing or browsing itself. It asks the Android
 * platform to find an app that can handle a tel: or https: URI (implicit intents).
 * That is how Android apps collaborate: the user keeps their preferred browser
 * and phone app, and we inherit their features and security policies for free.
 */
public class AboutActivity extends AppCompatActivity {

    private static final String PHONE_URI = "tel:+21612345678";
    private static final String WEBSITE_URI = "https://www.google.com";

    private TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        tvVersion = findViewById(R.id.tvVersion);
        loadVersion();

        MaterialButton btnContact = findViewById(R.id.btnContact);
        MaterialButton btnWebsite = findViewById(R.id.btnWebsite);
        MaterialButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnContact.setOnClickListener(v -> openDialer());
        btnWebsite.setOnClickListener(v -> openWebsite());
    }

    /**
     * Single source of truth: versionName lives in app/build.gradle. Reading it back
     * from the installed APK means the UI can never drift away from the built version.
     */
    @SuppressWarnings("deprecation")
    private void loadVersion() {
        try {
            String version = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            // Defensive: versionName can be null/empty if a build variant omits it.
            tvVersion.setText(version == null || version.isEmpty()
                    ? getString(R.string.version_fallback)
                    : getString(R.string.version_format, version));
        } catch (PackageManager.NameNotFoundException e) {
            // Should be impossible for our own package, but never crash the About screen.
            tvVersion.setText(getString(R.string.version_fallback));
        }
    }

    /**
     * ACTION_DIAL opens the dialer with the number pre-filled but does NOT place a call.
     * Unlike ACTION_CALL it needs no CALL_PHONE permission and works on every device.
     */
    private void openDialer() {
        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse(PHONE_URI));
        try {
            startActivity(dial);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.error_no_dialer), false);
        }
    }

    /** ACTION_VIEW lets the system pick the user's preferred browser for an https: URI. */
    private void openWebsite() {
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URI));
        try {
            startActivity(web);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.error_no_browser), false);
        }
    }

    /** Same green/red Toast style as the other screens in this app. */
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

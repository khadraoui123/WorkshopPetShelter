// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\CertificateActivity.java
package com.example.workshoppetshelter;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Smart Features: the adoption certificate.
 *
 * This screen is a celebration, not a workflow: it receives names through explicit
 * internal extras, renders a fixed "paper" design, and lets the OS share the moment.
 * Missing extras degrade to the localized empty marker instead of crashing.
 */
public class CertificateActivity extends AppCompatActivity {

    private String petName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate);

        String adopterName = getIntent().getStringExtra("user_name");
        petName = getIntent().getStringExtra("pet_name");

        adopterName = (adopterName == null || adopterName.trim().isEmpty())
                ? getString(R.string.empty_value)
                : adopterName;
        petName = (petName == null || petName.trim().isEmpty())
                ? getString(R.string.empty_value)
                : petName;

        String dateText = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(new Date());

        TextView tvAdopterName = findViewById(R.id.tvAdopterName);
        TextView tvPetName = findViewById(R.id.tvPetName);
        TextView tvDate = findViewById(R.id.tvDate);
        MaterialButton btnShareCert = findViewById(R.id.btnShareCert);

        tvAdopterName.setText(adopterName);
        tvPetName.setText(petName);
        tvDate.setText(getString(R.string.certificate_date_format, dateText));

        btnShareCert.setOnClickListener(v -> shareCertificate());
    }

    /** Native ACTION_SEND + chooser: the user shares through their own apps. */
    private void shareCertificate() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.certificate_share_format, petName));

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_no_share_app, Toast.LENGTH_SHORT).show();
        }
    }
}

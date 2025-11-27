package com.example.medicremainder1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity displayed when the user taps notification to confirm Taken or Not taken.
 */
public class ConfirmationActivity extends AppCompatActivity {
    private int scheduleId;
    private int medId;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        db = new DatabaseHelper(this);

        scheduleId = getIntent().getIntExtra("schedule_id", -1);
        medId = getIntent().getIntExtra("med_id", -1);

        TextView txt = findViewById(R.id.confirmText);
        Button btnTaken = findViewById(R.id.btnTaken);
        Button btnNot = findViewById(R.id.btnNotTaken);

        String medName = (medId == -1) ? "Medicine" : db.getMedicineById(medId).name;
        txt.setText("Did you take " + medName + "?");

        btnTaken.setOnClickListener(v -> {
            db.markTaken(scheduleId);
            finish();
        });

        btnNot.setOnClickListener(v -> {
            db.markMissed(scheduleId);
            finish();
        });
    }
}

package com.example.medicremainder1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * ConfirmationActivity - user confirms Taken or Not taken for a schedule entry
 * Expects extra: "schedule_id" (int)
 */
public class ConfirmationActivity extends AppCompatActivity {

    private int scheduleId = -1;
    private DatabaseHelper db;
    private TextView confirmText;
    private Button btnTaken, btnNot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        db = new DatabaseHelper(this);
        confirmText = findViewById(R.id.confirmText);
        btnTaken = findViewById(R.id.btnTaken);
        btnNot = findViewById(R.id.btnNotTaken);

        scheduleId = getIntent().getIntExtra("schedule_id", -1);
        if (scheduleId == -1) {
            finish();
            return;
        }

        DatabaseHelper.ScheduleEntry s = db.getScheduleEntryById(scheduleId);
        String medName = "Medicine";
        if (s != null) {
            DatabaseHelper.MedicineDef md = db.getMedicineById(s.medId);
            if (md != null) medName = md.name;
            confirmText.setText("Did you take " + medName + " at " + s.time + " ?");
        }

        // ----------------------------
        // YES -> mark taken + stop alarm
        // ----------------------------
        btnTaken.setOnClickListener(v -> {

            // STOP ALARM SOUND
            try {
                if (AlarmReceiver.player != null) {
                    AlarmReceiver.player.stop();
                    AlarmReceiver.player.release();
                    AlarmReceiver.player = null;
                }
            } catch (Exception ignored) {}

            db.markTaken(scheduleId);
            finish();
        });

        // ----------------------------------
        // NO -> mark missed + stop alarm
        // ----------------------------------
        btnNot.setOnClickListener(v -> {

            // STOP ALARM SOUND
            try {
                if (AlarmReceiver.player != null) {
                    AlarmReceiver.player.stop();
                    AlarmReceiver.player.release();
                    AlarmReceiver.player = null;
                }
            } catch (Exception ignored) {}

            db.markMissed(scheduleId);
            finish();
        });
    }
}

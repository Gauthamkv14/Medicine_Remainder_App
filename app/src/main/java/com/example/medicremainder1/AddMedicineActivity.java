package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddMedicineActivity extends AppCompatActivity {

    EditText nameEt;
    TextView timesTv;
    Button addTimeBtn, saveBtn;
    Spinner repeatSpinner, foodSpinner, severitySpinner;
    EditText intervalEt;
    CheckBox[] weekdayChecks;
    DatabaseHelper dbHelper;
    List<String> times = new ArrayList<>(); // "08:00"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        dbHelper = new DatabaseHelper(this);

        nameEt = findViewById(R.id.medicineName);
        timesTv = findViewById(R.id.timesText);
        addTimeBtn = findViewById(R.id.addTimeBtn);
        saveBtn = findViewById(R.id.saveButton);
        repeatSpinner = findViewById(R.id.repeatSpinner);
        intervalEt = findViewById(R.id.intervalEdit);
        foodSpinner = findViewById(R.id.foodSpinner);
        severitySpinner = findViewById(R.id.severitySpinner);

        // simple weekday checkboxes (IDs: cbMon..cbSun)
        weekdayChecks = new CheckBox[] {
                findViewById(R.id.cbMon),
                findViewById(R.id.cbTue),
                findViewById(R.id.cbWed),
                findViewById(R.id.cbThu),
                findViewById(R.id.cbFri),
                findViewById(R.id.cbSat),
                findViewById(R.id.cbSun)
        };

        addTimeBtn.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String hhmm = String.format("%02d:%02d", hourOfDay, minute);
                times.add(hhmm);
                refreshTimesText();
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        saveBtn.setOnClickListener(v -> onSave());
    }

    private void refreshTimesText() {
        timesTv.setText(String.join(", ", times));
    }

    private void onSave() {
        String name = nameEt.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter medicine name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (times.isEmpty()) {
            Toast.makeText(this, "Add at least one time", Toast.LENGTH_SHORT).show();
            return;
        }
        String timesCsv = String.join(",", times);
        String repeatType = repeatSpinner.getSelectedItem().toString(); // DAILY/EVERY_X/WEEKLY
        int repeatInterval = 1;
        try { repeatInterval = Integer.parseInt(intervalEt.getText().toString()); } catch (Exception e) { repeatInterval = 1; }
        // compute daysBitmap
        int daysBitmap = 0;
        for (int i = 0; i < weekdayChecks.length; i++) {
            if (weekdayChecks[i].isChecked()) daysBitmap |= (1 << i);
        }
        int durationDays = 7; // fixed weekly default; you can expose this field too
        String food = foodSpinner.getSelectedItem().toString(); // BEFORE / AFTER / NONE
        String severity = severitySpinner.getSelectedItem().toString();

        long medId = dbHelper.addMedicine(name, timesCsv, repeatType, repeatInterval, daysBitmap, durationDays, food, severity);

        // schedule alarms for newly created schedule entries
        schedulePendingAlarms();

        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void schedulePendingAlarms() {
        DatabaseHelper db = new DatabaseHelper(this);
        List<DatabaseHelper.ScheduleEntry> pending = db.getAllPendingEntries();
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        for (DatabaseHelper.ScheduleEntry s : pending) {
            Intent i = new Intent(this, AlarmReceiver.class);
            i.putExtra("schedule_id", s.id);
            i.putExtra("med_id", s.medId);
            i.putExtra("med_name", db.getMedicineById(s.medId).name);
            i.putExtra("med_time", s.time);

            // --- Start of Fix ---

            PendingIntent pi = PendingIntent.getBroadcast(this, s.id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

// Check for exact alarm permission before scheduling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.timestamp, pi);
                } else {
                    Toast.makeText(this, "Cannot set reminder. Please grant alarm permission in settings.", Toast.LENGTH_LONG).show();
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.timestamp, pi);
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.timestamp, pi);
        }
    }
}

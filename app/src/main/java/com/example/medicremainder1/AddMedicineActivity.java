package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * AddMedicineActivity
 * Supports:
 *  - Multiple times per day
 *  - Weekly selection (Mon–Sun)
 *  - Food timing
 *  - Generates 7-day rolling schedule through DatabaseHelper
 *  - Schedules alarms for all future pending entries
 */
public class AddMedicineActivity extends AppCompatActivity {

    private EditText nameEt;
    private TextView timesText;
    private Button addTimeBtn, saveBtn;
    private Spinner foodSpinner;
    private CheckBox[] weekdayChecks;

    private DatabaseHelper db;
    private final List<String> times = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        db = new DatabaseHelper(this);

        nameEt = findViewById(R.id.medicineName);
        timesText = findViewById(R.id.timesText);
        addTimeBtn = findViewById(R.id.addTimeBtn);
        saveBtn = findViewById(R.id.saveButton);
        foodSpinner = findViewById(R.id.foodSpinner);

        weekdayChecks = new CheckBox[]{
                findViewById(R.id.cbMon),
                findViewById(R.id.cbTue),
                findViewById(R.id.cbWed),
                findViewById(R.id.cbThu),
                findViewById(R.id.cbFri),
                findViewById(R.id.cbSat),
                findViewById(R.id.cbSun)
        };

        // Food timing dropdown
        ArrayAdapter<CharSequence> foodAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.food_options,
                android.R.layout.simple_spinner_item
        );
        foodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        foodSpinner.setAdapter(foodAdapter);

        addTimeBtn.setOnClickListener(v -> openTimePicker());
        saveBtn.setOnClickListener(v -> onSave());
    }

    /** Opens a TimePicker popup */
    private void openTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String hhmm = String.format("%02d:%02d", hourOfDay, minute);
            times.add(hhmm);
            refreshTimesText();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    /** Refreshes the times label */
    private void refreshTimesText() {
        if (times.isEmpty()) timesText.setText("Times: none");
        else timesText.setText("Times: " + String.join(", ", times));
    }

    /** Save button handler */
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

        // Build days bitmap
        int daysBitmap = 0;
        for (int i = 0; i < weekdayChecks.length; i++) {
            if (weekdayChecks[i].isChecked()) {
                daysBitmap |= (1 << i);
            }
        }
        // If nothing chosen → select all days
        if (daysBitmap == 0) daysBitmap = 0x7F;

        String foodTiming = foodSpinner.getSelectedItem().toString();

        long rowId = db.addMedicine(name, timesCsv, daysBitmap, foodTiming);
        if (rowId > 0) {

            // ----------- SCHEDULE ALL PENDING ALARMS -----------
            scheduleAllPendingAlarms();

            Toast.makeText(this, "Medicine saved", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error saving medicine", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Schedules alarms for all future PENDING entries
     * using the 7-day rolling schedule table
     */
    private void scheduleAllPendingAlarms() {
        List<DatabaseHelper.ScheduleEntry> pending = db.getAllPendingEntries();
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (pending == null || pending.isEmpty()) return;

        for (DatabaseHelper.ScheduleEntry s : pending) {

            Intent i = new Intent(this, AlarmReceiver.class);
            i.putExtra("schedule_id", s.id);
            i.putExtra("med_id", s.medId);

            DatabaseHelper.MedicineDef md = db.getMedicineById(s.medId);
            i.putExtra("med_name", md != null ? md.name : "Medicine");
            i.putExtra("med_time", s.time);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    s.id,
                    i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (am != null) {
                am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        s.timestamp,
                        pi
                );
            }
        }
    }
}

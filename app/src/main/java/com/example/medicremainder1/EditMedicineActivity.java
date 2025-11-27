package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * EditMedicineActivity:
 * - loads medicine by med_id
 * - pre-fills times, days, food timing
 * - allows editing all fields
 * - updates DB and regenerates schedule for next 7 days
 */
public class EditMedicineActivity extends AppCompatActivity {

    EditText nameEt;
    TextView timesText;
    Button addTimeBtn, updateBtn;
    Spinner foodSpinner;
    CheckBox[] weekdayChecks;
    DatabaseHelper db;
    List<String> times = new ArrayList<>();
    int medId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_medicine);

        db = new DatabaseHelper(this);

        nameEt = findViewById(R.id.editMedicineName);
        timesText = findViewById(R.id.editTimesText);
        addTimeBtn = findViewById(R.id.editAddTimeBtn);
        updateBtn = findViewById(R.id.updateButton);
        foodSpinner = findViewById(R.id.editFoodSpinner);

        weekdayChecks = new CheckBox[] {
                findViewById(R.id.editCbMon),
                findViewById(R.id.editCbTue),
                findViewById(R.id.editCbWed),
                findViewById(R.id.editCbThu),
                findViewById(R.id.editCbFri),
                findViewById(R.id.editCbSat),
                findViewById(R.id.editCbSun)
        };

        ArrayAdapter<CharSequence> foodAdapter = ArrayAdapter.createFromResource(this, R.array.food_options, android.R.layout.simple_spinner_item);
        foodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        foodSpinner.setAdapter(foodAdapter);

        medId = getIntent().getIntExtra("med_id", -1);
        if (medId == -1) {
            finish();
            return;
        }

        loadMedicine();

        addTimeBtn.setOnClickListener(v -> openTimePicker());
        updateBtn.setOnClickListener(v -> onUpdate());
    }

    private void loadMedicine() {
        DatabaseHelper.MedicineDef m = db.getMedicineById(medId);
        if (m == null) return;
        nameEt.setText(m.name);
        times.clear();
        if (m.timesCsv != null && !m.timesCsv.isEmpty()) {
            for (String t : m.timesCsv.split(",")) times.add(t.trim());
        }
        refreshTimesText();

        // days bitmap -> populate checkboxes
        int bitmap = m.daysBitmap;
        for (int i = 0; i < weekdayChecks.length; i++) {
            weekdayChecks[i].setChecked(((bitmap >> i) & 1) == 1);
        }

        // food spinner
        if (m.foodTiming != null) {
            String[] arr = getResources().getStringArray(R.array.food_options);
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].equalsIgnoreCase(m.foodTiming)) {
                    foodSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void openTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String hhmm = String.format("%02d:%02d", hourOfDay, minute);
            times.add(hhmm);
            refreshTimesText();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private void refreshTimesText() {
        if (times.isEmpty()) timesText.setText("Times: none");
        else timesText.setText("Times: " + String.join(", ", times));
    }

    private void onUpdate() {
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

        int daysBitmap = 0;
        for (int i = 0; i < weekdayChecks.length; i++) if (weekdayChecks[i].isChecked()) daysBitmap |= (1 << i);
        if (daysBitmap == 0) daysBitmap = 0x7F;

        String food = foodSpinner.getSelectedItem().toString();

        db.updateMedicine(medId, name, timesCsv, daysBitmap, food);

        // cancel existing alarms for next 7 days and reschedule fresh ones
        cancelPendingAlarmsForNext7Days();
        scheduleAllPendingAlarms();

        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void scheduleAllPendingAlarms() {
        List<DatabaseHelper.ScheduleEntry> pending = db.getAllPendingEntries();
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        for (DatabaseHelper.ScheduleEntry s : pending) {
            Intent i = new Intent(this, AlarmReceiver.class);
            i.putExtra("schedule_id", s.id);
            i.putExtra("med_id", s.medId);
            DatabaseHelper.MedicineDef md = db.getMedicineById(s.medId);
            i.putExtra("med_name", md != null ? md.name : "Medicine");
            i.putExtra("med_time", s.time);

            PendingIntent pi = PendingIntent.getBroadcast(this, s.id, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.timestamp, pi);
        }
    }

    /**
     * Cancel any pending PendingIntents for schedule entries in next 7 days
     */
    private void cancelPendingAlarmsForNext7Days() {
        List<DatabaseHelper.ScheduleEntry> pending = db.getAllPendingEntries();
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        for (DatabaseHelper.ScheduleEntry s : pending) {
            PendingIntent existing = PendingIntent.getBroadcast(this, s.id, new Intent(this, AlarmReceiver.class),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (existing != null && am != null) {
                am.cancel(existing);
                existing.cancel();
            }
        }
    }
}

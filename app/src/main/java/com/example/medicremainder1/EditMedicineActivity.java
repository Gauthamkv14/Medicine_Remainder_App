package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.PendingIntent;

import java.util.ArrayList;
import java.util.List;

public class EditMedicineActivity extends AppCompatActivity {

    EditText nameEt, intervalEt;
    TextView timesTv;
    Button addTimeBtn, updateBtn;
    Spinner repeatSpinner, foodSpinner, severitySpinner;
    CheckBox[] weekdayChecks;
    DatabaseHelper db;
    int medId;
    List<String> times = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_medicine);
        db = new DatabaseHelper(this);

        nameEt = findViewById(R.id.editMedicineName);
        timesTv = findViewById(R.id.editTimesText);
        addTimeBtn = findViewById(R.id.addTimeBtnEdit);
        updateBtn = findViewById(R.id.updateButton);
        repeatSpinner = findViewById(R.id.repeatSpinnerEdit);
        intervalEt = findViewById(R.id.intervalEditEdit);
        foodSpinner = findViewById(R.id.foodSpinnerEdit);
        severitySpinner = findViewById(R.id.severitySpinnerEdit);

        weekdayChecks = new CheckBox[] {
                findViewById(R.id.cbMonEdit),
                findViewById(R.id.cbTueEdit),
                findViewById(R.id.cbWedEdit),
                findViewById(R.id.cbThuEdit),
                findViewById(R.id.cbFriEdit),
                findViewById(R.id.cbSatEdit),
                findViewById(R.id.cbSunEdit)
        };

        medId = getIntent().getIntExtra("med_id", -1);
        if (medId == -1) finish();

        DatabaseHelper.MedicineDef m = db.getMedicineById(medId);
        if (m != null) {
            nameEt.setText(m.name);
            if (m.timesCsv != null && !m.timesCsv.isEmpty()) {
                times.clear();
                for (String t : m.timesCsv.split(",")) times.add(t);
                timesTv.setText(String.join(", ", times));
            }
            // set other spinners/checkboxes - omitted here for brevity: you should set selection based on m fields
        }

        addTimeBtn.setOnClickListener(v -> {
            // same as AddMedicine: open TimePicker and add
            // ... implementation omitted for brevity; reuse AddMedicineActivity implementation
        });

        updateBtn.setOnClickListener(v -> {
            String name = nameEt.getText().toString();
            String timesCsv = String.join(",", times);
            String repeatType = repeatSpinner.getSelectedItem().toString();
            int repeatInterval = 1;
            try { repeatInterval = Integer.parseInt(intervalEt.getText().toString()); } catch (Exception e) { }
            int daysBitmap = 0;
            for (int i = 0; i < weekdayChecks.length; i++) if (weekdayChecks[i].isChecked()) daysBitmap |= (1 << i);
            int durationDays = 7;
            String food = foodSpinner.getSelectedItem().toString();
            String severity = severitySpinner.getSelectedItem().toString();

            db.updateMedicine(medId, name, timesCsv, repeatType, repeatInterval, daysBitmap, durationDays, food, severity);

            // reschedule: cancel existing future alarms and schedule newly generated ones
            schedulePendingAlarms();

            finish();
        });
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

            PendingIntent pi = PendingIntent.getBroadcast(this, s.id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.timestamp, pi);
        }
    }
}

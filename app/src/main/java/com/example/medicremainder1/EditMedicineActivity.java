package com.example.medicremainder1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class EditMedicineActivity extends AppCompatActivity {
    private EditText nameEdit, timeEdit;
    private Button updateBtn;
    private DatabaseHelper dbHelper;
    private int medicineId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_medicine);

        dbHelper = new DatabaseHelper(this);
        nameEdit = findViewById(R.id.editMedicineName);
        timeEdit = findViewById(R.id.editMedicineTime);
        updateBtn = findViewById(R.id.updateButton);

        // Get medicine info from intent
        medicineId = getIntent().getIntExtra("id", -1);
        nameEdit.setText(getIntent().getStringExtra("name"));
        timeEdit.setText(getIntent().getStringExtra("time"));

        timeEdit.setOnClickListener(v -> {
            // Parse current time
            String currentTime = timeEdit.getText().toString();
            int hour = 12;
            int minute = 0;

            try {
                String[] timeParts = currentTime.split(":");
                hour = Integer.parseInt(timeParts[0].trim());
                minute = Integer.parseInt(timeParts[1].trim());
            } catch (Exception e) {
                Calendar cal = Calendar.getInstance();
                hour = cal.get(Calendar.HOUR_OF_DAY);
                minute = cal.get(Calendar.MINUTE);
            }

            new TimePickerDialog(this, (TimePicker view, int h, int m) -> {
                timeEdit.setText(String.format("%02d:%02d", h, m));
            }, hour, minute, true).show();
        });

        updateBtn.setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            String time = timeEdit.getText().toString().trim();

            if (name.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            updateMedicine(name, time);

            // Cancel old alarm and schedule new one
            AlarmScheduler.cancelAlarm(this, medicineId);
            AlarmScheduler.scheduleAlarm(this, medicineId, name, time);

            Toast.makeText(this, "Medicine updated!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateMedicine(String name, String time) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("time", time);

        db.update("medicines", values, "id=?", new String[]{String.valueOf(medicineId)});
    }
}
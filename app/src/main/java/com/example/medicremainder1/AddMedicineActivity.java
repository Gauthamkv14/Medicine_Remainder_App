package com.example.medicremainder1;

import androidx.appcompat.app.AppCompatActivity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class AddMedicineActivity extends AppCompatActivity {
    private EditText nameInput, timeInput;
    private Button saveBtn;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        dbHelper = new DatabaseHelper(this);
        nameInput = findViewById(R.id.medicineName);
        timeInput = findViewById(R.id.medicineTime);
        saveBtn = findViewById(R.id.saveButton);

        timeInput.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);

            new TimePickerDialog(this, (view, h, m) -> {
                timeInput.setText(String.format("%02d:%02d", h, m));
            }, hour, minute, true).show();
        });

        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String time = timeInput.getText().toString().trim();

            if (name.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            long id = dbHelper.addMedicine(name, time);

            if (id > 0) {
                // Schedule alarm
                AlarmScheduler.scheduleAlarm(this, (int)id, name, time);
                Toast.makeText(this, "Medicine reminder added!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to add medicine", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
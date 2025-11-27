package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicineSimpleAdapter adapter;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);

        // ✔ FIRST: request exact alarm permission
        requestExactAlarmPermission();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineSimpleAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddMedicineActivity.class))
        );

        View btnTT = findViewById(R.id.btnTimetable);
        if (btnTT != null)
            btnTT.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, TimetableActivity.class))
            );

        // regenerate schedules AFTER permission check
        db.regenerateAllSchedulesForNext7Days();

        // schedule alarms ONLY if permission is granted
        scheduleAllPendingAlarms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        List<DatabaseHelper.MedicineDef> meds = db.getAllMedicines();
        adapter.setList(meds);
    }

    private void scheduleAllPendingAlarms() {

        // ✔ Skip scheduling if permission is NOT granted
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                return;  // <-- DO NOT CRASH
            }
        }

        List<DatabaseHelper.ScheduleEntry> pending = db.getAllPendingEntries();
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

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

    private void requestExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }
}

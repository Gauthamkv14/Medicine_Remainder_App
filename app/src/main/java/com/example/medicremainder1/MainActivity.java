package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

        createNotificationChannel();   // ⭐ VERY IMPORTANT
        requestExactAlarmPermission();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineSimpleAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddMedicineActivity.class))
        );

        findViewById(R.id.btnTimetable).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TimetableActivity.class))
        );

        db.regenerateAllSchedulesForNext7Days();
        scheduleAllPendingAlarms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        adapter.setList(db.getAllMedicines());
    }

    // --- CREATE NOTIFICATION CHANNEL (SOUND + VIBRATE ENABLED) ---
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    AlarmReceiver.CHANNEL_ID,
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 600, 400, 600, 400, 600});
            channel.setSound(alarmSound, audioAttributes);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleAllPendingAlarms() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) return;
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

            am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    s.timestamp,
                    pi
            );
        }
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }
}

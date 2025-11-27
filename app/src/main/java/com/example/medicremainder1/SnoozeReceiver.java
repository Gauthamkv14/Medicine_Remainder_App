package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SnoozeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        int scheduleId = intent.getIntExtra("schedule_id", -1);
        int medId = intent.getIntExtra("med_id", -1);

        if (scheduleId == -1) return;

        DatabaseHelper db = new DatabaseHelper(context);
        DatabaseHelper.ScheduleEntry s = db.getScheduleEntryById(scheduleId);
        if (s == null) return;

        // LOOKUP THE MEDICINE NAME FROM DB
        DatabaseHelper.MedicineDef md = db.getMedicineById(s.medId);
        String medName = (md != null) ? md.name : "Medicine";

        Intent i = new Intent(context, AlarmReceiver.class);
        i.putExtra("schedule_id", scheduleId);
        i.putExtra("med_id", medId);
        i.putExtra("med_name", medName);
        i.putExtra("med_time", s.time);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                scheduleId,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long snoozeTime = 10 * 60 * 1000; // 10 minutes

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + snoozeTime,
                    pi
            );
        }
    }
}

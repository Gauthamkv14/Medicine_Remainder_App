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
        if (scheduleId == -1) return;
        DatabaseHelper db = new DatabaseHelper(context);
        DatabaseHelper.ScheduleEntry s = null;
        for (DatabaseHelper.ScheduleEntry se : db.getAllPendingEntries()) {
            if (se.id == scheduleId) { s = se; break; }
        }
        if (s == null) return;

        long newTs = System.currentTimeMillis() + (10L * 60L * 1000L); // 10 minutes
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        i.putExtra("schedule_id", s.id);
        i.putExtra("med_id", s.medId);
        i.putExtra("med_name", db.getMedicineById(s.medId).name);
        i.putExtra("med_time", s.time);

        PendingIntent pi = PendingIntent.getBroadcast(context, s.id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, newTs, pi);
    }
}

package com.example.medicremainder1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.util.Log;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            DatabaseHelper db = new DatabaseHelper(context);
            List<DatabaseHelper.ScheduleEntry> pending = db.getAllPendingEntries();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            for (DatabaseHelper.ScheduleEntry s : pending) {
                Intent i = new Intent(context, AlarmReceiver.class);
                i.putExtra("schedule_id", s.id);
                i.putExtra("med_id", s.medId);
                i.putExtra("med_time", s.time);
                i.putExtra("med_name", db.getMedicineById(s.medId).name);

                PendingIntent pi = PendingIntent.getBroadcast(context, s.id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.timestamp, pi);
            }
            Log.i(TAG, "Rescheduled " + pending.size() + " alarms after boot");
        }
    }
}

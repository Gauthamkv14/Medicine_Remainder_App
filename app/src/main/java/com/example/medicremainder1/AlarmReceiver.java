package com.example.medicremainder1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "med_channel";
    public static MediaPlayer player;

    @Override
    public void onReceive(Context context, Intent intent) {

        int scheduleId = intent.getIntExtra("schedule_id", -1);
        int medId = intent.getIntExtra("med_id", -1);
        String medName = intent.getStringExtra("med_name");
        String time = intent.getStringExtra("med_time");

        // -------------------------------
        // 1️⃣ START LONG-RINGING ALARM SOUND
        // -------------------------------
        try {
            if (player != null) {
                player.release();
            }

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            player = MediaPlayer.create(context, alarmSound);
            player.setLooping(true);
            player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // -------------------------------
        // 2️⃣ NOTIFICATION CHANNEL
        // -------------------------------
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Medicine Reminder",
                    NotificationManager.IMPORTANCE_HIGH
            );

            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 600, 400, 600, 400, 600});
            ch.setBypassDnd(true);

            nm.createNotificationChannel(ch);
        }

        // -------------------------------
        // ACTION: OPEN CONFIRMATION PAGE
        // -------------------------------
        Intent openIntent = new Intent(context, ConfirmationActivity.class);
        openIntent.putExtra("schedule_id", scheduleId);
        PendingIntent openPi = PendingIntent.getActivity(
                context,
                scheduleId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // -------------------------------
        // ACTION: SNOOZE (10 mins)
        // -------------------------------
        Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
        snoozeIntent.putExtra("schedule_id", scheduleId);
        snoozeIntent.putExtra("med_id", medId);
        snoozeIntent.putExtra("med_name", medName);
        snoozeIntent.putExtra("med_time", time);

        PendingIntent snoozePi = PendingIntent.getBroadcast(
                context,
                scheduleId + 100000,     // unique
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // -------------------------------
        // BUILD NOTIFICATION
        // -------------------------------
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle("Time to take: " + medName)
                .setContentText("Scheduled at " + time)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSound(null)
                .addAction(R.drawable.ic_snooze, "SNOOZE", snoozePi)
                .addAction(R.drawable.ic_done, "TAKEN", openPi)
                .setContentIntent(openPi)
                .setVibrate(new long[]{0, 700, 400, 700, 400, 700});

        nm.notify(scheduleId, builder.build());
    }
}

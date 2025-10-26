package com.example.medicremainder1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medicine_reminder_channel";
    private static final String CHANNEL_NAME = "Medicine Reminders";
    private static Ringtone ringtone;

    public static final String ACTION_SNOOZE = "com.example.medicremainder1.ACTION_SNOOZE";
    public static final String ACTION_DISMISS = "com.example.medicremainder1.ACTION_DISMISS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Handle snooze and dismiss actions
        if (ACTION_SNOOZE.equals(action)) {
            handleSnooze(context, intent);
            return;
        } else if (ACTION_DISMISS.equals(action)) {
            handleDismiss(context, intent);
            return;
        }

        // Regular alarm trigger
        String medicineName = intent.getStringExtra("medicine_name");
        int medicineId = intent.getIntExtra("medicine_id", 0);

        createNotificationChannel(context);
        showNotification(context, medicineName, medicineId);

        // Play LOUD alarm sound
        playAlarmSound(context);

        // Strong vibration pattern
        vibratePhone(context);
    }

    private void handleSnooze(Context context, Intent intent) {
        // Stop alarm sound
        stopAlarmSound();

        int medicineId = intent.getIntExtra("medicine_id", 0);
        String medicineName = intent.getStringExtra("medicine_name");

        // Cancel notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(medicineId);
        }

        // Reschedule for 5 minutes later
        AlarmScheduler.scheduleSnoozeAlarm(context, medicineId, medicineName, 5);
    }

    private void handleDismiss(Context context, Intent intent) {
        // Stop alarm sound
        stopAlarmSound();

        int medicineId = intent.getIntExtra("medicine_id", 0);

        // Cancel notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(medicineId);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Medicine reminder alarms");
            channel.enableVibration(true);
            channel.setSound(alarmSound, audioAttributes);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Context context, String medicineName, int medicineId) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Snooze action
        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra("medicine_id", medicineId);
        snoozeIntent.putExtra("medicine_name", medicineName);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId + 1000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Dismiss action
        Intent dismissIntent = new Intent(context, AlarmReceiver.class);
        dismissIntent.setAction(ACTION_DISMISS);
        dismissIntent.putExtra("medicine_id", medicineId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId + 2000,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ Medicine Reminder!")
                .setContentText("Time to take: " + medicineName)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("It's time to take your medicine: " + medicineName + "\n\nTap to open app"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 5min", snoozePendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(medicineId, builder.build());
        }
    }

    private void playAlarmSound(Context context) {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }

            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            ringtone = RingtoneManager.getRingtone(context, alarmUri);
            if (ringtone != null) {
                ringtone.play();

                // Stop after 30 seconds
                new android.os.Handler().postDelayed(() -> {
                    stopAlarmSound();
                }, 30000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void vibratePhone(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 500, 1000, 500, 1000, 500, 1000};

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    vibrator.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.example.medicremainder1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmScheduler {

    public static void scheduleAlarm(Context context, int medicineId, String medicineName, String time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Toast.makeText(context, "Cannot get Alarm Service", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0].trim());
            int minute = Integer.parseInt(timeParts[1].trim());

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Check for exact alarm permission before scheduling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    Toast.makeText(context, "Reminder set for " + time, Toast.LENGTH_SHORT).show();
                } else {
                    // Permission not granted. Guide user to settings.
                    Toast.makeText(context, "Permission needed to set reminders. Please enable it in settings.", Toast.LENGTH_LONG).show();
                    Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    context.startActivity(permissionIntent);
                }
            } else {
                // For older versions, the permission is granted at install time
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Toast.makeText(context, "Reminder set for " + time, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(context, "Invalid time format", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static void cancelAlarm(Context context, int medicineId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }

    public static void scheduleSnoozeAlarm(Context context, int medicineId, String medicineName, int minutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Toast.makeText(context, "Cannot get Alarm Service", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId + 5000, // Different ID for snooze
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000);

        // Check for exact alarm permission before scheduling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
                Toast.makeText(context, "Snoozed for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
            } else {
                // On snooze, we typically already have permission, but this is a safe fallback.
                Toast.makeText(context, "Cannot snooze. Alarm permission missing.", Toast.LENGTH_LONG).show();
            }
        } else {
            // For older versions
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
            Toast.makeText(context, "Snoozed for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
        }
    }
}

package com.example.medicremainder1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int scheduleId = intent.getIntExtra("schedule_id", -1);
        if (scheduleId == -1) return;

        DatabaseHelper db = new DatabaseHelper(context);

        // Only mark MISSED if status is still PENDING
        DatabaseHelper.ScheduleEntry s = db.getScheduleEntryById(scheduleId);
        if (s != null && "PENDING".equalsIgnoreCase(s.status)) {
            db.markMissed(scheduleId);
        }
    }
}

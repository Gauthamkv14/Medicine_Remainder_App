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

        // If schedule still PENDING, mark as MISSED
        // (we didn't implement a getter for single schedule entry above — simple strategy: call markMissed)
        db.markMissed(scheduleId);
    }
}

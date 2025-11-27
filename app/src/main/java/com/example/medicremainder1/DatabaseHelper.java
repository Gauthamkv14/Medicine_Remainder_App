package com.example.medicremainder1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * UPDATED DatabaseHelper:
 * - medicines table now holds repeat rules and times (times stored as comma-separated "HH:mm" strings)
 * - schedule_entries table stores one scheduled dose per date/time with status (PENDING/TAKEN/MISSED)
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DB_HELPER";

    private static final String DATABASE_NAME = "medic_remainder.db";
    private static final int DATABASE_VERSION = 3; // bump version if you already have an older DB

    // medicines table
    public static final String T_MED = "medicines";
    public static final String M_ID = "id";
    public static final String M_NAME = "name";
    public static final String M_TIMES = "times"; // comma-separated times "08:00,14:00,20:00"
    public static final String M_REPEAT_TYPE = "repeat_type"; // "DAILY","EVERY_X","WEEKLY"
    public static final String M_REPEAT_INTERVAL = "repeat_interval"; // integer (days)
    public static final String M_DAYS_BITMAP = "days_bitmap"; // for weekly: bitmask Mon=1<<0 ... Sun=1<<6
    public static final String M_DURATION_DAYS = "duration_days"; // how many days to create schedule for (default 7)
    public static final String M_FOOD = "food_timing"; // "BEFORE","AFTER","NONE"
    public static final String M_SEVERITY = "severity"; // "LOW","MEDIUM","HIGH"

    // schedule entries
    public static final String T_SCH = "schedule_entries";
    public static final String S_ID = "id";
    public static final String S_MED_ID = "medicine_id";
    public static final String S_DATE = "date_iso"; // yyyy-MM-dd
    public static final String S_TIME = "time"; // HH:mm
    public static final String S_TIMESTAMP = "time_in_millis"; // exact timestamp to schedule alarm
    public static final String S_STATUS = "status"; // "PENDING","TAKEN","MISSED"
    public static final String S_AUTO_MISS_TS = "auto_miss_ts"; // timestamp when auto miss will be executed (nullable)

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // medicines table
        String createMed = "CREATE TABLE " + T_MED + " (" +
                M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                M_NAME + " TEXT," +
                M_TIMES + " TEXT," +
                M_REPEAT_TYPE + " TEXT," +
                M_REPEAT_INTERVAL + " INTEGER DEFAULT 1," +
                M_DAYS_BITMAP + " INTEGER DEFAULT 0," +
                M_DURATION_DAYS + " INTEGER DEFAULT 7," +
                M_FOOD + " TEXT DEFAULT 'NONE'," +
                M_SEVERITY + " TEXT DEFAULT 'LOW'" +
                ")";
        db.execSQL(createMed);

        // schedule entries
        String createSch = "CREATE TABLE " + T_SCH + " (" +
                S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                S_MED_ID + " INTEGER," +
                S_DATE + " TEXT," +
                S_TIME + " TEXT," +
                S_TIMESTAMP + " INTEGER," +
                S_STATUS + " TEXT DEFAULT 'PENDING'," +
                S_AUTO_MISS_TS + " INTEGER," +
                "FOREIGN KEY(" + S_MED_ID + ") REFERENCES " + T_MED + "(" + M_ID + ")" +
                ")";
        db.execSQL(createSch);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // simple destructive migration for dev: you may want to write real migrations later
        Log.w(TAG, "Upgrading DB from " + oldVersion + " to " + newVersion + " - dropping tables");
        db.execSQL("DROP TABLE IF EXISTS " + T_SCH);
        db.execSQL("DROP TABLE IF EXISTS " + T_MED);
        onCreate(db);
    }

    // ------------ MEDICINES CRUD ----------------

    /**
     * Insert a medicine and returns its row id
     * timesCsv example: "08:00,14:00,20:00"
     * repeatType: "DAILY" | "EVERY_X" | "WEEKLY"
     * daysBitmap: used for WEEKLY (int bitmask Mon->1<<0 ... Sun->1<<6)
     */
    public long addMedicine(String name, String timesCsv, String repeatType, int repeatInterval,
                            int daysBitmap, int durationDays, String foodTiming, String severity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(M_NAME, name);
        v.put(M_TIMES, timesCsv);
        v.put(M_REPEAT_TYPE, repeatType);
        v.put(M_REPEAT_INTERVAL, repeatInterval);
        v.put(M_DAYS_BITMAP, daysBitmap);
        v.put(M_DURATION_DAYS, durationDays);
        v.put(M_FOOD, foodTiming);
        v.put(M_SEVERITY, severity);
        long id = db.insert(T_MED, null, v);

        // generate schedule entries for next durationDays
        if (id != -1) {
            generateScheduleForMedicine((int) id);
        }
        return id;
    }

    public void updateMedicine(int id, String name, String timesCsv, String repeatType, int repeatInterval,
                               int daysBitmap, int durationDays, String foodTiming, String severity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(M_NAME, name);
        v.put(M_TIMES, timesCsv);
        v.put(M_REPEAT_TYPE, repeatType);
        v.put(M_REPEAT_INTERVAL, repeatInterval);
        v.put(M_DAYS_BITMAP, daysBitmap);
        v.put(M_DURATION_DAYS, durationDays);
        v.put(M_FOOD, foodTiming);
        v.put(M_SEVERITY, severity);
        db.update(T_MED, v, M_ID + "=?", new String[]{String.valueOf(id)});

        // regenerate schedule for this med (simpler approach: remove future entries and recreate)
        deleteFutureScheduleForMed(id);
        generateScheduleForMedicine(id);
    }

    public void deleteMedicine(int medId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(T_MED, M_ID + "=?", new String[]{String.valueOf(medId)});
        db.delete(T_SCH, S_MED_ID + "=?", new String[]{String.valueOf(medId)});
    }

    public List<MedicineDef> getAllMedicines() {
        List<MedicineDef> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_MED, null, null, null, null, null, M_ID + " ASC");
        if (c.moveToFirst()) {
            do {
                MedicineDef m = new MedicineDef();
                m.id = c.getInt(c.getColumnIndexOrThrow(M_ID));
                m.name = c.getString(c.getColumnIndexOrThrow(M_NAME));
                m.timesCsv = c.getString(c.getColumnIndexOrThrow(M_TIMES));
                m.repeatType = c.getString(c.getColumnIndexOrThrow(M_REPEAT_TYPE));
                m.repeatInterval = c.getInt(c.getColumnIndexOrThrow(M_REPEAT_INTERVAL));
                m.daysBitmap = c.getInt(c.getColumnIndexOrThrow(M_DAYS_BITMAP));
                m.durationDays = c.getInt(c.getColumnIndexOrThrow(M_DURATION_DAYS));
                m.foodTiming = c.getString(c.getColumnIndexOrThrow(M_FOOD));
                m.severity = c.getString(c.getColumnIndexOrThrow(M_SEVERITY));
                list.add(m);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    // ------------ SCHEDULE MANAGEMENT ----------------

    /**
     * Generate schedule entries for the next durationDays for the given medicine.
     * This respects repeat_type: DAILY, EVERY_X, WEEKLY (daysBitmap).
     */
    public void generateScheduleForMedicine(int medId) {
        MedicineDef m = getMedicineById(medId);
        if (m == null) return;

        String[] times = m.timesCsv != null ? m.timesCsv.split(",") : new String[0];
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd");

        for (int d = 0; d < m.durationDays; d++) {
            // check repeat rule for date
            if (!shouldCreateForDate(m, cal, d)) {
                cal.add(Calendar.DATE, 1);
                continue;
            }

            String dateStr = isoDate.format(cal.getTime());
            for (String t : times) {
                t = t.trim();
                long timestamp = computeTimestampForDateTime(cal, t);
                insertScheduleEntry(medId, dateStr, t, timestamp);
            }
            cal.add(Calendar.DATE, 1);
        }
    }

    private boolean shouldCreateForDate(MedicineDef m, Calendar cal, int dayOffset) {
        // m.repeatType can be "DAILY", "EVERY_X", "WEEKLY"
        if ("DAILY".equalsIgnoreCase(m.repeatType)) return true;
        if ("EVERY_X".equalsIgnoreCase(m.repeatType)) {
            return (dayOffset % Math.max(1, m.repeatInterval)) == 0;
        }
        if ("WEEKLY".equalsIgnoreCase(m.repeatType)) {
            int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun ..7=Sat
            int idx = (dow + 5) % 7; // convert to Mon=0..Sun=6
            return ((m.daysBitmap >> idx) & 1) == 1;
        }
        return true;
    }

    private long computeTimestampForDateTime(Calendar dayCal, String hhmm) {
        try {
            String[] parts = hhmm.split(":");
            int hh = Integer.parseInt(parts[0]);
            int mm = Integer.parseInt(parts[1]);
            Calendar c = (Calendar) dayCal.clone();
            c.set(Calendar.HOUR_OF_DAY, hh);
            c.set(Calendar.MINUTE, mm);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            // if computed time is in past relative to now and day is today, move to future (optional)
            if (c.getTimeInMillis() < System.currentTimeMillis()) {
                // keep as-is - scheduling logic in app can decide to show immediate or skip
            }
            return c.getTimeInMillis();
        } catch (Exception ex) {
            ex.printStackTrace();
            return dayCal.getTimeInMillis();
        }
    }

    private long insertScheduleEntry(int medId, String dateIso, String timeHHmm, long ts) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(S_MED_ID, medId);
        v.put(S_DATE, dateIso);
        v.put(S_TIME, timeHHmm);
        v.put(S_TIMESTAMP, ts);
        v.put(S_STATUS, "PENDING");
        long id = db.insert(T_SCH, null, v);
        return id;
    }

    public MedicineDef getMedicineById(int medId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_MED, null, M_ID + "=?", new String[]{String.valueOf(medId)}, null, null, null);
        if (!c.moveToFirst()) { c.close(); return null; }
        MedicineDef m = new MedicineDef();
        m.id = c.getInt(c.getColumnIndexOrThrow(M_ID));
        m.name = c.getString(c.getColumnIndexOrThrow(M_NAME));
        m.timesCsv = c.getString(c.getColumnIndexOrThrow(M_TIMES));
        m.repeatType = c.getString(c.getColumnIndexOrThrow(M_REPEAT_TYPE));
        m.repeatInterval = c.getInt(c.getColumnIndexOrThrow(M_REPEAT_INTERVAL));
        m.daysBitmap = c.getInt(c.getColumnIndexOrThrow(M_DAYS_BITMAP));
        m.durationDays = c.getInt(c.getColumnIndexOrThrow(M_DURATION_DAYS));
        m.foodTiming = c.getString(c.getColumnIndexOrThrow(M_FOOD));
        m.severity = c.getString(c.getColumnIndexOrThrow(M_SEVERITY));
        c.close();
        return m;
    }

    public List<ScheduleEntry> getScheduleForWeek(String startDateIso, String endDateIso) {
        List<ScheduleEntry> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String selection = S_DATE + ">=? AND " + S_DATE + "<=?";
        Cursor c = db.query(T_SCH, null, selection, new String[]{startDateIso, endDateIso}, null, null, S_DATE + "," + S_TIME);
        if (c.moveToFirst()) {
            do {
                ScheduleEntry s = new ScheduleEntry();
                s.id = c.getInt(c.getColumnIndexOrThrow(S_ID));
                s.medId = c.getInt(c.getColumnIndexOrThrow(S_MED_ID));
                s.date = c.getString(c.getColumnIndexOrThrow(S_DATE));
                s.time = c.getString(c.getColumnIndexOrThrow(S_TIME));
                s.timestamp = c.getLong(c.getColumnIndexOrThrow(S_TIMESTAMP));
                s.status = c.getString(c.getColumnIndexOrThrow(S_STATUS));
                out.add(s);
            } while (c.moveToNext());
        }
        c.close();
        return out;
    }

    public void markTaken(int scheduleId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(S_STATUS, "TAKEN");
        db.update(T_SCH, v, S_ID + "=?", new String[]{String.valueOf(scheduleId)});
    }

    public void markMissed(int scheduleId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(S_STATUS, "MISSED");
        db.update(T_SCH, v, S_ID + "=?", new String[]{String.valueOf(scheduleId)});
    }

    private void deleteFutureScheduleForMed(int medId) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        db.delete(T_SCH, S_MED_ID + "=? AND " + S_TIMESTAMP + ">=?", new String[]{String.valueOf(medId), String.valueOf(now)});
    }

    // Reschedule all PENDING alarms (used on boot or after app update)
    public List<ScheduleEntry> getAllPendingEntries() {
        List<ScheduleEntry> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_SCH, null, S_STATUS + "=?", new String[]{"PENDING"}, null, null, S_TIMESTAMP + " ASC");
        if (c.moveToFirst()) {
            do {
                ScheduleEntry s = new ScheduleEntry();
                s.id = c.getInt(c.getColumnIndexOrThrow(S_ID));
                s.medId = c.getInt(c.getColumnIndexOrThrow(S_MED_ID));
                s.date = c.getString(c.getColumnIndexOrThrow(S_DATE));
                s.time = c.getString(c.getColumnIndexOrThrow(S_TIME));
                s.timestamp = c.getLong(c.getColumnIndexOrThrow(S_TIMESTAMP));
                s.status = c.getString(c.getColumnIndexOrThrow(S_STATUS));
                out.add(s);
            } while (c.moveToNext());
        }
        c.close();
        return out;
    }

    // Helper inner DTO classes
    public static class MedicineDef {
        public int id;
        public String name;
        public String timesCsv;
        public String repeatType;
        public int repeatInterval;
        public int daysBitmap;
        public int durationDays;
        public String foodTiming;
        public String severity;
    }

    public static class ScheduleEntry {
        public int id;
        public int medId;
        public String date;
        public String time;
        public long timestamp;
        public String status;
    }
}

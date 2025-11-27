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
import java.util.List;
import java.util.Locale;

/**
 * DatabaseHelper - final version for MedicRemainder1
 *
 * - Tables:
 *    medicines (T_MED)
 *    schedule_entries (T_SCH)  -- rolling 7-day schedule generated from medicines
 *
 * - DTOs:
 *    MedicineDef, ScheduleEntry
 *
 * - Important: column / method names are chosen to match usages across the project.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "medic_remainder.db";
    private static final int DATABASE_VERSION = 6;

    // --- medicines table
    public static final String T_MED = "medicines";
    public static final String M_ID = "id";
    public static final String M_NAME = "name";
    public static final String M_TIMES = "times";         // CSV "08:00,14:00"
    public static final String M_DAYS_BITMAP = "days_bitmap";
    public static final String M_FOOD = "food_timing";    // NONE/BEFORE/AFTER

    // --- schedule table (rolling 7-day)
    public static final String T_SCH = "schedule_entries";
    public static final String S_ID = "id";
    public static final String S_MED_ID = "medicine_id";
    public static final String S_DATE = "date_iso";           // yyyy-MM-dd
    public static final String S_TIME = "time";               // HH:mm
    public static final String S_TIMESTAMP = "time_in_millis";
    public static final String S_STATUS = "status";           // PENDING / TAKEN / MISSED
    public static final String S_FOOD = "food_timing";

    public DatabaseHelper(@Nullable Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ---------------------- CREATE / UPGRADE ----------------------
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMed = "CREATE TABLE " + T_MED + " (" +
                M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                M_NAME + " TEXT, " +
                M_TIMES + " TEXT, " +
                M_DAYS_BITMAP + " INTEGER DEFAULT 127, " + // default all days
                M_FOOD + " TEXT DEFAULT 'NONE'" +
                ")";
        db.execSQL(createMed);

        String createSch = "CREATE TABLE " + T_SCH + " (" +
                S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                S_MED_ID + " INTEGER, " +
                S_DATE + " TEXT, " +
                S_TIME + " TEXT, " +
                S_TIMESTAMP + " INTEGER, " +
                S_STATUS + " TEXT DEFAULT 'PENDING', " +
                S_FOOD + " TEXT, " +
                "FOREIGN KEY(" + S_MED_ID + ") REFERENCES " + T_MED + "(" + M_ID + ")" +
                ")";
        db.execSQL(createSch);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading DB from " + oldVersion + " to " + newVersion + ", destructive");
        db.execSQL("DROP TABLE IF EXISTS " + T_SCH);
        db.execSQL("DROP TABLE IF EXISTS " + T_MED);
        onCreate(db);
    }

    // ---------------------- MEDICINES CRUD ----------------------

    /**
     * Add medicine. After insert, generate schedule entries for next 7 days.
     * @param name medicine name
     * @param timesCsv CSV times "08:00,14:00"
     * @param daysBitmap bitmask Mon=1<<0 ... Sun=1<<6 (127 = all)
     * @param foodTiming "NONE"/"BEFORE"/"AFTER"
     */
    public long addMedicine(String name, String timesCsv, int daysBitmap, String foodTiming) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(M_NAME, name);
        v.put(M_TIMES, timesCsv);
        v.put(M_DAYS_BITMAP, daysBitmap);
        v.put(M_FOOD, foodTiming == null ? "NONE" : foodTiming);
        long id = db.insert(T_MED, null, v);

        if (id != -1) generateScheduleForMedicine((int) id);
        return id;
    }

    /**
     * Update medicine and regenerate schedule for next 7 days (replace future entries).
     */
    public int updateMedicine(int id, String name, String timesCsv, int daysBitmap, String foodTiming) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(M_NAME, name);
        v.put(M_TIMES, timesCsv);
        v.put(M_DAYS_BITMAP, daysBitmap);
        v.put(M_FOOD, foodTiming == null ? "NONE" : foodTiming);
        int rows = db.update(T_MED, v, M_ID + "=?", new String[]{String.valueOf(id)});

        // regenerate schedule for this med
        deleteScheduleForMedNext7Days(id);
        generateScheduleForMedicine(id);
        return rows;
    }

    /**
     * Delete medicine and its schedule entries.
     */
    public void deleteMedicine(int medId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_MED, M_ID + "=?", new String[]{String.valueOf(medId)});
        db.delete(T_SCH, S_MED_ID + "=?", new String[]{String.valueOf(medId)});
    }

    /**
     * Return all medicines ordered by id desc (newest first).
     */
    public List<MedicineDef> getAllMedicines() {
        List<MedicineDef> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_MED, null, null, null, null, null, M_ID + " DESC");
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    MedicineDef m = new MedicineDef();
                    m.id = c.getInt(c.getColumnIndexOrThrow(M_ID));
                    m.name = c.getString(c.getColumnIndexOrThrow(M_NAME));
                    m.timesCsv = c.getString(c.getColumnIndexOrThrow(M_TIMES));
                    m.daysBitmap = c.getInt(c.getColumnIndexOrThrow(M_DAYS_BITMAP));
                    m.foodTiming = c.getString(c.getColumnIndexOrThrow(M_FOOD));
                    out.add(m);
                } while (c.moveToNext());
            }
            c.close();
        }
        return out;
    }

    /**
     * Get single medicine by id.
     */
    public MedicineDef getMedicineById(int medId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_MED, null, M_ID + "=?", new String[]{String.valueOf(medId)}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                MedicineDef m = new MedicineDef();
                m.id = c.getInt(c.getColumnIndexOrThrow(M_ID));
                m.name = c.getString(c.getColumnIndexOrThrow(M_NAME));
                m.timesCsv = c.getString(c.getColumnIndexOrThrow(M_TIMES));
                m.daysBitmap = c.getInt(c.getColumnIndexOrThrow(M_DAYS_BITMAP));
                m.foodTiming = c.getString(c.getColumnIndexOrThrow(M_FOOD));
                c.close();
                return m;
            }
            c.close();
        }
        return null;
    }

    // ---------------------- SCHEDULE GENERATION (rolling 7-day) ----------------------

    /**
     * Generate schedule entries for the next 7 days for the given medicine.
     * Skips entries in the past (for today).
     */
    public void generateScheduleForMedicine(int medId) {
        MedicineDef m = getMedicineById(medId);
        if (m == null) return;

        String[] times = m.timesCsv == null ? new String[0] : m.timesCsv.split(",");
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        long now = System.currentTimeMillis();

        for (int d = 0; d < 7; d++) {
            int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun..7=Sat
            int idx = (dow + 5) % 7; // Mon=0 .. Sun=6
            boolean include = ((m.daysBitmap >> idx) & 1) == 1;
            if (include) {
                String dateIso = iso.format(cal.getTime());
                for (String t : times) {
                    t = t.trim();
                    long ts = computeTimestampForDateTime(cal, t);
                    if (ts >= now) {
                        insertScheduleEntry(medId, dateIso, t, ts, m.foodTiming);
                    }
                }
            }
            cal.add(Calendar.DATE, 1);
        }
    }

    private long insertScheduleEntry(int medId, String dateIso, String timeHHmm, long ts, String foodTiming) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(S_MED_ID, medId);
        v.put(S_DATE, dateIso);
        v.put(S_TIME, timeHHmm);
        v.put(S_TIMESTAMP, ts);
        v.put(S_STATUS, "PENDING");
        v.put(S_FOOD, foodTiming == null ? "NONE" : foodTiming);
        return db.insert(T_SCH, null, v);
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
            return c.getTimeInMillis();
        } catch (Exception e) {
            e.printStackTrace();
            return dayCal.getTimeInMillis();
        }
    }

    private void deleteScheduleForMedNext7Days(int medId) {
        SQLiteDatabase db = getWritableDatabase();
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String start = iso.format(cal.getTime());
        cal.add(Calendar.DATE, 6);
        String end = iso.format(cal.getTime());
        db.delete(T_SCH, S_MED_ID + "=? AND " + S_DATE + ">=? AND " + S_DATE + "<=?",
                new String[]{String.valueOf(medId), start, end});
    }

    /**
     * Regenerate all schedules for next 7 days (used at app start).
     * Removes entries in the range [today .. today+6] then recreates.
     */
    public void regenerateAllSchedulesForNext7Days() {
        List<MedicineDef> all = getAllMedicines();
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String start = iso.format(cal.getTime());
        cal.add(Calendar.DATE, 6);
        String end = iso.format(cal.getTime());

        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_SCH, S_DATE + ">=? AND " + S_DATE + "<=?", new String[]{start, end});

        for (MedicineDef m : all) generateScheduleForMedicine(m.id);
    }

    // ---------------------- QUERIES ----------------------

    /**
     * Return all pending schedule entries with timestamp >= now.
     * Used to schedule alarms.
     */
    public List<ScheduleEntry> getAllPendingEntries() {
        List<ScheduleEntry> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        long now = System.currentTimeMillis();

        Cursor c = db.query(T_SCH, null,
                S_STATUS + "=? AND " + S_TIMESTAMP + ">=?",
                new String[]{"PENDING", String.valueOf(now)},
                null, null, S_TIMESTAMP + " ASC");

        if (c != null) {
            if (c.moveToFirst()) {
                do out.add(cursorToSchedule(c));
                while (c.moveToNext());
            }
            c.close();
        }
        return out;
    }

    /**
     * Get schedule entries between start and end (inclusive), ordered by date,time.
     * Useful for TimetableActivity list for date range.
     */
    public List<ScheduleEntry> getScheduleForWeek(String startDateIso, String endDateIso) {
        List<ScheduleEntry> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sel = S_DATE + ">=? AND " + S_DATE + "<=?";
        Cursor c = db.query(T_SCH, null, sel, new String[]{startDateIso, endDateIso}, null, null, S_DATE + "," + S_TIME);
        if (c != null) {
            if (c.moveToFirst()) {
                do out.add(cursorToSchedule(c));
                while (c.moveToNext());
            }
            c.close();
        }
        return out;
    }

    /**
     * Get all entries for a single ISO date (yyyy-MM-dd) ordered by time asc.
     * Used by TimetableActivity day rendering.
     */
    public List<ScheduleEntry> getScheduleForDate(String dateIso) {
        List<ScheduleEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(T_SCH, null, S_DATE + "=?", new String[]{dateIso}, null, null, S_TIME + " ASC");
        if (c != null) {
            if (c.moveToFirst()) {
                do list.add(cursorToSchedule(c));
                while (c.moveToNext());
            }
            c.close();
        }
        return list;
    }

    /**
     * Get single schedule entry by id.
     * Used by ConfirmationActivity, SnoozeReceiver, BootReceiver etc.
     */
    public ScheduleEntry getScheduleEntryById(int scheduleId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(T_SCH, null, S_ID + "=?", new String[]{String.valueOf(scheduleId)}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                ScheduleEntry s = cursorToSchedule(c);
                c.close();
                return s;
            }
            c.close();
        }
        return null;
    }

    // ---------------------- STATUS UPDATES ----------------------

    /**
     * Mark an entry as TAKEN
     */
    public void markTaken(int scheduleId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(S_STATUS, "TAKEN");
        db.update(T_SCH, v, S_ID + "=?", new String[]{String.valueOf(scheduleId)});
    }

    /**
     * Mark an entry as MISSED
     */
    public void markMissed(int scheduleId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(S_STATUS, "MISSED");
        db.update(T_SCH, v, S_ID + "=?", new String[]{String.valueOf(scheduleId)});
    }

    // ---------------------- UTIL ----------------------

    private ScheduleEntry cursorToSchedule(Cursor c) {
        ScheduleEntry s = new ScheduleEntry();
        s.id = c.getInt(c.getColumnIndexOrThrow(S_ID));
        s.medId = c.getInt(c.getColumnIndexOrThrow(S_MED_ID));
        s.date = c.getString(c.getColumnIndexOrThrow(S_DATE));
        s.time = c.getString(c.getColumnIndexOrThrow(S_TIME));
        s.timestamp = c.getLong(c.getColumnIndexOrThrow(S_TIMESTAMP));
        s.status = c.getString(c.getColumnIndexOrThrow(S_STATUS));
        s.foodTiming = c.getString(c.getColumnIndexOrThrow(S_FOOD));
        return s;
    }

    // ---------------------- DTOs ----------------------

    public static class MedicineDef {
        public int id;
        public String name;
        public String timesCsv;
        public int daysBitmap;
        public String foodTiming;
    }

    public static class ScheduleEntry {
        public int id;
        public int medId;
        public String date; // yyyy-MM-dd
        public String time; // HH:mm
        public long timestamp;
        public String status; // PENDING/TAKEN/MISSED
        public String foodTiming;
    }
}

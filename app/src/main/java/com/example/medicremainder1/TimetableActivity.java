package com.example.medicremainder1;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * TimetableActivity - shows rolling 7-day grouped timetable using RecyclerView.
 */
public class TimetableActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private RecyclerView recycler;
    private TimetableAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        db = new DatabaseHelper(this);
        recycler = findViewById(R.id.timetableRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimetableAdapter(this);
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // compute next 7 days (today..today+6)
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String start = iso.format(cal.getTime());
        cal.add(Calendar.DATE, 6);
        String end = iso.format(cal.getTime());

        List<DatabaseHelper.ScheduleEntry> entries = db.getScheduleForWeek(start, end);
        adapter.setEntries(entries);
    }
}

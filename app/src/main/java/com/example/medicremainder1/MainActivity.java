package com.example.medicremainder1;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicineSimpleAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineSimpleAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddMedicineActivity.class)));

        // new button to open timetable
        findViewById(R.id.btnTimetable).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TimetableActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // load medicines
        List<DatabaseHelper.MedicineDef> meds = dbHelper.getAllMedicines();
        adapter.setList(meds);
    }
}

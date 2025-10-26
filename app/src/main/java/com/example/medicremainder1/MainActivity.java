package com.example.medicremainder1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.medicremainder1.model.Medicine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new DatabaseHelper(this);

        loadMedicines();

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddMedicineActivity.class));
            }
        });
    }

    private void loadMedicines() {
        List<Medicine> medicineList = dbHelper.getAllMedicines();
        if (adapter == null) {
            adapter = new MedicineAdapter(this, medicineList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(medicineList);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload medicines when returning from Add/Edit activities
        loadMedicines();
    }
}
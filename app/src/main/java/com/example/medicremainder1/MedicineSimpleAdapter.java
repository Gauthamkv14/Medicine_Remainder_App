package com.example.medicremainder1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for listing medicines on the main dashboard.
 * Shows:
 *  - medicine name
 *  - times (CSV)
 *  - edit button
 *  - delete button
 */
public class MedicineSimpleAdapter extends RecyclerView.Adapter<MedicineSimpleAdapter.MedViewHolder> {

    private Context context;
    private List<DatabaseHelper.MedicineDef> list = new ArrayList<>();
    private DatabaseHelper db;

    public MedicineSimpleAdapter(Context ctx) {
        this.context = ctx;
        this.db = new DatabaseHelper(ctx);
    }

    public void setList(List<DatabaseHelper.MedicineDef> data) {
        this.list = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.medicine_item, parent, false);
        return new MedViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
        DatabaseHelper.MedicineDef m = list.get(position);

        holder.nameTv.setText(m.name);
        holder.timeTv.setText("Times: " + m.timesCsv);

        // --- Edit button ---
        holder.editBtn.setOnClickListener(v -> {
            Intent i = new Intent(context, EditMedicineActivity.class);
            i.putExtra("med_id", m.id);
            context.startActivity(i);
        });

        // --- Delete button ---
        holder.deleteBtn.setOnClickListener(v -> {
            db.deleteMedicine(m.id);
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, list.size());
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class MedViewHolder extends RecyclerView.ViewHolder {

        TextView nameTv, timeTv;
        ImageButton editBtn, deleteBtn;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTv = itemView.findViewById(R.id.medicineName);
            timeTv = itemView.findViewById(R.id.medicineTime);

            editBtn = itemView.findViewById(R.id.editButton);
            deleteBtn = itemView.findViewById(R.id.deleteButton);
        }
    }
}

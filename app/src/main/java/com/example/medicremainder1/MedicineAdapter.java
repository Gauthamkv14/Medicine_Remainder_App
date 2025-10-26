package com.example.medicremainder1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicremainder1.model.Medicine;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    private Context context;
    private List<Medicine> list;
    private DatabaseHelper dbHelper;

    public MedicineAdapter(Context context, List<Medicine> list) {
        this.context = context;
        this.list = list;
        this.dbHelper = new DatabaseHelper(context);
    }

    public void updateList(List<Medicine> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.medicine_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine m = list.get(position);
        holder.medicineName.setText(m.getName());
        holder.medicineTime.setText("Time: " + m.getTime());

        // Click to edit
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditMedicineActivity.class);
            intent.putExtra("id", m.getId());
            intent.putExtra("name", m.getName());
            intent.putExtra("time", m.getTime());
            context.startActivity(intent);
        });

        // Delete button
        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Medicine")
                    .setMessage("Are you sure you want to delete " + m.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.deleteMedicine(m.getId());
                        list.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, list.size());

                        // Cancel alarm for this medicine
                        AlarmScheduler.cancelAlarm(context, m.getId());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView medicineName, medicineTime;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            medicineName = itemView.findViewById(R.id.medicineName);
            medicineTime = itemView.findViewById(R.id.medicineTime);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
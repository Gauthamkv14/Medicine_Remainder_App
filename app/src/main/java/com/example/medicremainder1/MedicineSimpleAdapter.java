package com.example.medicremainder1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MedicineSimpleAdapter extends RecyclerView.Adapter<MedicineSimpleAdapter.VH> {
    private final Context ctx;
    private List<DatabaseHelper.MedicineDef> list = new ArrayList<>();

    public MedicineSimpleAdapter(Context ctx) { this.ctx = ctx; }

    public void setList(List<DatabaseHelper.MedicineDef> l) { this.list = l; notifyDataSetChanged(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_medicine_simple, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DatabaseHelper.MedicineDef m = list.get(position);
        holder.name.setText(m.name);
        holder.times.setText(m.timesCsv);
        holder.btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(ctx, EditMedicineActivity.class);
            i.putExtra("med_id", m.id);
            ctx.startActivity(i);
        });
        holder.btnDelete.setOnClickListener(v -> {
            DatabaseHelper db = new DatabaseHelper(ctx);
            db.deleteMedicine(m.id);
            setList(db.getAllMedicines());
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, times;
        ImageButton btnEdit, btnDelete;
        VH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemName);
            times = itemView.findViewById(R.id.itemTimes);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

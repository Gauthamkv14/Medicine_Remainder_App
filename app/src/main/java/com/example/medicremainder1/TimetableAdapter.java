package com.example.medicremainder1;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.VH> {

    private final Context ctx;
    private List<DatabaseHelper.ScheduleEntry> flatList = new ArrayList<>();
    private Map<String, List<DatabaseHelper.ScheduleEntry>> byDate = new LinkedHashMap<>();

    public TimetableAdapter(Context ctx) { this.ctx = ctx; }

    public void setEntries(List<DatabaseHelper.ScheduleEntry> entries) {
        flatList = entries;
        groupByDate();
        notifyDataSetChanged();
    }

    private void groupByDate() {
        byDate.clear();
        for (DatabaseHelper.ScheduleEntry s : flatList) {
            List<DatabaseHelper.ScheduleEntry> l = byDate.getOrDefault(s.date, new ArrayList<>());
            l.add(s);
            byDate.put(s.date, l);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_timetable_date, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String date = new ArrayList<>(byDate.keySet()).get(position);
        holder.dateText.setText(date);
        holder.container.removeAllViews();
        List<DatabaseHelper.ScheduleEntry> items = byDate.get(date);
        for (DatabaseHelper.ScheduleEntry s : items) {
            TextView tv = new TextView(ctx);
            tv.setText(s.time + " - " + s.status);
            tv.setPadding(12,12,12,12);
            tv.setTextSize(14);
            if ("TAKEN".equalsIgnoreCase(s.status)) tv.setTextColor(Color.parseColor("#008000")); // green
            else if ("MISSED".equalsIgnoreCase(s.status)) tv.setTextColor(Color.parseColor("#FF0000"));
            holder.container.addView(tv);
        }
    }

    @Override
    public int getItemCount() {
        return byDate.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView dateText;
        LinearLayout container;
        VH(View v) {
            super(v);
            dateText = v.findViewById(R.id.dateText);
            container = v.findViewById(R.id.timesContainer);
        }
    }
}


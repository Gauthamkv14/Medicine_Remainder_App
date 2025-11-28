package com.example.medicremainder1;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Adapter: groups ScheduleEntry list by date (preserving order) and renders each date block.
 */
public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.VH> {

    private final Context ctx;
    private final DatabaseHelper db;
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat pretty = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    private final List<String> dates = new ArrayList<>();
    private final LinkedHashMap<String, List<DatabaseHelper.ScheduleEntry>> byDate = new LinkedHashMap<>();

    public TimetableAdapter(Context ctx) {
        this.ctx = ctx;
        this.db = new DatabaseHelper(ctx);
    }

    public void setEntries(List<DatabaseHelper.ScheduleEntry> entries) {
        dates.clear();
        byDate.clear();
        if (entries != null) {
            for (DatabaseHelper.ScheduleEntry s : entries) {
                List<DatabaseHelper.ScheduleEntry> lst = byDate.get(s.date);
                if (lst == null) {
                    lst = new ArrayList<>();
                    byDate.put(s.date, lst);
                    dates.add(s.date);
                }
                lst.add(s);
            }
        }
        // ensure we show days even if there are no entries? (DB returns only dates that have entries)
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_timetable_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String dateIso = dates.get(position);
        // pretty date
        try {
            Date d = iso.parse(dateIso);
            holder.dateText.setText(pretty.format(d));
        } catch (Exception e) {
            holder.dateText.setText(dateIso);
        }

        // clear body
        holder.tableBody.removeAllViews();

        List<DatabaseHelper.ScheduleEntry> items = byDate.get(dateIso);
        if (items == null || items.isEmpty()) {
            holder.emptyText.setVisibility(View.VISIBLE);
            holder.tableBody.setVisibility(View.GONE);
            return;
        } else {
            holder.emptyText.setVisibility(View.GONE);
            holder.tableBody.setVisibility(View.VISIBLE);
        }

        // Build rows: TIME | MEDICINE | FOOD | STATUS
        for (DatabaseHelper.ScheduleEntry s : items) {
            TableRow row = new TableRow(ctx);
            row.setPadding(6, 6, 6, 6);

            // TIME
            TextView tvTime = new TextView(ctx);
            tvTime.setText(s.time);
            tvTime.setTypeface(null, Typeface.NORMAL);
            tvTime.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(4, 4, 4, 4);
            tvTime.setLayoutParams(lp);

            // MEDICINE (use db to get name)
            TextView tvMed = new TextView(ctx);
            DatabaseHelper.MedicineDef md = db.getMedicineById(s.medId);
            tvMed.setText(md != null ? md.name : "Medicine");
            tvMed.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            tvMed.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // FOOD
            TextView tvFood = new TextView(ctx);
            tvFood.setText(s.foodTiming != null ? s.foodTiming : "-");
            tvFood.setGravity(Gravity.CENTER);
            tvFood.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

            // STATUS
            TextView tvStatus = new TextView(ctx);
            tvStatus.setGravity(Gravity.CENTER);
            tvStatus.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            String status = (s.status == null) ? "PENDING" : s.status.toUpperCase();
            switch (status) {
                case "TAKEN":
                    tvStatus.setText("✓");
                    break;
                case "MISSED":
                    tvStatus.setText("✗");
                    break;
                default:
                    tvStatus.setText("⧖");
                    break;
            }

            row.addView(tvTime);
            row.addView(tvMed);
            row.addView(tvFood);
            row.addView(tvStatus);
            holder.tableBody.addView(row);
        }
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView dateText;
        final TableLayout tableBody;
        final TextView emptyText;

        VH(@NonNull View v) {
            super(v);
            dateText = v.findViewById(R.id.dateText);
            tableBody = v.findViewById(R.id.tableBody);
            emptyText = v.findViewById(R.id.emptyText);
        }
    }
}

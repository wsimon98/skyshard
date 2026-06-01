package com.american2day.skyshard;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.Holder> {

    public interface RowAction {
        void onAction(String packageName);
    }

    public static class Row {
        public final String packageName;
        public final String label;
        public final Drawable icon;
        public final String actionLabel;
        public Row(String packageName, String label, Drawable icon, String actionLabel) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.actionLabel = actionLabel;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final RowAction onAction;

    public AppListAdapter(RowAction onAction) {
        this.onAction = onAction;
    }

    public void setRows(List<Row> next) {
        rows.clear();
        rows.addAll(next);
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder h, int position) {
        Row r = rows.get(position);
        h.label.setText(r.label);
        h.pkg.setText(r.packageName);
        h.icon.setImageDrawable(r.icon);
        h.action.setText(r.actionLabel);
        h.action.setOnClickListener(v -> onAction.onAction(r.packageName));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView pkg;
        final Button action;
        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.app_icon);
            label = v.findViewById(R.id.app_label);
            pkg = v.findViewById(R.id.app_pkg);
            action = v.findViewById(R.id.row_action);
        }
    }
}

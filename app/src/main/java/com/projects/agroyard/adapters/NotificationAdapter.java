package com.projects.agroyard.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projects.agroyard.R;
import com.projects.agroyard.models.NotificationModel;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<NotificationModel> notifications;
    private OnItemClickListener onItemClickListener;

    public NotificationAdapter(List<NotificationModel> notifications, OnItemClickListener onItemClickListener) {
        this.notifications = notifications;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notifications.get(position);

        // Set values to the views dynamically
        holder.titleTextView.setText(notification.getTitle());
        holder.descriptionTextView.setText(notification.getMessage());
        holder.timestampTextView.setText(String.valueOf(notification.getTimestamp()));
        holder.colorView.setBackgroundColor(Color.parseColor(notification.getColorCode()));

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(notification);  // Pass the notification to the listener
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView timestampTextView;
        View colorView;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notification_title);
            descriptionTextView = itemView.findViewById(R.id.notification_description);
            timestampTextView = itemView.findViewById(R.id.notification_timestamp);
            colorView = itemView.findViewById(R.id.notification_color);
        }
    }

    // Interface for handling item clicks
    public interface OnItemClickListener {
        void onItemClick(NotificationModel notification);
    }
}


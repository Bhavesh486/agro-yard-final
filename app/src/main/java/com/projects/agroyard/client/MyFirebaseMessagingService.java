package com.projects.agroyard.client;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.projects.agroyard.MainActivity;
import com.projects.agroyard.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String title = message.getData().get("title");
        String body = message.getData().get("body");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // however you get logged-in user ID
        if (title != null && body != null) {
            // Save notification into Realtime Database
            DatabaseReference notificationsRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference()
                    .child("notifications")
                    .child(userId)
                    .push(); // create a new unique ID

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("title", title);
            notificationData.put("body", body);
            notificationData.put("timestamp", ServerValue.TIMESTAMP);
            notificationData.put("isRead", false);
            notificationData.put("color", "#1123FF");

            notificationsRef.setValue(notificationData)
                    .addOnSuccessListener(aVoid -> Log.d("Notification", "Saved successfully"))
                    .addOnFailureListener(e -> Log.e("Notification", "Failed to save", e));

        }
    }
}


package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.projects.agroyard.R;
import com.projects.agroyard.adapters.NotificationAdapter;
import com.projects.agroyard.models.NotificationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private ImageView backButton;
    private TextView markAllReadButton;
    private CardView biddingSection;

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notifications;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Initialize views
        backButton = view.findViewById(R.id.back_button);
        markAllReadButton = view.findViewById(R.id.mark_all_read);

        // Set up click listeners
        backButton.setOnClickListener(v -> {
            // Go back to previous screen
            requireActivity().getSupportFragmentManager().popBackStack();
        });



        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("notifications").child(userId);

        notifications = new ArrayList<>();

        markAllReadButton.setOnClickListener(v -> {
            // Mark all notifications as read (in a real app, this would update a database)
            if(!notifications.isEmpty()) {
                notifications.forEach(notification -> {
                    if(!notification.isRead()) {
                        databaseRef.child(notification.getNotifiactionId()).child("isRead").setValue(true)
                                .addOnSuccessListener(aVoid -> Log.d("Notification", "Saved successfully"))
                                .addOnFailureListener(e -> Log.e("Notification", "Failed to save", e));

                        // Change the color of the notification
                        databaseRef.child(notification.getNotifiactionId()).child("color").setValue("#FFFFFF")
                                .addOnSuccessListener(aVoid -> Log.d("Notification", "Color updated successfully"))
                                .addOnFailureListener(e -> Log.e("Notification", "Failed to update color", e));
                    }
                });
            }
            Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show();
        });

        adapter = new NotificationAdapter(notifications, notification -> {
            if(!notification.isRead()) {
                databaseRef.child(notification.getNotifiactionId()).child("isRead").setValue(true)
                        .addOnSuccessListener(aVoid -> Log.d("Notification", "Saved successfully"))
                        .addOnFailureListener(e -> Log.e("Notification", "Failed to save", e));

                // Change the color of the notification
                databaseRef.child(notification.getNotifiactionId()).child("color").setValue("#FFFFFF")
                        .addOnSuccessListener(aVoid -> Log.d("Notification", "Color updated successfully"))
                        .addOnFailureListener(e -> Log.e("Notification", "Failed to update color", e));
            }
        });

        recyclerView.setAdapter(adapter);

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                notifications.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Retrieve the data and map it to the model
                    String notificationId = snapshot.getKey();
                    String title = snapshot.child("title").getValue(String.class);
                    String message = snapshot.child("body").getValue(String.class);  // Use "body" from Firebase
                    long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    String colorCode = snapshot.child("color").getValue(String.class);
                    boolean isRead = snapshot.child("isRead").getValue(Boolean.class);

                    // Create a NotificationModel object
                    NotificationModel notification = new NotificationModel(notificationId, title, message, timestamp, colorCode, isRead);

                    notifications.add(notification);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Notifications", "Failed to read notifications", databaseError.toException());
            }
        });

        // Set up bidding section click listeners
//        setupBiddingNotifications(view);

        return view;
    }

    /**
     * Set up click listeners for the bidding notifications
     */
//    private void setupBiddingNotifications(View view) {
//        // Find all the bidding notification items - in this example we're using the parent LinearLayouts
//        // This assumes the layout structure from fragment_notifications.xml
//
//        // For a real app, you would likely have a RecyclerView with adapters instead of hard-coded views
//
//        // Find each notification item by its index in the CardView's LinearLayout
//        // This is a simplification - in a real app with dynamic content, you would use IDs or a RecyclerView
//        try {
//            CardView biddingCard = view.findViewWithTag("bidding_section");
//            if (biddingCard != null) {
//                ViewGroup biddingLayout = (ViewGroup) biddingCard.getChildAt(0);
//
//                // Tomato bidding notification (first child)
//                biddingLayout.getChildAt(0).setOnClickListener(v -> {
//                    navigateToBidding("Fresh Tomatoes");
//                });
//
//                // Wheat bidding notification (third child, after the divider)
//                biddingLayout.getChildAt(2).setOnClickListener(v -> {
//                    navigateToBidding("Organic Wheat");
//                });
//
//                // Rice bidding notification (fifth child, after the divider)
//                biddingLayout.getChildAt(4).setOnClickListener(v -> {
//                    navigateToBidding("Basmati Rice");
//                });
//            }
//        } catch (Exception e) {
//            // Fallback - when the view structure doesn't match expectations
//            // Setup a click listener on the whole bidding card
//            View fallbackBiddingSection = view.findViewById(R.id.bidding_section);
//            if (fallbackBiddingSection != null) {
//                fallbackBiddingSection.setOnClickListener(v -> {
//                    navigateToBidding(null);
//                });
//            }
//        }
//    }

    /**
     * Navigate to the BettingFragment with the selected product
     */
    private void navigateToBidding(String productName) {
        // In a real app, you would pass the product name to the BettingFragment
        BettingFragment bettingFragment = new BettingFragment();

        if (productName != null) {
            Bundle args = new Bundle();
            args.putString("product_name", productName);
            bettingFragment.setArguments(args);

            Toast.makeText(requireContext(), "Viewing " + productName + " bidding", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Viewing all bidding activities", Toast.LENGTH_SHORT).show();
        }

        // Navigate to the betting fragment
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, bettingFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
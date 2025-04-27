package com.projects.agroyard.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.utils.FirestoreHelper;
import com.projects.agroyard.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class DeliveryRequestsFragment extends Fragment {
    
    private static final String TAG = "DeliveryRequestsFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyStateText;
    private LinearLayout emptyStateLayout;
    private ImageView backButton;
    
    private SessionManager sessionManager;
    private List<Map<String, Object>> deliveryRequests = new ArrayList<>();
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_requests, container, false);
        
        initializeViews(view);
        
        // Check if user is a farmer
        if (!Constants.FARMER.equals(sessionManager.getUserType())) {
            Toast.makeText(requireContext(), "Only farmers can view delivery requests", Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }
        
        // Setup back button
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadDeliveryRequests);
        
        // Load delivery requests
        loadDeliveryRequests();
        
        return view;
    }
    
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_delivery_requests);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyStateText = view.findViewById(R.id.text_empty_state);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
        backButton = view.findViewById(R.id.back_button);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }
    
    private void loadDeliveryRequests() {
        String farmerId = sessionManager.getUserId();
        if (farmerId == null || farmerId.isEmpty()) {
            Toast.makeText(requireContext(), "Farmer ID not found", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Farmer ID is null or empty");
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            emptyStateText.setText("Unable to load delivery requests.\nUser ID not found.");
            return;
        }
        
        // Debug message
        Log.d(TAG, "Loading delivery requests for farmer ID: " + farmerId);
        Toast.makeText(requireContext(), "Loading requests for: " + farmerId, Toast.LENGTH_SHORT).show();
        
        swipeRefreshLayout.setRefreshing(true);
        
        FirestoreHelper.getFarmerDeliveryRequests(farmerId, new FirestoreHelper.DeliveryRequestsCallback() {
            @Override
            public void onDeliveryRequestsLoaded(List<Map<String, Object>> requests) {
                swipeRefreshLayout.setRefreshing(false);
                deliveryRequests = requests;
                
                if (deliveryRequests.isEmpty()) {
                    // If no delivery requests exist, offer to create a test entry
                    recyclerView.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    emptyStateText.setText("No delivery requests yet.\nTap here to create a test entry.");
                    
                    // Make empty state clickable to create test data
                    emptyStateLayout.setOnClickListener(v -> createTestDeliveryRequest());
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateLayout.setVisibility(View.GONE);
                    recyclerView.setAdapter(new DeliveryRequestsAdapter(deliveryRequests));
                }
            }
            
            @Override
            public void onError(Exception e) {
                swipeRefreshLayout.setRefreshing(false);
                recyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                emptyStateText.setText("Unable to load delivery requests.\nPlease try again later.");
                
                // Log the error
                Log.e(TAG, "Error loading delivery requests", e);
                
                // Show a simpler toast message
                Toast.makeText(requireContext(), 
                    "Error loading delivery requests. Please try again.", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Creates a test delivery request entry for demonstration purposes
     */
    private void createTestDeliveryRequest() {
        String farmerId = sessionManager.getUserId();
        if (farmerId == null || farmerId.isEmpty()) {
            Toast.makeText(requireContext(), "Cannot create test entry: User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create test delivery data
        Map<String, Object> testDelivery = new HashMap<>();
        testDelivery.put("userId", "test_user_id");
        testDelivery.put("name", "Rahul Kumar (Member)");
        testDelivery.put("addressLine1", "123 Test Street");
        testDelivery.put("addressLine2", "Apartment 456");
        testDelivery.put("city", "Test City");
        testDelivery.put("state", "Test State");
        testDelivery.put("pinCode", "123456");
        testDelivery.put("phone", "9876543210");
        testDelivery.put("alternatePhone", "8765432109");
        testDelivery.put("landmark", "Near Test Market");
        testDelivery.put("farmerId", farmerId);
        testDelivery.put("farmerName", sessionManager.getName());
        testDelivery.put("timestamp", System.currentTimeMillis());
        
        Toast.makeText(requireContext(), "Creating test delivery request...", Toast.LENGTH_SHORT).show();
        
        // Save to Firestore
        FirestoreHelper.saveDeliveryAddress(testDelivery, new FirestoreHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Test delivery request created successfully", Toast.LENGTH_SHORT).show();
                // Reload the data
                loadDeliveryRequests();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), 
                    "Error creating test entry: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error creating test delivery entry", e);
            }
        });
    }
    
    private class DeliveryRequestsAdapter extends RecyclerView.Adapter<DeliveryRequestsAdapter.ViewHolder> {
        private List<Map<String, Object>> requests;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        
        public DeliveryRequestsAdapter(List<Map<String, Object>> requests) {
            this.requests = requests;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_delivery_request, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> request = requests.get(position);
            
            // Display member name more prominently
            String memberName = (String) request.get("name");
            holder.nameText.setText("Member: " + memberName);
            
            String address = request.get("addressLine1") + "\n" +
                    request.get("addressLine2") + "\n" +
                    request.get("city") + ", " + request.get("state") + " - " + request.get("pinCode");
            holder.addressText.setText("Delivery Address:\n" + address);
            
            // Store phone number for call button
            String phoneNumber = (String) request.get("phone");
            holder.phoneText.setText("Phone: " + phoneNumber);
            
            if (request.get("alternatePhone") != null && !request.get("alternatePhone").toString().isEmpty()) {
                holder.alternatePhoneText.setVisibility(View.VISIBLE);
                holder.alternatePhoneText.setText("Alt. Phone: " + request.get("alternatePhone"));
            } else {
                holder.alternatePhoneText.setVisibility(View.GONE);
            }
            
            if (request.get("landmark") != null && !request.get("landmark").toString().isEmpty()) {
                holder.landmarkText.setVisibility(View.VISIBLE);
                holder.landmarkText.setText("Landmark: " + request.get("landmark"));
            } else {
                holder.landmarkText.setVisibility(View.GONE);
            }
            
            // Format timestamp
            Long timestamp = (Long) request.get("timestamp");
            if (timestamp != null) {
                String date = dateFormat.format(new Date(timestamp));
                holder.timestampText.setText("Requested on: " + date);
            }
            
            // Set up call button
            holder.callButton.setOnClickListener(v -> {
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phoneNumber));
                    try {
                        requireContext().startActivity(callIntent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Unable to make call: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error making phone call", e);
                    }
                } else {
                    Toast.makeText(requireContext(), "Phone number not available", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return requests.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView addressText;
            TextView phoneText;
            TextView alternatePhoneText;
            TextView landmarkText;
            TextView timestampText;
            ImageButton callButton;
            
            ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.text_name);
                addressText = itemView.findViewById(R.id.text_address);
                phoneText = itemView.findViewById(R.id.text_phone);
                alternatePhoneText = itemView.findViewById(R.id.text_alternate_phone);
                landmarkText = itemView.findViewById(R.id.text_landmark);
                timestampText = itemView.findViewById(R.id.text_timestamp);
                callButton = itemView.findViewById(R.id.call_button);
            }
        }
    }
} 
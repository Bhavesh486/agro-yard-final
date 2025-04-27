package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;

import java.util.Map;

public class MyProfileFragment extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String TAG = "MyProfileFragment";
    
    // UI elements
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText stateEditText;
    private EditText districtEditText;
    private EditText addressEditText;
    private EditText marketNameEditText;
    private EditText marketIdEditText;
    private TextView userTypeTextView;
    private View loadingView;
    private View contentView;
    
    // Firebase references
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    
    public MyProfileFragment() {
        // Required empty public constructor
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Get current user ID if logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "Current user ID: " + userId);
        } else {
            Log.d(TAG, "No current Firebase user found");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_profile, container, false);
        
        // Initialize views
        initializeViews(view);
        
        // Show loading initially
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        
        // Set back button click listener
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            // Navigate back to ProfileFragment instead of popping back stack
            Fragment profileFragment = new ProfileFragment();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, profileFragment);
            transaction.commit(); // Don't add to back stack
        });
        
        // Set edit profile button click listener
        view.findViewById(R.id.edit_profile_button).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Edit profile - Coming soon", Toast.LENGTH_SHORT).show();
            // Enable editing of fields
            // toggleEditMode(true);
        });
        
        // Load user profile data
        loadUserProfile();
        
        return view;
    }
    
    private void initializeViews(View view) {
        nameEditText = view.findViewById(R.id.profile_name);
        emailEditText = view.findViewById(R.id.profile_email);
        phoneEditText = view.findViewById(R.id.profile_phone);
        stateEditText = view.findViewById(R.id.profile_state);
        districtEditText = view.findViewById(R.id.profile_district);
        addressEditText = view.findViewById(R.id.profile_address);
        userTypeTextView = view.findViewById(R.id.profile_user_type);
        loadingView = view.findViewById(R.id.loading_view);
        contentView = view.findViewById(R.id.profile_content);
        
        // Find additional views if they exist in the layout
        try {
            marketNameEditText = view.findViewById(R.id.profile_market_name);
            marketIdEditText = view.findViewById(R.id.profile_market_id);
        } catch (Exception e) {
            Log.w(TAG, "Some additional profile views not found in layout: " + e.getMessage());
        }
    }
    
    private void loadUserProfile() {
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    loadingView.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                    
                    if (documentSnapshot.exists()) {
                        updateProfileWithFirestoreData(documentSnapshot);
                        
                        // Get user type
                        String userType = documentSnapshot.getString("userType");
                        
                        // Show market fields for committee members
                        if (userType != null && userType.equalsIgnoreCase("Committee Member")) {
                            if (marketNameEditText != null) marketNameEditText.setVisibility(View.VISIBLE);
                            if (marketIdEditText != null) marketIdEditText.setVisibility(View.VISIBLE);
                        }
                        
                        // Show address field only for Members
                        if (userType != null && userType.equalsIgnoreCase(Constants.MEMBER)) {
                            if (addressEditText != null) addressEditText.setVisibility(View.VISIBLE);
                        } else {
                            if (addressEditText != null) addressEditText.setVisibility(View.GONE);
                        }
                        
                        // Log all available data for debugging
                        Map<String, Object> allData = documentSnapshot.getData();
                        if (allData != null) {
                            Log.d(TAG, "All user data: " + allData.toString());
                        }
                    } else {
                        // Fallback to shared preferences
                        loadProfileFromPrefs();
                        Log.w(TAG, "User document does not exist in Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    loadingView.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                    
                    // Fallback to shared preferences
                    loadProfileFromPrefs();
                    Toast.makeText(requireContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching user data: " + e.getMessage(), e);
                });
        } else {
            // No Firebase user, use shared preferences
            loadingView.setVisibility(View.GONE);
            contentView.setVisibility(View.VISIBLE);
            loadProfileFromPrefs();
            Log.w(TAG, "No user ID available, using shared preferences");
        }
    }
    
    private void updateProfileWithFirestoreData(DocumentSnapshot document) {
        try {
            // Basic user info
            String name = document.getString("name");
            String email = document.getString("email");
            String phone = document.getString("mobile");
            String userType = document.getString("userType");
            String state = document.getString("state");
            String district = document.getString("district");
            String address = document.getString("address");
            String marketName = document.getString("marketName");
            String marketId = document.getString("marketId");
            
            // Set text for required fields
            if (name != null) nameEditText.setText(name);
            if (email != null) emailEditText.setText(email);
            if (phone != null) phoneEditText.setText(phone);
            if (userType != null) userTypeTextView.setText(userType);
            if (state != null) stateEditText.setText(state);
            if (district != null) districtEditText.setText(district);
            if (address != null) addressEditText.setText(address);
            
            // Set text for optional fields if they exist
            if (marketName != null && marketNameEditText != null) marketNameEditText.setText(marketName);
            if (marketId != null && marketIdEditText != null) marketIdEditText.setText(marketId);
            
            // Save to SharedPreferences for future use
            saveToSharedPreferences(document);
            
            Log.d(TAG, "User profile updated from Firestore data");
        } catch (Exception e) {
            Log.e(TAG, "Error updating profile with Firestore data: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error displaying profile data", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveToSharedPreferences(DocumentSnapshot document) {
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        
        // Save all string fields to SharedPreferences
        Map<String, Object> data = document.getData();
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof String) {
                    editor.putString(entry.getKey(), (String) entry.getValue());
                }
            }
        }
        
        // Ensure critical fields are saved
        String name = document.getString("name");
        String email = document.getString("email");
        String phone = document.getString("mobile");
        String userType = document.getString("userType");
        String state = document.getString("state");
        String district = document.getString("district");
        
        if (name != null) editor.putString("userName", name);
        if (email != null) editor.putString("userEmail", email);
        if (phone != null) editor.putString("userPhone", phone);
        if (userType != null) editor.putString("userType", userType);
        if (state != null) editor.putString("userState", state);
        if (district != null) editor.putString("userDistrict", district);
        
        editor.apply();
    }
    
    private void loadProfileFromPrefs() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            
            // Try to load data with both key variations
            setEditTextFromPrefs(prefs, "name", "userName", nameEditText);
            setEditTextFromPrefs(prefs, "email", "userEmail", emailEditText);
            setEditTextFromPrefs(prefs, "mobile", "userPhone", phoneEditText); // Check for "mobile" first
            setEditTextFromPrefs(prefs, "userType", null, userTypeTextView);
            setEditTextFromPrefs(prefs, "state", "userState", stateEditText);
            setEditTextFromPrefs(prefs, "district", "userDistrict", districtEditText);
            setEditTextFromPrefs(prefs, "address", "userAddress", addressEditText);
            
            // Optional fields
            if (marketNameEditText != null) {
                setEditTextFromPrefs(prefs, "marketName", null, marketNameEditText);
            }
            if (marketIdEditText != null) {
                setEditTextFromPrefs(prefs, "marketId", null, marketIdEditText);
            }
            
            // Get user type from preferences
            String userType = prefs.getString("userType", null);
            
            // Show market fields for committee members
            if (userType != null && userType.equalsIgnoreCase("Committee Member")) {
                if (marketNameEditText != null) marketNameEditText.setVisibility(View.VISIBLE);
                if (marketIdEditText != null) marketIdEditText.setVisibility(View.VISIBLE);
            }
            
            // Show address field only for Members
            if (userType != null && userType.equalsIgnoreCase(Constants.MEMBER)) {
                if (addressEditText != null) addressEditText.setVisibility(View.VISIBLE);
            } else {
                if (addressEditText != null) addressEditText.setVisibility(View.GONE);
            }
            
            Log.d(TAG, "Profile loaded from SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile from SharedPreferences: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading profile data", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setEditTextFromPrefs(SharedPreferences prefs, String key1, String key2, TextView textView) {
        String value = prefs.getString(key1, null);
        if (value == null && key2 != null) {
            value = prefs.getString(key2, null);
        }
        if (value != null) {
            textView.setText(value);
        }
    }
    
    // Toggle between view and edit modes
    private void toggleEditMode(boolean editMode) {
        nameEditText.setEnabled(editMode);
        phoneEditText.setEnabled(editMode);
        stateEditText.setEnabled(editMode);
        districtEditText.setEnabled(editMode);
        addressEditText.setEnabled(editMode);
        
        if (marketNameEditText != null) marketNameEditText.setEnabled(editMode);
        if (marketIdEditText != null) marketIdEditText.setEnabled(editMode);
        
        // Email and user type should not be editable
        emailEditText.setEnabled(false);
        userTypeTextView.setEnabled(false);
    }
} 
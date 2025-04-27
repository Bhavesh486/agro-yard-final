package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.utils.SessionManager;

public class LoginUsers extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_NAME = "userName";
    private static final String TAG = "LoginUsers";
    
    private EditText emailEditText;
    private EditText passwordEditText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        // Initialize SessionManager
        sessionManager = new SessionManager(requireContext());
        
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToHome();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        emailEditText = view.findViewById(R.id.emailInput);
        passwordEditText = view.findViewById(R.id.passwordInput);

        // Handle login button click
        view.findViewById(R.id.loginButton).setOnClickListener(v -> loginUser());

        // Handle signup redirection
        view.findViewById(R.id.signupText).setOnClickListener(v -> openSignup());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading indicator
        // progressBar.setVisibility(View.VISIBLE);
        
        // Authenticate with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                // progressBar.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        // Fetch user data from Firestore
                        fetchUserDataFromFirestore(user.getUid());
                    }
                } else {
                    Toast.makeText(getContext(), "Login failed: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void fetchUserDataFromFirestore(String userId) {
        db.collection("users").document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // User data found in Firestore
                        String userType = document.getString("userType");
                        String userName = document.getString("name");
                        String email = document.getString("email");
                        String mobile = document.getString("mobile");
                        String profileImage = document.getString("profileImage");
                        
                        Log.d(TAG, "User data retrieved from Firestore - type: " + userType + ", name: " + userName);
                        
                        // Save to session manager for persistent login
                        sessionManager.createLoginSession(userId, userName, email, userType, mobile, profileImage);
                        
                        // Also save to SharedPreferences for backward compatibility
                        saveUserInfo(userType, userName);
                        
                        // Show success message
                        Toast.makeText(getContext(), "Welcome back, " + userName + "!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to home screen
                        navigateToHome();
                    } else {
                        Log.d(TAG, "No user data found in Firestore. Using email-based fallback.");
                        handleLoginWithoutFirestoreData();
                    }
                } else {
                    Log.d(TAG, "Failed to get user data from Firestore", task.getException());
                    handleLoginWithoutFirestoreData();
                }
            });
    }
    
    private void handleLoginWithoutFirestoreData() {
        // Fallback: Check if there's a pre-existing user type from SharedPreferences
        String email = emailEditText.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();
        String userId = user != null ? user.getUid() : "";
        
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existingUserType = prefs.getString(KEY_USER_TYPE, null);
        String existingUserName = prefs.getString(KEY_USER_NAME, null);
        
        // Use stored user type if available, otherwise determine based on email
        String userType;
        String userName;
        
        if (existingUserType != null && !existingUserType.equals("Select User Type")) {
            // Use the user type and name from SharedPreferences
            userType = existingUserType;
            userName = existingUserName != null ? existingUserName : email.split("@")[0];
            Log.d(TAG, "Using existing user type from SharedPreferences: " + userType + ", name: " + userName);
        } else {
            // Fall back to determining based on email
            userType = email.contains("member") ? Constants.MEMBER : Constants.FARMER;
            userName = email.split("@")[0];
            Log.d(TAG, "Determined user type from email: " + userType + ", name: " + userName);
        }
        
        // Store user info in SessionManager for persistent login
        sessionManager.createLoginSession(userId, userName, email, userType, "", "");
        
        // Also store in SharedPreferences for backward compatibility
        saveUserInfo(userType, userName);
        
        // Show login success message
        Toast.makeText(getContext(), "Logged in as " + userType, Toast.LENGTH_SHORT).show();
        
        // Navigate to home
        navigateToHome();
    }
    
    private void navigateToHome() {
        // Navigate to home screen
        Fragment homeFragment = new BottomNavigationFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, homeFragment);
        transaction.commit();
    }
    
    private void checkStoredUserType() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userType = prefs.getString(KEY_USER_TYPE, "None");
        String userName = prefs.getString(KEY_USER_NAME, "None");
        Log.d(TAG, "Stored user type: " + userType + ", name: " + userName);
    }
    
    private void saveUserInfo(String userType, String userName) {
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_USER_NAME, userName);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
        
        Log.d(TAG, "Saved user info - type: " + userType + ", name: " + userName);
    }

    private void openSignup() {
        Fragment signupFragment = new SignupUser();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, signupFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

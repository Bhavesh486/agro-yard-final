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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingKtxRegistrar;
import com.projects.agroyard.R;
import com.projects.agroyard.client.StateClient;
import com.projects.agroyard.client.callback.StateDataCallback;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.model.StateModel;
import com.projects.agroyard.model.StatesModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignupUser extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_NAME = "userName";
    private static final String TAG = "SignupUser";
    
    private FirebaseAuth mAuth; // Firebase Auth instance
    private FirebaseFirestore db; // Firestore database
    private EditText emailEditText, passwordEditText; // Fields for email and password

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase-related UI elements
        emailEditText = view.findViewById(R.id.emailInput);
        passwordEditText = view.findViewById(R.id.passwordInput);
        Button signupButton = view.findViewById(R.id.signupButton);
        EditText nameInput = view.findViewById(R.id.nameInput);
        EditText mobileNumber = view.findViewById(R.id.mobileNumber);
        EditText confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        EditText marketName = view.findViewById(R.id.marketName);
        EditText marketId = view.findViewById(R.id.marketId);
        EditText addressInput = view.findViewById(R.id.addressInput);
        Spinner userTypeSpinner = view.findViewById(R.id.userTypeSpinner);
        Spinner stateSpinner = view.findViewById(R.id.stateSpinner);
        Spinner districtSpinner = view.findViewById(R.id.districtSpinner);

        setSpinnerAdapter(view);

        getStateDistList(statesDistMap -> {
            Log.i("Map", statesDistMap.toString());
            if (statesDistMap != null && !statesDistMap.isEmpty()) {
                populateStateAndDistricts(view, statesDistMap);
            }
        });

        view.findViewById(R.id.loginText).setOnClickListener(v -> openLogin());

        // Signup button click listener
        signupButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String name = nameInput.getText().toString().trim();
            String mobile = mobileNumber.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();
            String userType = userTypeSpinner.getSelectedItem().toString();
            String state = stateSpinner.getSelectedItem().toString();
            String district = districtSpinner.getSelectedItem().toString();
            
            // Validate basic fields
            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || mobile.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate user type selection
            if (userType.equals("Select User Type")) {
                Toast.makeText(getContext(), "Please select a user type", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate state and district
            if (state.equals(Constants.SELECT_STATE) || district.equals(Constants.SELECT_DISTRICT)) {
                Toast.makeText(getContext(), "Please select state and district", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate password match
            if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get address and validate it's not empty
            String addressStr = addressInput.getText().toString().trim();
            if (addressStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter your address", Toast.LENGTH_SHORT).show();
                return;
            }
            
            
            // Validate Market ID for Member user type
            String marketNameStr = "";
            String marketIdStr = "";
             
            if (userType.equals(Constants.MEMBER)) {
                marketNameStr = marketName.getText().toString().trim();
                marketIdStr = marketId.getText().toString().trim();
                
                if (marketNameStr.isEmpty() || marketIdStr.isEmpty()) {
                    Toast.makeText(getContext(), "Market Name and Market ID are required for Members", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Prepare user data for Firestore
            final Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("email", email);
            userData.put("mobile", mobile);
            userData.put("userType", userType);
            userData.put("state", state);
            userData.put("district", district);
            userData.put("createdAt", System.currentTimeMillis());
            
            // Save address for all users (will be empty for non-members)
            userData.put("address", addressStr);
            
            // Add market details if user is a member
            if (userType.equals(Constants.MEMBER)) {
                userData.put("marketName", marketNameStr);
                userData.put("marketId", marketIdStr);
            }

            // Store user type in shared preferences for immediate use
            saveUserTypeAndName(userType, name);

            // Create user with Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            // Store user data in Firestore
                            storeUserToken(user.getUid());
                            storeUserDataInFirestore(user.getUid(), userData);
                            
                        } else {
                            Toast.makeText(getContext(), "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void storeUserToken(String userId) {
        FirebaseApp.initializeApp(requireContext());
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Get the FCM token
                    String token = task.getResult();
                    Log.d("FCM", "FCM Token: " + token);

                    // Now, send this token to Firestore

                });
    }

    private void storeUserDataInFirestore(String userId, Map<String, Object> userData) {

        FirebaseApp.initializeApp(requireContext());
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        Toast.makeText(getContext(), "Error saving user data: ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Get the FCM token
                    String token = task.getResult();
                    Log.d("FCM", "FCM Token: " + token);

                    userData.put("deviceToken", token);

                    db.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User data stored successfully in Firestore");
                                Toast.makeText(getContext(), "Signup successful!", Toast.LENGTH_SHORT).show();
                                openLogin(); // Redirect to login after signup
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error storing user data in Firestore", e);
                                Toast.makeText(getContext(), "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });

                });
    }
    
    private void saveUserTypeAndName(String userType, String name) {
        // Save user type and name to SharedPreferences
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
        
        Log.d(TAG, "Saved user type: " + userType + ", name: " + name);
    }

    private void getStateDistList(final StateDataCallback callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        StateClient stateClient = retrofit.create(StateClient.class);
        stateClient.getStateDistrict().enqueue(new Callback<StatesModel>() {
            @Override
            public void onResponse(Call<StatesModel> call, Response<StatesModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatesModel stateList = response.body();
                    if (!stateList.getStates().isEmpty()) {
                        Map<String, List<String>> statesDistMap = new HashMap<>();
                        stateList.getStates().forEach(stateModel -> statesDistMap.put(stateModel.getState(), stateModel.getDistricts()));
                        callback.onStateDataFetched(statesDistMap);
                    }
                } else {
                    Log.e("API Response", "Response was not successful. Status code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<StatesModel> call, Throwable throwable) {
                Log.e("API Error", "Failed to fetch data", throwable);
            }
        });
    }

    private void setSpinnerAdapter(View view) {
        int[] spinnerIds = {
                R.id.userTypeSpinner,
                R.id.stateSpinner,
                R.id.districtSpinner
        };

        int[] stringArrays = {
                R.array.user_types,
                R.array.states,
                R.array.districts
        };

        for (int i = 0; i < spinnerIds.length; i++) {
            Spinner spinner = view.findViewById(spinnerIds[i]);
            if (i > 0) {
                spinner.setEnabled(false);
            }
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    getContext(), stringArrays[i], R.layout.spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

        TextView marketName = view.findViewById(R.id.marketName);
        EditText marketId = view.findViewById(R.id.marketId);
        EditText addressInput = view.findViewById(R.id.addressInput);
        Spinner userTypeSpinner = view.findViewById(R.id.userTypeSpinner);

        userTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equals(Constants.MEMBER)) {
                    marketName.setVisibility(View.VISIBLE);
                    marketId.setVisibility(View.VISIBLE);
                } else {
                    marketName.setVisibility(View.GONE);
                    marketId.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void populateStateAndDistricts(View view, Map<String, List<String>> statesDistMap) {
        Spinner state = view.findViewById(R.id.stateSpinner);
        Spinner district = view.findViewById(R.id.districtSpinner);

        state.setEnabled(true);

        List<String> statesList = new ArrayList<>();
        statesList.add(Constants.SELECT_STATE);
        statesList.addAll(statesDistMap.keySet());

        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, statesList);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        state.setAdapter(stateAdapter);

        ArrayList<String> districtList = new ArrayList<>(List.of(Constants.SELECT_DISTRICT));
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, districtList);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        district.setAdapter(districtAdapter);

        state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    district.setEnabled(true);
                    String selectedState = statesList.get(position);
                    List<String> districts = statesDistMap.get(selectedState);
                    districts.add(0, Constants.SELECT_DISTRICT);

                    district.setSelection(0);
                    districtList.clear();
                    districtList.addAll(districts != null && !districts.isEmpty() ? districts : List.of("Select District"));
                    districtAdapter.notifyDataSetChanged();
                } else {
                    district.setEnabled(false);
                    district.setSelection(0);
                    districtList.clear();
                    districtList.add(Constants.SELECT_DISTRICT);
                    districtAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void openLogin() {
        // Clear previous user data when going to login from signup
        clearPreviousUserData();

        Fragment loginUsers = new LoginUsers();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginUsers);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    
    private void clearPreviousUserData() {
        // Clear user type data when moving to login page from signup
        // This ensures we'll use the freshly created user data
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(KEY_USER_TYPE);
        editor.remove(KEY_USER_NAME);
        editor.apply();
        
        Log.d("SignupUser", "Cleared previous user data");
    }
}
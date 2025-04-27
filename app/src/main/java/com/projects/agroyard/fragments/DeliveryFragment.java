package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.utils.FirestoreHelper;
import com.projects.agroyard.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeliveryFragment extends Fragment {
    
    private static final String PREF_DELIVERY_ADDRESSES = "DeliveryAddresses";
    private static final String TAG = "DeliveryFragment";
    
    private EditText nameInput;
    private EditText addressLine1Input;
    private EditText addressLine2Input;
    private EditText cityInput;
    private EditText stateInput;
    private EditText pinCodeInput;
    private EditText phoneNumberInput;
    private EditText alternatePhoneInput;
    private EditText landmarkInput;
    private Spinner farmerSpinner;
    private TextView farmerLabelText;
    private Button saveButton;
    private ImageView backButton;
    
    private SessionManager sessionManager;
    private SharedPreferences sharedPreferences;
    private List<Map<String, Object>> farmersList = new ArrayList<>();
    private List<String> farmerNames = new ArrayList<>();
    private Map<String, String> farmerNameToIdMap = new HashMap<>();
    
    public DeliveryFragment() {
        // Required empty public constructor
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        sharedPreferences = requireActivity().getSharedPreferences(PREF_DELIVERY_ADDRESSES, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery, container, false);
        
        // Initialize views
        initializeViews(view);
        
        // Check if user is a member
        if (!Constants.MEMBER.equals(sessionManager.getUserType())) {
            Toast.makeText(requireContext(), "Only members can add delivery addresses", Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }
        
        // Load existing address if available
        loadSavedAddress();
        
        // Setup back button
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        // Setup save button
        saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveAddress();
            }
        });
        
        // Load farmers from Firestore
        loadFarmers();
        
        return view;
    }
    
    private void initializeViews(View view) {
        nameInput = view.findViewById(R.id.input_full_name);
        addressLine1Input = view.findViewById(R.id.input_address_line1);
        addressLine2Input = view.findViewById(R.id.input_address_line2);
        cityInput = view.findViewById(R.id.input_city);
        stateInput = view.findViewById(R.id.input_state);
        pinCodeInput = view.findViewById(R.id.input_pincode);
        phoneNumberInput = view.findViewById(R.id.input_phone);
        alternatePhoneInput = view.findViewById(R.id.input_alternate_phone);
        landmarkInput = view.findViewById(R.id.input_landmark);
        farmerSpinner = view.findViewById(R.id.spinner_farmer);
        farmerLabelText = view.findViewById(R.id.text_farmer_label);
        saveButton = view.findViewById(R.id.button_save_address);
        backButton = view.findViewById(R.id.back_button);
    }
    
    private void loadFarmers() {
        FirestoreHelper.getAllFarmers(new FirestoreHelper.FarmersCallback() {
            @Override
            public void onFarmersLoaded(List<Map<String, Object>> farmers) {
                farmersList = farmers;
                farmerNames.clear();
                farmerNameToIdMap.clear();
                
                // Add a default option
                farmerNames.add("Select a Farmer");
                
                // Add all farmers to the list
                for (Map<String, Object> farmer : farmers) {
                    String name = (String) farmer.get("name");
                    String userId = (String) farmer.get("userId");
                    if (name != null && userId != null) {
                        farmerNames.add(name);
                        farmerNameToIdMap.put(name, userId);
                    }
                }
                
                // Create adapter for spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(), 
                        android.R.layout.simple_spinner_item, 
                        farmerNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                farmerSpinner.setAdapter(adapter);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), 
                        "Error loading farmers: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate required fields
        if (TextUtils.isEmpty(nameInput.getText())) {
            nameInput.setError("Full name is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(addressLine1Input.getText())) {
            addressLine1Input.setError("Address is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(cityInput.getText())) {
            cityInput.setError("City is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(stateInput.getText())) {
            stateInput.setError("State is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(pinCodeInput.getText())) {
            pinCodeInput.setError("PIN code is required");
            isValid = false;
        } else if (pinCodeInput.getText().length() != 6) {
            pinCodeInput.setError("PIN code must be 6 digits");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(phoneNumberInput.getText())) {
            phoneNumberInput.setError("Phone number is required");
            isValid = false;
        } else if (phoneNumberInput.getText().length() < 10) {
            phoneNumberInput.setError("Enter a valid phone number");
            isValid = false;
        }
        
        // Validate farmer selection
        if (farmerSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(requireContext(), "Please select a farmer", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        return isValid;
    }
    
    private void saveAddress() {
        // Get the user ID
        String userId = sessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected farmer
        String selectedFarmerName = farmerSpinner.getSelectedItem().toString();
        String selectedFarmerId = farmerNameToIdMap.get(selectedFarmerName);
        
        // Create delivery data map
        Map<String, Object> deliveryData = new HashMap<>();
        deliveryData.put("userId", userId);
        deliveryData.put("name", nameInput.getText().toString());
        deliveryData.put("addressLine1", addressLine1Input.getText().toString());
        deliveryData.put("addressLine2", addressLine2Input.getText().toString());
        deliveryData.put("city", cityInput.getText().toString());
        deliveryData.put("state", stateInput.getText().toString());
        deliveryData.put("pinCode", pinCodeInput.getText().toString());
        deliveryData.put("phone", phoneNumberInput.getText().toString());
        deliveryData.put("alternatePhone", alternatePhoneInput.getText().toString());
        deliveryData.put("landmark", landmarkInput.getText().toString());
        deliveryData.put("farmerId", selectedFarmerId);
        deliveryData.put("farmerName", selectedFarmerName);
        deliveryData.put("timestamp", System.currentTimeMillis());
        
        // Save to Firestore
        FirestoreHelper.saveDeliveryAddress(deliveryData, new FirestoreHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Delivery address saved successfully", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), 
                    "Error saving delivery address: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadSavedAddress() {
        // Get the user ID
        String userId = sessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            return; // No user ID available
        }
        
        // Load address
        String addressJson = sharedPreferences.getString(userId, null);
        if (addressJson == null) {
            return; // No saved address
        }
        
        try {
            // Simple parsing of the JSON (using a proper JSON library would be better)
            String name = extractValue(addressJson, "name");
            String addressLine1 = extractValue(addressJson, "addressLine1");
            String addressLine2 = extractValue(addressJson, "addressLine2");
            String city = extractValue(addressJson, "city");
            String state = extractValue(addressJson, "state");
            String pinCode = extractValue(addressJson, "pinCode");
            String phone = extractValue(addressJson, "phone");
            String alternatePhone = extractValue(addressJson, "alternatePhone");
            String landmark = extractValue(addressJson, "landmark");
            String farmerName = extractValue(addressJson, "farmerName");
            
            // Populate the form
            nameInput.setText(name);
            addressLine1Input.setText(addressLine1);
            addressLine2Input.setText(addressLine2);
            cityInput.setText(city);
            stateInput.setText(state);
            pinCodeInput.setText(pinCode);
            phoneNumberInput.setText(phone);
            alternatePhoneInput.setText(alternatePhone);
            landmarkInput.setText(landmark);
            
            // We'll set the farmer spinner after the data is loaded in loadFarmers()
        } catch (Exception e) {
            // Handle parsing error
            Toast.makeText(requireContext(), "Error loading saved address", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String extractValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\":\"");
        if (keyIndex == -1) return "";
        
        int valueStartIndex = json.indexOf("\":\"", keyIndex) + 3;
        int valueEndIndex = json.indexOf("\"", valueStartIndex);
        
        return json.substring(valueStartIndex, valueEndIndex);
    }
} 
package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.projects.agroyard.R;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PaymentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PaymentFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView selectedFarmerName;
    private TextView selectedFarmerBusiness;
    private TextView selectedFarmerLocation;
    private TextView selectedFarmerPhone;
    private TextView paymentFarmerName;
    private TextView paymentAmountDisplay;
    private EditText paymentAmountInput;
    private EditText paymentPurposeInput;
    private EditText upiIdInput;
    private Button payNowButton;
    private LinearLayout selectedFarmerLayout;
    private LinearLayout farmersListLayout;

    private boolean isSelectingFarmer = false;

    public PaymentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PaymentFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PaymentFragment newInstance(String param1, String param2) {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);

        // Initialize views
        initializeViews(view);
        
        // Set up listeners
        setupListeners();
        
        // Set up farmer selection
        setupFarmerSelection(view);
        
        return view;
    }

    private void initializeViews(View view) {
        selectedFarmerName = view.findViewById(R.id.selected_farmer_name);
        selectedFarmerBusiness = view.findViewById(R.id.selected_farmer_business);
        selectedFarmerLocation = view.findViewById(R.id.selected_farmer_location);
        selectedFarmerPhone = view.findViewById(R.id.selected_farmer_phone);
        paymentFarmerName = view.findViewById(R.id.payment_farmer_name);
        paymentAmountDisplay = view.findViewById(R.id.payment_amount_display);
        paymentAmountInput = view.findViewById(R.id.payment_amount_input);
        paymentPurposeInput = view.findViewById(R.id.payment_purpose_input);
        upiIdInput = view.findViewById(R.id.upi_id_input);
        payNowButton = view.findViewById(R.id.pay_now_button);
        selectedFarmerLayout = view.findViewById(R.id.selected_farmer_layout);
        farmersListLayout = view.findViewById(R.id.farmers_list_layout);
    }

    private void setupListeners() {
        // Farmer selection toggle
        selectedFarmerLayout.setOnClickListener(v -> toggleFarmerSelection());
        
        // Payment amount input watcher
        paymentAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePaymentAmountDisplay(s.toString());
            }
        });
        
        // Pay button click listener
        payNowButton.setOnClickListener(v -> processPayment());
    }
    
    private void setupFarmerSelection(View view) {
        // Find all farmer items in the layout
        int childCount = farmersListLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View farmerView = farmersListLayout.getChildAt(i);
            setupFarmerItemClickListener(farmerView);
        }
    }
    
    private void setupFarmerItemClickListener(View farmerView) {
        farmerView.setOnClickListener(v -> {
            // Find the TextViews within this farmer item
            LinearLayout infoLayout = (LinearLayout) ((LinearLayout) v).getChildAt(1);
            
            String name = ((TextView) infoLayout.getChildAt(0)).getText().toString();
            String business = ((TextView) infoLayout.getChildAt(1)).getText().toString();
            
            LinearLayout locationPhoneLayout = (LinearLayout) infoLayout.getChildAt(2);
            String location = ((TextView) locationPhoneLayout.getChildAt(0)).getText().toString();
            String phone = ((TextView) locationPhoneLayout.getChildAt(2)).getText().toString();
            
            // Select this farmer
            selectFarmer(name, business, location, phone);
        });
    }
    
    private void toggleFarmerSelection() {
        isSelectingFarmer = !isSelectingFarmer;
        farmersListLayout.setVisibility(isSelectingFarmer ? View.VISIBLE : View.GONE);
    }
    
    private void updatePaymentAmountDisplay(String amountStr) {
        try {
            if (amountStr.isEmpty()) {
                paymentAmountDisplay.setText("₹0.00");
                return;
            }
            
            double amount = Double.parseDouble(amountStr);
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            paymentAmountDisplay.setText(format.format(amount));
        } catch (NumberFormatException e) {
            paymentAmountDisplay.setText("₹0.00");
        }
    }
    
    private void processPayment() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }
        
        // In a real app, this would handle the UPI payment processing
        // For now, we'll just show a success message
        Toast.makeText(requireContext(), 
                "Payment initiated successfully!", 
                Toast.LENGTH_SHORT).show();
        
        // Clear form after successful payment
        clearForm();
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate payment amount
        if (paymentAmountInput.getText().toString().trim().isEmpty()) {
            paymentAmountInput.setError("Please enter the payment amount");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(paymentAmountInput.getText().toString());
                if (amount <= 0) {
                    paymentAmountInput.setError("Amount must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                paymentAmountInput.setError("Please enter a valid amount");
                isValid = false;
            }
        }
        
        // Validate payment purpose
        if (paymentPurposeInput.getText().toString().trim().isEmpty()) {
            paymentPurposeInput.setError("Please enter payment purpose");
            isValid = false;
        }
        
        // Validate UPI ID
        String upiId = upiIdInput.getText().toString().trim();
        if (upiId.isEmpty()) {
            upiIdInput.setError("Please enter your UPI ID");
            isValid = false;
        } else if (!upiId.contains("@")) {
            upiIdInput.setError("Please enter a valid UPI ID (e.g., name@bank)");
            isValid = false;
        }
        
        return isValid;
    }
    
    private void clearForm() {
        paymentAmountInput.setText("");
        paymentPurposeInput.setText("");
        upiIdInput.setText("");
        updatePaymentAmountDisplay("");
    }
    
    private void selectFarmer(String name, String business, String location, String phone) {
        // Update selected farmer details
        selectedFarmerName.setText(name);
        selectedFarmerBusiness.setText(business);
        selectedFarmerLocation.setText(location);
        selectedFarmerPhone.setText(phone);
        
        // Update payment summary
        paymentFarmerName.setText(name);
        
        // Hide farmer list
        toggleFarmerSelection();
        
        // Show toast confirmation
        Toast.makeText(requireContext(), 
                name + " selected for payment", 
                Toast.LENGTH_SHORT).show();
    }
}
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.models.Receipt;
import com.projects.agroyard.services.ReceiptService;
import com.projects.agroyard.utils.SessionManager;

import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.text.NumberFormat;
import java.util.Locale;

public class ReceiptsFragment extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_TYPE = "userType";
    private static final String TAG = "ReceiptsFragment";
    
    private String userType;
    private TextView titleText;
    private TextView emptyView;
    private LinearLayout receiptsList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ReceiptService receiptService;
    private SessionManager sessionManager;
    
    // Track already displayed receipt IDs to prevent duplicates
    private Set<String> displayedReceiptIds = new HashSet<>();
    
    // Add button to create test receipt
    private View createTestButton;
    
    public ReceiptsFragment() {
        // Required empty public constructor
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize session manager
        sessionManager = new SessionManager(requireContext());
        
        // Get user type from SessionManager first, fallback to SharedPreferences
        userType = sessionManager.getUserType();
        Log.d("RECEIPT_DEBUG", "Initial userType from SessionManager: " + userType);
        
        if (userType == null || userType.isEmpty()) {
            // Fallback to SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userType = prefs.getString(KEY_USER_TYPE, Constants.FARMER); // Default to Farmer if not set
            Log.d("RECEIPT_DEBUG", "Fallback userType from SharedPreferences: " + userType);
        }
        
        // Initialize receipt service with context
        receiptService = new ReceiptService(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receipts, container, false);
        
        try {
            titleText = view.findViewById(R.id.title_text);
            emptyView = view.findViewById(R.id.empty_view);
            receiptsList = view.findViewById(R.id.receipts_list);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
            
            // Safety check for views
            if (receiptsList == null || swipeRefreshLayout == null) {
                Log.e("RECEIPT_DEBUG", "Critical views are null! Check your layout XML");
                return view;
            }
            
            // Set title to show all transactions
            if (titleText != null) titleText.setText("Your Transaction Receipts");
            
            // Set up swipe refresh
            swipeRefreshLayout.setOnRefreshListener(this::loadReceipts);
            
            // Safely add the button
            try {
                // Create button programmatically
                createTestButton = new android.widget.Button(requireContext());
                ((android.widget.Button) createTestButton).setText("Create Real Receipt");
                createTestButton.setOnClickListener(v -> createTestReceipt());
                
                // Add to layout before receipts list
                ViewGroup parent = (ViewGroup) receiptsList.getParent();
                if (parent != null) {
                    int index = parent.indexOfChild(receiptsList);
                    parent.addView(createTestButton, index);
                } else {
                    Log.e("RECEIPT_DEBUG", "Parent view is null, can't add button");
                }
            } catch (Exception e) {
                Log.e("RECEIPT_DEBUG", "Error adding button: " + e.getMessage());
            }
            
            // Try to load real receipts (if they exist)
            loadReceipts();
            
        } catch (Exception e) {
            Log.e("RECEIPT_DEBUG", "Error in onCreateView: " + e.getMessage(), e);
            // Show error toast
            Toast.makeText(requireContext(), "Error loading receipts view", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }
    
    private void loadReceipts() {
        try {
            // Clear existing receipts
            if (receiptsList != null) {
                receiptsList.removeAllViews();
            }
            
            // Clear the set of displayed receipt IDs
            displayedReceiptIds.clear();
            
            // Set views to correct initial state
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
            
            // Show loading indicator
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
            
            // Hide demo button while loading
            if (createTestButton != null) {
                createTestButton.setVisibility(View.GONE);
            }
            
            // Check if we're not attached to activity
            if (getActivity() == null || !isAdded()) {
                Log.e("RECEIPT_DEBUG", "Fragment not attached to activity");
                return;
            }
            
            // CRITICAL FIX: Re-check userType to ensure it's set properly
            if (userType == null || userType.isEmpty()) {
                userType = sessionManager.getUserType();
                Log.d("RECEIPT_DEBUG", "Re-checking userType from SessionManager: " + userType);
                
                if (userType == null || userType.isEmpty()) {
                    // Fallback to SharedPreferences
                    SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    userType = prefs.getString(KEY_USER_TYPE, Constants.FARMER); // Default to Farmer if not set
                    Log.d("RECEIPT_DEBUG", "Re-checked fallback userType from SharedPreferences: " + userType);
                }
            }
            
            // Get user phone number from SessionManager
            String userPhone = sessionManager.getMobile();
            
            // Check if we have a phone number
            if (userPhone == null || userPhone.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Phone number not available. Please update your profile.", Toast.LENGTH_SHORT).show();
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                        if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                    });
                }
                return;
            }
            
            Log.d("RECEIPT_DEBUG", "========= RECEIPT DEBUGGING =========");
            Log.d("RECEIPT_DEBUG", "Current user phone: " + userPhone);
            Log.d("RECEIPT_DEBUG", "User type: " + userType);
            Log.d("RECEIPT_DEBUG", "User name: " + sessionManager.getName());
            
            // Determine user type from SessionManager
            // CRITICAL FIX: Use more flexible condition and show both types for debugging
            boolean isFarmer = Constants.FARMER.equalsIgnoreCase(userType);
            boolean isMember = Constants.MEMBER.equalsIgnoreCase(userType);
            
            // EMERGENCY FIX: If user type is still not recognized, show both types of receipts
            if (!isFarmer && !isMember) {
                Log.d("RECEIPT_DEBUG", "⚠️ User type not recognized! Showing all receipt types as fallback");
                isFarmer = true;
                isMember = true;
            }
            
            Log.d("RECEIPT_DEBUG", "Is Farmer: " + isFarmer + ", Is Member: " + isMember);
            
            // Store for lambda
            final boolean finalIsFarmer = isFarmer;
            final boolean finalIsMember = isMember;
            
            // Load all receipts for this user by phone number (both as farmer and member)
            Log.d("RECEIPT_DEBUG", "Attempting to load ALL receipts for user by phone");
            receiptService.getAllReceiptsByPhone()
                .addOnSuccessListener(receipts -> {
                        if (!isAdded() || getActivity() == null) {
                            Log.e("RECEIPT_DEBUG", "Fragment detached during query");
                            return;
                        }
                        
                    Log.d("RECEIPT_DEBUG", "Query successful, receipt count: " + receipts.size());
                    
                    // Debug each receipt
                    for (Receipt receipt : receipts) {
                        Log.d("RECEIPT_DEBUG", "Receipt: " + receipt.getProductName() + 
                              ", Farmer: " + receipt.getFarmerName() + " (" + receipt.getFarmerPhone() + ")" +
                              ", Member: " + receipt.getMemberName() + " (" + receipt.getMemberPhone() + ")");
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (receipts.isEmpty()) {
                            Log.d("RECEIPT_DEBUG", "No receipts found for user");
                            // Show empty view
                            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                            if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        } else {
                            // Sort receipts by timestamp (newest first)
                            receipts.sort((r1, r2) -> {
                                // Handle null timestamps (put them at the end)
                                if (r1.getTimestamp() == null) return 1;
                                if (r2.getTimestamp() == null) return -1;
                                // Compare timestamps (reverse order for newest first)
                                return r2.getTimestamp().compareTo(r1.getTimestamp());
                            });
                            
                            // Separate receipts into farmer and member categories
                            List<Receipt> farmerReceipts = new ArrayList<>();
                            List<Receipt> memberReceipts = new ArrayList<>();
                            
                            // Get current user phone
                            String currentUserPhone = sessionManager.getMobile();
                            Log.d("RECEIPT_DEBUG", "Current user phone for filtering: " + currentUserPhone);
                            
                            for (Receipt receipt : receipts) {
                                if (currentUserPhone != null && currentUserPhone.equals(receipt.getFarmerPhone())) {
                                    farmerReceipts.add(receipt);
                                    Log.d("RECEIPT_DEBUG", "Added to farmer receipts: " + receipt.getProductName());
                                } else if (currentUserPhone != null && currentUserPhone.equals(receipt.getMemberPhone())) {
                                    memberReceipts.add(receipt);
                                    Log.d("RECEIPT_DEBUG", "Added to member receipts: " + receipt.getProductName());
                                } else {
                                    Log.d("RECEIPT_DEBUG", "⚠️ Receipt doesn't match user's phone: " + 
                                          receipt.getProductName() + ", Farmer phone: " + receipt.getFarmerPhone() + 
                                          ", Member phone: " + receipt.getMemberPhone());
                                }
                            }
                            
                            // Display all receipts in separate sections
                            Log.d("RECEIPT_DEBUG", "Found " + receipts.size() + " receipts in total: " + 
                                  farmerReceipts.size() + " as farmer, " + memberReceipts.size() + " as member");
                            
                            if (emptyView != null) emptyView.setVisibility(View.GONE);
                            
                            // FIXED: Use the lambda-captured finals
                            boolean showedAnyReceipts = false;
                            
                            // Only show farmer receipts if the user is a farmer
                            if (finalIsFarmer && !farmerReceipts.isEmpty()) {
                                addSectionHeader("Products You Sold", R.color.colorPrimary);
                                for (Receipt receipt : farmerReceipts) {
                                    displayReceipt(receipt);
                                    showedAnyReceipts = true;
                                }
                            }
                            
                            // Only show member receipts if the user is a member
                            if (finalIsMember && !memberReceipts.isEmpty()) {
                                addSectionHeader("Products You Purchased", R.color.colorAccent);
                                for (Receipt receipt : memberReceipts) {
                                    displayReceipt(receipt);
                                    showedAnyReceipts = true;
                                }
                            }
                            
                            // If this user has receipts, but they're not shown due to user type, or no receipts at all
                            if (!showedAnyReceipts) {
                                Log.d("RECEIPT_DEBUG", "⚠️ No receipts were displayed despite having " + 
                                      receipts.size() + " total receipts!");
                                
                                if (emptyView != null) {
                                    emptyView.setVisibility(View.VISIBLE);
                                    if (finalIsFarmer) {
                                        emptyView.setText("No sales receipts found");
                                    } else if (finalIsMember) {
                                        emptyView.setText("No purchase receipts found");
            } else {
                                        emptyView.setText("No receipts found");
                                    }
                                }
                                
                                // EMERGENCY FIX: If no receipts shown but we have receipts, show all of them
                                if (!receipts.isEmpty()) {
                                    Log.d("RECEIPT_DEBUG", "⚠️ EMERGENCY FIX: Showing all receipts despite user type");
                                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                                    
                                    // Show farmer receipts
                                    if (!farmerReceipts.isEmpty()) {
                                        addSectionHeader("Your Sales (Emergency View)", R.color.colorPrimary);
                                        for (Receipt receipt : farmerReceipts) {
                                            displayReceipt(receipt);
                                        }
                                    }
                                    
                                    // Show member receipts
                                    if (!memberReceipts.isEmpty()) {
                                        addSectionHeader("Your Purchases (Emergency View)", R.color.colorAccent);
                                        for (Receipt receipt : memberReceipts) {
                                            displayReceipt(receipt);
                                        }
                                    }
                                }
                            }
                            
                            // Hide loading indicator
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                        }
                    });
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getActivity() == null) return;
                        
                        Log.e("RECEIPT_DEBUG", "Query FAILED: " + e.getMessage());
                        Log.e("RECEIPT_DEBUG", "Error details: ", e);
                        handleError(e);
                    });
        } catch (Exception e) {
            Log.e("RECEIPT_DEBUG", "Critical error in loadReceipts: " + e.getMessage(), e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error loading receipts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                    if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                });
            }
        }
    }
    
    /**
     * Add a section header to separate different types of receipts
     */
    private void addSectionHeader(String title, int colorResId) {
        TextView headerView = new TextView(requireContext());
        
        // Set layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 32, 0, 16); // top margin of 32dp, bottom margin of 16dp
        headerView.setLayoutParams(params);
        
        // Set text and appearance
        headerView.setText(title);
        headerView.setTextSize(20); // 20sp
        headerView.setTypeface(null, android.graphics.Typeface.BOLD);
        headerView.setTextColor(getResources().getColor(colorResId));
        headerView.setPadding(32, 8, 8, 8); // left padding of 32dp
        
        // Add a bottom border
        headerView.setBackgroundResource(R.drawable.border_bottom);
        
        // Add to the receipts list
        receiptsList.addView(headerView);
    }
    
    private void processReceipts(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        if (getActivity() == null || !isAdded()) {
            Log.e("RECEIPT_DEBUG", "Activity is null or fragment detached, can't process receipts");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            try {
                List<Receipt> receipts = new ArrayList<>();
                // Track receipt IDs at this level too
                Set<String> uniqueIds = new HashSet<>();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        Receipt receipt = document.toObject(Receipt.class);
                        if (receipt != null) {
                            String receiptId = receipt.getId();
                            // Only add if we haven't seen this ID before
                            if (receiptId == null || !uniqueIds.contains(receiptId)) {
                                if (receiptId != null) {
                                    uniqueIds.add(receiptId);
                                }
                        receipts.add(receipt);
                        Log.d("RECEIPT_DEBUG", "Receipt processed: " + receipt.getProductName() + 
                              ", ID: " + receipt.getId());
                            } else {
                                Log.d("RECEIPT_DEBUG", "Skipped duplicate receipt: " + receiptId);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("RECEIPT_DEBUG", "Error converting document: " + e.getMessage());
                        Log.e("RECEIPT_DEBUG", "Document data: " + document.getData().toString());
                    }
                }
                
                // Add receipts to the list
                if (receipts.isEmpty()) {
                    // If no receipts, show empty view
                    Log.d("RECEIPT_DEBUG", "No receipts found, showing empty view");
                    if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                } else {
                    // Sort receipts by timestamp (newest first)
                    receipts.sort((r1, r2) -> {
                        // Handle null timestamps (put them at the end)
                        if (r1.getTimestamp() == null) return 1;
                        if (r2.getTimestamp() == null) return -1;
                        // Compare timestamps (reverse order for newest first)
                        return r2.getTimestamp().compareTo(r1.getTimestamp());
                    });
                    
                    // Add the receipts to the view
                    Log.d("RECEIPT_DEBUG", "Found " + receipts.size() + " receipts, displaying newest first");
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    if (receiptsList != null) {
                        for (Receipt receipt : receipts) {
                            displayReceipt(receipt);
                        }
                    }
                }
                
                // Hide loading indicator
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e("RECEIPT_DEBUG", "Error processing receipts: " + e.getMessage(), e);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void handleError(Exception e) {
        if (getActivity() == null || !isAdded()) return;
        
        getActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error loading receipts", e);
            Toast.makeText(requireContext(), "Error loading receipts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Show empty view
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
            if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
            
            // Hide loading indicator
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        });
    }
    
    private void displayReceipt(Receipt receipt) {
        // Check if this receipt has already been displayed to prevent duplicates
        String receiptId = receipt.getId();
        if (receiptId != null && !receiptId.isEmpty()) {
            // Skip if already displayed (prevents duplicates)
            if (displayedReceiptIds.contains(receiptId)) {
                Log.d("RECEIPT_DEBUG", "Skipping duplicate receipt: " + receiptId);
                return;
            }
            
            // Add to tracking set
            displayedReceiptIds.add(receiptId);
        } else {
            // If receipt has no ID, use a combination of fields to create a unique identifier
            String syntheticId = receipt.getProductId() + "_" + 
                                receipt.getFarmerPhone() + "_" + 
                                receipt.getMemberPhone() + "_" + 
                                receipt.getQuantity() + "_" + 
                                receipt.getPricePerKg();
            
            if (displayedReceiptIds.contains(syntheticId)) {
                Log.d("RECEIPT_DEBUG", "Skipping duplicate receipt with synthetic ID: " + syntheticId);
                return;
            }
            
            // Add to tracking set
            displayedReceiptIds.add(syntheticId);
        }
        
        View receiptItem = getLayoutInflater().inflate(R.layout.item_receipt, receiptsList, false);
        
        TextView productNameView = receiptItem.findViewById(R.id.product_name);
        TextView quantityView = receiptItem.findViewById(R.id.quantity);
        TextView pricePerKgView = receiptItem.findViewById(R.id.price_per_kg);
        TextView totalPriceView = receiptItem.findViewById(R.id.total_price);
        TextView personNameView = receiptItem.findViewById(R.id.person_name);
        TextView phoneNumberView = receiptItem.findViewById(R.id.phone_number);
        TextView dateTimeView = receiptItem.findViewById(R.id.receipt_date_time);
        
        // Calculate and format the total price
        // Use the new method that ensures proper multiplication
        long totalPrice = receipt.getTotalPriceLong(); // Use long to avoid overflow
        
        // Direct formatting without currency formatter to ensure accuracy
        String formattedTotalPrice = "₹" + totalPrice;
        
        // Log the values for debugging
        Log.d("RECEIPT_DEBUG", "Product: " + receipt.getProductName());
        Log.d("RECEIPT_DEBUG", "Quantity: " + receipt.getQuantity());
        Log.d("RECEIPT_DEBUG", "Price per kg: " + receipt.getPricePerKg());
        Log.d("RECEIPT_DEBUG", "Total price calculation: " + receipt.getQuantity() + " * " + receipt.getPricePerKg() + " = " + totalPrice);
        
        // Set product information
        productNameView.setText(receipt.getProductName());
        quantityView.setText("Quantity: " + receipt.getQuantity() + "kg");
        pricePerKgView.setText("Price: ₹" + receipt.getPricePerKg() + "/kg");
        totalPriceView.setText(formattedTotalPrice);
        
        // Format and set the date and time
        if (receipt.getTimestamp() != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            String formattedDate = receipt.getTimestamp();
            dateTimeView.setText(formattedDate);
        } else {
            dateTimeView.setText("Date not available");
        }
        
        // Get the current user's phone number
        String currentUserPhone = sessionManager.getMobile();
        
        // ENHANCEMENT: Add a TextView to clearly indicate the transaction type (sale/purchase)
        TextView transactionTypeView = new TextView(requireContext());
        transactionTypeView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        transactionTypeView.setPadding(16, 8, 16, 8);
        transactionTypeView.setTextSize(16);
        transactionTypeView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Check if this receipt is for the user as a farmer or as a member
        boolean userIsFarmer = currentUserPhone != null && currentUserPhone.equals(receipt.getFarmerPhone());
        boolean userIsMember = currentUserPhone != null && currentUserPhone.equals(receipt.getMemberPhone());
        
        if (userIsFarmer) {
            // User is the farmer (seller) in this transaction
            transactionTypeView.setText("YOU SOLD");
            transactionTypeView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            
            // Show member (buyer) information
            personNameView.setText("Buyer: " + receipt.getMemberName());
            phoneNumberView.setText("Contact: " + receipt.getMemberPhone());
        } else if (userIsMember) {
            // User is the member (buyer) in this transaction
            transactionTypeView.setText("YOU PURCHASED");
            transactionTypeView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            
            // Show farmer (seller) information
            personNameView.setText("Seller: " + receipt.getFarmerName());
            phoneNumberView.setText("Contact: " + receipt.getFarmerPhone());
        } else {
            // Edge case - user is neither (shouldn't happen, but just in case)
            transactionTypeView.setText("TRANSACTION");
            transactionTypeView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            
            // Show both parties
            personNameView.setText("Seller: " + receipt.getFarmerName() + " | Buyer: " + receipt.getMemberName());
            phoneNumberView.setText("Seller: " + receipt.getFarmerPhone() + " | Buyer: " + receipt.getMemberPhone());
        }
        
        // Add the transaction type view at the top of the receipt item
        if (receiptItem instanceof ViewGroup) {
            ViewGroup itemRoot = (ViewGroup) receiptItem;
            itemRoot.addView(transactionTypeView, 0);
        }
        
        receiptsList.addView(receiptItem);
    }
    
    // Fallback methods for demo purposes
    private void addFarmerReceipts() {
        // Clear any existing receipt items first
        receiptsList.removeAllViews();
        
        // Add dummy data that's clearly labeled as such
        addReceiptItem("Rice (DEMO)", 50, 50, "Demo Member", "+91 9876543210");
        addReceiptItem("Wheat (DEMO)", 100, 42, "Demo Member", "+91 9876543211");
        
        // Hide empty view if there are receipts
        if (receiptsList.getChildCount() > 0) {
            emptyView.setVisibility(View.GONE);
        }
        
        // Show toast to indicate this is demo data
        Toast.makeText(requireContext(), "Showing demo receipts (real receipts not available)", Toast.LENGTH_LONG).show();
    }
    
    private void addMemberReceipts() {
        // Clear any existing receipt items first
        receiptsList.removeAllViews();
        
        // Add dummy data that's clearly labeled as such
        addReceiptItem("Rice (DEMO)", 50, 50, "Demo Farmer", "+91 9876543212");
        addReceiptItem("Tomatoes (DEMO)", 25, 72, "Demo Farmer", "+91 9876543213");
        
        // Hide empty view if there are receipts
        if (receiptsList.getChildCount() > 0) {
            emptyView.setVisibility(View.GONE);
        }
        
        // Show toast to indicate this is demo data
        Toast.makeText(requireContext(), "Showing demo receipts (real receipts not available)", Toast.LENGTH_LONG).show();
    }
    
    private void addReceiptItem(String productName, int quantity, int pricePerKg, String personName, String phoneNumber) {
        View receiptItem = getLayoutInflater().inflate(R.layout.item_receipt, receiptsList, false);
        
        TextView productNameView = receiptItem.findViewById(R.id.product_name);
        TextView quantityView = receiptItem.findViewById(R.id.quantity);
        TextView pricePerKgView = receiptItem.findViewById(R.id.price_per_kg);
        TextView totalPriceView = receiptItem.findViewById(R.id.total_price);
        TextView personNameView = receiptItem.findViewById(R.id.person_name);
        TextView phoneNumberView = receiptItem.findViewById(R.id.phone_number);
        TextView dateTimeView = receiptItem.findViewById(R.id.receipt_date_time);
        
        // Calculate total price using long to avoid overflow issues
        long totalPrice = (long) quantity * (long) pricePerKg;
        
        // Direct formatting without currency formatter to ensure accuracy
        String formattedTotalPrice = "₹" + totalPrice;
        
        // Set values
        productNameView.setText(productName);
        quantityView.setText("Quantity: " + quantity + "kg");
        pricePerKgView.setText("Price: ₹" + pricePerKg + "/kg");
        totalPriceView.setText(formattedTotalPrice);
        personNameView.setText(personName);
        phoneNumberView.setText(phoneNumber);
        
        // Set current date and time for sample receipts
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        String formattedDate = dateFormat.format(new java.util.Date());
        dateTimeView.setText(formattedDate);
        
        receiptsList.addView(receiptItem);
    }
    
    // Add method to create a test receipt
    private void createTestReceipt() {
        // Show loading
        swipeRefreshLayout.setRefreshing(true);
        
        // Get current user ID and info from SessionManager
        String currentUserId = sessionManager.getUserId();
        String userName = sessionManager.getName();
        String userPhone = sessionManager.getMobile();
        
        // Check if we have a phone number
        if (userPhone == null || userPhone.isEmpty()) {
            Toast.makeText(requireContext(), "Phone number not available. Please update your profile.", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        // If no user ID in SessionManager, fall back to auth
        if (currentUserId == null || currentUserId.isEmpty()) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "You must be logged in to create receipts", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            currentUserId = auth.getCurrentUser().getUid();
        }
        
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "You must be logged in to create receipts", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        String productId = "testProduct" + System.currentTimeMillis();
        String productName = "Test Product (Real)";
        int quantity = 75;
        int pricePerKg = 60;
        
        // Create receipt based on user type
        if (userType.equals(Constants.FARMER)) {
            // Create a receipt where current user is the farmer
            receiptService.createReceiptAsFarmer(
                requireContext(),
                productId,
                productName,
                quantity,
                pricePerKg,
                "testMember" + System.currentTimeMillis(),
                "Test Member",
                "+91 8765432101" // Use a different test phone number
            )
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(requireContext(), 
                    "Test receipt created! Pull down to refresh. New receipts appear at the top.",
                    Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
                
                // Log success
                Log.d("RECEIPT_DEBUG", "Test receipt created with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(),
                    "Failed to create test receipt: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
                
                // Log error
                Log.e("RECEIPT_DEBUG", "Error creating test receipt", e);
            });
        } else {
            // Create a receipt where current user is the member
            receiptService.createReceiptAsMember(
                requireContext(),
                productId,
                productName,
                quantity,
                pricePerKg,
                "testFarmer" + System.currentTimeMillis(),
                "Test Farmer",
                "+91 8765432102" // Use a different test phone number
            )
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(requireContext(), 
                    "Test receipt created! Pull down to refresh. New receipts appear at the top.",
                    Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
                
                // Log success
                Log.d("RECEIPT_DEBUG", "Test receipt created with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(),
                    "Failed to create test receipt: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
                
                // Log error
                Log.e("RECEIPT_DEBUG", "Error creating test receipt", e);
            });
        }
    }
} 
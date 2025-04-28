package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.models.Product;
import com.projects.agroyard.models.Receipt;
import com.projects.agroyard.services.ReceiptService;
import com.projects.agroyard.utils.FirestoreHelper;
import com.projects.agroyard.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;

import java.util.UUID;

public class BettingFragment extends Fragment {
    private static final String TAG = "BettingFragment";

    private LinearLayout productCardsContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SessionManager sessionManager;
    private String currentUserMobile;
    private String currentUserName;

    private static final long INITIAL_BIDDING_TIME = 90000; // 90 seconds (1:30 minutes) in milliseconds
    private static final long SUBSEQUENT_BIDDING_TIME = 90000; // 90 seconds (1:30 minutes) in milliseconds
    private static final float MINIMUM_BID_INCREMENT = 1.0f; // Minimum bid increment in rupees

    // Maps to track product-specific timers, bid status, and bids
    private Map<Integer, CountDownTimer> productTimers = new HashMap<>();
    private Map<Integer, Boolean> productBidStatus = new HashMap<>();
    private Map<Integer, Boolean> productBiddingActive = new HashMap<>();
    private Map<Integer, TextView> productTimerViews = new HashMap<>();
    private Map<Integer, EditText> productBidInputs = new HashMap<>();
    private Map<Integer, TextView> productYourBidViews = new HashMap<>();
    private Map<Integer, TextView> productCurrentBidViews = new HashMap<>();
    private Map<String, ListenerRegistration> bidListeners = new HashMap<>();
    private Map<String, ListenerRegistration> timerListeners = new HashMap<>();
    private Map<String, Long> productEndTimes = new HashMap<>();

    private List<Product> productList = new ArrayList<>();
    private static final String API_URL = Constants.DB_URL_BASE + "/get_products.php"; // Legacy HTTP endpoint
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_betting_new, container, false);
        
        // Initialize session manager
        sessionManager = new SessionManager(requireContext());
        currentUserMobile = sessionManager.getMobile();
        currentUserName = sessionManager.getName();
        
        // Initialize views
        productCardsContainer = view.findViewById(R.id.product_cards_container);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::fetchBiddableProducts);

        // Fetch all products for bidding
        fetchBiddableProducts();

        return view;
    }

    private void fetchBiddableProducts() {
        swipeRefreshLayout.setRefreshing(true);
        
        // Clear previous product data
        productList.clear();
        
        // Clear all listeners first
        clearBidListeners();
        clearTimerListeners();
        
        Log.d(TAG, "Fetching biddable products from Firestore...");
        
        // Force refresh from server without caching
        FirebaseFirestore.getInstance().clearPersistence();
        
        // Set a server timestamp to force refresh data
        Map<String, Object> forceRefreshData = new HashMap<>();
        forceRefreshData.put("last_refresh", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("app_state").document("refresh_marker")
            .set(forceRefreshData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Force refresh timestamp set");
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error setting refresh timestamp", e);
            });
        
        FirestoreHelper.getBiddingProducts(new FirestoreHelper.ProductsCallback() {
            @Override
            public void onProductsLoaded(List<Map<String, Object>> products) {
                if (getActivity() == null) return;
                
                Log.d(TAG, "Loaded " + products.size() + " biddable products");
                
                for (Map<String, Object> productData : products) {
                    try {
                        Product product = new Product(productData);

                        FirestoreHelper.getDistrict(product.getFarmerId(), new FirestoreHelper.UserCallback() {
                            @Override
                            public void onUserLoaded(String district) {
                                if(district != null) {
                                    FirestoreHelper.getDistrict(FirebaseAuth.getInstance().getCurrentUser().getUid(), new FirestoreHelper.UserCallback() {
                                        @Override
                                        public void onUserLoaded(String userDistrict) {
                                            if(userDistrict != null && district.equalsIgnoreCase(userDistrict)) {
                                                productList.add(product);
                                            }
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Log.w(TAG, "Error loading user", e);

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.w(TAG, "Error loading user", e);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating product from data: " + e.getMessage(), e);
                    }
                }
                
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    displayProductsForBidding();
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching products from Firestore", e);
                
                if (getActivity() == null) return;
                
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(
                        requireContext(),
                        "Error loading products: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                    ).show();
                    
                    // Try to fetch from HTTP as fallback
                    fetchBiddableProductsFromHttp();
                });
            }
        });
    }
    
    /**
     * Clear all active bid listeners to prevent memory leaks
     */
    private void clearBidListeners() {
        for (ListenerRegistration listener : bidListeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        bidListeners.clear();
    }
    
    /**
     * Clear all active timer listeners to prevent memory leaks
     */
    private void clearTimerListeners() {
        for (ListenerRegistration listener : timerListeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        timerListeners.clear();
        
        // Also cancel all running timers
        for (CountDownTimer timer : productTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
        productTimers.clear();
    }
    
    /**
     * Legacy method to fetch products from HTTP endpoint as fallback
     */
    private void fetchBiddableProductsFromHttp() {
        swipeRefreshLayout.setRefreshing(true);

        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "Error fetching products: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP error: " + response.code());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), "Error: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                try {
                    if (response.body() == null) {
                        Log.e(TAG, "Empty response body");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                Toast.makeText(getContext(), "Error: Empty response from server",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        return;
                    }

                    String responseData = response.body().string();
                    Log.d(TAG, "API Response: " + responseData);

                    JSONObject jsonResponse = new JSONObject(responseData);

                    if (jsonResponse.getBoolean("success")) {
                        JSONArray productsArray = jsonResponse.getJSONArray("products");
                        List<Product> newProducts = new ArrayList<>();

                        Log.d(TAG, "Found " + productsArray.length() + " products");

                        for (int i = 0; i < productsArray.length(); i++) {
                            JSONObject productJson = productsArray.getJSONObject(i);

                            // Filter products that are registered for bidding
                            boolean isForBidding = productJson.optBoolean("register_for_bidding", true);
                            if (isForBidding) {
                                newProducts.add(new Product(productJson));
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                productList.clear();
                                productList.addAll(newProducts);
                                displayProductsForBidding();
                                swipeRefreshLayout.setRefreshing(false);

                                if (newProducts.isEmpty()) {
                                    Toast.makeText(getContext(), "No products available for bidding",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d(TAG, "Loaded " + newProducts.size() + " products for bidding");
                                }
                            });
                        }
                    } else {
                        final String errorMessage = jsonResponse.has("message") ?
                                jsonResponse.optString("message", "Unknown error") : "Unknown error";

                        Log.e(TAG, "API error: " + errorMessage);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                Toast.makeText(getContext(),
                                        "Error: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(),
                                    "Error parsing data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void displayProductsForBidding() {
        // Clear existing views and timer maps
        productCardsContainer.removeAllViews();
        productTimerViews.clear();
        productBidInputs.clear();
        productYourBidViews.clear();
        productCurrentBidViews.clear();
        productEndTimes.clear();

        // Sort products - ready for bidding first, then waiting
        List<Product> readyProducts = new ArrayList<>();
        List<Product> waitingProducts = new ArrayList<>();

        for (Product product : productList) {
            Map<String, Object> productData = product.getOriginalData();
            if (productData != null && productData.containsKey("bidding_start_time")) {
                // Get the bidding start time
                long biddingStartTime = 0;
                Object startTimeObj = productData.get("bidding_start_time");
                if (startTimeObj instanceof Long) {
                    biddingStartTime = (Long) startTimeObj;
                }
                
                long currentTime = System.currentTimeMillis();
                if (biddingStartTime > currentTime) {
                    // Not ready for bidding yet
                    waitingProducts.add(product);
                } else {
                    // Ready for bidding
                    readyProducts.add(product);
                }
            } else {
                // No start time specified, consider it ready
                readyProducts.add(product);
            }
        }
        
        // First add ready products
        for (Product product : readyProducts) {
            View productCard = createProductBidCard(product, false);
            productCardsContainer.addView(productCard);

            // Initialize bid status
            int productId = product.getId();
            productBidStatus.put(productId, false);
            productBiddingActive.put(productId, false);

            // Check if there's already a timer running in Firestore
            checkOrStartBiddingSession(product);
            
            // Set up real-time bid listener for this product
            setupBidListener(product);
        }
        
        // Then add waiting products
        for (Product product : waitingProducts) {
            View productCard = createProductBidCard(product, true);
            productCardsContainer.addView(productCard);
            
            // Start a waiting timer for this product
            startWaitingTimer(product);
        }
    }
    
    /**
     * Set up real-time bid listener for a specific product
     */
    private void setupBidListener(Product product) {
        String productId = product.getProductId();
        if (productId == null || productId.isEmpty()) {
            Log.w(TAG, "Cannot setup bid listener for product with empty ID");
            return;
        }
        
        // Remove existing listener if any
        if (bidListeners.containsKey(productId)) {
            bidListeners.get(productId).remove();
            bidListeners.remove(productId);
        }
        
        // Create new listener
        ListenerRegistration registration = FirestoreHelper.listenForBidUpdates(
            productId, 
            new FirestoreHelper.BidUpdateListener() {
                @Override
                public void onBidUpdated(FirestoreHelper.BidInfo bidInfo) {
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        // Update UI with new bid information
                        updateBidUI(product.getId(), bidInfo);
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error in bid listener for product " + product.getProductName(), e);
                }
            }
        );
        
        // Store the listener registration
        bidListeners.put(productId, registration);
    }
    
    /**
     * Set up real-time timer listener for a specific product
     */
    private void setupTimerListener(Product product) {
        String productId = product.getProductId();
        if (productId == null || productId.isEmpty()) {
            Log.w(TAG, "Cannot setup timer listener for product with empty ID");
            return;
        }
        
        // Remove existing listener if any
        if (timerListeners.containsKey(productId)) {
            timerListeners.get(productId).remove();
            timerListeners.remove(productId);
        }
        
        // Create new listener
        ListenerRegistration registration = FirestoreHelper.listenForTimerUpdates(
            productId, 
            new FirestoreHelper.TimerUpdateListener() {
                @Override
                public void onTimerUpdated(long endTimeMillis) {
                    if (getActivity() == null) return;
                    
                    int internalProductId = product.getId();
                    
                    // Update the local end time
                    productEndTimes.put(productId, endTimeMillis);
                    
                    // Calculate remaining time
                    long currentTime = System.currentTimeMillis();
                    long remainingTime = endTimeMillis - currentTime;
                    
                    mainHandler.post(() -> {
                        // If the timer is active, update it
                        if (remainingTime > 0) {
                            // Cancel any existing timer
                            if (productTimers.containsKey(internalProductId)) {
                                productTimers.get(internalProductId).cancel();
                            }
                            
                            // Create a new timer with the synchronized remaining time
                            startSynchronizedTimer(internalProductId, remainingTime);
                            productBiddingActive.put(internalProductId, true);
                        } else {
                            // If timer has expired, disable bidding
                            if (productBiddingActive.getOrDefault(internalProductId, false)) {
                                productBiddingActive.put(internalProductId, false);
                                handleBiddingCompletion(internalProductId);
                            }
                        }
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error in timer listener for product " + product.getProductName(), e);
                }
            }
        );
        
        // Store the listener registration
        timerListeners.put(productId, registration);
    }
    
    /**
     * Check if there's an existing bidding session or start a new one
     */
    private void checkOrStartBiddingSession(Product product) {
        String productId = product.getProductId();
        int internalProductId = product.getId();
        Log.d(TAG, "checkOrStartBiddingSession for product: " + product.getProductName() + 
              " (ID: " + productId + ", internal ID: " + internalProductId + ")");
        
        // We ALWAYS want to show the correct base price for this product
        double basePrice = product.getPrice();
        TextView currentBidView = productCurrentBidViews.get(internalProductId);
        if (currentBidView != null) {
            currentBidView.setText(String.format("₹%.0f/kg", basePrice));
            Log.d(TAG, "Explicitly setting current bid display to base price: " + basePrice);
        }
        
        // Always reset "Your Bid" for new or restarted products
        TextView yourBidView = productYourBidViews.get(internalProductId);
        if (yourBidView != null) {
            yourBidView.setText("None/kg");
        }
        
        // First check if this product is already sold
        if (product.isSold()) {
            // Product is sold, disable bidding
            disableBidding(internalProductId);
            return;
        }
        
        Map<String, Object> productData = product.getOriginalData();
        
        // Verify if current_bid is reasonable (should not use values from other products)
        boolean needsReset = false;
        
        // Force reset for all new or recently uploaded products
        if (productData != null && productData.containsKey("timestamp")) {
            long uploadTime = 0;
            Object timeObj = productData.get("timestamp");
            if (timeObj instanceof Long) {
                uploadTime = (Long) timeObj;
                
                // Consider products uploaded in the last 30 minutes as "new" and needing reset
                long currentTime = System.currentTimeMillis();
                if (currentTime - uploadTime < 1800000) { // 30 minutes
                    Log.d(TAG, "Product " + product.getProductName() + " was recently uploaded, forcing reset");
                    needsReset = true;
                }
            }
        }
        
        // Reset if current bid value is suspicious
        if (productData != null && productData.containsKey("current_bid")) {
            // Get the current bid
            Object bidObj = productData.get("current_bid");
            double currentBid = 0;
            if (bidObj instanceof Double) {
                currentBid = (Double) bidObj;
            } else if (bidObj instanceof Long) {
                currentBid = ((Long) bidObj).doubleValue();
            } else if (bidObj instanceof Integer) {
                currentBid = ((Integer) bidObj).doubleValue();
            }
            
            // If current bid is unreasonably different from the product's base price,
            // reset the bidding session (e.g., if it's a new product but has old bid data)
            if (currentBid < basePrice || currentBid > basePrice * 3 || 
                Math.abs(currentBid - basePrice) > 0.01) { // Add small tolerance check
                Log.d(TAG, "Current bid (" + currentBid + ") is unreasonable for product " + 
                      product.getProductName() + " with base price " + basePrice + ". Resetting bidding session.");
                needsReset = true;
            }
            
            // Check for inconsistent timestamps suggesting this is a new product with old data
            long currentTime = System.currentTimeMillis();
            long uploadTime = 0;
            
            if (productData.containsKey("timestamp")) {
                Object timeObj = productData.get("timestamp");
                if (timeObj instanceof Long) {
                    uploadTime = (Long) timeObj;
                }
            }
            
            // If the product was uploaded recently but has bid data from before it was uploaded
            if (uploadTime > 0 && productData.containsKey("bid_timestamp")) {
                Object bidTimeObj = productData.get("bid_timestamp");
                if (bidTimeObj instanceof Long) {
                    long bidTime = (Long) bidTimeObj;
                    // If bid time is before product upload time, that's inconsistent
                    if (bidTime < uploadTime) {
                        Log.d(TAG, "Bid timestamp (" + bidTime + ") is before product upload time (" + 
                              uploadTime + ") for " + product.getProductName() + ". Resetting bidding session.");
                        needsReset = true;
                    }
                }
            }
            
            // Check if bidding data is stale (from a different bidding session)
            if (productData.containsKey("bid_end_time")) {
                Object endTimeObj = productData.get("bid_end_time");
                if (endTimeObj instanceof Long) {
                    long endTime = (Long) endTimeObj;
                    // If the end time is in the past by more than 10 minutes and there's no sold status,
                    // that likely means this is stale data
                    if (endTime < currentTime - 600000 && !product.isSold()) {
                        Log.d(TAG, "Bid end time is more than 10 minutes in the past for product " +
                              product.getProductName() + ". Resetting bidding session.");
                        needsReset = true;
                    }
                }
            }
        } else {
            // No current_bid field exists - definitely needs reset
            needsReset = true;
        }
        
        if (needsReset) {
            // Bid data seems incorrect, start a fresh session
            Log.d(TAG, "Starting a fresh bidding session due to reset condition for: " + product.getProductName());
            startNewBiddingSession(product);
            return;
        }
        
        if (productData != null && productData.containsKey("bid_end_time")) {
            // There's an existing timer in Firestore
            Object endTimeObj = productData.get("bid_end_time");
            if (endTimeObj instanceof Long) {
                long endTimeMillis = (Long) endTimeObj;
                long currentTime = System.currentTimeMillis();
                long remainingTime = endTimeMillis - currentTime;
                
                if (remainingTime > 0) {
                    // There's still time left, start a synchronized timer
                    startSynchronizedTimer(internalProductId, remainingTime);
                    productBiddingActive.put(internalProductId, true);
                    
                    // Store the end time
                    productEndTimes.put(productId, endTimeMillis);
                    
                    // Set up timer listener to keep it synchronized
                    setupTimerListener(product);
                } else {
                    // The timer has already expired
                    handleBiddingCompletion(internalProductId);
                    productBiddingActive.put(internalProductId, false);
                }
            } else {
                // Invalid end time, start a new session
                startNewBiddingSession(product);
            }
        } else {
            // No existing timer, start a new session
            startNewBiddingSession(product);
        }
    }
    
    /**
     * Start a completely new bidding session
     */
    private void startNewBiddingSession(Product product) {
        if (product == null) {
            Log.e(TAG, "❌ Cannot start bidding session - product is null");
            return;
        }
        
        int internalProductId = product.getId();
        String productId = product.getProductId();
        String productName = product.getProductName();
        
        if (productId == null || productId.isEmpty()) {
            Log.e(TAG, "❌ Cannot start bidding session - product ID is null or empty");
            return;
        }
        
        // Log the beginning of the new bidding session
        Log.d(TAG, "🔄 Starting new bidding session for product: " + productName + 
              " (ID: " + productId + ", internal ID: " + internalProductId + ")");
        
        // Calculate the end time
        long endTime = System.currentTimeMillis() + INITIAL_BIDDING_TIME;
        
        // Create fresh bidding data with this product's base price
        Map<String, Object> freshBidData = new HashMap<>();
        freshBidData.put("bid_end_time", endTime);
        freshBidData.put("current_bid", product.getPrice()); // Always use this product's base price
        freshBidData.put("bidder_name", null);
        freshBidData.put("bidder_mobile", null);
        freshBidData.put("bidder_id", null);
        freshBidData.put("bid_timestamp", null);
        freshBidData.put("bid_status", "active");
        freshBidData.put("restarting", false);
        freshBidData.put("is_sold", false);
        freshBidData.put("restart_time", null); // Clear restart time
        
        // Reset ALL previous bidding history to ensure a completely clean state
        freshBidData.put("sold_to", null);
        freshBidData.put("sold_amount", null);
        freshBidData.put("sold_at", null);
        freshBidData.put("receipt_created", false);
        freshBidData.put("final_bid_amount", null);
        freshBidData.put("final_bidder_name", null);
        freshBidData.put("final_bidder_mobile", null);
        freshBidData.put("final_bid_timestamp", null);
        freshBidData.put("bidding_completed_at", null);
        
        Log.d(TAG, "🔍 Fresh bid data: base price=" + product.getPrice() + 
              ", end_time=" + endTime + " (in " + (INITIAL_BIDDING_TIME/1000) + " seconds)");
        
        // IMPORTANT: Update the UI IMMEDIATELY to ensure the user sees the change
        // regardless of Firestore update status
        if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(() -> {
                try {
                    // Update current bid display
                    TextView currentBidView = productCurrentBidViews.get(internalProductId);
                    if (currentBidView != null) {
                        Log.d(TAG, "✅ Updating current bid display to base price: " + product.getPrice());
                        currentBidView.setText(String.format("₹%.0f/kg", product.getPrice()));
                        currentBidView.setTextColor(getResources().getColor(android.R.color.black));
                    } else {
                        Log.w(TAG, "⚠️ Current bid view not found for product ID: " + internalProductId);
                    }
                    
                    // Reset your bid
                    TextView yourBidView = productYourBidViews.get(internalProductId);
                    if (yourBidView != null) {
                        yourBidView.setText("None/kg");
                    } else {
                        Log.w(TAG, "⚠️ Your bid view not found for product ID: " + internalProductId);
                    }
                    
                    // Update the timer text
                    TextView timerView = productTimerViews.get(internalProductId);
                    if (timerView != null) {
                        timerView.setText("BIDDING ACTIVE");
                        timerView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        Log.w(TAG, "⚠️ Timer view not found for product ID: " + internalProductId);
                    }
                    
                    // Find and update the bid button
                    EditText bidInput = productBidInputs.get(internalProductId);
                    if (bidInput != null) {
                        // Re-enable input
                        bidInput.setEnabled(true);
                        bidInput.setText("");
                        
                        // Find the button
                        View productCard = bidInput.getRootView();
                        Button bidButton = productCard.findViewById(R.id.product_bid_button);
                        if (bidButton != null) {
                            bidButton.setEnabled(true);
                            bidButton.setText("PLACE BID");
                            bidButton.setTextColor(getResources().getColor(android.R.color.white));
                            bidButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        }
                        
                        // Highlight the card briefly
                        ValueAnimator colorAnim = ValueAnimator.ofObject(
                            new ArgbEvaluator(),
                            getResources().getColor(android.R.color.holo_green_light),
                            getResources().getColor(android.R.color.white)
                        );
                        colorAnim.setDuration(1000);
                        colorAnim.addUpdateListener(animator -> {
                            productCard.setBackgroundColor((int) animator.getAnimatedValue());
                        });
                        colorAnim.start();
                    }
                    
                    // Show notification
                    Toast.makeText(requireContext(),
                            "🔄 Bidding active for " + productName + "! Place your bids now!",
                            Toast.LENGTH_LONG).show();
                            
                    // Start the timer UI immediately
                    startSynchronizedTimer(internalProductId, INITIAL_BIDDING_TIME);
                    productBiddingActive.put(internalProductId, true);
                    
                    // Store the end time
                    productEndTimes.put(productId, endTime);
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error updating UI in startNewBiddingSession: " + e.getMessage(), e);
                }
            });
        }
        
        // Store it in Firestore with fresh data
        Log.d(TAG, "🔄 Updating Firestore with fresh bidding data for product: " + productId);
        FirestoreHelper.updateProduct(productId, freshBidData, new FirestoreHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Firestore updated successfully with fresh bidding data");
                    
                    // Set up timer listener to keep it synchronized
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                    setupTimerListener(product);
                    });
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Error updating Firestore: " + e.getMessage(), e);
                // UI was already updated, so bidding can continue even if Firestore update failed
            }
        });
    }
    
    /**
     * Start a synchronized timer with the given remaining time
     */
    private void startSynchronizedTimer(int productId, long remainingTimeMillis) {
        // Get the timer text view
        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;
        
        // Reset visual state
        timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        
        // Clean up existing timer
        cleanupProductTimers(productId);
        
        // Create new timer with the synchronized remaining time
        CountDownTimer timer = new CountDownTimer(remainingTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the timer text
                updateTimerText(productId, millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // When timer completes
                if (timerText != null) {
                    timerText.setText("00:00");
                }
                productBiddingActive.put(productId, false);
                handleBiddingCompletion(productId);
            }
        }.start();
        
        // Store the timer
        productTimers.put(productId, timer);
        
        Log.d(TAG, "⏱️ Started new synchronized timer for product ID: " + productId + 
              " with " + (remainingTimeMillis / 1000) + " seconds");
    }

    /**
     * Update UI with new bid information
     */
    private void updateBidUI(int productId, FirestoreHelper.BidInfo bidInfo) {
        if (bidInfo.getBidAmount() == null) return;
        
        // Update current bid view
        TextView currentBidView = productCurrentBidViews.get(productId);
        if (currentBidView != null) {
            currentBidView.setText(String.format("₹%.0f/kg", bidInfo.getBidAmount()));
        }
        
        // If the bid was placed by current user, update "Your Bid" too
        if (currentUserMobile != null && currentUserMobile.equals(bidInfo.getBidderMobile())) {
            TextView yourBidView = productYourBidViews.get(productId);
            if (yourBidView != null) {
                yourBidView.setText(String.format("₹%.0f/kg", bidInfo.getBidAmount()));
            }
        }
        
        // If a new bid came in and bidding is active, the timer will be updated via the timer listener
        if (bidInfo.getTimestamp() != null && 
            bidInfo.getTimestamp() > System.currentTimeMillis() - 5000 && // Within last 5 seconds
            !currentUserMobile.equals(bidInfo.getBidderMobile())) { // Not our own bid
            
            // Show toast about the new bid
            String bidderName = bidInfo.getBidderName() != null ? 
                bidInfo.getBidderName() : "Another user";
            
            // Find product name
            String productName = "";
            for (Product product : productList) {
                if (product.getId() == productId) {
                    productName = product.getProductName();
                    break;
                }
            }
            
            Toast.makeText(
                requireContext(),
                bidderName + " placed a bid of ₹" + bidInfo.getBidAmount() + 
                    " for " + productName,
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    private View createProductBidCard(Product product, boolean isWaiting) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View productCard = inflater.inflate(R.layout.item_product_bid_card, productCardsContainer, false);

        // Get views
        TextView productNameView = productCard.findViewById(R.id.product_name);
        TextView quantityView = productCard.findViewById(R.id.product_quantity);
        TextView farmerView = productCard.findViewById(R.id.product_farmer);
        TextView basePriceView = productCard.findViewById(R.id.product_base_price);
        TextView currentBidView = productCard.findViewById(R.id.product_current_bid);
        TextView yourBidView = productCard.findViewById(R.id.product_your_bid);
        EditText bidInputView = productCard.findViewById(R.id.product_bid_input);
        Button bidButton = productCard.findViewById(R.id.product_bid_button);
        TextView timerView = productCard.findViewById(R.id.product_timer);
        ImageView productImage = productCard.findViewById(R.id.product_image);

        // Store references to views we need to update
        int productId = product.getId();
        productTimerViews.put(productId, timerView);
        productBidInputs.put(productId, bidInputView);
        productYourBidViews.put(productId, yourBidView);
        productCurrentBidViews.put(productId, currentBidView);

        // Set product info
        productNameView.setText(product.getProductName());
        quantityView.setText(String.format("Quantity: %.0fkg", product.getQuantity()));
        farmerView.setText(String.format("Farmer: %s", product.getFarmerName()));
        basePriceView.setText(String.format("Base Price: ₹%.0f/kg", product.getPrice()));

        // Check if the product is already sold
        boolean isSold = product.isSold();
        Map<String, Object> productData = product.getOriginalData();
        
        if (isSold) {
            // Product is already sold, disable bidding and show sold status
            bidButton.setEnabled(false);
            bidButton.setText("SOLD");
            bidButton.setTextColor(getResources().getColor(android.R.color.white));
            bidButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            bidButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            bidButton.setTypeface(bidButton.getTypeface(), android.graphics.Typeface.BOLD);
            bidInputView.setEnabled(false);
            
            // Show winner information if available
            if (productData != null) {
                // Show sold info in timer
                if (productData.containsKey("sold_to")) {
                    String soldTo = (String) productData.get("sold_to");
                    if (soldTo != null && !soldTo.isEmpty()) {
                        timerView.setText("SOLD TO: " + soldTo);
                        timerView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        timerView.setText("PRODUCT SOLD");
                        timerView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    }
                } else {
                    timerView.setText("PRODUCT SOLD");
                    timerView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
                
                // Show sold amount in button if available
                if (productData.containsKey("sold_amount")) {
                    Object soldAmountObj = productData.get("sold_amount");
                    if (soldAmountObj instanceof Double) {
                        double soldAmount = (Double) soldAmountObj;
                        bidButton.setText("SOLD: ₹" + (int)soldAmount);
                    } else if (soldAmountObj instanceof Long) {
                        long soldAmount = (Long) soldAmountObj;
                        bidButton.setText("SOLD: ₹" + soldAmount);
                    } else if (soldAmountObj instanceof Integer) {
                        int soldAmount = (Integer) soldAmountObj;
                        bidButton.setText("SOLD: ₹" + soldAmount);
                    }
                }
                
                // Show the final bid in the current bid view
                if (productData.containsKey("sold_amount")) {
                    Object soldAmountObj = productData.get("sold_amount");
                    if (soldAmountObj instanceof Double) {
                        double soldAmount = (Double) soldAmountObj;
                        currentBidView.setText(String.format("₹%.0f/kg", soldAmount));
                    } else if (soldAmountObj instanceof Long) {
                        long soldAmount = (Long) soldAmountObj;
                        currentBidView.setText(String.format("₹%d/kg", soldAmount));
                    } else if (soldAmountObj instanceof Integer) {
                        int soldAmount = (Integer) soldAmountObj;
                        currentBidView.setText(String.format("₹%d/kg", soldAmount));
                    }
                }
            } else {
                timerView.setText("PRODUCT SOLD");
                timerView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
        }
        // If product is waiting for bidding to start, disable bidding controls
        else if (isWaiting) {
            bidButton.setEnabled(false);
            bidInputView.setEnabled(false);
            
            // Get the waiting time
            if (productData != null && productData.containsKey("bidding_start_time")) {
                long biddingStartTime = 0;
                Object startTimeObj = productData.get("bidding_start_time");
                if (startTimeObj instanceof Long) {
                    biddingStartTime = (Long) startTimeObj;
                }
                
                long currentTime = System.currentTimeMillis();
                long waitTime = biddingStartTime - currentTime;
                
                if (waitTime > 0) {
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(waitTime) % 60;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(waitTime);
                    timerView.setText(String.format(Locale.getDefault(), 
                        "Bidding starts in: %02d:%02d", minutes, seconds));
                }
            }
            
            currentBidView.setText("Waiting");
            yourBidView.setText("Waiting");
            
            // Set timer color to blue to indicate waiting
            timerView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            // Regular active product for bidding
            // ALWAYS use product's base price for new products
            
            // For safety, ALWAYS ensure we have the real base price
            double basePrice = product.getPrice();
            Log.d(TAG, "Creating bid card for " + product.getProductName() + " with base price: " + basePrice);
            
            // Default to the product's base price always
            double currentBid = basePrice;
            
            // Check if this is a product that should never show old bid data
            boolean isFreshBiddingSession = false;
            
            // Determine if this is a fresh product or reset session based on timestamps
            if (productData != null) {
                // Get the product upload timestamp
                long uploadTime = 0;
                if (productData.containsKey("timestamp")) {
                    Object timeObj = productData.get("timestamp");
                    if (timeObj instanceof Long) {
                        uploadTime = (Long) timeObj;
                    }
                }
                
                // Get the most recent bid timestamp
                long bidTime = 0;
                if (productData.containsKey("bid_timestamp")) {
                    Object bidTimeObj = productData.get("bid_timestamp");
                    if (bidTimeObj instanceof Long) {
                        bidTime = (Long) bidTimeObj;
                    }
                }
                
                // If no bids yet or product was uploaded/reset recently, consider it fresh
                long currentTime = System.currentTimeMillis();
                
                // Product was reset or uploaded recently (within last hour)
                if (uploadTime > 0 && (currentTime - uploadTime < 3600000)) {
                    Log.d(TAG, "Product " + product.getProductName() + " was recently uploaded/reset - treating as fresh session");
                    isFreshBiddingSession = true;
                }
                
                // The bidding status indicates a reset or new product
                if (productData.containsKey("bid_status")) {
                    Object statusObj = productData.get("bid_status");
                    if (statusObj instanceof String) {
                        String status = (String) statusObj;
                        if (status.equals("pending") || status.equals("restart_pending")) {
                            Log.d(TAG, "Product " + product.getProductName() + " has status " + status + " - treating as fresh session");
                            isFreshBiddingSession = true;
                        }
                    }
                }
                
                // Bidding was reset recently
                if (productData.containsKey("restarting")) {
                    Object restartingObj = productData.get("restarting");
                    if (restartingObj instanceof Boolean && (Boolean) restartingObj) {
                        Log.d(TAG, "Product " + product.getProductName() + " is marked for restarting - treating as fresh session");
                        isFreshBiddingSession = true;
                    }
                }
            }
            
            // If this is a fresh session, ALWAYS use base price
            if (isFreshBiddingSession) {
                Log.d(TAG, "Using BASE PRICE for fresh bidding session of " + product.getProductName() + ": " + basePrice);
                currentBid = basePrice;
            }
            // Otherwise check for existing current_bid only for active bidding products with valid bid data
            else if (productData != null && 
                productData.containsKey("current_bid") && 
                productData.containsKey("bidder_name") &&
                productData.get("bidder_name") != null) {
                
                // This product has an active bid with a bidder - use the current bid value
                Object currentBidObj = productData.get("current_bid");
                if (currentBidObj instanceof Double) {
                    currentBid = (Double) currentBidObj;
                } else if (currentBidObj instanceof Long) {
                    currentBid = ((Long) currentBidObj).doubleValue();
                } else if (currentBidObj instanceof Integer) {
                    currentBid = ((Integer) currentBidObj).doubleValue();
                }
                
                // Sanity check - don't use unreasonable values or values that differ from base price
                if (currentBid < basePrice || Math.abs(currentBid - basePrice) < 0.01) {
                    currentBid = basePrice; // Reset to base price if incorrect
                    Log.d(TAG, "Resetting current bid to base price for " + product.getProductName());
                }
            } else {
                // No active bid yet, initialize with base price
                Log.d(TAG, "No active bidding yet for product " + product.getProductName() + 
                      ", using base price: " + basePrice);
            }
            
            // Ensure we're displaying the right value
            currentBidView.setText(String.format("₹%.0f/kg", currentBid));

            // Check if current user has already bid on this product
            boolean userHasBid = false;
            String userBid = "None/kg";
            
            // Only show user's bid if it's not a fresh session
            if (!isFreshBiddingSession && 
                productData != null && 
                productData.containsKey("bidder_mobile") && 
                currentUserMobile != null && 
                currentUserMobile.equals(productData.get("bidder_mobile"))) {
                
                userHasBid = true;
                userBid = String.format("₹%.0f/kg", currentBid);
            }
            
            yourBidView.setText(userHasBid ? userBid : "None/kg");
        }

        // Load product image
        String imagePath = product.getImagePath();
        String imageUrl = product.getImageUrl();
        
        Log.d(TAG, "Loading image for product: " + product.getProductName());
        
        // Set default placeholder
        productImage.setImageResource(R.drawable.ic_image_placeholder);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Direct Cloudinary URL
            Log.d(TAG, "Loading from Cloudinary URL: " + imageUrl);
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(productImage);
        } else if (imagePath != null && !imagePath.isEmpty()) {
            // Relative path - append to base URL
            String fullImageUrl = Constants.DB_URL_BASE + imagePath;
            Log.d(TAG, "Loading from HTTP path: " + fullImageUrl);
            Glide.with(requireContext())
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(productImage);
        } else {
            Log.w(TAG, "No image URL or path found for product: " + product.getProductName());
        }

        // Set bid button listener
        bidButton.setOnClickListener(v -> {
            String bidAmount = bidInputView.getText().toString().trim();
            if (!bidAmount.isEmpty()) {
                placeBid(product, bidAmount);
            } else {
                Toast.makeText(requireContext(), "Please enter a bid amount", Toast.LENGTH_SHORT).show();
            }
        });
        
        return productCard;
    }

    /**
     * Reset bidding timer for a specific product to 1:30 minutes
     * This happens when someone places a new bid
     */
    private void resetBiddingTimer(int internalProductId) {
        // Find the product with this internal ID
        Product foundProduct = null;
        for (Product product : productList) {
            if (product.getId() == internalProductId) {
                foundProduct = product;
                break;
            }
        }
        
        if (foundProduct == null) {
            Log.e(TAG, "❌ Cannot reset timer - product not found for ID: " + internalProductId);
            return;
        }
        
        // Create a final copy for use in the inner class
        final Product targetProduct = foundProduct;
        
        String productId = targetProduct.getProductId();
        if (productId == null || productId.isEmpty()) {
            Log.e(TAG, "❌ Cannot reset timer - product ID is null or empty");
            return;
        }
        
        // Calculate the new end time
        long currentTime = System.currentTimeMillis();
        long endTime = currentTime + SUBSEQUENT_BIDDING_TIME;
        
        Log.d(TAG, "🔄 Resetting bid timer for " + targetProduct.getProductName() + 
              " - extending to " + (SUBSEQUENT_BIDDING_TIME/1000) + " seconds from now");
        
        // Update timer display immediately
        TextView timerView = productTimerViews.get(internalProductId);
        if (timerView != null) {
            timerView.setText("BIDDING EXTENDED!");
            timerView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            
            // Add animation to highlight extension
            timerView.setAlpha(0.7f);
            timerView.animate().alpha(1.0f).setDuration(500).start();
            
            // Revert to normal color after 1.5 seconds
            new Handler().postDelayed(() -> {
                if (isAdded() && timerView != null) {
                    timerView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
            }, 1500);
        }
        
        // Cancel and reset the current timer UI
        cleanupProductTimers(internalProductId);
        
        // Start a new timer right away for immediate local feedback
        startSynchronizedTimer(internalProductId, SUBSEQUENT_BIDDING_TIME);
        productBiddingActive.put(internalProductId, true);
        
        // Store the new end time locally
        productEndTimes.put(productId, endTime);
        
        // Update it in Firestore - the timer listeners will pick up the change
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("bid_end_time", endTime);
        timerData.put("bid_status", "active");
        timerData.put("restarting", false);
        timerData.put("timer_extended_at", currentTime);
        
        FirestoreHelper.updateBidTimerInfo(productId, endTime, new FirestoreHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) return;
                
                Log.d(TAG, "✅ Successfully extended timer in database for " + targetProduct.getProductName());
                
                // The timer will be updated via the listener
                String productName = targetProduct.getProductName();
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "⏱️ Bidding extended for " + productName + " - new bids can be placed for 1:30 minutes!",
                            Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Error resetting bidding timer: " + e.getMessage(), e);
                
                // Keep the local timer running anyway
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                            "Local bidding time extended, but there was a sync error. Your bids will still be accepted.",
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Handle completion of bidding session for a specific product
     */
    private void handleBiddingCompletion(int productId) {
        Log.d(TAG, "⚠️ Bidding completed for product ID: " + productId + " - Checking for valid bids...");
        
        // Find product details
        Product product = null;
        for (Product p : productList) {
            if (p.getId() == productId) {
                product = p;
                break;
            }
        }

        if (product == null) {
            Log.e(TAG, "❌ Cannot find product for ID: " + productId);
            return;
        }
        
        // Make product final to use in lambda
        final Product finalProduct = product;
        
        // Get the product details
        final String productIdStr = product.getProductId();
        final String productName = product.getProductName();
        final int quantity = (int) product.getQuantity(); // Cast double to int
        Log.d(TAG, "🔍 Found product: " + productName + ", ID: " + productIdStr + ", quantity: " + quantity + ", base price: " + product.getPrice());
        
        // Check if there was a valid bid
            FirestoreHelper.getBidInfo(productIdStr, new FirestoreHelper.BidInfoCallback() {
                @Override
                public void onBidInfoRetrieved(FirestoreHelper.BidInfo bidInfo) {
                // First temporarily disable bidding while we check
                disableBiddingUI(productId, "Checking bids...");
                
                // Check if there is ANY bid with an amount greater than zero
                boolean hasValidBid = bidInfo != null && 
                                     bidInfo.getBidAmount() != null && 
                                     bidInfo.getBidAmount() > 0;
                
                // We need a bidder name - if missing, try to get from current bid data
                if (hasValidBid && (bidInfo.getBidderName() == null || bidInfo.getBidderName().isEmpty())) {
                    Log.d(TAG, "⚠️ Bid amount found but missing bidder name - will try to use current_bid data");
                    
                    // Check current bid data directly from the product
                    Map<String, Object> productData = finalProduct.getOriginalData();
                    if (productData != null && productData.containsKey("bidder_name")) {
                        String bidderName = (String) productData.get("bidder_name");
                        if (bidderName != null && !bidderName.isEmpty()) {
                            Log.d(TAG, "✅ Found bidder name in product data: " + bidderName);
                            
                            // Create a new BidInfo with the existing data plus the bidder name
                            String bidderMobile = (String) productData.get("bidder_mobile");
                            String bidderId = (String) productData.get("bidder_id");
                            
                            // Make sure the bid info is complete
                            bidInfo = new FirestoreHelper.BidInfo(
                                bidInfo.getBidAmount(),
                                bidderId,
                                bidderName,
                                bidderMobile,
                                bidInfo.getTimestamp() != null ? bidInfo.getTimestamp() : System.currentTimeMillis()
                            );
                            
                            // Reset hasValidBid check with updated info
                            hasValidBid = true;
                        }
                    }
                }
                
                Log.d(TAG, "🔍 Bid check - hasValidBid: " + hasValidBid + 
                      (bidInfo != null ? ", amount: " + bidInfo.getBidAmount() + ", bidder: " + bidInfo.getBidderName() : ", bidInfo: null"));
                
                // Create a final copy of bidInfo for use in lambdas
                final FirestoreHelper.BidInfo finalBidInfo = bidInfo;
                
                // If there is ANY valid bid greater than zero, mark as sold
                if (hasValidBid) {
                    // A valid bid was placed - finalize the bidding process
                    Log.d(TAG, "✅ Valid bid found for " + productName + ": " + 
                          finalBidInfo.getBidAmount() + " by " + finalBidInfo.getBidderName() + " - Finalizing sale");
                    
                    // Permanently disable bidding and mark as sold
                    disableBidding(productId);
                    
                    // Show a more prominent toast notification for winning bid
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(),
                                "🏆 SOLD! " + finalBidInfo.getBidderName() + " won " + productName + 
                                " with a bid of ₹" + finalBidInfo.getBidAmount().intValue() + " per kg!",
                                Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    // Store the final bid data
                    Map<String, Object> finalBidData = new HashMap<>();
                    finalBidData.put("final_bid_amount", finalBidInfo.getBidAmount());
                    finalBidData.put("final_bidder_name", finalBidInfo.getBidderName());
                    finalBidData.put("final_bidder_mobile", finalBidInfo.getBidderMobile());
                    finalBidData.put("final_bid_timestamp", finalBidInfo.getTimestamp());
                    finalBidData.put("bidding_completed_at", System.currentTimeMillis());
                    finalBidData.put("bid_status", "completed");
                    finalBidData.put("restarting", false);
                    finalBidData.put("is_sold", true);
                    finalBidData.put("sold_to", finalBidInfo.getBidderName());
                    finalBidData.put("sold_amount", finalBidInfo.getBidAmount());
                    finalBidData.put("sold_at", System.currentTimeMillis());
                    
                    // Update the product with final bid data
                    FirestoreHelper.updateProduct(productIdStr, finalBidData, new FirestoreHelper.SaveCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Successfully stored final bid info for product: " + productName);
                            
                            // Create receipts with the provided bid info
                            checkAndCreateReceipts(finalProduct, finalBidInfo);
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "❌ Error storing final bid info: " + e.getMessage());
                            
                            // Still proceed with receipt check
                            checkAndCreateReceipts(finalProduct, finalBidInfo);
                        }
                    });
                } else {
                    // No valid bid was placed - restart bidding after delay
                    Log.d(TAG, "⚠️ No valid bid found for " + productName + ". Will restart bidding after 30 seconds");
                    
                    // Show waiting status in UI with countdown animation
                    TextView timerText = productTimerViews.get(productId);
                    if (timerText != null) {
                        timerText.setText("🔄 NO BIDS PLACED - RESTARTING IN 30s");
                        timerText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        
                        // Add visual animation to timer to indicate restarting
                        timerText.setAlpha(0.7f);
                        timerText.animate().alpha(1.0f).setDuration(500).start();
                    }
                    
                    // Update bid button to show status
                    Button bidButton = null;
                    EditText bidInput = productBidInputs.get(productId);
                    if (bidInput != null) {
                        View productCard = bidInput.getRootView();
                        bidButton = productCard.findViewById(R.id.product_bid_button);
                        
                        if (bidButton != null) {
                            bidButton.setText("RESTARTING SOON");
                            bidButton.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                        }
                    }
                    
                    // Show toast notification for no bids
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                "🔄 No bids were placed for " + productName + ". Bidding will restart in 30 seconds!", 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    // Set restart flag in Firestore
                    Map<String, Object> restartData = new HashMap<>();
                    restartData.put("bid_status", "restart_pending");
                    restartData.put("restart_time", System.currentTimeMillis() + 30000); // 30 seconds
                    restartData.put("restarting", true);
                    restartData.put("is_sold", false); // Ensure product is not marked as sold
                    
                    // Use finalProduct instead of product for price access - FIX THE TYPO HERE
                    restartData.put("current_bid", finalProduct.getPrice()); // Reset to base price
                    
                    Log.d(TAG, "🔄 Setting restart data for product " + productName + 
                          ": bid_status=restart_pending, restart_time=" + (System.currentTimeMillis() + 30000) + 
                          ", base_price=" + finalProduct.getPrice());
                    
                    // Store the final reference to be used in countdown timer
                    final TextView finalTimerText = timerText;
                    final Button finalBidButton = bidButton;
                    
                    FirestoreHelper.updateProduct(productIdStr, restartData, new FirestoreHelper.SaveCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Successfully set restart_pending status for " + productName);
                            
                            // Create explicit countdown timer instead of using Handler.postDelayed
                            CountDownTimer restartTimer = new CountDownTimer(30000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    if (getActivity() == null || !isAdded()) return;
                                    
                                    // Update the timer text with countdown
                                    long seconds = millisUntilFinished / 1000;
                                    if (finalTimerText != null) {
                        getActivity().runOnUiThread(() -> {
                                            finalTimerText.setText("🔄 RESTARTING IN " + seconds + "s");
                                        });
                                    }
                                    
                                    Log.d(TAG, "⏱️ Restart countdown for " + productName + ": " + seconds + " seconds remaining");
                                }
                                
                                @Override
                                public void onFinish() {
                                    if (getActivity() == null || !isAdded()) {
                                        Log.e(TAG, "❌ Fragment not attached when restart timer finished");
                                        return;
                                    }
                                    
                                    Log.d(TAG, "⏱️ RESTART TIMER FINISHED for " + productName + " - EXECUTING RESTART NOW!");
                                    
                                    // Update UI to show restart is happening
                                    getActivity().runOnUiThread(() -> {
                                        try {
                                            // Update UI elements first
                                            if (finalTimerText != null) {
                                                finalTimerText.setText("🔄 RESTARTING NOW...");
                                                finalTimerText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                                            }
                                            
                                            if (finalBidButton != null) {
                                                finalBidButton.setText("RESTARTING...");
                                                finalBidButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                                            }
                                            
                                            // CRITICAL: Force a direct call to startNewBiddingSession without going through restartBidding
                                            // This avoids the extra Firestore checks that might be causing delays
                                            Log.d(TAG, "🚀 DIRECT START of new bidding session for " + productName);
                                            
                                            // Set base price again to ensure it's correct
                                            double basePrice = finalProduct.getPrice();
                                            
                                            // Show toast notification to indicate restart is happening
                            Toast.makeText(requireContext(),
                                                "🔄 Restarting bidding for " + productName + " now!",
                                    Toast.LENGTH_SHORT).show();
                                            
                                            // Direct call to startNewBiddingSession bypassing restartBidding
                                            startNewBiddingSession(finalProduct);
                                            
                                            // Log completion
                                            Log.d(TAG, "✅ SUCCESSFULLY RESTARTED bidding for " + productName);
                                        } catch (Exception e) {
                                            Log.e(TAG, "❌ Error in restart execution: " + e.getMessage(), e);
                                            
                                            // One more emergency attempt with a slight delay
                                            new Handler().postDelayed(() -> {
                                                try {
                                                    Log.d(TAG, "⚠️ Attempting emergency direct restart after error");
                                                    startNewBiddingSession(finalProduct);
                                                    Log.d(TAG, "✅ Emergency restart succeeded");
                                                } catch (Exception ex) {
                                                    Log.e(TAG, "❌ Critical failure in emergency restart: " + ex.getMessage(), ex);
                                                }
                                            }, 1000); // Short delay for recovery
                                        }
                                    });
                                }
                            };
                            
                            // Start the countdown timer
                            restartTimer.start();
                            
                            // Store reference to timer so we can cancel it if needed
                            productTimers.put(productId, restartTimer);
                            
                            Log.d(TAG, "✅ Successfully started restart countdown timer for " + productName);
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "❌ Failed to set restart status: " + e.getMessage());
                            
                            // Try a direct restart anyway after delay
                            try {
                                if (getActivity() != null && isAdded()) {
                                    // Create a direct timer that doesn't rely on Firestore updates
                                    CountDownTimer emergencyTimer = new CountDownTimer(30000, 1000) {
                                        @Override
                                        public void onTick(long millisUntilFinished) {
                                            // Just tick but don't update UI to minimize chances of failure
                                        }
                                        
                                        @Override
                                        public void onFinish() {
                                            if (getActivity() == null || !isAdded()) return;
                                            Log.d(TAG, "⚠️ Emergency restart timer fired - attempting restart");
                        getActivity().runOnUiThread(() -> {
                                                try {
                                                    restartBidding(finalProduct);
                                                } catch (Exception ex) {
                                                    Log.e(TAG, "❌ Error in emergency restart: " + ex.getMessage(), ex);
                                                }
                                            });
                                        }
                                    };
                                    
                                    emergencyTimer.start();
                                    Log.d(TAG, "⚠️ Started emergency timer due to Firestore error");
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "❌ Critical failure setting up emergency timer: " + ex.getMessage(), ex);
                            }
                        }
                    });
                }
                }
                
                @Override
                public void onError(Exception e) {
                Log.e(TAG, "❌ Error checking bid info: " + e.getMessage());
                
                // Error getting bid info - assume no valid bid and restart
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                            "Error checking bids for " + productName + ". Will try to restart bidding.",
                            Toast.LENGTH_SHORT).show();
                        
                        // Use simple countdown timer
                        try {
                            CountDownTimer emergencyTimer = new CountDownTimer(30000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    // Minimal processing to avoid issues
                                }
                                
                                @Override
                                public void onFinish() {
                                    if (getActivity() == null || !isAdded()) return;
                                    Log.d(TAG, "⚠️ Emergency fallback timer fired");
                                    getActivity().runOnUiThread(() -> {
                                        if (isAdded()) {
                                            Log.d(TAG, "🔄 Attempting emergency restart for " + productName + " after bid check error");
                                            restartBidding(finalProduct);
                                        }
                                    });
                                }
                            };
                            
                            emergencyTimer.start();
                            Log.d(TAG, "⚠️ Started fallback timer due to bid check error");
                        } catch (Exception ex) {
                            Log.e(TAG, "❌ Error scheduling emergency bidding restart: " + ex.getMessage(), ex);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Just disable the UI elements for bidding without finalizing the sale
     */
    private void disableBiddingUI(int productId, String message) {
        // Find the product views
        EditText bidInput = productBidInputs.get(productId);
        if (bidInput == null) return;

        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;

        // Find the bid button
        View productCard = bidInput.getRootView();
        Button bidButton = productCard.findViewById(R.id.product_bid_button);
        if (bidButton == null) return;

        // Disable bidding controls temporarily
        bidButton.setEnabled(false);
        bidInput.setEnabled(false);
        
        // Update timer text with status message
        timerText.setText(message);
        timerText.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }
    
    /**
     * Restart bidding for a product after no bids were placed
     */
    private void restartBidding(Product product) {
        if (product == null) {
            Log.e(TAG, "❌ Cannot restart bidding - product is null");
            return;
        }
        
        if (getActivity() == null || !isAdded()) {
            Log.e(TAG, "❌ Cannot restart bidding - fragment not attached");
            return;
        }
        
        int productId = product.getId();
        String productName = product.getProductName();
        String productIdStr = product.getProductId();
        
        Log.d(TAG, "🔄 RESTARTING BIDDING for product: " + productName + " (ID: " + productId + ")");
        
        // First verify that restart is still needed/valid
        FirestoreHelper.getProductById(productIdStr, new FirestoreHelper.ProductCallback() {
            @Override
            public void onProductLoaded(Map<String, Object> productData) {
                if (productData == null) {
                    Log.e(TAG, "❌ Product data not found for restart: " + productIdStr);
                    return;
                }
                
                Log.d(TAG, "🔍 Checking product data for restart eligibility: " + productData.toString());
                
                // Check if product is already sold
                boolean isSold = false;
                if (productData.containsKey("is_sold")) {
                    Object soldObj = productData.get("is_sold");
                    if (soldObj instanceof Boolean) {
                        isSold = (Boolean) soldObj;
                    }
                }
                
                if (isSold) {
                    Log.d(TAG, "⚠️ Product already sold, no restart needed: " + productName);
                    return;
                }
                
                // Check if bid status was changed from restart_pending
                String bidStatus = "unknown";
                if (productData.containsKey("bid_status")) {
                    Object statusObj = productData.get("bid_status");
                    if (statusObj instanceof String) {
                        bidStatus = (String) statusObj;
                    }
                }
                
                boolean isRestarting = false;
                if (productData.containsKey("restarting")) {
                    Object restartingObj = productData.get("restarting");
                    if (restartingObj instanceof Boolean) {
                        isRestarting = (Boolean) restartingObj;
                    }
                }
                
                Log.d(TAG, "🔍 Restart check - isSold: " + isSold + ", bidStatus: " + bidStatus + ", isRestarting: " + isRestarting);
                
                if (!isRestarting && !"restart_pending".equals(bidStatus) && !"unknown".equals(bidStatus)) {
                    Log.d(TAG, "⚠️ Bid status changed to " + bidStatus + ", not restarting: " + productName);
                    return;
                }
                
                // Double-check that we haven't already created a receipt
                boolean receiptCreated = false;
                if (productData.containsKey("receipt_created")) {
                    Object receiptObj = productData.get("receipt_created");
                    if (receiptObj instanceof Boolean) {
                        receiptCreated = (Boolean) receiptObj;
                    }
                }
                
                if (receiptCreated) {
                    Log.d(TAG, "⚠️ Receipt already created, not restarting bidding: " + productName);
                    return;
                }
                
                // Check that the restart time has passed
                long restartTime = 0;
                if (productData.containsKey("restart_time")) {
                    Object restartObj = productData.get("restart_time");
                    if (restartObj instanceof Long) {
                        restartTime = (Long) restartObj;
                    }
                }
                
                long currentTime = System.currentTimeMillis();
                if (restartTime > 0 && currentTime < restartTime) {
                    // Not time to restart yet
                    long timeRemaining = restartTime - currentTime;
                    Log.d(TAG, "⏱️ Not time to restart yet - " + (timeRemaining / 1000) + " seconds remaining");
                    
                    // Re-schedule for the correct remaining time
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded()) {
                            Log.d(TAG, "⏱️ Restart timer rescheduled and now firing for " + productName);
                            restartBidding(product);
                        }
                    }, timeRemaining);
                    return;
                }
                
                // Proceed with restart - start a new bidding session
                Log.d(TAG, "✅ Product " + productName + " eligible for restart - proceeding with restart!");
                
                getActivity().runOnUiThread(() -> {
                    try {
                        // Reset UI elements
                        TextView timerText = productTimerViews.get(productId);
                        EditText bidInput = productBidInputs.get(productId);
                        
                        if (bidInput == null) {
                            Log.e(TAG, "❌ Bid input view not found for product ID: " + productId);
                            return;
                        }
                        
                        View productCard = bidInput.getRootView();
                        Button bidButton = productCard.findViewById(R.id.product_bid_button);
                        
                        // Re-enable controls
                        bidButton.setEnabled(true);
                        bidButton.setText("PLACE BID");
                        bidButton.setTextColor(getResources().getColor(android.R.color.white));
                        bidButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        bidInput.setEnabled(true);
                        
                        // Add animation to highlight the restart
                        if (productCard != null) {
                            // Flash effect to draw attention
                            ValueAnimator colorAnim = ValueAnimator.ofObject(
                                new ArgbEvaluator(),
                                getResources().getColor(android.R.color.holo_green_light),
                                getResources().getColor(android.R.color.white)
                            );
                            colorAnim.setDuration(1000);
                            colorAnim.addUpdateListener(animator -> {
                                productCard.setBackgroundColor((int) animator.getAnimatedValue());
                            });
                            colorAnim.start();
                        }
                        
                        // Reset current bid to base price
                        TextView currentBidView = productCurrentBidViews.get(productId);
                        if (currentBidView != null) {
                            currentBidView.setText(String.format("₹%.0f/kg", product.getPrice()));
                            
                            // Flash animation for the price too
                            currentBidView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            new Handler().postDelayed(() -> {
                                if (isAdded()) {
                                    currentBidView.setTextColor(getResources().getColor(android.R.color.black));
                                }
                            }, 1500);
                        }
                        
                        // Reset your bid view
                        TextView yourBidView = productYourBidViews.get(productId);
                        if (yourBidView != null) {
                            yourBidView.setText("None/kg");
                        }
                        
                        // Clear any existing product data in memory first
                        if (productBidStatus.containsKey(productId)) {
                            productBidStatus.put(productId, false);
                        }
                        
                        if (productTimers.containsKey(productId)) {
                            CountDownTimer existingTimer = productTimers.get(productId);
                            if (existingTimer != null) {
                                existingTimer.cancel();
                            }
                            productTimers.remove(productId);
                        }
                        
                        // Reset bidding state in Firestore first with a cleanup operation
                        Map<String, Object> cleanupData = new HashMap<>();
                        cleanupData.put("current_bid", product.getPrice());
                        cleanupData.put("bidder_name", null);
                        cleanupData.put("bidder_mobile", null);
                        cleanupData.put("bidder_id", null);
                        cleanupData.put("bid_timestamp", null);
                        cleanupData.put("sold_to", null);
                        cleanupData.put("sold_amount", null);
                        cleanupData.put("sold_at", null);
                        cleanupData.put("receipt_created", false);
                        cleanupData.put("final_bid_amount", null);
                        cleanupData.put("final_bidder_name", null);
                        cleanupData.put("final_bidder_mobile", null);
                        cleanupData.put("final_bid_timestamp", null);
                        
                        FirestoreHelper.updateProduct(productIdStr, cleanupData, new FirestoreHelper.SaveCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "✅ Successfully cleared previous bid data for " + productName);
                                // Start a fresh bidding session
                                startNewBiddingSession(product);
                                
                                // Notify user
                                Toast.makeText(requireContext(),
                                    "🔄 Bidding restarted for " + productName + "! You can now place bids again.",
                                    Toast.LENGTH_LONG).show();
                                    
                                Log.d(TAG, "✅ Successfully restarted bidding for " + productName);
                            }
                            
                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "⚠️ Error clearing previous bid data: " + e.getMessage());
                                // Still try to start a new session
                                startNewBiddingSession(product);
                            }
                        });
                        
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error during UI update for bidding restart: " + e.getMessage(), e);
                        
                        // Try one more time with just the essential restart
                        try {
                            startNewBiddingSession(product);
                            Toast.makeText(requireContext(), 
                                "Bidding restarted for " + productName, 
                                Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "✅ Successfully performed emergency restart for " + productName);
                        } catch (Exception ex) {
                            Log.e(TAG, "❌ Critical failure during emergency restart: " + ex.getMessage(), ex);
                            Toast.makeText(requireContext(),
                                "Error restarting bidding. Please refresh the page.",
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Error checking product for restart: " + e.getMessage(), e);
                
                // Make an emergency attempt to restart if we can't get product data
                try {
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TAG, "⚠️ Attempting emergency restart without product data check");
                            
                            // Just try to start a new bidding session directly
                            startNewBiddingSession(product);
                            
                            Toast.makeText(requireContext(),
                                "Emergency restart of bidding for " + productName,
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "❌ Critical failure during emergency restart: " + ex.getMessage(), ex);
                }
            }
        });
    }
    
    /**
     * Update the timer text with the remaining time for a specific product
     */
    private void updateTimerText(int productId, long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;

        timerText.setText(timeLeftFormatted);
        
        // Change color to red when less than 10 seconds left
        if (minutes == 0 && seconds < 10) {
            timerText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }
    
    /**
     * Disable bidding for a specific product when timer ends
     */
    private void disableBidding(int productId) {
        // Find the product views
        EditText bidInput = productBidInputs.get(productId);
        if (bidInput == null) return;

        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;

        // Find the bid button
        View productCard = bidInput.getRootView();
        Button bidButton = productCard.findViewById(R.id.product_bid_button);
        if (bidButton == null) return;

        // IMPORTANT: Kill any restart timers
        cleanupProductTimers(productId);

        // Disable bidding controls permanently
        bidButton.setEnabled(false);
        bidButton.setText("SOLD");
        bidButton.setTextColor(getResources().getColor(android.R.color.white));
        bidButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        bidInput.setEnabled(false);
        
        // Update timer text with status message to clearly show product is sold
        timerText.setText("SOLD - BIDDING CLOSED");
        timerText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

        // Find the product name and product ID
        String productName = "";
        String productIdStr = "";
        for (Product product : productList) {
            if (product.getId() == productId) {
                productName = product.getProductName();
                productIdStr = product.getProductId();
                
                // Also update the product's status in Firestore to mark it as sold
                if (product.getProductId() != null && !product.getProductId().isEmpty()) {
                    updateProductSoldStatus(product.getProductId(), true);
                }
                
                break;
            }
        }

        // Create final copies for use in lambda expressions
        final String finalProductIdStr = productIdStr;
        final String finalProductName = productName;
        
        // Get the winner information to display
        if (finalProductIdStr != null && !finalProductIdStr.isEmpty()) {
            final TextView finalTimerText = timerText;
            final Button finalBidButton = bidButton;
            final TextView currentBidView = productCurrentBidViews.get(productId);
            
            FirestoreHelper.getBidInfo(finalProductIdStr, new FirestoreHelper.BidInfoCallback() {
                @Override
                public void onBidInfoRetrieved(FirestoreHelper.BidInfo bidInfo) {
                    if (bidInfo != null && bidInfo.getBidAmount() != null && bidInfo.getBidAmount() > 0) {
                        // Get bidder name, either from bidInfo or from product data if missing
                        String bidderName = "Unknown Bidder";
                        if (bidInfo.getBidderName() != null && !bidInfo.getBidderName().isEmpty()) {
                            bidderName = bidInfo.getBidderName();
                        } else {
                            // Try to find from product data
                            for (Product p : productList) {
                                if (p.getProductId().equals(finalProductIdStr)) {
                                    Map<String, Object> productData = p.getOriginalData();
                                    if (productData != null && productData.containsKey("bidder_name")) {
                                        String name = (String) productData.get("bidder_name");
                                        if (name != null && !name.isEmpty()) {
                                            bidderName = name;
                                            
                                            // Complete bidInfo with product data
                                            String bidderMobile = (String) productData.get("bidder_mobile");
                                            String bidderId = (String) productData.get("bidder_id");
                                            
                                            bidInfo = new FirestoreHelper.BidInfo(
                                                bidInfo.getBidAmount(),
                                                bidderId,
                                                name,
                                                bidderMobile,
                                                bidInfo.getTimestamp() != null ? bidInfo.getTimestamp() : System.currentTimeMillis()
                                            );
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        
                        // Create final copies for use in lambdas
                        final String displayBidderName = bidderName;
                        final FirestoreHelper.BidInfo finalBidInfo = bidInfo;
                        
                        // Update timer text to show the winner
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Show winner info in a more prominent way
                                finalTimerText.setText("SOLD TO: " + displayBidderName);
                                finalTimerText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                
                                // Add winning bid amount to the button text
                                if (finalBidInfo.getBidAmount() != null) {
                                    finalBidButton.setText("SOLD: ₹" + finalBidInfo.getBidAmount().intValue());
                                    
                                    // Also update the current bid view to show final amount
                                    if (currentBidView != null) {
                                        currentBidView.setText(String.format("₹%.0f/kg", finalBidInfo.getBidAmount()));
                                    }
                                }
                                
                                // Store the final bid data in Firestore to ensure it persists
                                Map<String, Object> finalBidData = new HashMap<>();
                                finalBidData.put("final_bid_amount", finalBidInfo.getBidAmount());
                                finalBidData.put("final_bidder_name", displayBidderName);
                                finalBidData.put("final_bidder_mobile", finalBidInfo.getBidderMobile());
                                finalBidData.put("is_sold", true);
                                finalBidData.put("sold_to", displayBidderName);
                                finalBidData.put("sold_amount", finalBidInfo.getBidAmount());
                                finalBidData.put("sold_at", System.currentTimeMillis());
                                finalBidData.put("bid_status", "completed");
                                finalBidData.put("restarting", false);
                                
                                // Use the effectively final variable
                                FirestoreHelper.updateProduct(finalProductIdStr, finalBidData,
                                    new FirestoreHelper.SaveCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Final bid data stored for product: " + finalProductIdStr);
                                            
                                            // Find product to create receipt
                                            for (Product p : productList) {
                                                if (p.getProductId().equals(finalProductIdStr)) {
                                                    // Create receipts with the updated bid info
                                                    checkAndCreateReceipts(p, finalBidInfo);
                                                    break;
                                                }
                                            }
                                        }
                                        
                                        @Override
                                        public void onError(Exception e) {
                                            Log.e(TAG, "Error storing final bid data: " + e.getMessage());
                                        }
                                    });
                            });
                        }
                    } else {
                        // No valid bid amount found - unusual case, but handle it
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                finalTimerText.setText("BIDDING CLOSED");
                                finalTimerText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                            });
                        }
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error getting winner info", e);
                }
            });
        }

        if (!finalProductName.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Bidding for " + finalProductName + " is now closed. Product marked as SOLD OUT.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Update the product's sold status in Firestore
     */
    private void updateProductSoldStatus(String productId, boolean isSold) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("is_sold", isSold);
        updateData.put("sold_at", System.currentTimeMillis());
           
        // Replace direct access to private productsCollection with proper method call
        FirestoreHelper.updateProduct(productId, updateData, 
            new FirestoreHelper.SaveCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Product marked as sold: " + productId);
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to mark product as sold: " + productId, e);
                }
            });
    }
    
    /**
     * Handle bid placement
     */
    private void placeBid(Product product, String bidAmountStr) {
        int productId = product.getId();
        String productIdStr = product.getProductId();
        TextView yourBidText = productYourBidViews.get(productId);
        String productName = product.getProductName();
        
        Log.d(TAG, "🔍 PLACING BID for " + productName + " - amount: " + bidAmountStr);
        
        try {
            // Parse bid amount
            float newBid = Float.parseFloat(bidAmountStr);
            
            // Validate bid value
            if (newBid <= 0) {
                Toast.makeText(requireContext(), "Bid amount must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get current bid from the current bid view
            TextView currentBidView = productCurrentBidViews.get(productId);
            if (currentBidView == null) return;
            
            String currentBidText = currentBidView.getText().toString();
            float currentBid = 0.0f;
            if (!currentBidText.isEmpty()) {
                try {
                currentBid = Float.parseFloat(currentBidText.replace("₹", "").replace("/kg", ""));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing current bid: " + currentBidText);
                    currentBid = (float)product.getPrice(); // Fallback to base price
                }
            }

            // Validate that the new bid is higher than the current bid
            if (newBid <= currentBid) {
                Toast.makeText(
                        requireContext(),
                        "Your bid of ₹" + newBid + " must be higher than the current bid of ₹" + currentBid + "/kg",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            // Ensure the bid meets minimum increment
            if (currentBid > 0 && (newBid - currentBid) < MINIMUM_BID_INCREMENT) {
                Toast.makeText(
                        requireContext(),
                        "Bid must increase by at least ₹" + MINIMUM_BID_INCREMENT + "/kg (current: ₹" + currentBid + ")",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            // Check if product is already sold
            if (product.isSold()) {
                Toast.makeText(requireContext(),
                        "This product has already been sold",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if bidding is active for this product
            if (!productBiddingActive.getOrDefault(productId, false)) {
                Toast.makeText(requireContext(),
                        "Bidding is not currently active for this product",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Mark this product as bid on
            productBidStatus.put(productId, true);
            
            // Create final copies for use in lambdas
            final float finalNewBid = newBid;
            
            Log.d(TAG, "✅ Bid validation passed - placing bid of ₹" + finalNewBid + 
                  " by " + currentUserName + " on " + productName + 
                  " (current price: ₹" + currentBid + ")");
            
            // Update the UI immediately for responsiveness
            if (yourBidText != null) {
                yourBidText.setText("₹" + finalNewBid + "/kg");
            }
            if (currentBidView != null) {
                currentBidView.setText("₹" + finalNewBid + "/kg");
                // Highlight the new bid with a color flash
                ValueAnimator colorAnim = ValueAnimator.ofObject(
                    new ArgbEvaluator(),
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.black)
                );
                colorAnim.setDuration(1000);
                colorAnim.addUpdateListener(animator -> {
                    currentBidView.setTextColor((int) animator.getAnimatedValue());
                });
                colorAnim.start();
            }
            
            // Ensure essential user info is available
            String bidderName = currentUserName;
            String bidderMobile = currentUserMobile;
            
            if (bidderName == null || bidderName.isEmpty()) {
                bidderName = "Anonymous Bidder";
            }
            
            if (bidderMobile == null || bidderMobile.isEmpty()) {
                // Use device ID or generate random ID
                bidderMobile = "unknown-" + UUID.randomUUID().toString().substring(0, 8);
            }
            
            // Create final copies for lambda use
            final String finalBidderName = bidderName;
            final String finalBidderMobile = bidderMobile;
            
            // Reset bidding timer (extend by +90 seconds)
            // Do this first to ensure the timer doesn't expire
            resetBiddingTimer(productId);
            
            // Update in Firestore
            Map<String, Object> bidData = new HashMap<>();
            bidData.put("current_bid", finalNewBid);
            bidData.put("bidder_name", finalBidderName);
            bidData.put("bidder_mobile", finalBidderMobile);
            bidData.put("bidder_id", sessionManager.getUserId());
            bidData.put("bid_timestamp", System.currentTimeMillis());
            
            Log.d(TAG, "Updating Firestore with bid data: " + bidData);
            
            FirestoreHelper.updateProductBid(productIdStr, finalNewBid, finalBidderName, finalBidderMobile, 
                new FirestoreHelper.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() == null) return;
                        
                        Log.d(TAG, "✅ Successfully placed bid of ₹" + finalNewBid + " for " + productName);
                        
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(
                                requireContext(),
                                "Your bid of ₹" + finalNewBid + " placed for " + productName,
                                Toast.LENGTH_SHORT
                            ).show();
                            
                            // Clear the input field after successful bid
                            EditText bidInput = productBidInputs.get(productId);
                            if (bidInput != null) {
                                bidInput.setText("");
                                bidInput.requestFocus();
                            }
                            
                            // Highlight the product card to show the bid was successful
                            if (bidInput != null) {
                                final View productCard = bidInput.getRootView();
                                if (productCard != null) {
                                    // Flash effect to indicate success
                                    ValueAnimator colorAnim = ValueAnimator.ofObject(
                                        new ArgbEvaluator(),
                                        getResources().getColor(android.R.color.holo_green_light),
                                        getResources().getColor(android.R.color.white)
                                    );
                                    colorAnim.setDuration(800);
                                    colorAnim.addUpdateListener(animator -> {
                                        productCard.setBackgroundColor((int) animator.getAnimatedValue());
                                    });
                                    colorAnim.start();
                                }
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        if (getActivity() == null) return;
                        
                        Log.e(TAG, "❌ Error placing bid: " + e.getMessage(), e);
                        
                        getActivity().runOnUiThread(() -> {
            Toast.makeText(
                requireContext(),
                                "Failed to place bid: " + e.getMessage(),
                Toast.LENGTH_SHORT
            ).show();
                        });
                    }
                }
            );

        } catch (NumberFormatException e) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid bid amount",
                Toast.LENGTH_SHORT
            ).show();
        }
    }
    
    /**
     * Start a timer to wait until bidding starts for a product
     */
    private void startWaitingTimer(Product product) {
        int productId = product.getId();
        Map<String, Object> productData = product.getOriginalData();
        
        if (productData == null || !productData.containsKey("bidding_start_time")) {
            return;
        }
        
        // Get the bidding start time
        long biddingStartTime = 0;
        Object startTimeObj = productData.get("bidding_start_time");
        if (startTimeObj instanceof Long) {
            biddingStartTime = (Long) startTimeObj;
        }
        
        long currentTime = System.currentTimeMillis();
        long waitTime = biddingStartTime - currentTime;
        
        if (waitTime <= 0) {
            // Already ready for bidding
            refreshBiddingStatus(product);
            return;
        }
        
        // Get the timer text view
        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;
        
        // Create a timer to count down until bidding starts
        CountDownTimer timer = new CountDownTimer(waitTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                timerText.setText(String.format(Locale.getDefault(), 
                    "Bidding starts in: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                // Refresh the fragment to show active bidding
                refreshBiddingStatus(product);
            }
        }.start();
        
        productTimers.put(productId, timer);
    }
    
    /**
     * Refresh the status of a product when it becomes ready for bidding
     */
    private void refreshBiddingStatus(Product product) {
        // Create a new product card for active bidding
        View oldCard = productCardsContainer.getChildAt(product.getId());
        if (oldCard != null) {
            productCardsContainer.removeView(oldCard);
        }
        
        View newCard = createProductBidCard(product, false);
        productCardsContainer.addView(newCard);
        
        // Initialize bid status
        int productId = product.getId();
        productBidStatus.put(productId, false);
        productBiddingActive.put(productId, false);
        
        // Check if there's already a timer running in Firestore
        checkOrStartBiddingSession(product);
        
        // Set up real-time bid listener for this product
        setupBidListener(product);
        
        // Notify user
        Toast.makeText(requireContext(), 
            "Bidding is now active for " + product.getProductName(), 
            Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Check if receipts exist for a product and create them if not
     */
    private void checkAndCreateReceipts(Product product, FirestoreHelper.BidInfo bidInfo) {
        String productIdStr = product.getProductId();
        
        // Check if a receipt has already been created for this product
        FirestoreHelper.getProductById(productIdStr, new FirestoreHelper.ProductCallback() {
            @Override
            public void onProductLoaded(Map<String, Object> productData) {
                if (productData == null) {
                    Log.e(TAG, "Error: Could not find product data for " + productIdStr);
                    return;
                }
                
                // Check if receipt_created flag exists and is true
                Boolean receiptCreated = false;
                if (productData.containsKey("receipt_created")) {
                    Object receiptObj = productData.get("receipt_created");
                    if (receiptObj instanceof Boolean) {
                        receiptCreated = (Boolean) receiptObj;
                    }
                }
                
                if (receiptCreated) {
                    Log.d(TAG, "Receipt already created for product " + product.getProductName() + ". Skipping creation.");
                    return;
                }
                
                // Continue with receipt creation since none exists yet
                if (bidInfo != null) {
                    // Create receipts with the provided bid info
                    createReceiptsForProduct(product, bidInfo);
                } else {
                    // Fall back to just the product if no bid info available
                    createReceiptsForProduct(product);
                }
                
                // Mark product as having receipts created to prevent duplicates
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("receipt_created", true);
                
                FirestoreHelper.updateProduct(productIdStr, updateData, new FirestoreHelper.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Product marked as having receipts created: " + productIdStr);
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error marking product as having receipts: " + e.getMessage());
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking product receipt status: " + e.getMessage());
            }
        });
    }
    
    /**
     * Create receipts for a product that has completed bidding with known bid info
     */
    private void createReceiptsForProduct(Product product, FirestoreHelper.BidInfo bidInfo) {
        if (bidInfo == null || bidInfo.getBidAmount() == null || bidInfo.getBidAmount() <= 0) {
            // Fall back to regular method if bid info is invalid
            createReceiptsForProduct(product);
            return;
        }
        
        final String productIdStr = product.getProductId();
        final String productName = product.getProductName();
        final int quantity = (int) product.getQuantity();
        
        // Get current user info to avoid lambda capturing issues
        final SessionManager sessionManager = new SessionManager(requireContext());
        final String currentUserPhone = sessionManager.getMobile();
        
        // We already have the bid info, no need to query again
        Log.d(TAG, "Using provided bid info: " + bidInfo.getBidAmount() + " by " + bidInfo.getBidderName());
            
        // Get farmer details from product data
        final String farmerId;
        final String farmerName;
        final String farmerPhone;
        
        Map<String, Object> productData = product.getOriginalData(); 
            
        if (productData != null) {
            farmerId = productData.containsKey("farmer_id") ? 
                String.valueOf(productData.get("farmer_id")) : "";
            farmerName = productData.containsKey("farmer_name") ? 
                String.valueOf(productData.get("farmer_name")) : "";
            farmerPhone = productData.containsKey("farmer_mobile") ? 
                String.valueOf(productData.get("farmer_mobile")) : "";
        } else {
            farmerId = "";
            farmerName = "";
            farmerPhone = "";
        }
        
        // Debug logs for farmer info
        Log.d(TAG, "Farmer details - ID: " + farmerId + ", Name: " + farmerName + ", Phone: " + farmerPhone);
            
        // Get member (winner) details from bid info
        final String memberId = bidInfo.getBidderId() != null ? bidInfo.getBidderId() : "";
        final String memberName = bidInfo.getBidderName() != null ? bidInfo.getBidderName() : "";
        final String memberPhone = bidInfo.getBidderMobile() != null ? bidInfo.getBidderMobile() : "";
        
        // Debug logs for member info
        Log.d(TAG, "Member details - ID: " + memberId + ", Name: " + memberName + ", Phone: " + memberPhone);
            
        // Create a receipt for this winning bid
        final int bidAmount = bidInfo.getBidAmount().intValue();
        final int finalQuantity = quantity;
        
        // Debug log for bid amount and quantity
        Log.d(TAG, "Bid amount: " + bidAmount + ", Quantity: " + finalQuantity);
        
        // Get the user type from SessionManager
        String userType = sessionManager.getUserType();
        String currentUserId = sessionManager.getUserId();
        
        // Debug logs for current user
        Log.d(TAG, "Current user - ID: " + currentUserId + ", Phone: " + currentUserPhone + ", Type: " + userType);
        
        // Ensure we have a valid product name
        String verifiedProductName = product.getProductName();
        
        if (verifiedProductName == null || verifiedProductName.isEmpty()) {
            Log.w(TAG, "Product name is invalid - attempting to get correct name");
            
            // Try to get the product name from the original data
            if (productData != null && productData.containsKey("product_name")) {
                String nameFromData = (String) productData.get("product_name");
                if (nameFromData != null && !nameFromData.isEmpty()) {
                    verifiedProductName = nameFromData;
                    Log.d(TAG, "Using product name from original data: " + verifiedProductName);
                }
            }
            
            // If we still don't have a valid name, create one from the ID
            if (verifiedProductName == null || verifiedProductName.isEmpty()) {
                verifiedProductName = "Product-" + productIdStr.substring(0, Math.min(productIdStr.length(), 8));
                Log.d(TAG, "Using derived product name: " + verifiedProductName);
            }
        }
        
        // Add debugging to verify product name
        Log.d(TAG, "Creating receipt with product name: " + verifiedProductName);
        Log.d(TAG, "Product details - ID: " + productIdStr + ", Name: " + verifiedProductName + ", Quantity: " + finalQuantity);
        
        // Use the verified product name for all receipt creation calls
        final String finalVerifiedProductName = verifiedProductName;
        
        // Create receipt service with context
        ReceiptService receiptService = new ReceiptService(requireContext());
        
        // Flag to track if we've created a receipt
        final boolean[] receiptCreated = {false};
        
        // Store these final references for use in lambdas
        final String finalFarmerPhone = farmerPhone;
        final String finalMemberPhone = memberPhone;
        
        // Always create both receipts to ensure they're properly recorded
        // Receipt for the farmer
        receiptService.createReceiptAsFarmer(
            requireContext(),
                        productIdStr,
            finalVerifiedProductName,
                        finalQuantity,
                        bidAmount,
                        memberId,
                        memberName,
                        memberPhone,
                ""
        ).addOnSuccessListener(documentReference -> {
            Log.d(TAG, "Farmer receipt created successfully with ID: " + documentReference.getId());
            receiptCreated[0] = true;
                    
                        if (getActivity() == null) return;
                        
            if (currentUserPhone != null && currentUserPhone.equals(finalFarmerPhone)) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(),
                            "Receipt created for " + memberName + "'s winning bid on " + finalVerifiedProductName + "!",
                                    Toast.LENGTH_SHORT).show();
                        });
            }
                    }).addOnFailureListener(e -> {
                        if (getActivity() == null) return;
                        
            Log.e(TAG, "Error creating farmer receipt", e);
        });
        
        // Receipt for the member/buyer
        receiptService.createReceiptAsMember(
            requireContext(),
            productIdStr,
            finalVerifiedProductName,
            finalQuantity,
            bidAmount,
            farmerId,
            farmerName,
            farmerPhone,
                ""
        ).addOnSuccessListener(documentReference -> {
            Log.d(TAG, "Member receipt created successfully with ID: " + documentReference.getId());
            receiptCreated[0] = true;
            
            if (getActivity() == null) return;
            
            if (currentUserPhone != null && currentUserPhone.equals(finalMemberPhone)) {
                        getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Receipt created for your winning bid on " + finalVerifiedProductName + "!",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> {
            if (getActivity() == null) return;
            
            Log.e(TAG, "Error creating member receipt", e);
        });
        
        // Mark the product as sold
        Map<String, Object> soldUpdate = new HashMap<>();
        soldUpdate.put("is_sold", true);
        soldUpdate.put("sold_at", System.currentTimeMillis());
        soldUpdate.put("sold_to", memberName);
        soldUpdate.put("sold_to_mobile", memberPhone);
        soldUpdate.put("sold_amount", bidAmount);
        
        FirestoreHelper.updateProduct(productIdStr, soldUpdate, new FirestoreHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Product marked as sold in database: " + productIdStr);
                }
                
                @Override
                public void onError(Exception e) {
                Log.e(TAG, "Error marking product as sold", e);
            }
        });
    }
    
    /**
     * Create receipts for a product that has completed bidding 
     * (original method without explicit bid info)
     */
    private void createReceiptsForProduct(Product product) {
        final String productIdStr = product.getProductId();
        final String productName = product.getProductName();
        final int quantity = (int) product.getQuantity();
        
        // Get current user info to avoid lambda capturing issues
        final SessionManager sessionManager = new SessionManager(requireContext());
        final String currentUserPhone = sessionManager.getMobile();
        
        FirestoreHelper.getBidInfo(productIdStr, new FirestoreHelper.BidInfoCallback() {
            @Override
            public void onBidInfoRetrieved(FirestoreHelper.BidInfo bidInfo) {
                if (bidInfo == null || bidInfo.getBidAmount() == null || bidInfo.getBidAmount() <= 0) {
                    // No valid bid found - check for final bid data in product
                    FirestoreHelper.getProductById(productIdStr, new FirestoreHelper.ProductCallback() {
                        @Override
                        public void onProductLoaded(Map<String, Object> productData) {
                            if (productData != null && productData.containsKey("final_bid_amount")) {
                                // Use the stored final bid info
                                Double finalBidAmount = null;
                                String finalBidderName = null;
                                String finalBidderMobile = null;
                                
                                Object bidAmountObj = productData.get("final_bid_amount");
                                if (bidAmountObj instanceof Double) {
                                    finalBidAmount = (Double) bidAmountObj;
                                } else if (bidAmountObj instanceof Long) {
                                    finalBidAmount = ((Long) bidAmountObj).doubleValue();
                                } else if (bidAmountObj instanceof Integer) {
                                    finalBidAmount = ((Integer) bidAmountObj).doubleValue();
                                }
                                
                                finalBidderName = (String) productData.get("final_bidder_name");
                                finalBidderMobile = (String) productData.get("final_bidder_mobile");
                                
                                if (finalBidAmount != null && finalBidAmount > 0) {
                                    // Create a synthetic BidInfo and use it
                                    FirestoreHelper.BidInfo syntheticBidInfo = 
                                        new FirestoreHelper.BidInfo(
                                            finalBidAmount, null, finalBidderName, finalBidderMobile, null);
                                    
                                    createReceiptsForProduct(product, syntheticBidInfo);
                                    return;
                                }
                            }
                            
                            // If no valid bid data found at all
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), 
                                        "No valid bids were placed for " + productName + ". No receipt created.", 
                                        Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error getting product for receipt creation: " + e.getMessage());
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), 
                                        "Error creating receipt: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                    return;
                }
                
                // If we got valid bid info, use the other method
                createReceiptsForProduct(product, bidInfo);
            }
                
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting bid info for receipt creation", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "Error creating receipt: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel all timers to prevent memory leaks
        for (CountDownTimer timer : productTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
        
        // Remove all bid listeners
        clearBidListeners();
        
        // Remove all timer listeners
        clearTimerListeners();
        
        // Clear the handler callbacks
        mainHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Clean up any existing timers for a product
     */
    private void cleanupProductTimers(int productId) {
        // Cancel existing timer if any
        if (productTimers.containsKey(productId)) {
            CountDownTimer existingTimer = productTimers.get(productId);
            if (existingTimer != null) {
                Log.d(TAG, "🧹 Cancelling existing timer for product ID: " + productId);
                existingTimer.cancel();
            }
            productTimers.remove(productId);
        }
    }
} 
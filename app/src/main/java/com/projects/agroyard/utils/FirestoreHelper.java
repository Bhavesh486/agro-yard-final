package com.projects.agroyard.utils;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.projects.agroyard.constants.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";
    private static final String PRODUCTS_COLLECTION = "products";

    private static final String USERS_COLLECTION = "users";

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static CollectionReference productsCollection = db.collection(PRODUCTS_COLLECTION);
    private static CollectionReference usersCollection = db.collection(USERS_COLLECTION);

    public static void getUser(String userId, UserNameCallback userCallback) {
        if (userId == null || userId.isEmpty()) {
            userCallback.onError(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        usersCollection.document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> userData = documentSnapshot.getData();
                        // Ensure product ID is included
                        if (userData != null && userData.containsKey("name")) {

                            userCallback.onUserLoaded((String) userData.get("name"));
                        }
                    } else {
                        Log.w(TAG, "User not found with ID: " + userId);
                        userCallback.onUserLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting User by ID: " + userId, e);
                    userCallback.onError(e);
                });
    }


    // Interface for callbacks
    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    // Interface for retrieving products
    public interface ProductsCallback {
        void onProductsLoaded(List<Map<String, Object>> products);
        void onError(Exception e);
    }

    public interface UserCallback {
        void onUserLoaded(String district);
        void onError(Exception e);
    }

    public interface UserNameCallback {
        void onUserLoaded(String userName);
        void onError(Exception e);
    }

    // Interface for retrieving a single product
    public interface ProductCallback {
        void onProductLoaded(Map<String, Object> productData);
        void onError(Exception e);
    }

    // Interface for retrieving farmers
    public interface FarmersCallback {
        void onFarmersLoaded(List<Map<String, Object>> farmers);
        void onError(Exception e);
    }

    public interface SaveCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface DeliveryRequestsCallback {
        void onDeliveryRequestsLoaded(List<Map<String, Object>> deliveryRequests);
        void onError(Exception e);
    }

    public interface BidInfoCallback {
        void onBidInfoRetrieved(BidInfo bidInfo);
        void onError(Exception e);
    }

    /**
     * Add a product to Firestore
     * @param productData Map containing product data
     * @param callback Callback to handle success/failure
     */
    public static void addProduct(Map<String, Object> productData, FirestoreCallback callback) {
        // Add timestamp field
        productData.put("timestamp", System.currentTimeMillis());
        
        // Explicitly ensure bidding fields are properly initialized with clean values
        // This prevents any chance of inheriting data from previously viewed products
        double basePrice = 0;
        if (productData.containsKey("price")) {
            Object priceObj = productData.get("price");
            if (priceObj instanceof Double) {
                basePrice = (Double) priceObj;
            } else if (priceObj instanceof Long) {
                basePrice = ((Long) priceObj).doubleValue();
            } else if (priceObj instanceof Integer) {
                basePrice = ((Integer) priceObj).doubleValue();
            } else if (priceObj instanceof String) {
                try {
                    basePrice = Double.parseDouble((String) priceObj);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid price format: " + priceObj);
                }
            }
        }
        
        // Initialize with clean values regardless of what was passed
        productData.put("current_bid", basePrice);
        productData.put("bidder_name", null);
        productData.put("bidder_mobile", null);
        productData.put("bidder_id", null);
        productData.put("bid_timestamp", null);
        productData.put("is_sold", false);
        productData.put("sold_to", null);
        productData.put("sold_amount", null);
        productData.put("sold_at", null);
        productData.put("final_bid_amount", null);
        productData.put("final_bidder_name", null);
        productData.put("final_bidder_mobile", null);
        productData.put("receipt_created", false);
        
        Log.d(TAG, "Creating new product with clean bidding state. Base price: " + basePrice);
        
        productsCollection.add(productData)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    String documentId = documentReference.getId();
                    Log.d(TAG, "Product added with ID: " + documentId);
                    
                    // Update the document with its ID for easy reference
                    Map<String, Object> idUpdate = new HashMap<>();
                    idUpdate.put("product_id", documentId);
                    
                    documentReference.update(idUpdate)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(documentId))
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error updating document ID", e);
                            // Return success anyway since product was created
                            callback.onSuccess(documentId);
                        });
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error adding product", e);
                    callback.onFailure(e);
                }
            });
    }

    /**
     * Retrieve all products from Firestore
     * @param callback Callback to handle retrieved products
     */
    public static void getAllProducts(ProductsCallback callback) {
        productsCollection.orderBy("timestamp")
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Map<String, Object>> productsList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> productData = document.getData();
                            // Ensure the product ID is included
                            if (!productData.containsKey("product_id")) {
                                productData.put("product_id", document.getId());
                            }
                            productsList.add(productData);
                        }
                        callback.onProductsLoaded(productsList);
                    } else {
                        Log.w(TAG, "Error getting products", task.getException());
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Retrieve all products that are available for bidding from Firestore
     * Products are considered available for bidding if register_for_bidding is true or not specified
     * @param callback Callback to handle retrieved products
     */
    public static void getBiddingProducts(ProductsCallback callback) {
        productsCollection
            .orderBy("timestamp")
            // Force server query, don't use cache
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Map<String, Object>> biddingProductsList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> productData = document.getData();
                            
                            // Ensure the product ID is included
                            if (!productData.containsKey("product_id")) {
                                productData.put("product_id", document.getId());
                            }
                            
                            // Log product image data
                            String productName = productData.containsKey("product_name") ? 
                                    productData.get("product_name").toString() : "Unknown";
                            
                            Log.d(TAG, "Processing product document: " + document.getId() + " - " + productName);
                            
                            // Check if the product is registered for bidding (default to true if not specified)
                            Boolean isForBidding = true;
                            if (productData.containsKey("register_for_bidding")) {
                                if (productData.get("register_for_bidding") instanceof Boolean) {
                                    isForBidding = (Boolean) productData.get("register_for_bidding");
                                } else {
                                    // Handle non-Boolean values (convert numeric to boolean)
                                    Object regObj = productData.get("register_for_bidding");
                                    if (regObj instanceof Long) {
                                        isForBidding = ((Long) regObj) == 1;
                                    } else if (regObj instanceof Integer) {
                                        isForBidding = ((Integer) regObj) == 1;
                                    }
                                }
                            }
                            
                            // Add to bidding products list if it's for bidding
                            if (isForBidding) {
                                // Check if this product has a delayed bidding start time
                                if (productData.containsKey("bidding_start_time")) {
                                    long biddingStartTime = 0;
                                    Object startTimeObj = productData.get("bidding_start_time");
                                    if (startTimeObj instanceof Long) {
                                        biddingStartTime = (Long) startTimeObj;
                                    }
                                    
                                    long currentTime = System.currentTimeMillis();
                                    if (biddingStartTime > currentTime) {
                                        // Bidding hasn't started yet, but we still show the product
                                        productData.put("bidding_status", "waiting");
                                        Log.d(TAG, "Product waiting for bidding to start: " + productName);
                                    } else {
                                        // Bidding is active
                                        productData.put("bidding_status", "active");
                                        Log.d(TAG, "Product active for bidding: " + productName);
                                    }
                                } else {
                                    // No start time specified, consider it active
                                    productData.put("bidding_status", "active");
                                }
                                
                                // Explicitly clean up any old bidding data for recently uploaded products
                                if (productData.containsKey("timestamp")) {
                                    long uploadTime = 0;
                                    Object timeObj = productData.get("timestamp");
                                    if (timeObj instanceof Long) {
                                        uploadTime = (Long) timeObj;
                                        
                                        // If uploaded in last 5 minutes, ensure clean initial state
                                        long currentTime = System.currentTimeMillis();
                                        if (currentTime - uploadTime < 300000) { // 5 minutes
                                            Log.d(TAG, "Recently uploaded product, ensuring clean state: " + productName);
                                            
                                            // Ensure base price is set as current bid
                                            if (productData.containsKey("price")) {
                                                Object priceObj = productData.get("price");
                                                if (priceObj instanceof Double || priceObj instanceof Long || priceObj instanceof Integer) {
                                                    productData.put("current_bid", priceObj);
                                                }
                                            }
                                            
                                            // Clear bidder information
                                            productData.put("bidder_name", null);
                                            productData.put("bidder_mobile", null);
                                            productData.put("bidder_id", null);
                                        }
                                    }
                                }
                                
                                biddingProductsList.add(productData);
                                Log.d(TAG, "Product available for bidding: " + productName);
                            }
                        }
                        callback.onProductsLoaded(biddingProductsList);
                    } else {
                        Log.w(TAG, "Error getting bidding products", task.getException());
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Get products for a specific farmer
     * @param farmerMobile The farmer's mobile number
     * @param callback Callback to handle retrieved products
     */
    public static void getFarmerProducts(String farmerMobile, ProductsCallback callback) {
        productsCollection.whereEqualTo("farmer_mobile", farmerMobile)
            .orderBy("timestamp")
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Map<String, Object>> productsList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> productData = document.getData();
                            // Ensure the product ID is included
                            if (!productData.containsKey("product_id")) {
                                productData.put("product_id", document.getId());
                            }
                            productsList.add(productData);
                        }
                        callback.onProductsLoaded(productsList);
                    } else {
                        Log.w(TAG, "Error getting farmer products", task.getException());
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Delete a product from Firestore
     * @param productId The product ID to delete
     * @param callback Callback to handle success/failure
     */
    public static void deleteProduct(String productId, SaveCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }
        
        productsCollection.document(productId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Product successfully deleted: " + productId);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error deleting product", e);
                callback.onError(e);
            });
    }

    /**
     * Get all farmers from Firestore
     * @param callback Callback to handle retrieved farmers
     */
    public static void getAllFarmers(FarmersCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
            .whereEqualTo("userType", Constants.FARMER)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Map<String, Object>> farmersList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String, Object> farmerData = document.getData();
                        // Ensure the user ID is included
                        if (!farmerData.containsKey("userId")) {
                            farmerData.put("userId", document.getId());
                        }
                        farmersList.add(farmerData);
                        Log.d(TAG, "Farmer retrieved: " + farmerData.get("name"));
                    }
                    callback.onFarmersLoaded(farmersList);
                } else {
                    Log.w(TAG, "Error getting farmers", task.getException());
                    callback.onError(task.getException());
                }
            });
    }

    public static void saveDeliveryAddress(Map<String, Object> deliveryData, SaveCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = (String) deliveryData.get("userId");
        
        // Create a unique document ID for this delivery address
        String deliveryId = db.collection("delivery_addresses").document().getId();
        deliveryData.put("deliveryId", deliveryId);
        
        db.collection("delivery_addresses")
            .document(deliveryId)
            .set(deliveryData)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onError(e);
                }
            });
    }

    /**
     * Get all delivery requests for a specific farmer
     * @param farmerId The farmer's ID
     * @param callback Callback to handle retrieved delivery requests
     */
    public static void getFarmerDeliveryRequests(String farmerId, DeliveryRequestsCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("delivery_addresses")
            .whereEqualTo("farmerId", farmerId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Map<String, Object>> deliveryRequests = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String, Object> requestData = document.getData();
                        // Ensure the delivery ID is included
                        if (!requestData.containsKey("deliveryId")) {
                            requestData.put("deliveryId", document.getId());
                        }
                        deliveryRequests.add(requestData);
                        Log.d(TAG, "Delivery request retrieved for farmer: " + requestData.get("farmerName"));
                    }
                    
                    // Sort locally instead of in the query
                    deliveryRequests.sort((r1, r2) -> {
                        Long t1 = (Long) r1.get("timestamp");
                        Long t2 = (Long) r2.get("timestamp");
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t2.compareTo(t1); // Sort by newest first
                    });
                    
                    callback.onDeliveryRequestsLoaded(deliveryRequests);
                } else {
                    Log.w(TAG, "Error getting delivery requests", task.getException());
                    callback.onError(task.getException());
                }
            });
    }

    /**
     * Get products for a specific farmer without using orderBy to avoid composite index requirement
     * @param farmerMobile The farmer's mobile number
     * @param callback Callback to handle retrieved products
     */
    public static void getProductsForCurrentUser(String farmerMobile, ProductsCallback callback) {
        productsCollection.whereEqualTo("farmer_mobile", farmerMobile)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Map<String, Object>> productsList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> productData = document.getData();
                            // Ensure the product ID is included
                            if (!productData.containsKey("product_id")) {
                                productData.put("product_id", document.getId());
                            }
                            productsList.add(productData);
                        }
                        
                        // Sort results by timestamp manually (newest first)
                        if (!productsList.isEmpty()) {
                            productsList.sort((a, b) -> {
                                Long timestampA = (Long) a.getOrDefault("timestamp", 0L);
                                Long timestampB = (Long) b.getOrDefault("timestamp", 0L);
                                return timestampB.compareTo(timestampA); // Descending order
                            });
                        }
                        
                        callback.onProductsLoaded(productsList);
                    } else {
                        Log.w(TAG, "Error getting farmer products", task.getException());
                        callback.onError(task.getException());
                    }
                }
            });
    }

    /**
     * Update a product's current bid information
     * @param productId The product ID to update
     * @param bidAmount The new bid amount
     * @param bidderName The name of the bidder (optional)
     * @param bidderMobile The mobile number of the bidder
     * @param callback Callback to handle success/failure
     */
    public static void updateProductBid(String productId, double bidAmount, String bidderName, 
                                       String bidderMobile, SaveCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }
        
        Map<String, Object> bidData = new HashMap<>();
        bidData.put("current_bid", bidAmount);
        bidData.put("bidder_mobile", bidderMobile);
        if (bidderName != null && !bidderName.isEmpty()) {
            bidData.put("bidder_name", bidderName);
        }
        bidData.put("bid_timestamp", System.currentTimeMillis());
        
        productsCollection.document(productId)
            .update(bidData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Bid successfully updated for product: " + productId);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error updating bid", e);
                callback.onError(e);
            });
    }

    public static void updateProductBidStatus(String productId, String status, SaveCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }

        Map<String, Object> bidData = Map.of("bid_status", status);
        productsCollection.document(productId)
                .update(bidData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bid status updated for product: " + productId + " to " + status);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating bid status", e);
                    callback.onError(e);
                });

    }
    
    /**
     * Set up real-time listener for bids on a specific product
     * @param productId The product ID to listen for bid changes
     * @param listener Listener to handle bid updates
     * @return The registration object that can be used to remove the listener
     */
    public static com.google.firebase.firestore.ListenerRegistration listenForBidUpdates(
            String productId, BidUpdateListener listener) {
        return productsCollection.document(productId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed for product " + productId, e);
                    listener.onError(e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    if (data != null) {
                        // Extract bid information
                        Double currentBid = null;
                        String bidderName = null;
                        String bidderMobile = null;
                        Long bidTimestamp = null;
                        
                        if (data.containsKey("current_bid")) {
                            Object bidObj = data.get("current_bid");
                            if (bidObj instanceof Double) {
                                currentBid = (Double) bidObj;
                            } else if (bidObj instanceof Long) {
                                currentBid = ((Long) bidObj).doubleValue();
                            } else if (bidObj instanceof Integer) {
                                currentBid = ((Integer) bidObj).doubleValue();
                            }
                        }
                        
                        if (data.containsKey("bidder_name")) {
                            bidderName = (String) data.get("bidder_name");
                        }
                        
                        if (data.containsKey("bidder_mobile")) {
                            bidderMobile = (String) data.get("bidder_mobile");
                        }
                        
                        if (data.containsKey("bid_timestamp")) {
                            bidTimestamp = (Long) data.get("bid_timestamp");
                        }
                        
                        // Create bid info object
                        BidInfo bidInfo = new BidInfo(currentBid, bidderName, bidderMobile, bidTimestamp);
                        listener.onBidUpdated(bidInfo);
                    }
                } else {
                    Log.d(TAG, "Current data: null for product " + productId);
                }
            });
    }
    
    /**
     * Class to hold bid information
     */
    public static class BidInfo {
        private Double bidAmount;
        private String bidderId;
        private String bidderName;
        private String bidderMobile;
        private Long timestamp;
        
        // Constructor for compatibility with existing code (without bidderId)
        public BidInfo(Double bidAmount, String bidderName, String bidderMobile, Long timestamp) {
            this.bidAmount = bidAmount;
            this.bidderName = bidderName;
            this.bidderMobile = bidderMobile;
            this.timestamp = timestamp;
        }
        
        // New constructor with bidderId
        public BidInfo(Double bidAmount, String bidderId, String bidderName, String bidderMobile, Long timestamp) {
            this.bidAmount = bidAmount;
            this.bidderId = bidderId;
            this.bidderName = bidderName;
            this.bidderMobile = bidderMobile;
            this.timestamp = timestamp;
        }
        
        public Double getBidAmount() {
            return bidAmount;
        }
        
        public String getBidderId() {
            return bidderId;
        }
        
        public String getBidderName() {
            return bidderName;
        }
        
        public String getBidderMobile() {
            return bidderMobile;
        }
        
        public Long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Interface for real-time bid updates
     */
    public interface BidUpdateListener {
        void onBidUpdated(BidInfo bidInfo);
        void onError(Exception e);
    }

    /**
     * Update bidding timer information for a product in Firestore
     * @param productId The product ID to update
     * @param endTimeMillis The end time of the bidding session in milliseconds (System.currentTimeMillis() + remaining time)
     * @param callback Callback to handle success/failure
     */
    public static void updateBidTimerInfo(String productId, long endTimeMillis, SaveCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }
        
        Map<String, Object> timerData = new HashMap<>();
        timerData.put("bid_end_time", endTimeMillis);
        timerData.put("timer_updated_at", System.currentTimeMillis());
        
        productsCollection.document(productId)
            .update(timerData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Timer info successfully updated for product: " + productId);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error updating timer info", e);
                callback.onError(e);
            });
    }
    
    /**
     * Interface for real-time timer updates
     */
    public interface TimerUpdateListener {
        void onTimerUpdated(long endTimeMillis);
        void onError(Exception e);
    }
    
    /**
     * Set up real-time listener for timer updates on a specific product
     * @param productId The product ID to listen for timer changes
     * @param listener Listener to handle timer updates
     * @return The registration object that can be used to remove the listener
     */
    public static com.google.firebase.firestore.ListenerRegistration listenForTimerUpdates(
            String productId, TimerUpdateListener listener) {
        return productsCollection.document(productId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed for timer updates on product " + productId, e);
                    listener.onError(e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Map<String, Object> data = snapshot.getData();
                    if (data != null && data.containsKey("bid_end_time")) {
                        Object endTimeObj = data.get("bid_end_time");
                        if (endTimeObj instanceof Long) {
                            long endTimeMillis = (Long) endTimeObj;
                            listener.onTimerUpdated(endTimeMillis);
                        }
                    }
                }
            });
    }

    /**
     * Get the current bid information for a product
     * @param productId The product ID to get bid information for
     * @param callback Callback to handle the retrieved bid information
     */
    public static void getBidInfo(String productId, BidInfoCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }
        
        productsCollection.document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null) {
                        // Extract bid information
                        Double currentBid = null;
                        String bidderId = null;
                        String bidderName = null;
                        String bidderMobile = null;
                        Long bidTimestamp = null;
                        
                        if (data.containsKey("current_bid")) {
                            Object bidObj = data.get("current_bid");
                            if (bidObj instanceof Double) {
                                currentBid = (Double) bidObj;
                            } else if (bidObj instanceof Long) {
                                currentBid = ((Long) bidObj).doubleValue();
                            } else if (bidObj instanceof Integer) {
                                currentBid = ((Integer) bidObj).doubleValue();
                            }
                        }
                        
                        if (data.containsKey("bidder_id")) {
                            bidderId = (String) data.get("bidder_id");
                        }
                        
                        if (data.containsKey("bidder_name")) {
                            bidderName = (String) data.get("bidder_name");
                        }
                        
                        if (data.containsKey("bidder_mobile")) {
                            bidderMobile = (String) data.get("bidder_mobile");
                        }
                        
                        if (data.containsKey("bid_timestamp")) {
                            bidTimestamp = (Long) data.get("bid_timestamp");
                        }
                        
                        // Create bid info object with additional bidder ID
                        BidInfo bidInfo = new BidInfo(currentBid, bidderId, bidderName, bidderMobile, bidTimestamp);
                        callback.onBidInfoRetrieved(bidInfo);
                    } else {
                        callback.onError(new Exception("No data found for product ID: " + productId));
                    }
                } else {
                    callback.onError(new Exception("No document found for product ID: " + productId));
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error getting bid info for product: " + productId, e);
                callback.onError(e);
            });
    }
    
    /**
     * Update a product in Firestore
     * @param productId The product ID to update
     * @param updateData Map containing fields to update
     * @param callback Callback to handle success/failure
     */
    public static void updateProduct(String productId, Map<String, Object> updateData, SaveCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }
        
        productsCollection.document(productId)
            .update(updateData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Product successfully updated: " + productId);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error updating product: " + productId, e);
                callback.onError(e);
            });
    }
    
    /**
     * Special update method for resetting all bidding data on a product
     * This ensures all previous bidding data is cleared to start fresh
     * @param productId The product ID to update
     * @param updateData Map containing fresh bidding data
     * @param callback Callback to handle success/failure
     */
    public static void updateProductWithBidReset(String productId, Map<String, Object> updateData, SaveCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }
        
        // Add update timestamp
        updateData.put("timer_updated_at", System.currentTimeMillis());
        
        // Get current product data first
        productsCollection.document(productId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> existingData = documentSnapshot.getData();
                    
                    // Create a map of fields to explicitly set to null or reset
                    Map<String, Object> resetData = new HashMap<>();
                    
                    // Make sure these fields are included in our update
                    if (!updateData.containsKey("current_bid")) {
                        if (existingData.containsKey("price")) {
                            Object priceObj = existingData.get("price");
                            if (priceObj instanceof Double) {
                                resetData.put("current_bid", (Double) priceObj);
                            } else if (priceObj instanceof Long) {
                                resetData.put("current_bid", ((Long) priceObj).doubleValue());
                            } else if (priceObj instanceof Integer) {
                                resetData.put("current_bid", ((Integer) priceObj).doubleValue());
                            } else {
                                resetData.put("current_bid", 0.0);
                            }
                        } else {
                            resetData.put("current_bid", 0.0);
                        }
                    }
                    
                    // Always reset bidder information
                    resetData.put("bidder_name", null);
                    resetData.put("bidder_mobile", null);
                    resetData.put("bidder_id", null);
                    resetData.put("bid_timestamp", null);
                    
                    // Merge with the update data
                    resetData.putAll(updateData);
                    
                    // Perform the update with reset data
                    productsCollection.document(productId)
                        .update(resetData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Product bidding data reset for: " + productId);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error resetting product bidding data: " + productId, e);
                            callback.onError(e);
                        });
                } else {
                    callback.onError(new Exception("Product not found with ID: " + productId));
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Error getting product for reset: " + productId, e);
                callback.onError(e);
            });
    }

    /**
     * Get a specific product by ID from Firestore
     * @param productId The product ID to retrieve
     * @param callback Callback to handle the retrieved product
     */
    public static void getProductById(String productId, ProductCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }
        
        productsCollection.document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> productData = documentSnapshot.getData();
                    // Ensure product ID is included
                    if (productData != null && !productData.containsKey("product_id")) {
                        productData.put("product_id", documentSnapshot.getId());
                    }
                    callback.onProductLoaded(productData);
                } else {
                    Log.w(TAG, "Product not found with ID: " + productId);
                    callback.onProductLoaded(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting product by ID: " + productId, e);
                callback.onError(e);
            });
    }

    public static void getDistrict(String userId, UserCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Product ID cannot be null or empty"));
            return;
        }

        usersCollection.document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> userData = documentSnapshot.getData();
                        // Ensure product ID is included
                        if (userData != null && userData.containsKey("district")) {

                            callback.onUserLoaded((String) userData.get("district"));
                        }
                    } else {
                        Log.w(TAG, "User not found with ID: " + userId);
                        callback.onUserLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting User by ID: " + userId, e);
                    callback.onError(e);
                });

    }
} 
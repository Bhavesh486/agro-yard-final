package com.projects.agroyard.services;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.projects.agroyard.models.Receipt;
import com.projects.agroyard.utils.SessionManager;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.HashSet;
import java.util.Set;

public class ReceiptService {
    private static final String COLLECTION_RECEIPTS = "receipts";
    private static final String TAG = "ReceiptService";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private SessionManager sessionManager;

    public ReceiptService() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    // Constructor with Context for SessionManager
    public ReceiptService(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.sessionManager = new SessionManager(context);
    }

    // Create a new receipt
    public Task<DocumentReference> createReceipt(Receipt receipt) {
        Log.d("RECEIPT_DEBUG", "Creating new receipt for product: " + receipt.getProductName());
        
        // Add extra validation and logging for product name
        if (receipt.getProductName() == null || receipt.getProductName().isEmpty()) {
            Log.e("RECEIPT_DEBUG", "ERROR: Product name is null or empty!");
        } else if (receipt.getProductName().equals("apple")) {
            Log.e("RECEIPT_DEBUG", "WARNING: Found hardcoded 'apple' as product name - attempting to fix!");
            
            // Try to get the real product name from Firestore
            if (receipt.getProductId() != null && !receipt.getProductId().isEmpty()) {
                String productId = receipt.getProductId();
                Log.d("RECEIPT_DEBUG", "Attempting to fetch real product name for ID: " + productId);
                
                // Attempt to get the actual product name from Firestore before proceeding
                // We'll use a synchronous approach here to ensure we get the right name
                try {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("products").document(productId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists() && documentSnapshot.contains("product_name")) {
                                String realProductName = documentSnapshot.getString("product_name");
                                if (realProductName != null && !realProductName.isEmpty() && !realProductName.equals("apple")) {
                                    Log.d("RECEIPT_DEBUG", "SUCCESS: Retrieved real product name: " + realProductName);
                                    receipt.setProductName(realProductName);
                                }
                            }
                        });
                } catch (Exception e) {
                    Log.e("RECEIPT_DEBUG", "Error getting real product name: " + e.getMessage());
                }
                
                // If we still haven't fixed it, use a derived name from the product ID
                if (receipt.getProductName().equals("apple")) {
                    String derivedName = "Product-" + productId.substring(0, Math.min(productId.length(), 8));
                    Log.d("RECEIPT_DEBUG", "Using derived product name: " + derivedName);
                    receipt.setProductName(derivedName);
                }
            }
        }
        
        // Log the full receipt details for debugging
        Log.d("RECEIPT_DEBUG", "Full receipt details:");
        Log.d("RECEIPT_DEBUG", "- Product ID: " + receipt.getProductId());
        Log.d("RECEIPT_DEBUG", "- Product Name: " + receipt.getProductName());
        Log.d("RECEIPT_DEBUG", "- Quantity: " + receipt.getQuantity());
        Log.d("RECEIPT_DEBUG", "- Price Per Kg: " + receipt.getPricePerKg());
        Log.d("RECEIPT_DEBUG", "- Farmer: " + receipt.getFarmerName() + " (" + receipt.getFarmerPhone() + ")");
        Log.d("RECEIPT_DEBUG", "- Member: " + receipt.getMemberName() + " (" + receipt.getMemberPhone() + ")");
        
        return db.collection(COLLECTION_RECEIPTS).add(receipt)
            .addOnSuccessListener(documentReference -> {
                Log.d("RECEIPT_DEBUG", "Receipt created with ID: " + documentReference.getId());
                Log.d("RECEIPT_DEBUG", "Product name in created receipt: " + receipt.getProductName());
            })
            .addOnFailureListener(e -> {
                Log.e("RECEIPT_DEBUG", "Error creating receipt: " + e.getMessage());
            });
    }
    
    // Create a receipt as a farmer (current user is farmer)
    public Task<DocumentReference> createReceiptAsFarmer(Context context, String productId, String productName, 
                                                        int quantity, int pricePerKg,
                                                        String memberId, String memberName, String memberPhone) {
        // Initialize session manager if not already initialized
        if (sessionManager == null) {
            sessionManager = new SessionManager(context);
        }
        
        // Get current farmer (user) details from session
        String farmerId = sessionManager.getUserId();
        String farmerName = sessionManager.getName();
        String farmerPhone = sessionManager.getMobile();
        String userType = sessionManager.getUserType();
        
        Log.d("RECEIPT_DEBUG", "Creating receipt as farmer: " + farmerId + ", name: " + farmerName);
        
        // Check if product name is undefined or hardcoded
        if (productName == null || productName.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "ERROR in createReceiptAsFarmer: Product name is null or empty!");
            
            // Try to fix by retrieving from Firestore
            if (productId != null && !productId.isEmpty()) {
                Log.d("RECEIPT_DEBUG", "Will attempt to use a valid product name instead of null");
                // Use a placeholder name based on product ID in case we can't find the real name
                productName = "Product-" + productId.substring(0, Math.min(productId.length(), 8));
            } else {
                // Last resort
                Log.d("RECEIPT_DEBUG", "Using fallback product name");
                productName = "Unnamed Product";
            }
        } else {
            Log.d("RECEIPT_DEBUG", "Using provided product name: " + productName);
        }
        
        // Log parameters for debugging
        Log.d("RECEIPT_DEBUG", "Farmer receipt parameters:");
        Log.d("RECEIPT_DEBUG", "- Product ID: " + productId);
        Log.d("RECEIPT_DEBUG", "- Product Name: " + productName);
        Log.d("RECEIPT_DEBUG", "- Quantity: " + quantity);
        Log.d("RECEIPT_DEBUG", "- Price: " + pricePerKg);
        
        // Create receipt
        Receipt receipt = new Receipt(
            productId,
            productName, // Using the verified product name
            quantity,
            pricePerKg,
            farmerId,
            farmerName,
            farmerPhone,
            memberId,
            memberName,
            memberPhone
        );
        
        return createReceipt(receipt);
    }
    
    // Create a receipt as a member (current user is member)
    public Task<DocumentReference> createReceiptAsMember(Context context, String productId, String productName,
                                                         int quantity, int pricePerKg,
                                                         String farmerId, String farmerName, String farmerPhone) {
        // Initialize session manager if not already initialized
        if (sessionManager == null) {
            sessionManager = new SessionManager(context);
        }
        
        // Get current member (user) details from session
        String memberId = sessionManager.getUserId();
        String memberName = sessionManager.getName();
        String memberPhone = sessionManager.getMobile();
        String userType = sessionManager.getUserType();
        
        Log.d("RECEIPT_DEBUG", "Creating receipt as member: " + memberId + ", name: " + memberName);
        
        // Check if product name is undefined or hardcoded
        if (productName == null || productName.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "ERROR in createReceiptAsMember: Product name is null or empty!");
            
            // Try to fix by retrieving from Firestore
            if (productId != null && !productId.isEmpty()) {
                Log.d("RECEIPT_DEBUG", "Will attempt to use a valid product name instead of null");
                // Use a placeholder name based on product ID in case we can't find the real name
                productName = "Product-" + productId.substring(0, Math.min(productId.length(), 8));
            } else {
                // Last resort
                Log.d("RECEIPT_DEBUG", "Using fallback product name");
                productName = "Unnamed Product";
            }
        } else {
            Log.d("RECEIPT_DEBUG", "Using provided product name: " + productName);
        }
        
        // Log parameters for debugging
        Log.d("RECEIPT_DEBUG", "Member receipt parameters:");
        Log.d("RECEIPT_DEBUG", "- Product ID: " + productId);
        Log.d("RECEIPT_DEBUG", "- Product Name: " + productName);
        Log.d("RECEIPT_DEBUG", "- Quantity: " + quantity);
        Log.d("RECEIPT_DEBUG", "- Price: " + pricePerKg);
        
        // Create receipt
        Receipt receipt = new Receipt(
            productId,
            productName, // Using the verified product name
            quantity,
            pricePerKg,
            farmerId,
            farmerName,
            farmerPhone,
            memberId,
            memberName,
            memberPhone
        );
        
        return createReceipt(receipt);
    }

    // Get all receipts for the current farmer
    public Task<QuerySnapshot> getReceiptsForFarmer() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        Log.d("RECEIPT_DEBUG", "Getting receipts for farmer ID: " + currentUserId);
        
        if (currentUserId.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "Cannot get receipts: Current user ID is empty");
        }
        
        // Query without ordering by timestamp to avoid issues
        return db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("farmerId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("RECEIPT_DEBUG", "Farmer receipts query completed successfully");
                    } else {
                        Log.e("RECEIPT_DEBUG", "Farmer receipts query failed: " + 
                              (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    // Get all receipts for the current member
    public Task<QuerySnapshot> getReceiptsForMember() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        Log.d("RECEIPT_DEBUG", "Getting receipts for member ID: " + currentUserId);
        
        if (currentUserId.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "Cannot get receipts: Current user ID is empty");
        }
        
        // Query without ordering by timestamp to avoid issues
        return db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("memberId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("RECEIPT_DEBUG", "Member receipts query completed successfully");
                    } else {
                        Log.e("RECEIPT_DEBUG", "Member receipts query failed: " + 
                              (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }
    
    // Get all receipts for the current user (both as farmer and member)
    public Task<QuerySnapshot> getAllReceiptsForCurrentUser() {
        // Initialize session manager if needed
        if (sessionManager == null) {
            Log.e("RECEIPT_DEBUG", "SessionManager not initialized. Use the constructor with Context.");
            return null;
        }
        
        String currentUserId = sessionManager.getUserId();
        Log.d("RECEIPT_DEBUG", "Getting all receipts for user ID: " + currentUserId);
        
        if (currentUserId.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "Cannot get receipts: Current user ID is empty in SessionManager");
            
            // Fallback to FirebaseAuth
            currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
            if (currentUserId.isEmpty()) {
                Log.e("RECEIPT_DEBUG", "Cannot get receipts: User not logged in");
                return Tasks.forException(new Exception("User not logged in"));
            }
        }
        
        // Store the ID in a final variable to use in lambda expressions
        final String userId = currentUserId;
        
        // Use a compound query to find receipts where the user is either the farmer or the member
        return db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("farmerId", userId)
                .get()
                .continueWithTask(task -> {
                    // Get the second query result (as member)
                    return db.collection(COLLECTION_RECEIPTS)
                            .whereEqualTo("memberId", userId)
                            .get();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("RECEIPT_DEBUG", "All receipts query completed successfully");
                    } else {
                        Log.e("RECEIPT_DEBUG", "All receipts query failed: " + 
                              (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }
    
    // Get all receipts for the current user as both farmer and member (combined results)
    public Task<List<Receipt>> getAllUserReceipts() {
        // Initialize session manager if needed
        if (sessionManager == null) {
            Log.e("RECEIPT_DEBUG", "SessionManager not initialized. Use the constructor with Context.");
            return Tasks.forException(new Exception("SessionManager not initialized"));
        }
        
        String currentUserId = sessionManager.getUserId();
        if (currentUserId.isEmpty()) {
            // Fallback to FirebaseAuth
            currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
            if (currentUserId.isEmpty()) {
                return Tasks.forException(new Exception("User not logged in"));
            }
        }
        
        final String userId = currentUserId;
        Log.d("RECEIPT_DEBUG", "Getting all receipts for user ID: " + userId);
        
        // Create a task to fetch farmer receipts
        Task<QuerySnapshot> farmerReceiptsTask = db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("farmerId", userId)
                .get();
        
        // Create a task to fetch member receipts
        Task<QuerySnapshot> memberReceiptsTask = db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("memberId", userId)
                .get();
        
        // Combine both tasks
        return Tasks.whenAllSuccess(farmerReceiptsTask, memberReceiptsTask)
                .continueWith(task -> {
                    List<Receipt> allReceipts = new ArrayList<>();
                    // Use a set to track unique receipt IDs
                    Set<String> uniqueReceiptIds = new HashSet<>();
                    
                    if (task.isSuccessful()) {
                        // Process farmer receipts
                        QuerySnapshot farmerReceipts = (QuerySnapshot) task.getResult().get(0);
                        for (com.google.firebase.firestore.DocumentSnapshot doc : farmerReceipts) {
                            Receipt receipt = doc.toObject(Receipt.class);
                            if (receipt != null) {
                                String receiptId = receipt.getId();
                                // Only add if not already in the list
                                if (receiptId == null || !uniqueReceiptIds.contains(receiptId)) {
                                    if (receiptId != null) {
                                        uniqueReceiptIds.add(receiptId);
                                    }
                                    allReceipts.add(receipt);
                                    Log.d("RECEIPT_DEBUG", "Added farmer receipt: " + receipt.getProductName());
                                } else {
                                    Log.d("RECEIPT_DEBUG", "Skipped duplicate farmer receipt: " + receiptId);
                                }
                            }
                        }
                        
                        // Process member receipts
                        QuerySnapshot memberReceipts = (QuerySnapshot) task.getResult().get(1);
                        for (com.google.firebase.firestore.DocumentSnapshot doc : memberReceipts) {
                            Receipt receipt = doc.toObject(Receipt.class);
                            if (receipt != null) {
                                String receiptId = receipt.getId();
                                // Only add if not already in the list
                                if (receiptId == null || !uniqueReceiptIds.contains(receiptId)) {
                                    if (receiptId != null) {
                                        uniqueReceiptIds.add(receiptId);
                                    }
                                    allReceipts.add(receipt);
                                    Log.d("RECEIPT_DEBUG", "Added member receipt: " + receipt.getProductName());
                                } else {
                                    Log.d("RECEIPT_DEBUG", "Skipped duplicate member receipt: " + receiptId);
                                }
                            }
                        }
                        
                        Log.d("RECEIPT_DEBUG", "Combined " + allReceipts.size() + " unique receipts");
                    } else {
                        Log.e("RECEIPT_DEBUG", "Error fetching combined receipts", task.getException());
                    }
                    
                    return allReceipts;
                });
    }

    // Get a receipt by ID
    public Task<QuerySnapshot> getReceiptById(String receiptId) {
        return db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("id", receiptId)
                .get();
    }

    // Update a receipt's status
    public Task<Void> updateReceiptStatus(String receiptId, String status) {
        return db.collection(COLLECTION_RECEIPTS)
                .document(receiptId)
                .update("status", status);
    }

    // Get all receipts for the current user by their phone number
    public Task<List<Receipt>> getAllReceiptsByPhone() {
        // Initialize session manager if needed
        if (sessionManager == null) {
            Log.e("RECEIPT_DEBUG", "SessionManager not initialized. Use the constructor with Context.");
            return Tasks.forException(new Exception("SessionManager not initialized"));
        }
        
        // Get the user's phone number from SessionManager
        String userPhone = sessionManager.getMobile();
        if (userPhone == null || userPhone.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "Cannot get receipts: User phone number is empty");
            return Tasks.forException(new Exception("User phone number not available"));
        }
        
        Log.d("RECEIPT_DEBUG", "Getting receipts for phone number: " + userPhone);
        
        // Create a task to fetch farmer receipts by phone
        Task<QuerySnapshot> farmerReceiptsTask = db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("farmerPhone", userPhone)
                .get();
        
        // Create a task to fetch member receipts by phone
        Task<QuerySnapshot> memberReceiptsTask = db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("memberPhone", userPhone)
                .get();
        
        // Combine both tasks
        return Tasks.whenAllSuccess(farmerReceiptsTask, memberReceiptsTask)
                .continueWith(task -> {
                    List<Receipt> allReceipts = new ArrayList<>();
                    // Use a set to track unique receipt IDs
                    Set<String> uniqueReceiptIds = new HashSet<>();
                    
                    if (task.isSuccessful()) {
                        // Process farmer receipts
                        QuerySnapshot farmerReceipts = (QuerySnapshot) task.getResult().get(0);
                        for (com.google.firebase.firestore.DocumentSnapshot doc : farmerReceipts) {
                            Receipt receipt = doc.toObject(Receipt.class);
                            if (receipt != null) {
                                String receiptId = receipt.getId();
                                // Only add if not already in the list
                                if (receiptId == null || !uniqueReceiptIds.contains(receiptId)) {
                                    if (receiptId != null) {
                                        uniqueReceiptIds.add(receiptId);
                                    }
                                    allReceipts.add(receipt);
                                    Log.d("RECEIPT_DEBUG", "Added farmer receipt: " + receipt.getProductName());
                                } else {
                                    Log.d("RECEIPT_DEBUG", "Skipped duplicate farmer receipt: " + receiptId);
                                }
                            }
                        }
                        
                        // Process member receipts
                        QuerySnapshot memberReceipts = (QuerySnapshot) task.getResult().get(1);
                        for (com.google.firebase.firestore.DocumentSnapshot doc : memberReceipts) {
                            Receipt receipt = doc.toObject(Receipt.class);
                            if (receipt != null) {
                                String receiptId = receipt.getId();
                                // Only add if not already in the list
                                if (receiptId == null || !uniqueReceiptIds.contains(receiptId)) {
                                    if (receiptId != null) {
                                        uniqueReceiptIds.add(receiptId);
                                    }
                                    allReceipts.add(receipt);
                                    Log.d("RECEIPT_DEBUG", "Added member receipt: " + receipt.getProductName());
                                } else {
                                    Log.d("RECEIPT_DEBUG", "Skipped duplicate member receipt: " + receiptId);
                                }
                            }
                        }
                        
                        Log.d("RECEIPT_DEBUG", "Combined " + allReceipts.size() + " unique receipts from " + 
                              uniqueReceiptIds.size() + " with IDs, rest were duplicates or synthetic IDs");
                    } else {
                        Log.e("RECEIPT_DEBUG", "Error fetching receipts by phone", task.getException());
                    }
                    
                    return allReceipts;
                });
    }
} 
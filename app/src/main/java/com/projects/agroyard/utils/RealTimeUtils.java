package com.projects.agroyard.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.projects.agroyard.adapters.BidProductAdapter;
import com.projects.agroyard.client.ApiCaller;
import com.projects.agroyard.models.BidModel;
import com.projects.agroyard.models.BidProductModel;
import com.projects.agroyard.models.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealTimeUtils {
    // Function to insert a bid into Firebase Realtime Database
    public static void placeBid(String productId, long bidAmount, final OnBidPlacedListener listener) {
        // Get the current user's ID (assuming user is logged in)
        String userId = null;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Fetch user data from Firestore
            userId = user.getUid();
        }

        if(userId != null) {
            // Get reference to the bidders node of the product in Realtime Database
            DatabaseReference bidsRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("bids").child(productId).child("bidders");

            // Prepare the bid data
            Map<String, Object> bidData = Map.of("amount", bidAmount);

            // Insert or update the bid for the specific user
            bidsRef.child(userId).setValue(bidData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Notify that the bid was placed successfully
                    listener.onBidPlaced(true, "Bid placed successfully!");
                } else {
                    // Notify that there was an error placing the bid
                    listener.onBidPlaced(false, "Failed to place bid. Please try again.");
                }
            });
        }
    }

    // Function to register a bidder when they decide to participate in the bidding
    public static void registerBidder(String productId, long price, final OnBidderRegisteredListener listener) {
        // Get the current user's ID (assuming user is logged in)
        String userId = null;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Fetch user data from Firestore
            userId = user.getUid();
        }

        if(userId != null) {
            // Get reference to the bidders node for the product in Realtime Database
            DatabaseReference biddersRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("bids").child(productId).child("bidders").child(userId);

            // Prepare the bidder registration data (could include additional info if needed)
            Map<String, Object> bidderData = Map.of("amount", price);// This just registers the userId as a bidder

            // Add the user to the bidders list (could store additional info here as needed)
            biddersRef.child("amount").setValue(price).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Notify that the bidder was registered successfully
                    listener.onBidderRegistered(true, "Bidder registered successfully!");
                } else {
                    // Notify that there was an error registering the bidder
                    listener.onBidderRegistered(false, "Failed to register bidder. Please try again.");
                }
            });
        }
    }


    public static void getBids(DataSnapshot snapshot, final OnBidsFetchedListener listener) {
        // Get the current user's ID (assuming user is logged in)
        String userId = null;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Fetch user data from Firestore
            userId = user.getUid();
        }

        if(userId != null) {
            // Get reference to the bidders node for the product in Realtime Database
                    List<BidModel> bidModels = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot bidSnapshot : snapshot.getChildren()) {
                            BidModel bidModel = new BidModel();
                            bidModel.setProductId(bidSnapshot.getKey());
                            bidModel.setStatus(bidSnapshot.child("status").getValue(String.class));
                            bidModel.setFarmerDistrict(bidSnapshot.child("farmerDistrict").getValue(String.class));
                            bidModel.setImage(bidSnapshot.child("image").getValue(String.class));
                            bidModel.setProductName(bidSnapshot.child("productName").getValue(String.class));
                            bidModel.setFarmerName(bidSnapshot.child("farmerName").getValue(String.class));
                            bidModel.setUpdatedAt(String.valueOf(bidSnapshot.child("updatedAt").getValue(Long.class)));
                            bidModel.setPrice(bidSnapshot.child("price").getValue(Long.class));
                            bidModel.setQuantity(bidSnapshot.child("quantity").getValue(Long.class));
                            bidModel.setEndTime(String.valueOf(bidSnapshot.child("endTime").getValue(Long.class) != null
                                    ? bidSnapshot.child("endTime").getValue(Long.class)
                                    : "0"
                            ));

                            if("Completed".equalsIgnoreCase(bidModel.getStatus())) {
                                bidModel.setSoldTo(bidSnapshot.child("soldTo").getValue(String.class));
                            }
                            bidModel.setBidders(new HashMap<>());
                            // Fetch bidders
                            if(!"Starting".equalsIgnoreCase(bidModel.getStatus())) {
                                for (DataSnapshot bidderSnapshot : bidSnapshot.child("bidders").getChildren()) {
                                    BidModel.Bidders bidder = new BidModel.Bidders();

                                    bidderSnapshot.getChildren().forEach(child -> {
                                        Long amount = child.getValue(Long.class);
                                        if (amount != null) {
                                            bidder.setAmount(amount);
                                        }
                                        bidModel.getBidders().put(bidderSnapshot.getKey(), bidder);
                                    });

                                }
                            }
                            bidModels.add(bidModel);
                        }
                        loadProducts(bidModels, listener);

                    } else {
                        listener.onBidsFetched(false, null);
                    }

        }
    }

    public static void addBid(String productId) {
        String userId = null;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Fetch user data from Firestore
            userId = user.getUid();
        }

        if (userId != null) {
            // Get reference to the bidders node of the product in Realtime Database
            DatabaseReference bidsRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference().child("bids").child(productId);
            bidsRef.setValue(Map.of("status", "Starting"));
        }
    }

    public static void setBidCompleted(String productId, OnBidCompletedListener onBidCompletedListener) {
        if(productId != null) {
            DatabaseReference bidRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("bids").child(productId);
            bidRef.child("status").setValue("Completed");
            bidRef.child("bidders").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long maxBid = 0;
                    String highestBidderId = null;

                    // Loop through the bidders to find the maximum bid and the corresponding user ID
                    for (DataSnapshot bidderSnapshot : snapshot.getChildren()) {
                        Long bidAmount = bidderSnapshot.child("amount").getValue(Long.class);
                        if (bidAmount != null && bidAmount > maxBid) {
                            maxBid = bidAmount;
                            highestBidderId = bidderSnapshot.getKey();  // Get the user ID of the highest bidder
                        }
                    }
                    if (highestBidderId != null) {
                        // Set the status to "Completed"

                        long finalMaxBid = maxBid;
                        FirestoreHelper.getUser(highestBidderId, new FirestoreHelper.UserNameCallback() {
                            @Override
                            public void onUserLoaded(String userName) {
                                bidRef.child("status").setValue("Completed");
                                bidRef.child("soldTo").setValue(userName);

                                FirestoreHelper.updateProduct(productId, Map.of("bid_status", "Completed", "is_sold", true, "sold_to", userName, "sold_at", System.currentTimeMillis(), "sold_amount", finalMaxBid), new FirestoreHelper.SaveCallback() {
                                    @Override
                                    public void onSuccess() {
                                        onBidCompletedListener.onBidCompleted(true, "Bid Successfully Completed");
                                        ApiCaller.generateReciept(productId);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e("BidCompleted", "Error Updating products: " + e.getMessage());
                                    }
                                });

                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("BidCompleted", "Error fetching users: " + e.getMessage());
                            }
                        });

                        // Optionally trigger the callback (if required)
                        if (onBidCompletedListener != null) {
                            onBidCompletedListener.onBidCompleted(true, "Bidding Ended");
                        }
                    } else {
                        onBidCompletedListener.onBidCompleted(false, "No bids found");

                        // If no bids were placed, we can choose to handle it here
                        Log.d("BidCompleted", "No bids placed.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("BidCompleted", "Error fetching bidders: " + error.getMessage());
                }
            });
        }
    }

    // Interface to handle bid placement result
    public interface OnBidPlacedListener {
        void onBidPlaced(boolean success, String message);
    }

    public interface OnBidCompletedListener {
        void onBidCompleted(boolean success, String message);
    }

    // Interface to handle bidder registration result
    public interface OnBidderRegisteredListener {
        void onBidderRegistered(boolean success, String message);
    }
    public interface OnBidsFetchedListener {
        void onBidsFetched(boolean success, List<BidModel> bidModel);
    }

    private static void loadProducts(List<BidModel> bidsList, OnBidsFetchedListener listener) {
        final String TAG = "Fetch Products";
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirestoreHelper.getDistrict(userId, new FirestoreHelper.UserCallback() {
            @Override
            public void onUserLoaded(String userDistrict) {
                if (userDistrict == null) {
                    Log.e(TAG, "User district not found");
                    return;
                }

                // To hold the product data until we process everything
                List<BidModel> newCombinedList = new ArrayList<>();

                for (BidModel bid : bidsList) {
                    if(userDistrict.equalsIgnoreCase(bid.getFarmerDistrict())) {
                        if(bid.getBidders().containsKey(userId)) {
                            newCombinedList.add(bid);
                        } else if("Starting".equalsIgnoreCase(bid.getStatus())) {
                            newCombinedList.add(bid);
                        }
                    }
                }
                listener.onBidsFetched(true, newCombinedList);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching user district: " + e.getMessage(), e);
            }
        });
    }

}

package com.projects.agroyard.fragments;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.projects.agroyard.R;
import com.projects.agroyard.WebSocket.WebSocketManager;
import com.projects.agroyard.client.ApiCaller;
import com.projects.agroyard.models.Product;
import com.projects.agroyard.utils.FirestoreHelper;
import com.projects.agroyard.utils.RealTimeUtils;
import com.projects.agroyard.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BidMonitorFragment extends Fragment {

    private LinearLayout bidsContainer;
    private TextView activeBidsCount;
    private TextView pendingBidsCount;
    private TextView totalValueText;
    private Button allFilterBtn;
    private Button activeFilterBtn;
    private Button pendingFilterBtn;
    private Button notRegistered;
    private Button completed;

    private List<BidRecord> allBidRecords = new ArrayList<>();
    private String currentFilter = "all";

    private ProgressBar loadingSpinner;
    private TextView noRecordsFound;
    private String currentUserMobile;

    private WebSocketManager webSocketManager;

    public BidMonitorFragment() {
        // Required empty public constructor
    }

    private interface FetchBidRecordsCallback {
        void onRecordsFetched(List<BidRecord> bidRecords);
    }

    @Override
    public void onStart() {
        super.onStart();
        // call your private method here
        webSocketManager = new WebSocketManager(
                requireContext(),
                "ws://192.168.180.130:3000/",
                this::loadBidData
        );
        webSocketManager.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (webSocketManager != null) {
            webSocketManager.stop();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bid_monitor, container, false);

        currentUserMobile = new SessionManager(requireContext()).getMobile();

        bidsContainer = view.findViewById(R.id.bids_container);
        activeBidsCount = view.findViewById(R.id.active_bids_count);
        pendingBidsCount = view.findViewById(R.id.pending_bids_count);
        totalValueText = view.findViewById(R.id.total_value);

        allFilterBtn = view.findViewById(R.id.filter_all);
        activeFilterBtn = view.findViewById(R.id.filter_active);
        pendingFilterBtn = view.findViewById(R.id.filter_pending);
        notRegistered = view.findViewById(R.id.filter_notregistered);
        completed = view.findViewById(R.id.filter_completed);

        loadingSpinner = view.findViewById(R.id.loading_spinner);
        noRecordsFound = view.findViewById(R.id.no_records_found);

        setupFilterListeners();
        loadBidData();
        DatabaseReference biddersRef = FirebaseDatabase.getInstance("https://agro-yard-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("bids");
        biddersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadBidData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //Nothing
            }
        });

        return view;
    }

    private void setupFilterListeners() {
        allFilterBtn.setOnClickListener(v -> setFilter("all", allFilterBtn));
        activeFilterBtn.setOnClickListener(v -> setFilter("Active", activeFilterBtn));
        pendingFilterBtn.setOnClickListener(v -> setFilter("Pending", pendingFilterBtn));
        notRegistered.setOnClickListener(v -> setFilter("Not Registered", notRegistered));
        completed.setOnClickListener(v -> setFilter("Completed", completed));
    }

    private void setFilter(String filter, Button selectedButton) {
        currentFilter = filter;
        refreshBidRecordViews();

        updateButtonState(allFilterBtn, allFilterBtn == selectedButton);
        updateButtonState(activeFilterBtn, activeFilterBtn == selectedButton);
        updateButtonState(pendingFilterBtn, pendingFilterBtn == selectedButton);
        updateButtonState(notRegistered, notRegistered == selectedButton);
        updateButtonState(completed, completed == selectedButton);
    }

    private void updateButtonState(Button button, boolean selected) {
        if (selected) {
            button.setBackgroundResource(R.drawable.button_primary_bg);
            button.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            button.setBackgroundResource(R.drawable.button_outline_bg);
            button.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    private void loadBidData() {
        loadingSpinner.setVisibility(View.VISIBLE);
        bidsContainer.setVisibility(View.GONE);
        noRecordsFound.setVisibility(View.GONE);

        fetchBidRecords(records -> {
            allBidRecords = records;
            updateBidStatistics();
            refreshBidRecordViews();

            loadingSpinner.setVisibility(View.GONE);

            if (records.isEmpty()) {
                noRecordsFound.setVisibility(View.VISIBLE);
            } else {
                bidsContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void refreshBidRecordViews() {
        bidsContainer.removeAllViews();

        for (BidRecord record : allBidRecords) {
            if (currentFilter.equals("all") || record.status.equalsIgnoreCase(currentFilter)) {
                addBidRecordView(record);
            }
        }
    }

    private void updateBidStatistics() {
        int activeCount = 0;
        int pendingCount = 0;
        int totalValue = 0;

        for (BidRecord record : allBidRecords) {
            if ("Active".equalsIgnoreCase(record.status)) {
                activeCount++;
                try {
                    int bidAmount = Integer.parseInt(record.highestBid.replace(",", ""));
                    int quantityValue = Integer.parseInt(record.quantity.replace(" kg", ""));
                    totalValue += bidAmount * quantityValue;
                } catch (NumberFormatException ignored) { }
            } else if ("Pending".equalsIgnoreCase(record.status)) {
                pendingCount++;
            }
        }

        activeBidsCount.setText(String.valueOf(activeCount));
        pendingBidsCount.setText(String.valueOf(pendingCount));
        totalValueText.setText("₹" + String.format("%,d", totalValue));
    }

    private void addBidRecordView(BidRecord record) {
        View recordView = getLayoutInflater().inflate(R.layout.item_bid_record, bidsContainer, false);

        TextView productNameView = recordView.findViewById(R.id.product_name);
        TextView quantityView = recordView.findViewById(R.id.product_quantity);
        TextView highestBidView = recordView.findViewById(R.id.highest_bid);
        TextView numberOfBidsView = recordView.findViewById(R.id.number_of_bids);
        TextView bidderNameView = recordView.findViewById(R.id.bidder_name);
        TextView bidStatusView = recordView.findViewById(R.id.bid_status);
        TextView expiresInView = recordView.findViewById(R.id.expires_in);
        Button registerBid = recordView.findViewById(R.id.register_bid_button);

        productNameView.setText(record.productName);
        quantityView.setText(record.quantity);
        highestBidView.setText("₹" + record.highestBid + "/kg");
        numberOfBidsView.setText(record.numberOfBids + " bids");
        bidderNameView.setText("by " + record.bidderName);
        bidStatusView.setText(record.status);
        expiresInView.setText("Expires in: " + record.expiresIn);

        if ("Active".equalsIgnoreCase(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            bidStatusView.setBackgroundResource(R.drawable.green_rounded_background);
        } else if ("Pending".equalsIgnoreCase(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            bidStatusView.setBackgroundResource(R.drawable.orange_rounded_background);
            registerBid.setText("Start Bid");
        } else if ("Expired".equalsIgnoreCase(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            bidStatusView.setBackgroundResource(R.drawable.red_rounded_background);
        } else if ("Completed".equalsIgnoreCase(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            bidStatusView.setBackgroundResource(R.drawable.red_rounded_background);
        }else if ("Starting".equalsIgnoreCase(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_blue_bright));
            bidStatusView.setBackgroundResource(R.drawable.blue_rounded_background);
        } else {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            bidStatusView.setBackgroundResource(R.drawable.red_rounded_background);
            registerBid.setText("Register for Bid");
        }

        if ("Pending".equalsIgnoreCase(record.status) || "Not Registered".equalsIgnoreCase(record.status)) {
            registerBid.setVisibility(View.VISIBLE);
        } else {
            registerBid.setVisibility(View.GONE);
        }


        CardView cardView = recordView.findViewById(R.id.bid_card);
        cardView.setOnClickListener(v ->
                Toast.makeText(getContext(), "Viewing details for " + record.productName, Toast.LENGTH_SHORT).show()
        );


        registerBid.setOnClickListener(v -> {
            if ("Not Registered".equalsIgnoreCase(record.status)) {
                // When product is not registered
                FirestoreHelper.updateProductBidStatus(record.productId, "Pending", new FirestoreHelper.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Registered for Bid!", Toast.LENGTH_SHORT).show();
                            loadBidData();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Unable to register for bid", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else if ("Pending".equalsIgnoreCase(record.status)) {
                // When product is pending and needs to start
                FirestoreHelper.updateProductBidStatus(record.productId, "Starting", new FirestoreHelper.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Bid is starting... Will become Active after 60 seconds.", Toast.LENGTH_SHORT).show();
                            loadBidData();
                        });
                        new Thread(() -> {
//                            RealTimeUtils.addBid(record.productId);
                            ApiCaller.updateBidStatus(record.productId);
                        }).start();

                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Failed to start bid", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        bidsContainer.addView(recordView);
    }

    private void fetchBidRecords(FetchBidRecordsCallback callback) {
        if (currentUserMobile != null && !currentUserMobile.isEmpty()) {
            FirestoreHelper.getProductsForCurrentUser(currentUserMobile, new FirestoreHelper.ProductsCallback() {
                @Override
                public void onProductsLoaded(List<Map<String, Object>> products) {
                    List<BidRecord> records = new ArrayList<>();
                    if (!products.isEmpty()) {
                        for (Map<String, Object> product : products) {
                            Product productData = new Product(product);
                            BidRecord bidRecord = new BidRecord(
                                    productData.getProductName(),
                                    productData.getProductId(),
                                    String.valueOf(productData.getQuantity()),
                                    productData.getHighestBid(),
                                    productData.getNumberOfBids(),
                                    productData.getBidderName(),
                                    productData.getBidStatus(),
                                    productData.getExpiresIn()
                            );
                            records.add(bidRecord);
                        }
                    }
                    callback.onRecordsFetched(records);
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Error loading products: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }

    private static class BidRecord {
        final String productName;
        final String productId;
        final String quantity;
        final String highestBid;
        final String numberOfBids;
        final String bidderName;
        final String status;
        final String expiresIn;

        BidRecord(String productName, String productId, String quantity, String highestBid, String numberOfBids, String bidderName, String status, String expiresIn) {
            this.productName = productName;
            this.productId = productId;
            this.quantity = quantity;
            this.highestBid = highestBid;
            this.numberOfBids = numberOfBids;
            this.bidderName = bidderName;
            this.status = status;
            this.expiresIn = expiresIn;
        }
    }
}

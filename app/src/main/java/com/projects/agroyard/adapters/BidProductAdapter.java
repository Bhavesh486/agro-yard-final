package com.projects.agroyard.adapters;


import android.os.Handler;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.projects.agroyard.R;
import com.projects.agroyard.models.BidModel;
import com.projects.agroyard.models.BidProductModel;
import com.projects.agroyard.models.Product;
import com.projects.agroyard.utils.RealTimeUtils;

import java.util.List;


public class BidProductAdapter extends RecyclerView.Adapter<BidProductAdapter.BidProductViewHolder> {

    private List<BidModel> bidProductList;

    public BidProductAdapter(List<BidModel> bidProductList) {
        this.bidProductList = bidProductList;
    }

    @NonNull
    @Override
    public BidProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_bid_card, parent, false);
        return new BidProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BidProductViewHolder holder, int position) {
        BidModel model = bidProductList.get(position);
            holder.productName.setText(model.getProductName());
            holder.productQuantity.setText("Quantity: " + model.getQuantity() + "kg");
            holder.productFarmer.setText("Farmer: " + model.getFarmerName());
            holder.productBasePrice.setText("Base Price: ₹" + model.getPrice() + "/kg");

            // Show current highest bid (if any)
            if (model.getBidders() != null && !model.getBidders().isEmpty()) {
                long highestBid = model.getBidders()
                        .values()
                        .stream()
                        .mapToLong(BidModel.Bidders::getAmount)
                        .max()
                        .orElse((long) model.getPrice());
                holder.productCurrentBid.setText("₹" + highestBid + "/kg");
            } else {
                holder.productCurrentBid.setText("₹" + model.getPrice() + "/kg");
            }

            // Load product image (optional, if you have image URL)
            Glide.with(holder.itemView.getContext())
                    .load(model.getImage()) // your Product must have image URL
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.productImage);

            // Timer logic for "Bidding Ends In"
        if (model.getEndTime() != null && !model.getEndTime().equalsIgnoreCase("0")) {
            long endTimeMillis = Long.parseLong(model.getEndTime());

            // Create a Handler to update the timer every second
            final Handler handler = new Handler();

            // Define a Runnable to update the timer every second
            Runnable updateTimer = new Runnable() {
                @Override
                public void run() {
                    long currentTimeMillis = System.currentTimeMillis();
                    long remainingTimeMillis = endTimeMillis - currentTimeMillis;

                    if (remainingTimeMillis > 0) {
                        holder.bidMain.setVisibility(View.VISIBLE);
                        holder.bidFormText.setVisibility(View.VISIBLE);
                        // Calculate remaining time
                        long hours = (remainingTimeMillis / (1000 * 60 * 60)) % 24;
                        long minutes = (remainingTimeMillis / (1000 * 60)) % 60;
                        long seconds = (remainingTimeMillis / 1000) % 60;

                        // Format the time and update the UI
                        String timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                        holder.productTimer.setText("Ends in: " + timeLeftFormatted);
                    } else {
                        // Bidding ended, show "Bidding Ended"

                        holder.bidMain.setVisibility(View.GONE);
                        holder.bidFormText.setVisibility(View.GONE);
                        holder.productTimer.setText("Bidding Ended");

                        if(model.getSoldTo() != null && !model.getSoldTo().isEmpty()) {
                            holder.biddEndIn.setText("Sold To");
                            holder.productTimer.setText(model.getSoldTo());
                        }

                        handler.removeCallbacks(this);
                        if(!model.getStatus().equalsIgnoreCase("Completed")) {
                            RealTimeUtils.setBidCompleted(model.getProductId(), ((success, message) -> {
                                Toast.makeText(holder.itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                            }));
                        }
                    }


                    // Post the Runnable again with a delay of 1000ms (1 second)
                    if (remainingTimeMillis > 0) {
                        handler.postDelayed(this, 1000);  // Re-run the Runnable after 1 second
                    }
                }
            };

            // Start the timer update
            handler.post(updateTimer); // Post the first update immediately
        }
            if("Starting".equalsIgnoreCase(model.getStatus())) {
                holder.bidInput.setVisibility(View.GONE);
                holder.productTimer.setText("Bidding Not Started");
                holder.bidButton.setText("Register For Bidding");
                holder.bidButton.setOnClickListener(v -> {
                    RealTimeUtils.registerBidder(model.getProductId(), model.getPrice(), (success, message) -> {
                        if(success) {
                            v.setVisibility(View.GONE);
                            Toast.makeText(holder.itemView.getContext(), "User Registered for Bid", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                // Handle bid button click (you can implement listener)
                holder.bidInput.setVisibility(View.VISIBLE);
                holder.bidButton.setVisibility(View.VISIBLE);
                holder.bidButton.setText("Bid");

                holder.bidButton.setOnClickListener(v -> {
                    String bidAmountStr = holder.bidInput.getText().toString().trim();
                    if (!bidAmountStr.isEmpty()) {
                        long bidAmount = Long.parseLong(bidAmountStr);
                        long highestBid = model.getBidders()
                                .values()
                                .stream()
                                .mapToLong(BidModel.Bidders::getAmount)
                                .max()
                                .orElse(model.getPrice());

                        bidAmount = bidAmount + highestBid;
                        RealTimeUtils.placeBid(model.getProductId(), bidAmount, (success, message) -> {
                            if (success) {
                                Toast.makeText(holder.itemView.getContext(), "Bid Placed", Toast.LENGTH_SHORT).show();
                            }
                            holder.bidInput.setText("");
                        });
                    }
                });

                if(model.getSoldTo() != null && !model.getSoldTo().isEmpty()) {
                    holder.biddEndIn.setText("Sold To");
                    holder.productTimer.setText(model.getSoldTo());
                }
            }
    }

    @Override
    public int getItemCount() {
        return bidProductList.size();
    }

    static class BidProductViewHolder extends RecyclerView.ViewHolder {

        ImageView productImage;
        TextView productName, productQuantity, productFarmer, productBasePrice, productCurrentBid, productTimer, bidFormText, biddEndIn;
        EditText bidInput;
        Button bidButton;

        LinearLayout bidMain;

        public BidProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productQuantity = itemView.findViewById(R.id.product_quantity);
            productFarmer = itemView.findViewById(R.id.product_farmer);
            productBasePrice = itemView.findViewById(R.id.product_base_price);
            productCurrentBid = itemView.findViewById(R.id.product_current_bid);
            productTimer = itemView.findViewById(R.id.product_timer);
            bidInput = itemView.findViewById(R.id.product_bid_input);
            bidButton = itemView.findViewById(R.id.product_bid_button);
            bidFormText = itemView.findViewById(R.id.bid_form_text);
            biddEndIn = itemView.findViewById(R.id.bidd_end_in);
            bidMain = itemView.findViewById(R.id.bid_form_main);
        }
    }
}

package com.projects.agroyard.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.models.Product;

public class ProductDetailFragment extends Fragment {
    private static final String ARG_PRODUCT_ID = "product_id";
    private static final String ARG_PRODUCT_NAME = "product_name";
    private static final String ARG_FARMER_NAME = "farmer_name";
    private static final String ARG_FARMER_MOBILE = "farmer_mobile";
    private static final String ARG_FARMING_TYPE = "farming_type";
    private static final String ARG_HARVESTING_DATE = "harvesting_date";
    private static final String ARG_QUANTITY = "quantity";
    private static final String ARG_PRICE = "price";
    private static final String ARG_EXPECTED_PRICE = "expected_price";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_IMAGE_PATH = "image_path";
    private static final String ARG_IMAGE_FILENAME = "image_filename";
    private static final String ARG_REGISTER_FOR_BIDDING = "register_for_bidding";

    private static final String TAG = "ProductDetailFragment";
    // Base URL for emulator to access images on localhost
    private static final String EMULATOR_BASE_URL = Constants.DB_URL_BASE + "uploads/";

    // UI elements
    private ImageView productImage;
    private TextView productNameText;
    private TextView farmerNameText;
    private TextView farmerMobileText;
    private TextView farmingTypeText;
    private TextView harvestingDateText;
    private TextView quantityText;
    private TextView priceText;
    private TextView expectedPriceText;
    private TextView descriptionText;
    private Button callFarmerButton;
    private Button placeBidButton;
    private View bidButtonContainer;

    public static ProductDetailFragment newInstance(Product product) {
        ProductDetailFragment fragment = new ProductDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PRODUCT_ID, product.getId());
        args.putString(ARG_PRODUCT_NAME, product.getProductName());
        args.putString(ARG_FARMER_NAME, product.getFarmerName());
        args.putString(ARG_FARMER_MOBILE, product.getFarmerMobile());
        args.putString(ARG_FARMING_TYPE, product.getFarmingType());
        args.putString(ARG_HARVESTING_DATE, product.getHarvestingDate());
        args.putDouble(ARG_QUANTITY, product.getQuantity());
        args.putDouble(ARG_PRICE, product.getPrice());
        args.putDouble(ARG_EXPECTED_PRICE, product.getExpectedPrice());
        args.putString(ARG_DESCRIPTION, product.getDescription());
        args.putString(ARG_IMAGE_URL, product.getImageUrl());
        args.putString(ARG_IMAGE_PATH, product.getImagePath());
        args.putString(ARG_IMAGE_FILENAME, product.getImageFilename());
        args.putBoolean(ARG_REGISTER_FOR_BIDDING, product.isRegisterForBidding());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_detail, container, false);

        initializeViews(view);
        populateProductDetails();

        return view;
    }

    private void initializeViews(View view) {
        productImage = view.findViewById(R.id.detail_product_image);
        productNameText = view.findViewById(R.id.detail_product_name);
        farmerNameText = view.findViewById(R.id.detail_farmer_name);
        farmerMobileText = view.findViewById(R.id.detail_farmer_mobile);
        farmingTypeText = view.findViewById(R.id.detail_farming_type);
        harvestingDateText = view.findViewById(R.id.detail_harvesting_date);
        quantityText = view.findViewById(R.id.detail_quantity);
        priceText = view.findViewById(R.id.detail_price);
        expectedPriceText = view.findViewById(R.id.detail_expected_price);
        descriptionText = view.findViewById(R.id.detail_description);
        callFarmerButton = view.findViewById(R.id.call_farmer_button);
        placeBidButton = view.findViewById(R.id.place_bid_button);
        bidButtonContainer = view.findViewById(R.id.bid_button_container);

        // Initialize back button
        ImageView backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void populateProductDetails() {
        Bundle args = getArguments();
        if (args == null) return;

        // Set text views
        productNameText.setText(args.getString(ARG_PRODUCT_NAME));
        farmerNameText.setText("Farmer: " + args.getString(ARG_FARMER_NAME));
        farmerMobileText.setText("Contact: " + args.getString(ARG_FARMER_MOBILE));
        farmingTypeText.setText("Farming Type: " + args.getString(ARG_FARMING_TYPE));
        harvestingDateText.setText("Harvested on: " + args.getString(ARG_HARVESTING_DATE));
        quantityText.setText(String.format("Available Quantity: %.2f kg", args.getDouble(ARG_QUANTITY)));
        priceText.setText(String.format("Price: ₹%.2f per kg", args.getDouble(ARG_PRICE)));
        expectedPriceText.setText(String.format("Expected Price: ₹%.2f per kg", args.getDouble(ARG_EXPECTED_PRICE)));
        descriptionText.setText(args.getString(ARG_DESCRIPTION));

        // Enhanced image loading with image_path approach
        String imagePath = args.getString(ARG_IMAGE_PATH);
        String imageUrl = args.getString(ARG_IMAGE_URL);
        String productName = args.getString(ARG_PRODUCT_NAME);

        Log.d(TAG, "Loading detail image for product: " + productName);
        Log.d(TAG, "Detail Image Path: " + imagePath);
        Log.d(TAG, "Detail Image URL: " + imageUrl);

        // Set placeholder first
        productImage.setImageResource(R.drawable.ic_image_placeholder);

        // Try image_path first
        if (imagePath != null && !imagePath.isEmpty()) {
            String fullUrl = Constants.DB_URL_BASE + imagePath;
            Log.d(TAG, "Loading from image_path: " + fullUrl);
            loadWithGlide(fullUrl, "image_path");
        }
        // Fall back to image_url if available
        else if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading from image_url: " + imageUrl);
            loadWithGlide(imageUrl, "image_url");
        }
        else {
            Log.w(TAG, "No image path or URL available for product: " + productName);
        }

        // Setup call button
        final String farmerMobile = args.getString(ARG_FARMER_MOBILE);
        callFarmerButton.setOnClickListener(v -> {
            if (farmerMobile != null && !farmerMobile.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + farmerMobile));
                startActivity(callIntent);
            } else {
                Toast.makeText(getContext(), "Farmer contact number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup bidding button based on register_for_bidding flag
        boolean isRegisteredForBidding = args.getBoolean(ARG_REGISTER_FOR_BIDDING, true);
        if (isRegisteredForBidding) {
            placeBidButton.setVisibility(View.VISIBLE);
            placeBidButton.setOnClickListener(v -> {
                // Navigate to BettingFragment with product info
                Fragment bettingFragment = new BettingFragment();
                Bundle bidArgs = new Bundle();
                bidArgs.putInt("product_id", args.getInt(ARG_PRODUCT_ID));
                bidArgs.putString("product_name", args.getString(ARG_PRODUCT_NAME));
                bidArgs.putDouble("product_price", args.getDouble(ARG_PRICE));
                bidArgs.putDouble("product_quantity", args.getDouble(ARG_QUANTITY));
                bidArgs.putString("farmer_name", args.getString(ARG_FARMER_NAME));
                bidArgs.putString("image_path", args.getString(ARG_IMAGE_PATH));
                bettingFragment.setArguments(bidArgs);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, bettingFragment)
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            placeBidButton.setVisibility(View.GONE);
        }
    }

    private void loadWithGlide(String imageUrl, String source) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .timeout(30000) // 30 seconds timeout
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "❌ Detail image load FAILED for " + source + ": " + imageUrl, e);
                        if (e != null) {
                            for (Throwable t : e.getRootCauses()) {
                                Log.e(TAG, "Caused by: " + t.getMessage());
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "✅ Detail image load SUCCESS from " + source + ": " + imageUrl);
                        return false;
                    }
                })
                .into(productImage);
    }
} 
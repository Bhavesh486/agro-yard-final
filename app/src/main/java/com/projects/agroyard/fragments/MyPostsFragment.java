package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.projects.agroyard.R;
import com.projects.agroyard.models.Product;
import com.projects.agroyard.utils.FirestoreHelper;
import com.projects.agroyard.utils.SessionManager;

import java.util.List;
import java.util.Map;

public class MyPostsFragment extends Fragment {
    private static final String TAG = "MyPostsFragment";
    
    // UI elements
    private View loadingView;
    private TextView noPostsText;
    private LinearLayout postsContainer;
    private ImageButton backButton;
    
    // Session management
    private SessionManager sessionManager;
    private String currentUserMobile;
    
    public MyPostsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        
        // Initialize session manager
        sessionManager = new SessionManager(requireContext());
        currentUserMobile = sessionManager.getMobile();
        
        // Initialize UI elements
        loadingView = view.findViewById(R.id.loading_view);
        noPostsText = view.findViewById(R.id.no_posts_text);
        postsContainer = view.findViewById(R.id.posts_container);
        backButton = view.findViewById(R.id.back_button);
        
        // Set back button click listener
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        
        // Load farmer's products
        loadFarmerProducts();
        
        return view;
    }
    
    private void loadFarmerProducts() {
        // Show loading view
        showLoading(true);
        
        // Only attempt to load products if the user is logged in and has a mobile number
        if (currentUserMobile != null && !currentUserMobile.isEmpty()) {
            Log.d(TAG, "Loading products for farmer with mobile: " + currentUserMobile);
            
            // Option 1: Remove orderBy to avoid needing a composite index
            FirestoreHelper.getProductsForCurrentUser(currentUserMobile, new FirestoreHelper.ProductsCallback() {
                @Override
                public void onProductsLoaded(List<Map<String, Object>> products) {
                    if (getActivity() == null) return; // Fragment might be detached
                    
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (products.isEmpty()) {
                            showNoPostsMessage(true);
                        } else {
                            showNoPostsMessage(false);
                            displayProducts(products);
                        }
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    if (getActivity() == null) return; // Fragment might be detached
                    
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showNoPostsMessage(true);
                        Toast.makeText(requireContext(), "Error loading products: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading farmer products", e);
                    });
                }
            });
        } else {
            showLoading(false);
            showNoPostsMessage(true);
            Log.w(TAG, "No mobile number available for current user");
        }
    }
    
    private void displayProducts(List<Map<String, Object>> products) {
        // Clear existing views
        postsContainer.removeAllViews();
        
        // Inflate product items
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        
        for (Map<String, Object> productData : products) {
            // Convert to Product object
            Product product = new Product(productData);
            
            // Inflate product item view
            View itemView = inflater.inflate(R.layout.item_product, postsContainer, false);
            
            // Get references to views in the product item
            TextView productNameText = itemView.findViewById(R.id.product_name);
            TextView farmerNameText = itemView.findViewById(R.id.farmer_name);
            TextView farmingTypeText = itemView.findViewById(R.id.farming_type);
            TextView quantityText = itemView.findViewById(R.id.quantity);
            TextView priceText = itemView.findViewById(R.id.price);
            TextView harvestingDateText = itemView.findViewById(R.id.harvesting_date);
            TextView descriptionText = itemView.findViewById(R.id.description);
            android.widget.ImageView productImage = itemView.findViewById(R.id.product_image);
            
            // Set product data
            productNameText.setText(product.getProductName());
            farmerNameText.setText(product.getFarmerName());
            farmingTypeText.setText(product.getFarmingType());
            quantityText.setText(String.format("%.2f kg", product.getQuantity()));
            priceText.setText(String.format("₹%.2f/kg", product.getPrice()));
            harvestingDateText.setText(product.getHarvestingDate());
            descriptionText.setText(product.getDescription());
            
            // Load product image using Glide
            String imageUrl = product.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.placeholder_image);
            }
            
            // Remove or hide the "Place Bid" button since this is the farmer's own products
            View bidButton = itemView.findViewById(R.id.btnBid);
            if (bidButton != null) {
                bidButton.setVisibility(View.GONE);
            }
            
            // Show and set up the delete button
            Button deleteButton = itemView.findViewById(R.id.btnDelete);
            if (deleteButton != null) {
                deleteButton.setVisibility(View.VISIBLE);
                
                deleteButton.setOnClickListener(v -> {
                    // Show confirmation dialog before deleting
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle("Delete Product");
                    builder.setMessage("Are you sure you want to delete " + product.getProductName() + "?");
                    
                    builder.setPositiveButton("Delete", (dialog, which) -> {
                        // Show loading indicator
                        showLoading(true);
                        
                        // Delete the product
                        FirestoreHelper.deleteProduct(product.getProductId(), new FirestoreHelper.SaveCallback() {
                            @Override
                            public void onSuccess() {
                                if (getActivity() == null) return; // Fragment might be detached
                                
                                getActivity().runOnUiThread(() -> {
                                    // Hide loading indicator
                                    showLoading(false);
                                    
                                    // Remove the view from the container
                                    postsContainer.removeView(itemView);
                                    
                                    // Show message
                                    Toast.makeText(requireContext(), 
                                        product.getProductName() + " has been deleted", 
                                        Toast.LENGTH_SHORT).show();
                                    
                                    // If no more products, show the no posts message
                                    if (postsContainer.getChildCount() == 0) {
                                        showNoPostsMessage(true);
                                    }
                                });
                            }
                            
                            @Override
                            public void onError(Exception e) {
                                if (getActivity() == null) return; // Fragment might be detached
                                
                                getActivity().runOnUiThread(() -> {
                                    // Hide loading indicator
                                    showLoading(false);
                                    
                                    // Show error message
                                    Toast.makeText(requireContext(), 
                                        "Failed to delete product: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                    
                                    Log.e(TAG, "Error deleting product", e);
                                });
                            }
                        });
                    });
                    
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                });
            }
            
            // Add the product view to the container
            postsContainer.addView(itemView);
        }
    }
    
    private void navigateToProductDetail(Product product) {
        ProductDetailFragment fragment = new ProductDetailFragment();
        Bundle args = new Bundle();
        args.putString("product_id", product.getProductId());
        args.putString("product_name", product.getProductName());
        args.putString("farmer_name", product.getFarmerName());
        args.putString("farming_type", product.getFarmingType());
        args.putDouble("quantity", product.getQuantity());
        args.putDouble("price", product.getPrice());
        args.putString("harvesting_date", product.getHarvestingDate());
        args.putString("description", product.getDescription());
        args.putString("image_url", product.getImageUrl());
        fragment.setArguments(args);
        
        if (getActivity() != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
    
    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void showNoPostsMessage(boolean show) {
        if (noPostsText != null) {
            noPostsText.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
} 
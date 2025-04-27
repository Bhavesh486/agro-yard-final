package com.projects.agroyard.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private final Context context;
    private List<Product> productList;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    public void updateProducts(List<Product> newProducts) {
        this.productList = newProducts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productName;
        private final TextView farmerName;
        private final TextView farmingType;
        private final TextView quantity;
        private final TextView price;
        private final TextView harvestingDate;
        private final TextView description;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            farmerName = itemView.findViewById(R.id.farmer_name);
            farmingType = itemView.findViewById(R.id.farming_type);
            quantity = itemView.findViewById(R.id.quantity);
            price = itemView.findViewById(R.id.price);
            harvestingDate = itemView.findViewById(R.id.harvesting_date);
            description = itemView.findViewById(R.id.description);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onProductClick(productList.get(position));
                }
            });
        }

        public void bind(Product product) {
            productName.setText(product.getProductName());
            farmerName.setText("Farmer: " + product.getFarmerName());
            farmingType.setText("Type: " + product.getFarmingType());
            quantity.setText(String.format("Quantity: %.2f kg", product.getQuantity()));
            price.setText(String.format("Price: ₹%.2f/kg", product.getPrice()));
            harvestingDate.setText("Harvested on: " + product.getHarvestingDate());
            description.setText(product.getDescription());

            // Enhanced image loading using image_path
            String imagePath = product.getImagePath();

            Log.d("ProductAdapter", "Loading image for: " + product.getProductName());
            Log.d("ProductAdapter", "Image Path: " + imagePath);

            // Set placeholder first to avoid blank images
            productImage.setImageResource(R.drawable.ic_image_placeholder);

            // Try to load the image using the image path
            if (imagePath != null && !imagePath.isEmpty()) {
                // Construct direct URL using the image path
                String imageUrl = Constants.DB_URL_BASE + imagePath;
                Log.d("ProductAdapter", "Loading from constructed URL: " + imageUrl);
                loadWithGlide(imageUrl, "image_path");
            }
            // Fallback to legacy image URL if available
            else if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Log.d("ProductAdapter", "Falling back to direct image URL: " + product.getImageUrl());
                loadWithGlide(product.getImageUrl(), "image_url");
            }
        }

        private void loadWithGlide(String imageUrl, String source) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .timeout(30000) // 30 seconds timeout
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("ProductAdapter", "❌ Image load FAILED for " + source + ": " + imageUrl, e);
                            if (e != null) {
                                for (Throwable t : e.getRootCauses()) {
                                    Log.e("ProductAdapter", "Caused by: " + t.getMessage());
                                }
                            }
                            return false; // Let Glide handle the error drawable
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("ProductAdapter", "✅ Image load SUCCESS from " + source + ": " + imageUrl);
                            return false; // Let Glide handle setting the resource
                        }
                    })
                    .into(productImage);
        }
    }
} 
package com.projects.agroyard.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class Product {
    private static final String TAG = "Product";
    
    private int id;
    private String productId; // Firestore document ID
    private String farmerName;
    private String farmerMobile;
    private String productName;
    private String harvestingDate;
    private String farmingType;
    private double quantity;
    private double price;
    private double expectedPrice;
    private String description;
    private String imageUrl;
    private String imagePath;
    private boolean registerForBidding;
    private boolean is_sold = false;
    private Map<String, Object> originalData; // Store the original data for reference
    // Bid Information
    private String highestBid;
    private String numberOfBids;
    private String bidderName;

    private String farmerId;

    public String getFarmerId() {
        return farmerId;
    }

    public String getHighestBid() {
        return highestBid;
    }

    public String getNumberOfBids() {
        return numberOfBids;
    }

    public String getBidderName() {
        return bidderName;
    }

    public String getBidStatus() {
        return bidStatus;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    private String bidStatus;
    private String expiresIn;

    public Product() {
        // Empty constructor required for JSON parsing
    }

    /**
     * Constructor for creating Product from Firestore data
     * @param firestoreData Map data from Firestore
     */
    public Product(Map<String, Object> firestoreData) {
        try {
            // Store the original data
            this.originalData = firestoreData;
            
            // Get product ID (Firestore document ID)
            this.productId = (String) firestoreData.get("product_id");
            
            // Attempt to get legacy ID if available
            if (firestoreData.containsKey("id")) {
                if (firestoreData.get("id") instanceof Long) {
                    this.id = ((Long) firestoreData.get("id")).intValue();
                } else if (firestoreData.get("id") instanceof Integer) {
                    this.id = (Integer) firestoreData.get("id");
                } else if (firestoreData.get("id") instanceof String) {
                    try {
                        this.id = Integer.parseInt((String) firestoreData.get("id"));
                    } catch (NumberFormatException e) {
                        this.id = 0;
                    }
                }
            } else {
                this.id = 0; // Default ID for new Firestore records
            }
            
            this.farmerName = (String) firestoreData.get("farmer_name");
            this.farmerMobile = (String) firestoreData.get("farmer_mobile");
            this.productName = (String) firestoreData.get("product_name");
            this.harvestingDate = (String) firestoreData.get("harvesting_date");
            this.farmingType = (String) firestoreData.get("farming_type");
            
            // Handle number conversions safely
            if (firestoreData.get("quantity") instanceof Double) {
                this.quantity = (Double) firestoreData.get("quantity");
            } else if (firestoreData.get("quantity") instanceof Long) {
                this.quantity = ((Long) firestoreData.get("quantity")).doubleValue();
            }
            
            if (firestoreData.get("price") instanceof Double) {
                this.price = (Double) firestoreData.get("price");
            } else if (firestoreData.get("price") instanceof Long) {
                this.price = ((Long) firestoreData.get("price")).doubleValue();
            }
            
            if (firestoreData.get("expected_price") instanceof Double) {
                this.expectedPrice = (Double) firestoreData.get("expected_price");
            } else if (firestoreData.get("expected_price") instanceof Long) {
                this.expectedPrice = ((Long) firestoreData.get("expected_price")).doubleValue();
            }
            
            this.description = (String) firestoreData.get("description");
            
            // Check for register_for_bidding
            if (firestoreData.containsKey("register_for_bidding")) {
                if (firestoreData.get("register_for_bidding") instanceof Boolean) {
                    this.registerForBidding = (Boolean) firestoreData.get("register_for_bidding");
                } else if (firestoreData.get("register_for_bidding") instanceof Long) {
                    this.registerForBidding = ((Long) firestoreData.get("register_for_bidding")) == 1;
                } else if (firestoreData.get("register_for_bidding") instanceof Integer) {
                    this.registerForBidding = ((Integer) firestoreData.get("register_for_bidding")) == 1;
                }
            } else {
                this.registerForBidding = true; // Default to true
            }
            
            // Check for is_sold field
            if (firestoreData.containsKey("is_sold")) {
                if (firestoreData.get("is_sold") instanceof Boolean) {
                    this.is_sold = (Boolean) firestoreData.get("is_sold");
                } else if (firestoreData.get("is_sold") instanceof Long) {
                    this.is_sold = ((Long) firestoreData.get("is_sold")) == 1;
                } else if (firestoreData.get("is_sold") instanceof Integer) {
                    this.is_sold = ((Integer) firestoreData.get("is_sold")) == 1;
                }
            }
            
            // Enhanced image handling with better logging
            if (firestoreData.containsKey("image_url")) {
                this.imageUrl = (String) firestoreData.get("image_url");
                Log.d(TAG, "Got image_url from Firestore for " + this.productName + ": " + this.imageUrl);
            }
            
            if (firestoreData.containsKey("image_path")) {
                this.imagePath = (String) firestoreData.get("image_path");
                Log.d(TAG, "Got image_path from Firestore for " + this.productName + ": " + this.imagePath);
            } else if (firestoreData.containsKey("image_filename")) {
                // Fallback to image_filename for compatibility
                this.imagePath = (String) firestoreData.get("image_filename");
                Log.d(TAG, "Using image_filename as fallback for " + this.productName + ": " + this.imagePath);
            }
            
            // For storing images in Cloudinary (look for these fields)
            if (firestoreData.containsKey("cloudinary_url")) {
                this.imageUrl = (String) firestoreData.get("cloudinary_url");
                Log.d(TAG, "Got cloudinary_url from Firestore: " + this.imageUrl);
            }

            this.highestBid = (String) firestoreData.getOrDefault("highest_bid", "0");
            this.numberOfBids = (String) firestoreData.getOrDefault("number_of_bids", "0");
            this.bidderName = (String) firestoreData.getOrDefault("bidder_name", "");
            this.bidStatus = (String) firestoreData.getOrDefault("bid_status", "");
            this.expiresIn = (String) firestoreData.getOrDefault("expires_in", "");
            this.farmerId = (String) firestoreData.getOrDefault("farmerId", "");

            if (firestoreData.containsKey("image_public_id")) {
                Log.d(TAG, "Product has Cloudinary public_id: " + firestoreData.get("image_public_id"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating Product from Firestore data: " + e.getMessage(), e);
        }
    }

    public Product(JSONObject jsonObject) throws JSONException {
        try {
            this.id = jsonObject.getInt("id");
            this.farmerName = jsonObject.getString("farmer_name");
            this.farmerMobile = jsonObject.optString("farmer_mobile", "");
            this.productName = jsonObject.getString("product_name");
            this.harvestingDate = jsonObject.getString("harvesting_date");
            this.farmingType = jsonObject.getString("farming_type");
            this.quantity = jsonObject.getDouble("quantity");
            this.price = jsonObject.getDouble("price");
            this.expectedPrice = jsonObject.getDouble("expected_price");
            this.description = jsonObject.getString("description");
            
            // Check if product is registered for bidding
            this.registerForBidding = jsonObject.optBoolean("register_for_bidding", true);
            
            // Check if product is sold
            this.is_sold = jsonObject.optBoolean("is_sold", false);
            
            // Handle image paths with proper logging for debugging
            if (jsonObject.has("image_path")) {
                this.imagePath = jsonObject.getString("image_path");
                Log.d(TAG, "Got image_path from JSON: " + this.imagePath + " for product: " + this.productName);
            } else {
                Log.w(TAG, "No image_path for product: " + this.productName);
            }
            
            // Also check for image_url for backwards compatibility
            if (jsonObject.has("image_url")) {
                this.imageUrl = jsonObject.getString("image_url");
                Log.d(TAG, "Got image_url from JSON: " + this.imageUrl + " for product: " + this.productName);
            }
            
            // For backwards compatibility, also check image_filename
            if (jsonObject.has("image_filename") && (this.imagePath == null || this.imagePath.isEmpty())) {
                this.imagePath = jsonObject.getString("image_filename");
                Log.d(TAG, "Using image_filename as fallback: " + this.imagePath);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing product JSON: " + e.getMessage(), e);
            throw e;
        }
    }

    // Getters
    public int getId() { return id; }
    public String getProductId() { return productId; }
    public String getFarmerName() { return farmerName; }
    public String getFarmerMobile() { return farmerMobile; }
    public String getProductName() { return productName; }
    public String getHarvestingDate() { return harvestingDate; }
    public String getFarmingType() { return farmingType; }
    public double getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getExpectedPrice() { return expectedPrice; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getImagePath() { return imagePath; }
    public String getImageFilename() { return imagePath; }
    public boolean isRegisterForBidding() { return registerForBidding; }
    public boolean isSold() { return is_sold; }
    public Map<String, Object> getOriginalData() {
        return originalData;
    }

    // Added setter method for is_sold field
    public void setSold(boolean sold) {
        this.is_sold = sold;
    }
} 
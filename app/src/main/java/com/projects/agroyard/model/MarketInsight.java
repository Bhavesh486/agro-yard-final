package com.projects.agroyard.model;

public class MarketInsight {
    private String cropName;
    private String price;
    private String changePercentage;
    private String imageUrl;
    private String description;

    public MarketInsight(String cropName, String price, String changePercentage, String imageUrl, String description) {
        this.cropName = cropName;
        this.price = price;
        this.changePercentage = changePercentage;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    // Public getters
    public String getCropName() { return cropName; }
    public String getPrice() { return price; }
    public String getChangePercentage() { return changePercentage; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
} 
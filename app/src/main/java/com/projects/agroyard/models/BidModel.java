package com.projects.agroyard.models;

import java.util.Map;

public class BidModel {
    private String productId;
    private String status;

    private String updatedAt;
    private String farmerDistrict;
    private String image;
    private String productName;

    private String endTime;

    private String soldTo;

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSoldTo() {
        return soldTo;
    }

    public void setSoldTo(String soldTo) {
        this.soldTo = soldTo;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    private String farmerName;
    private long price;

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getFarmerDistrict() {
        return farmerDistrict;
    }

    public void setFarmerDistrict(String farmerDistrict) {
        this.farmerDistrict = farmerDistrict;
    }

    private long quantity;

    private Map<String, Bidders> bidders;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Bidders> getBidders() {
        return bidders;
    }

    public void setBidders(Map<String, Bidders> bidders) {
        this.bidders = bidders;
    }

    public static class Bidders {
        private long amount;

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public long getAmount() {
            return amount;
        }
    }
}

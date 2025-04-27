package com.projects.agroyard.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

public class Receipt {
    @DocumentId
    private String id;
    private String productId;
    private String productName;
    private int quantity;
    private int pricePerKg;
    private String farmerId;
    private String farmerName;
    private String farmerPhone;
    private String memberId;
    private String memberName;
    private String memberPhone;
    @ServerTimestamp
    private Timestamp timestamp;
    private String status; // "pending", "completed", "cancelled"

    // Required empty constructor for Firestore
    public Receipt() {
    }

    public Receipt(String productId, String productName, int quantity, int pricePerKg,
                  String farmerId, String farmerName, String farmerPhone,
                  String memberId, String memberName, String memberPhone) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.pricePerKg = pricePerKg;
        this.farmerId = farmerId;
        this.farmerName = farmerName;
        this.farmerPhone = farmerPhone;
        this.memberId = memberId;
        this.memberName = memberName;
        this.memberPhone = memberPhone;
        this.status = "completed"; // By default set to completed
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPricePerKg() {
        return pricePerKg;
    }

    public void setPricePerKg(int pricePerKg) {
        this.pricePerKg = pricePerKg;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getFarmerPhone() {
        return farmerPhone;
    }

    public void setFarmerPhone(String farmerPhone) {
        this.farmerPhone = farmerPhone;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberPhone() {
        return memberPhone;
    }

    public void setMemberPhone(String memberPhone) {
        this.memberPhone = memberPhone;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Helper method to calculate total price
    public int getTotalPrice() {
        // Log the values used in the calculation
        int total = quantity * pricePerKg;
        return total;
    }
    
    // New method to get total price as a long to avoid any precision issues
    public long getTotalPriceLong() {
        return (long) quantity * (long) pricePerKg;
    }
    
    // New method to get formatted total price as string with currency symbol
    public String getFormattedTotalPrice() {
        long total = getTotalPriceLong();
        return "₹" + total;
    }
} 
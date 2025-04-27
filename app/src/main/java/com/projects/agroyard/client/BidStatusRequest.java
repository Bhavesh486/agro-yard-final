package com.projects.agroyard.client;

public class BidStatusRequest {
    private String productId;

    public BidStatusRequest(String productId) {
        this.productId = productId;
    }

    // Getter (optional, but good practice)
    public String getProductId() {
        return productId;
    }
}


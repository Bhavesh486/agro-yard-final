package com.projects.agroyard.models;

public class BidProductModel {
    private BidModel bid;
    private Product product;

    public BidProductModel(BidModel bid, Product product) {
        this.bid = bid;
        this.product = product;
    }

    public BidModel getBid() {
        return bid;
    }

    public Product getProduct() {
        return product;
    }
}

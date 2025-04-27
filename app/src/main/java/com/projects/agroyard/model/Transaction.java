package com.projects.agroyard.model;

public class Transaction {
    private String transactionName;
    private String amount;
    private String quantity;
    private String type;

    public Transaction(String transactionName, String amount, String quantity, String type) {
        this.transactionName = transactionName;
        this.amount = amount;
        this.quantity = quantity;
        this.type = type;
    }

    // Public getters
    public String getTransactionName() { return transactionName; }
    public String getAmount() { return amount; }
    public String getQuantity() { return quantity; }
    public String getType() { return type; }
} 
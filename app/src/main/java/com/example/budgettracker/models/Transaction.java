package com.example.budgettracker.models;

import java.util.Date;

public class Transaction {
    private String id;
    private String userId;
    private String type; // "income" or "expense"
    private double amount;
    private String category;
    private String description;
    private Date date;
    private long timestamp;

    public Transaction() {
    }

    public Transaction(String userId, String type, double amount, String category, String description, Date date) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.timestamp = date.getTime();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        this.timestamp = date.getTime();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
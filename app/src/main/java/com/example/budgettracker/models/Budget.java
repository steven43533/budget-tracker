package com.example.budgettracker.models;

public class Budget {
    private String id;
    private String userId;
    private String category;
    private double limit;
    private double spent;
    private String month; // Format: "YYYY-MM"

    public Budget() {
    }

    public Budget(String userId, String category, double limit, String month) {
        this.userId = userId;
        this.category = category;
        this.limit = limit;
        this.spent = 0.0;
        this.month = month;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getRemaining() {
        return limit - spent;
    }

    public int getPercentageUsed() {
        if (limit == 0) return 0;
        return (int) ((spent / limit) * 100);
    }
}
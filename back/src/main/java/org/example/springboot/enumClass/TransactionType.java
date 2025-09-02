package org.example.springboot.enumClass;

public enum TransactionType {
    INCOME(1, "收入"),
    EXPENSE(2, "支出");

    private final Integer value;
    private final String description;

    TransactionType(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public Integer getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
} 
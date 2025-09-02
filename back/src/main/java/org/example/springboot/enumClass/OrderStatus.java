package org.example.springboot.enumClass;

public enum OrderStatus {
    WAITING_PAYMENT(0, "待支付"),
    PAID_WAITING_CONFIRM(1, "已支付待确认"),
    CONFIRMED(2, "已确认"),
    CANCELED(3, "已取消"),
    REFUNDED(4, "已退款");

    private final Integer value;
    private final String description;

    OrderStatus(Integer value, String description) {
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
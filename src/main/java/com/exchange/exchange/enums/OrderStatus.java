package com.exchange.exchange.enums;

public enum OrderStatus {
    NEW,
    PARTIAL_FILLED,
    FILLED,
    CANCELED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

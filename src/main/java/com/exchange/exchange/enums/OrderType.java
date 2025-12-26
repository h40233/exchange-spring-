package com.exchange.exchange.enums;

public enum OrderType {
    MARKET,
    LIMIT;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

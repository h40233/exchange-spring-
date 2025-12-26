package com.exchange.exchange.enums;

public enum OrderSide {
    BUY,
    SELL;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

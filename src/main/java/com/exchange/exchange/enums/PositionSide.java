package com.exchange.exchange.enums;

public enum PositionSide {
    LONG,
    SHORT;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

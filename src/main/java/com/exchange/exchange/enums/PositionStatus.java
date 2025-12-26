package com.exchange.exchange.enums;

public enum PositionStatus {
    OPEN,
    CLOSED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

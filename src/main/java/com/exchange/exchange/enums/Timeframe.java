package com.exchange.exchange.enums;

public enum Timeframe {
    _1D("1D"),
    _1H("1H"),
    _30m("30m"),
    _15m("15m"),
    _5m("5m"),
    _1m("1m");

    private final String value;

    Timeframe(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

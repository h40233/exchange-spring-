package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// Timeframe 定義 K 線圖的時間粒度。
// 用於 Candle 實體中，決定一根 K 線代表多長的時間區間。
public enum Timeframe {
    _1D("1D"),   // 日線
    _1H("1H"),   // 小時線
    _30m("30m"), // 30分
    _15m("15m"), // 15分
    _5m("5m"),   // 5分
    _1m("1m");   // 1分

    private final String value;

    Timeframe(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
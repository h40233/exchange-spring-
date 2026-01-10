package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// Timeframe 枚舉定義了 K 線圖 (Candlestick) 的時間粒度。
// 用於指定一根 K 線代表多長的時間區間。
// 由於 Java 變數名稱不能以數字開頭，因此使用底線前綴 (如 _1m)。
public enum Timeframe {
    _1D("1D"),   // 日線 (1 Day)
    _1H("1H"),   // 小時線 (1 Hour)
    _30m("30m"), // 30分鐘 (30 Minutes)
    _15m("15m"), // 15分鐘 (15 Minutes)
    _5m("5m"),   // 5分鐘 (5 Minutes)
    _1m("1m");   // 1分鐘 (1 Minute)

    // 儲存實際的字串值 (去除底線)
    private final String value;

    // 建構子
    Timeframe(String value) {
        this.value = value;
    }

    // 取得實際的字串值 (例如 "1m")，用於資料庫儲存或 API 傳輸
    public String getValue() {
        return value;
    }
}
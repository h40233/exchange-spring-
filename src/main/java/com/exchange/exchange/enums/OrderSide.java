package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// OrderSide 枚舉定義了交易的方向。
// BUY: 買入 (Bid)
// SELL: 賣出 (Ask)
public enum OrderSide {
    BUY,
    SELL;

    // 覆寫 toString 方法，使其回傳小寫字串 (用於日誌或簡單顯示)
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
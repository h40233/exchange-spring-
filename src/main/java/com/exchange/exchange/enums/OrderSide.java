package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// OrderSide 枚舉定義了交易的買賣方向。
// 核心用途：
// BUY: 代表買入行為 (Bid)，希望以基礎幣換取報價幣。
// SELL: 代表賣出行為 (Ask)，希望以報價幣換取基礎幣。
public enum OrderSide {
    BUY,
    SELL;

    // 覆寫 toString 方法，使其回傳小寫字串
    // 這在日誌記錄或簡單的前端顯示時很有用 (例如顯示為 "buy" 而非 "BUY")
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
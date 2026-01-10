package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// OrderStatus 枚舉定義了訂單在生命週期中的各種狀態。
// NEW: 新訂單 (已成功掛入訂單簿，尚未有任何成交)。
// PARTIAL_FILLED: 部分成交 (已撮合部分數量，剩餘部分繼續掛單等待成交)。
// FILLED: 完全成交 (委託數量已全部撮合完畢，訂單生命週期結束)。
// CANCELED: 已取消 (使用者手動撤單或系統自動撤單，剩餘未成交部分失效)。
public enum OrderStatus {
    NEW,
    PARTIAL_FILLED,
    FILLED,
    CANCELED;

    // 覆寫 toString 回傳小寫
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
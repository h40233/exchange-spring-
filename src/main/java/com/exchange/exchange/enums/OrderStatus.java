package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// OrderStatus 定義訂單的生命週期。
// NEW: 新訂單 (已掛入 OrderBook，尚未成交)。
// PARTIAL_FILLED: 部分成交 (已撮合部分數量，剩餘部分繼續掛單)。
// FILLED: 完全成交 (所有數量均已撮合)。
// CANCELED: 已取消 (剩餘未成交部分被撤回)。
public enum OrderStatus {
    NEW,
    PARTIAL_FILLED,
    FILLED,
    CANCELED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
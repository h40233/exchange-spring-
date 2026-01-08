package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// OrderType 定義訂單的價格策略。
// MARKET: 市價單 (不計成本，立即成交，吃單 Taker)。
// LIMIT: 限價單 (指定價格，掛單 Maker 或 吃單 Taker)。
public enum OrderType {
    MARKET,
    LIMIT;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// OrderType 枚舉定義了訂單的價格策略類型。
// MARKET: 市價單 (Market Order)。
//         - 不指定價格，以當前市場最優價格立即成交。
//         - 扮演 Taker (吃單者) 角色，通常手續費較高。
// LIMIT: 限價單 (Limit Order)。
//         - 指定價格，只有當市場價格達到或優於指定價格時才成交。
//         - 可扮演 Maker (掛單者) 提供流動性，或 Taker (吃單者)。
public enum OrderType {
    MARKET,
    LIMIT;

    // 覆寫 toString 回傳小寫
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
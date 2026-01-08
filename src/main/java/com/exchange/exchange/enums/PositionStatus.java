package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// PositionStatus 定義合約倉位的狀態。
// OPEN: 持倉中 (尚有未平倉數量)。
// CLOSED: 已結清 (數量歸零，盈虧已結算)。
public enum PositionStatus {
    OPEN,
    CLOSED;

    // 覆寫 toString 以回傳小寫 (如 "open")
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
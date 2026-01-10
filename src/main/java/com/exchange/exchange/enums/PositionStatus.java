package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// PositionStatus 枚舉定義了合約倉位的當前狀態。
// OPEN: 持倉中 (Open Position)。
//       - 帳戶中持有該合約的部位，會隨市場價格產生浮動盈虧。
// CLOSED: 已結清 (Closed Position)。
//         - 倉位數量已歸零，盈虧已實現並結算至餘額。
public enum PositionStatus {
    OPEN,
    CLOSED;

    // 覆寫 toString 回傳小寫 (如 "open")
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
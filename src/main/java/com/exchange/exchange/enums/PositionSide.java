package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// PositionSide 枚舉定義了合約持倉的方向。
// LONG: 做多 (Long Position)。
//       - 買入開倉，預期價格上漲以獲利。
// SHORT: 做空 (Short Position)。
//        - 賣出開倉，預期價格下跌以獲利。
public enum PositionSide {
    LONG,
    SHORT;

    // 覆寫 toString 回傳小寫
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
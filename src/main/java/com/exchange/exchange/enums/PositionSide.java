package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// PositionSide 定義合約持倉的方向。
// LONG: 做多 (買入開倉，預期上漲)。
// SHORT: 做空 (賣出開倉，預期下跌)。
public enum PositionSide {
    LONG,
    SHORT;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
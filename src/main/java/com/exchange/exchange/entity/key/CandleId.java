package com.exchange.exchange.entity.key;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import com.exchange.exchange.enums.Timeframe;

// ====== 檔案總結 ======
// CandleId 類別定義了 `Candle` 實體的複合主鍵。
// 識別一根 K 線的三要素：Symbol (幣對) + Timeframe (週期) + OpenTime (時間)。
public class CandleId implements Serializable {

    private String symbolId;
    private Timeframe timeframe;
    private LocalDateTime openTime;

    public CandleId() {
    }

    public CandleId(String symbolId, Timeframe timeframe, LocalDateTime openTime) {
        this.symbolId = symbolId;
        this.timeframe = timeframe;
        this.openTime = openTime;
    }

    // --- Getters & Setters ---

    public String getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    public Timeframe getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
    }

    public LocalDateTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandleId candleId = (CandleId) o;
        return Objects.equals(symbolId, candleId.symbolId) && timeframe == candleId.timeframe && Objects.equals(openTime, candleId.openTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbolId, timeframe, openTime);
    }
}
package com.exchange.exchange.entity.key;

import com.exchange.exchange.enums.Timeframe;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

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
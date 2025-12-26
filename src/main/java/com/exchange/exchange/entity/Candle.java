package com.exchange.exchange.entity;

import com.exchange.exchange.entity.key.CandleId;
import com.exchange.exchange.enums.Timeframe;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "candles")
@IdClass(CandleId.class)
public class Candle {
    @Id
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Timeframe timeframe;

    @Id
    @Column(name = "open_time")
    private LocalDateTime openTime;

    @Column(precision = 36, scale = 18)
    private BigDecimal open;

    @Column(precision = 36, scale = 18)
    private BigDecimal high;

    @Column(precision = 36, scale = 18)
    private BigDecimal low;

    @Column(precision = 36, scale = 18)
    private BigDecimal close;

    @Column(name = "close_time")
    private LocalDateTime closeTime;

    public Candle() {
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

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }
}
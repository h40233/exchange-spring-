package com.exchange.exchange.entity;

// 引入複合主鍵類別
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.exchange.exchange.entity.key.CandleId;
import com.exchange.exchange.enums.Timeframe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Candle 實體類別對應 `candles` 表。
// 儲存金融市場的 OHLCV (Open, High, Low, Close, Volume) K線資料。
// 使用 @IdClass 定義複合主鍵，因為單一 K 線需由 (交易對 + 週期 + 時間) 共同唯一決定。
@Entity
@Table(name = "candles")
@IdClass(CandleId.class)
public class Candle {

    // 複合主鍵 1: 交易對
    @Id
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 複合主鍵 2: 時間週期 (如 1m, 1h)
    @Id
    @Column(length = 10)
    private Timeframe timeframe;

    // 複合主鍵 3: K線開盤時間
    @Id
    @Column(name = "open_time")
    private LocalDateTime openTime;

    // 開盤價
    @Column(precision = 36, scale = 18)
    private BigDecimal open;

    // 最高價
    @Column(precision = 36, scale = 18)
    private BigDecimal high;

    // 最低價
    @Column(precision = 36, scale = 18)
    private BigDecimal low;

    // 收盤價 (即時行情中為最新成交價)
    @Column(precision = 36, scale = 18)
    private BigDecimal close;

    // K線結束時間
    @Column(name = "close_time")
    private LocalDateTime closeTime;

    public Candle() {
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
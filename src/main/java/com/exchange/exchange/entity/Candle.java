package com.exchange.exchange.entity;

// 引入複合主鍵類別
import com.exchange.exchange.entity.key.CandleId;
// 引入時間週期枚舉 (如 1m, 5m, 1h, 1d)
import com.exchange.exchange.enums.Timeframe;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// Candle 實體對應 `candles` 表。
// 儲存市場的 OHLCV 數據 (Open, High, Low, Close, Volume)。
// 使用複合主鍵：(Symbol + Timeframe + OpenTime) 唯一決定一根 K 線。
@Entity
@Table(name = "candles")
@IdClass(CandleId.class) // 指定複合主鍵類別
public class Candle {

    // 主鍵 1: 交易對 ID
    @Id
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 主鍵 2: 時間週期 (1分, 1小時等)
    @Id
    @Column(length = 10)
    private Timeframe timeframe;

    // 主鍵 3: 開盤時間 (K線的起始點)
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

    // 收盤價
    @Column(precision = 36, scale = 18)
    private BigDecimal close;

    // 收盤時間 (通常是 openTime + timeframe - 1秒)
    @Column(name = "close_time")
    private LocalDateTime closeTime;

    // 無參數建構子
    public Candle() {
    }

    // 取得交易對
    public String getSymbolId() {
        return symbolId;
    }

    // 設定交易對
    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    // 取得時間週期
    public Timeframe getTimeframe() {
        return timeframe;
    }

    // 設定時間週期
    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
    }

    // 取得開盤時間
    public LocalDateTime getOpenTime() {
        return openTime;
    }

    // 設定開盤時間
    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
    }

    // 取得開盤價
    public BigDecimal getOpen() {
        return open;
    }

    // 設定開盤價
    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    // 取得最高價
    public BigDecimal getHigh() {
        return high;
    }

    // 設定最高價
    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    // 取得最低價
    public BigDecimal getLow() {
        return low;
    }

    // 設定最低價
    public void setLow(BigDecimal low) {
        this.low = low;
    }

    // 取得收盤價
    public BigDecimal getClose() {
        return close;
    }

    // 設定收盤價
    public void setClose(BigDecimal close) {
        this.close = close;
    }

    // 取得收盤時間
    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    // 設定收盤時間
    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }
}
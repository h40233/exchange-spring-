package com.exchange.exchange.entity.key;

// 引入枚舉與時間
import com.exchange.exchange.enums.Timeframe;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

// ====== 檔案總結 ======
// CandleId 用於定義 `Candle` 實體的複合主鍵。
// 唯一識別一根 K 線的條件是：
// 1. 哪個交易對 (symbolId)
// 2. 哪種時間週期 (timeframe，如 1小時)
// 3. 哪個時間點 (openTime)
public class CandleId implements Serializable {

    private String symbolId;
    private Timeframe timeframe;
    private LocalDateTime openTime;

    // 無參數建構子
    public CandleId() {
    }

    // 全參數建構子
    public CandleId(String symbolId, Timeframe timeframe, LocalDateTime openTime) {
        this.symbolId = symbolId;
        this.timeframe = timeframe;
        this.openTime = openTime;
    }

    // 取得交易對 ID
    public String getSymbolId() {
        return symbolId;
    }

    // 設定交易對 ID
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

    // 覆寫 equals (複合鍵必須)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandleId candleId = (CandleId) o;
        return Objects.equals(symbolId, candleId.symbolId) && timeframe == candleId.timeframe && Objects.equals(openTime, candleId.openTime);
    }

    // 覆寫 hashCode (複合鍵必須)
    @Override
    public int hashCode() {
        return Objects.hash(symbolId, timeframe, openTime);
    }
}
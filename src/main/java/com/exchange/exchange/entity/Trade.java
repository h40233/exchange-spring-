package com.exchange.exchange.entity;

import com.exchange.exchange.enums.OrderSide;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// Trade 實體對應 `trades` 表。
// 紀錄撮合引擎配對成功的每一筆交易詳情。
@Entity
@Table(name = "trades")
public class Trade {

    // 成交紀錄 ID (自動遞增)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tradesID")
    private Integer tradeId;

    // 交易對 ID
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // Taker (主動方) 訂單 ID
    @Column(name = "taker_orderID")
    private Integer takerOrderId;

    // Maker (被動方) 訂單 ID
    @Column(name = "maker_orderID")
    private Integer makerOrderId;

    // 成交價格
    @Column(precision = 36, scale = 18)
    private BigDecimal price;

    // 成交數量
    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    // Taker 的方向 (BUY/SELL)
    @Column(name = "taker_side", length = 10, columnDefinition = "enum('buy','sell') COLLATE utf8mb3_bin")
    private OrderSide takerSide;

    // 成交時間
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    // 手續費幣種
    @Column(name = "fee_currency", length = 45)
    private String feeCurrency;

    // 手續費金額
    @Column(name = "fee_amount", precision = 36, scale = 18)
    private BigDecimal feeAmount;

    // 交易類型 (SPOT/CONTRACT)
    @Column(name = "trade_type", length = 10, columnDefinition = "enum('spot','contract') COLLATE utf8mb3_bin")
    private com.exchange.exchange.enums.TradeType tradeType;

    // 無參數建構子
    public Trade() {
    }

    // 取得交易類型
    public com.exchange.exchange.enums.TradeType getTradeType() {
        return tradeType;
    }

    // 設定交易類型
    public void setTradeType(com.exchange.exchange.enums.TradeType tradeType) {
        this.tradeType = tradeType;
    }

    // 取得成交 ID
    public Integer getTradeId() {
        return tradeId;
    }

    // 設定成交 ID
    public void setTradeId(Integer tradeId) {
        this.tradeId = tradeId;
    }

    // 取得交易對 ID
    public String getSymbolId() {
        return symbolId;
    }

    // 設定交易對 ID
    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    // 取得 Taker 訂單 ID
    public Integer getTakerOrderId() {
        return takerOrderId;
    }

    // 設定 Taker 訂單 ID
    public void setTakerOrderId(Integer takerOrderId) {
        this.takerOrderId = takerOrderId;
    }

    // 取得 Maker 訂單 ID
    public Integer getMakerOrderId() {
        return makerOrderId;
    }

    // 設定 Maker 訂單 ID
    public void setMakerOrderId(Integer makerOrderId) {
        this.makerOrderId = makerOrderId;
    }

    // 取得成交價格
    public BigDecimal getPrice() {
        return price;
    }

    // 設定成交價格
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // 取得成交數量
    public BigDecimal getQuantity() {
        return quantity;
    }

    // 設定成交數量
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    // 取得 Taker 方向
    public OrderSide getTakerSide() {
        return takerSide;
    }

    // 設定 Taker 方向
    public void setTakerSide(OrderSide takerSide) {
        this.takerSide = takerSide;
    }

    // 取得成交時間
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    // 設定成交時間
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    // 取得手續費幣種
    public String getFeeCurrency() {
        return feeCurrency;
    }

    // 設定手續費幣種
    public void setFeeCurrency(String feeCurrency) {
        this.feeCurrency = feeCurrency;
    }

    // 取得手續費金額
    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    // 設定手續費金額
    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }
}
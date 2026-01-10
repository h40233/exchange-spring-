package com.exchange.exchange.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.exchange.exchange.enums.OrderSide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Trade 實體類別對應 `trades` 表。
// 職責：記錄撮合引擎成功匹配的每一筆交易。
// 每一筆 Trade 必然連結兩個 Order：一個 Taker (主動單) 和一個 Maker (被動單)。
@Entity
@Table(name = "trades")
public class Trade {

    // 成交流水號 (Primary Key)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tradesID")
    private Integer tradeId;

    // 交易對代碼
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // Taker (主動吃單者) 的訂單 ID
    @Column(name = "taker_orderID")
    private Integer takerOrderId;

    // Maker (被動掛單者) 的訂單 ID
    @Column(name = "maker_orderID")
    private Integer makerOrderId;

    // 最終成交價格
    @Column(precision = 36, scale = 18)
    private BigDecimal price;

    // 最終成交數量
    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    // Taker 的買賣方向 (用於判斷是由買方發起還是賣方發起)
    @Column(name = "taker_side", length = 10, columnDefinition = "enum('buy','sell') COLLATE utf8mb3_bin")
    private OrderSide takerSide;

    // 成交時間
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    // 手續費幣種 (例如 USDT 或 BNB)
    @Column(name = "fee_currency", length = 45)
    private String feeCurrency;

    // 手續費金額
    @Column(name = "fee_amount", precision = 36, scale = 18)
    private BigDecimal feeAmount;

    // 交易模式 (SPOT/CONTRACT)
    @Column(name = "trade_type", length = 10, columnDefinition = "enum('spot','contract') COLLATE utf8mb3_bin")
    private com.exchange.exchange.enums.TradeType tradeType;

    public Trade() {
    }

    // --- Getters & Setters ---

    public com.exchange.exchange.enums.TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(com.exchange.exchange.enums.TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public Integer getTradeId() {
        return tradeId;
    }

    public void setTradeId(Integer tradeId) {
        this.tradeId = tradeId;
    }

    public String getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    public Integer getTakerOrderId() {
        return takerOrderId;
    }

    public void setTakerOrderId(Integer takerOrderId) {
        this.takerOrderId = takerOrderId;
    }

    public Integer getMakerOrderId() {
        return makerOrderId;
    }

    public void setMakerOrderId(Integer makerOrderId) {
        this.makerOrderId = makerOrderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public OrderSide getTakerSide() {
        return takerSide;
    }

    public void setTakerSide(OrderSide takerSide) {
        this.takerSide = takerSide;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getFeeCurrency() {
        return feeCurrency;
    }

    public void setFeeCurrency(String feeCurrency) {
        this.feeCurrency = feeCurrency;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }
}
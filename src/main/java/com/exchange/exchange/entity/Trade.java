package com.exchange.exchange.entity;

import com.exchange.exchange.enums.OrderSide;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tradesID")
    private Integer tradeId;

    @Column(name = "symbolID", length = 45)
    private String symbolId;

    @Column(name = "taker_orderID")
    private Integer takerOrderId;

    @Column(name = "maker_orderID")
    private Integer makerOrderId;

    @Column(precision = 36, scale = 18)
    private BigDecimal price;

    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    @Column(name = "taker_side", length = 10, columnDefinition = "enum('buy','sell') COLLATE utf8mb3_bin")
    private OrderSide takerSide;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "fee_currency", length = 45)
    private String feeCurrency;

    @Column(name = "fee_amount", precision = 36, scale = 18)
    private BigDecimal feeAmount;

    @Column(name = "trade_type", length = 10, columnDefinition = "enum('spot','contract') COLLATE utf8mb3_bin")
    private com.exchange.exchange.enums.TradeType tradeType;

    public Trade() {
    }

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
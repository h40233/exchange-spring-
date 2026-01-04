package com.exchange.exchange.entity;

import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.OrderType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderID")
    private Integer orderId;

    @Column(name = "memberID")
    private Integer memberId;

    @Column(name = "symbolID", length = 45)
    private String symbolId;

    @Column(length = 10, columnDefinition = "enum('buy','sell') COLLATE utf8mb3_bin")
    private OrderSide side;

    @Column(length = 10, columnDefinition = "enum('market','limit') COLLATE utf8mb3_bin")
    private OrderType type;

    @Column(precision = 36, scale = 18)
    private BigDecimal price;

    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    @Column(name = "filled_quantity", precision = 36, scale = 18)
    private BigDecimal filledQuantity;

    @Column(name = "cum_quote_qty", precision = 36, scale = 18)
    private BigDecimal cumQuoteQty = BigDecimal.ZERO;

    @Column(length = 20, columnDefinition = "enum('new','partial_filled','filled','canceled') COLLATE utf8mb3_bin")
    private OrderStatus status;

    @Column(name = "post_only")
    private Boolean postOnly;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "trade_type", length = 10, columnDefinition = "enum('spot','contract') COLLATE utf8mb3_bin")
    private com.exchange.exchange.enums.TradeType tradeType;

    public Order() {
    }

    public com.exchange.exchange.enums.TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(com.exchange.exchange.enums.TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
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

    public BigDecimal getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(BigDecimal filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    public BigDecimal getCumQuoteQty() {
        return cumQuoteQty;
    }

    public void setCumQuoteQty(BigDecimal cumQuoteQty) {
        this.cumQuoteQty = cumQuoteQty;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Boolean getPostOnly() {
        return postOnly;
    }

    public void setPostOnly(Boolean postOnly) {
        this.postOnly = postOnly;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
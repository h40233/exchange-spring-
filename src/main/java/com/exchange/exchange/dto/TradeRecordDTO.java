package com.exchange.exchange.dto;

import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.TradeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeRecordDTO {
    private Integer tradeId;
    private String symbolId;
    private OrderSide side; 
    private BigDecimal price;
    private BigDecimal quantity;
    private LocalDateTime executedAt;
    private TradeType tradeType;
    private String role; // MAKER or TAKER

    public TradeRecordDTO() {}

    public Integer getTradeId() { return tradeId; }
    public void setTradeId(Integer tradeId) { this.tradeId = tradeId; }

    public String getSymbolId() { return symbolId; }
    public void setSymbolId(String symbolId) { this.symbolId = symbolId; }

    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public TradeType getTradeType() { return tradeType; }
    public void setTradeType(TradeType tradeType) { this.tradeType = tradeType; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

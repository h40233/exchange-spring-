package com.exchange.exchange.dto;

import java.math.BigDecimal;

import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderType;

// ====== 檔案總結 ======
// OrderRequest 用於封裝前端發送的「下單請求」參數。
// 這是一個純資料物件 (POJO)，不包含任何業務邏輯。
// Controller 層會接收此物件並傳遞給 OrderService 進行處理。
public class OrderRequest {
    
    // 交易對 (如 BTCUSDT)
    private String symbolId;
    
    // 訂單方向 (BUY/SELL)
    private OrderSide side;
    
    // 訂單類型 (LIMIT/MARKET)
    private OrderType type;
    
    // 委託價格 (市價單可為空或忽略)
    private BigDecimal price;
    
    // 委託數量
    private BigDecimal quantity;
    
    // 交易模式 (SPOT/CONTRACT)
    private com.exchange.exchange.enums.TradeType tradeType;

    // --- Getters & Setters ---

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

    public com.exchange.exchange.enums.TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(com.exchange.exchange.enums.TradeType tradeType) {
        this.tradeType = tradeType;
    }
}
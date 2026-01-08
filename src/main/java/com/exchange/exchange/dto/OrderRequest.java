package com.exchange.exchange.dto;

import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderType;
import java.math.BigDecimal;

// ====== 檔案總結 ======
// OrderRequest 封裝前端的下單請求參數。
// 這是一個單純的 POJO，不含業務邏輯。
public class OrderRequest {
    private String symbolId;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal quantity;
    private com.exchange.exchange.enums.TradeType tradeType;

    // 取得交易對 ID
    public String getSymbolId() {
        return symbolId;
    }

    // 設定交易對 ID
    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    // 取得方向
    public OrderSide getSide() {
        return side;
    }

    // 設定方向
    public void setSide(OrderSide side) {
        this.side = side;
    }

    // 取得類型
    public OrderType getType() {
        return type;
    }

    // 設定類型
    public void setType(OrderType type) {
        this.type = type;
    }

    // 取得價格
    public BigDecimal getPrice() {
        return price;
    }

    // 設定價格
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // 取得數量
    public BigDecimal getQuantity() {
        return quantity;
    }

    // 設定數量
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    // 取得交易模式
    public com.exchange.exchange.enums.TradeType getTradeType() {
        return tradeType;
    }

    // 設定交易模式
    public void setTradeType(com.exchange.exchange.enums.TradeType tradeType) {
        this.tradeType = tradeType;
    }
}
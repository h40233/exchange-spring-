package com.exchange.exchange.dto;

import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.TradeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// TradeRecordDTO 用於 API 回傳成交紀錄。
// 將複雜的 Trade 實體轉換為使用者視角的單一紀錄。
// role 欄位標記該使用者在此次交易中是 TAKER (吃單) 還是 MAKER (掛單)。
public class TradeRecordDTO {
    private Integer tradeId;
    private String symbolId;
    private OrderSide side; 
    private BigDecimal price;
    private BigDecimal quantity;
    private LocalDateTime executedAt;
    private TradeType tradeType;
    private String role; // MAKER or TAKER

    // 無參數建構子
    public TradeRecordDTO() {}

    // 取得成交 ID
    public Integer getTradeId() { return tradeId; }
    
    // 設定成交 ID
    public void setTradeId(Integer tradeId) { this.tradeId = tradeId; }

    // 取得交易對
    public String getSymbolId() { return symbolId; }
    
    // 設定交易對
    public void setSymbolId(String symbolId) { this.symbolId = symbolId; }

    // 取得方向
    public OrderSide getSide() { return side; }
    
    // 設定方向
    public void setSide(OrderSide side) { this.side = side; }

    // 取得成交價格
    public BigDecimal getPrice() { return price; }
    
    // 設定成交價格
    public void setPrice(BigDecimal price) { this.price = price; }

    // 取得成交數量
    public BigDecimal getQuantity() { return quantity; }
    
    // 設定成交數量
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    // 取得成交時間
    public LocalDateTime getExecutedAt() { return executedAt; }
    
    // 設定成交時間
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    // 取得交易類型
    public TradeType getTradeType() { return tradeType; }
    
    // 設定交易類型
    public void setTradeType(TradeType tradeType) { this.tradeType = tradeType; }

    // 取得角色 (MAKER/TAKER)
    public String getRole() { return role; }
    
    // 設定角色
    public void setRole(String role) { this.role = role; }
}
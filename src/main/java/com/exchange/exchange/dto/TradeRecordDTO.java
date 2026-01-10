package com.exchange.exchange.dto;

import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.TradeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// TradeRecordDTO 是一個 Data Transfer Object (DTO)，專門用於 API 回傳成交紀錄。
// 目的：
// 1. 資料篩選：僅回傳前端需要的欄位，隱藏不必要的資料庫細節。
// 2. 格式轉換：將後端複雜的關聯資料 (如 Trade 實體) 扁平化為單一物件。
// 3. 角色標記：新增 `role` 欄位，明確告知使用者在此筆交易中是 Maker 還是 Taker。
public class TradeRecordDTO {
    
    // 成交流水號
    private Integer tradeId;
    
    // 交易對 (如 BTCUSDT)
    private String symbolId;
    
    // 交易方向 (BUY/SELL) - 對應使用者自己的操作方向
    private OrderSide side; 
    
    // 成交價格
    private BigDecimal price;
    
    // 成交數量
    private BigDecimal quantity;
    
    // 成交時間
    private LocalDateTime executedAt;
    
    // 交易類型 (SPOT/CONTRACT)
    private TradeType tradeType;
    
    // 角色標記 (MAKER: 掛單者 / TAKER: 吃單者)
    // 用於前端顯示不同的費率或標籤
    private String role; 

    // 無參數建構子 (JSON 序列化所需)
    public TradeRecordDTO() {}

    // --- Getters & Setters ---

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
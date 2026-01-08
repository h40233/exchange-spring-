package com.exchange.exchange.entity;

// 引入枚舉：方向、狀態、類型
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.OrderType;
// 引入 JPA 註解
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// Order 實體對應資料庫中的 `orders` 表。
// 核心職責：記錄使用者委託單的完整生命週期 (從建立、部分成交到完全成交或取消)。
// 關鍵欄位：
// - filledQuantity: 已成交數量。
// - cumQuoteQty: 累計成交金額 (用於計算均價)。
@Entity
@Table(name = "orders")
public class Order {

    // 主鍵：訂單 ID (自動遞增)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderID")
    private Integer orderId;

    // 所屬會員 ID (關聯至 members 表)
    @Column(name = "memberID")
    private Integer memberId;

    // 交易對 ID (如 "BTCUSDT")
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 訂單方向 (買入/賣出)
    @Column(length = 10, columnDefinition = "enum('buy','sell') COLLATE utf8mb3_bin")
    private OrderSide side;

    // 訂單類型 (市價單/限價單)
    @Column(length = 10, columnDefinition = "enum('market','limit') COLLATE utf8mb3_bin")
    private OrderType type;

    // 委託價格 (限價單必填)
    // 使用 BigDecimal(36, 18) 確保高精度金額計算
    @Column(precision = 36, scale = 18)
    private BigDecimal price;

    // 委託原始數量
    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    // 已成交數量 (初始為 0)
    // 當此值等於 quantity 時，狀態應轉為 FILLED
    @Column(name = "filled_quantity", precision = 36, scale = 18)
    private BigDecimal filledQuantity;

    // 累計成交總金額 (Cumulative Quote Quantity)
    // 用途：計算平均成交價 = cumQuoteQty / filledQuantity
    @Column(name = "cum_quote_qty", precision = 36, scale = 18)
    private BigDecimal cumQuoteQty = BigDecimal.ZERO;

    // 訂單狀態 (NEW, PARTIAL_FILLED, FILLED, CANCELED)
    @Column(length = 20, columnDefinition = "enum('new','partial_filled','filled','canceled') COLLATE utf8mb3_bin")
    private OrderStatus status;

    // 是否為 Post-Only (僅做 Maker)
    // 若設為 true，則該訂單保證不會吃單 (Taker)，否則系統會自動取消或調整
    @Column(name = "post_only")
    private Boolean postOnly;

    // 建立時間
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 最後更新時間
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 交易模式 (現貨/合約)
    @Column(name = "trade_type", length = 10, columnDefinition = "enum('spot','contract') COLLATE utf8mb3_bin")
    private com.exchange.exchange.enums.TradeType tradeType;

    // 無參數建構子 (JPA 規範必須)
    public Order() {
    }

    // 取得交易模式
    public com.exchange.exchange.enums.TradeType getTradeType() {
        return tradeType;
    }

    // 設定交易模式
    public void setTradeType(com.exchange.exchange.enums.TradeType tradeType) {
        this.tradeType = tradeType;
    }

    // 取得訂單 ID
    public Integer getOrderId() {
        return orderId;
    }

    // 設定訂單 ID
    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    // 取得會員 ID
    public Integer getMemberId() {
        return memberId;
    }

    // 設定會員 ID
    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    // 取得交易對 ID
    public String getSymbolId() {
        return symbolId;
    }

    // 設定交易對 ID
    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    // 取得訂單方向
    public OrderSide getSide() {
        return side;
    }

    // 設定訂單方向
    public void setSide(OrderSide side) {
        this.side = side;
    }

    // 取得訂單類型
    public OrderType getType() {
        return type;
    }

    // 設定訂單類型
    public void setType(OrderType type) {
        this.type = type;
    }

    // 取得委託價格
    public BigDecimal getPrice() {
        return price;
    }

    // 設定委託價格
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // 取得委託數量
    public BigDecimal getQuantity() {
        return quantity;
    }

    // 設定委託數量
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    // 取得已成交數量
    public BigDecimal getFilledQuantity() {
        return filledQuantity;
    }

    // 設定已成交數量
    public void setFilledQuantity(BigDecimal filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    // 取得累計成交金額
    public BigDecimal getCumQuoteQty() {
        return cumQuoteQty;
    }

    // 設定累計成交金額
    public void setCumQuoteQty(BigDecimal cumQuoteQty) {
        this.cumQuoteQty = cumQuoteQty;
    }

    // 取得訂單狀態
    public OrderStatus getStatus() {
        return status;
    }

    // 設定訂單狀態
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    // 取得 Post-Only 設定
    public Boolean getPostOnly() {
        return postOnly;
    }

    // 設定 Post-Only 設定
    public void setPostOnly(Boolean postOnly) {
        this.postOnly = postOnly;
    }

    // 取得建立時間
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // 設定建立時間
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 取得更新時間
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 設定更新時間
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
// ====== 備註區 ======
/*
[註1] 狀態管理建議：
      目前的 setter 方法允許隨意更改狀態。建議在 Entity 內部實作狀態轉移邏輯 (State Transition Logic)，
      例如 `cancel()` 方法只能在 `NEW` 或 `PARTIAL_FILLED` 狀態下被呼叫，否則拋出異常。
*/
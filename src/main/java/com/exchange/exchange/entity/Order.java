package com.exchange.exchange.entity;

// 引入枚舉：定義訂單的買賣方向、狀態與類型
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.OrderType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Order 實體類別對應資料庫中的 `orders` 表。
// 核心職責：
// 1. 記錄使用者的委託單詳情 (價格、數量、方向)。
// 2. 追蹤訂單生命週期 (新建 -> 部分成交 -> 完全成交/取消)。
// 3. 紀錄成交進度 (filledQuantity, cumQuoteQty)。
@Entity
@Table(name = "orders")
public class Order {

    // 訂單唯一識別碼 (Primary Key)，由資料庫自動遞增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderID")
    private Integer orderId;

    // 所屬會員 ID (Foreign Key -> members.memberID)
    @Column(name = "memberID")
    private Integer memberId;

    // 交易對代碼 (例如 "BTCUSDT")
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 訂單方向 (BUY: 買入, SELL: 賣出)
    @Column(length = 10, columnDefinition = "enum('buy','sell') COLLATE utf8mb3_bin")
    private OrderSide side;

    // 訂單類型 (MARKET: 市價, LIMIT: 限價)
    @Column(length = 10, columnDefinition = "enum('market','limit') COLLATE utf8mb3_bin")
    private OrderType type;

    // 委託價格 (對於限價單有效)
    // 使用高精度 BigDecimal 避免浮點數誤差
    @Column(precision = 36, scale = 18)
    private BigDecimal price;

    // 委託原始數量
    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    // 已成交數量
    // 初始為 0，隨著撮合進行而增加，當等於 quantity 時狀態轉為 FILLED
    @Column(name = "filled_quantity", precision = 36, scale = 18)
    private BigDecimal filledQuantity;

    // 累計成交總金額 (Cumulative Quote Quantity)
    // 用於計算成交均價 = cumQuoteQty / filledQuantity
    @Column(name = "cum_quote_qty", precision = 36, scale = 18)
    private BigDecimal cumQuoteQty = BigDecimal.ZERO;

    // 訂單狀態 (NEW, PARTIAL_FILLED, FILLED, CANCELED)
    @Column(length = 20, columnDefinition = "enum('new','partial_filled','filled','canceled') COLLATE utf8mb3_bin")
    private OrderStatus status;

    // Post-Only 標記 (僅做 Maker)
    // 若為 true，則該訂單若會立即成交 (吃單) 將被系統取消或調整，確保其為掛單提供流動性
    @Column(name = "post_only")
    private Boolean postOnly;

    // 訂單建立時間
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 訂單最後更新時間
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 交易模式 (SPOT: 現貨, CONTRACT: 合約)
    @Column(name = "trade_type", length = 10, columnDefinition = "enum('spot','contract') COLLATE utf8mb3_bin")
    private com.exchange.exchange.enums.TradeType tradeType;

    // JPA 規範要求的無參數建構子
    public Order() {
    }

    // --- Getters & Setters ---

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
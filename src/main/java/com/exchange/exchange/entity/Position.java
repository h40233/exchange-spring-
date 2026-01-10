package com.exchange.exchange.entity;

// 引入枚舉：倉位方向、狀態
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.exchange.exchange.enums.PositionSide;
import com.exchange.exchange.enums.PositionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Position 實體類別對應 `positions` 表。
// 紀錄使用者在合約交易中的持倉狀態。
// 關鍵欄位包含持倉均價 (avgprice) 與已實現盈虧 (pnl)。
@Entity
@Table(name = "positions")
public class Position {

    // 倉位 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "positionID")
    private Integer positionId;

    // 會員 ID
    @Column(name = "memberID")
    private Integer memberId;

    // 交易對 ID
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 持倉方向 (LONG 做多 / SHORT 做空)
    @Column(length = 10)
    private PositionSide side;

    // 持倉數量
    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    // 平均開倉價格 (Weighted Average Entry Price)
    // 每次加倉時會重新計算
    @Column(precision = 36, scale = 18)
    private BigDecimal avgprice;

    // 累計已實現盈虧 (Realized PnL)
    // 紀錄該倉位在部分平倉或完全平倉後產生的損益
    @Column(precision = 36, scale = 18)
    private BigDecimal pnl;

    // 倉位狀態 (OPEN / CLOSED)
    @Column(length = 10)
    private PositionStatus status;

    // 開倉時間
    @Column(name = "open_at")
    private LocalDateTime openAt;

    // 平倉時間
    @Column(name = "close_at")
    private LocalDateTime closeAt;

    public Position() {
    }

    // --- Getters & Setters ---

    public Integer getPositionId() {
        return positionId;
    }

    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
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

    public PositionSide getSide() {
        return side;
    }

    public void setSide(PositionSide side) {
        this.side = side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAvgprice() {
        return avgprice;
    }

    public void setAvgprice(BigDecimal avgprice) {
        this.avgprice = avgprice;
    }

    public BigDecimal getPnl() {
        return pnl;
    }

    public void setPnl(BigDecimal pnl) {
        this.pnl = pnl;
    }

    public PositionStatus getStatus() {
        return status;
    }

    public void setStatus(PositionStatus status) {
        this.status = status;
    }

    public LocalDateTime getOpenAt() {
        return openAt;
    }

    public void setOpenAt(LocalDateTime openAt) {
        this.openAt = openAt;
    }

    public LocalDateTime getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(LocalDateTime closeAt) {
        this.closeAt = closeAt;
    }
}
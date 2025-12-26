package com.exchange.exchange.entity;

import com.exchange.exchange.enums.PositionSide;
import com.exchange.exchange.enums.PositionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions")
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "positionID")
    private Integer positionId;

    @Column(name = "memberID")
    private Integer memberId;

    @Column(name = "symbolID", length = 45)
    private String symbolId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PositionSide side;

    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    @Column(precision = 36, scale = 18)
    private BigDecimal avgprice;

    @Column(precision = 36, scale = 18)
    private BigDecimal pnl;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PositionStatus status;

    @Column(name = "open_at")
    private LocalDateTime openAt;

    @Column(name = "close_at")
    private LocalDateTime closeAt;

    public Position() {
    }

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
package com.exchange.exchange.entity;

// 引入枚舉：倉位方向 (多/空)、狀態 (持倉/平倉)
import com.exchange.exchange.enums.PositionSide;
import com.exchange.exchange.enums.PositionStatus;
// 引入 JPA 與數學庫
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// Position 實體對應 `positions` 表。
// 用於記錄合約交易的持倉狀態。
// 關鍵概念：
// - avgprice (持倉均價)：每次加倉都會重新計算加權平均價。
// - pnl (已實現盈虧)：平倉時結算的獲利或虧損。
@Entity
@Table(name = "positions")
public class Position {

    // 倉位 ID (主鍵)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "positionID")
    private Integer positionId;

    // 所屬會員 ID
    @Column(name = "memberID")
    private Integer memberId;

    // 交易對 ID (如 BTCUSDT)
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 持倉方向 (LONG 做多 / SHORT 做空)
    @Column(length = 10)
    private PositionSide side;

    // 持倉數量
    @Column(precision = 36, scale = 18)
    private BigDecimal quantity;

    // 平均開倉價格 (Average Entry Price)
    // 計算公式：(舊量*舊價 + 新量*新價) / 總量
    @Column(precision = 36, scale = 18)
    private BigDecimal avgprice;

    // 已實現盈虧 (Realized PnL)
    // 注意：此欄位通常記錄「平倉後」落袋為安的金額，而非浮動盈虧
    @Column(precision = 36, scale = 18)
    private BigDecimal pnl;

    // 倉位狀態 (OPEN 持倉中 / CLOSED 已平倉)
    @Column(length = 10)
    private PositionStatus status;

    // 開倉時間
    @Column(name = "open_at")
    private LocalDateTime openAt;

    // 平倉時間 (完全平倉時更新)
    @Column(name = "close_at")
    private LocalDateTime closeAt;

    // 無參數建構子
    public Position() {
    }

    // 取得倉位 ID
    public Integer getPositionId() {
        return positionId;
    }

    // 設定倉位 ID
    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }

    // 取得會員 ID
    public Integer getMemberId() {
        return memberId;
    }

    // 設定會員 ID
    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    // 取得交易對
    public String getSymbolId() {
        return symbolId;
    }

    // 設定交易對
    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    // 取得方向
    public PositionSide getSide() {
        return side;
    }

    // 設定方向
    public void setSide(PositionSide side) {
        this.side = side;
    }

    // 取得數量
    public BigDecimal getQuantity() {
        return quantity;
    }

    // 設定數量
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    // 取得均價
    public BigDecimal getAvgprice() {
        return avgprice;
    }

    // 設定均價
    public void setAvgprice(BigDecimal avgprice) {
        this.avgprice = avgprice;
    }

    // 取得盈虧
    public BigDecimal getPnl() {
        return pnl;
    }

    // 設定盈虧
    public void setPnl(BigDecimal pnl) {
        this.pnl = pnl;
    }

    // 取得狀態
    public PositionStatus getStatus() {
        return status;
    }

    // 設定狀態
    public void setStatus(PositionStatus status) {
        this.status = status;
    }

    // 取得開倉時間
    public LocalDateTime getOpenAt() {
        return openAt;
    }

    // 設定開倉時間
    public void setOpenAt(LocalDateTime openAt) {
        this.openAt = openAt;
    }

    // 取得平倉時間
    public LocalDateTime getCloseAt() {
        return closeAt;
    }

    // 設定平倉時間
    public void setCloseAt(LocalDateTime closeAt) {
        this.closeAt = closeAt;
    }
}
// ====== 備註區 ======
/*
[註1] 保證金機制 (Margin):
      目前的 Entity 缺乏 `margin` (保證金) 與 `leverage` (槓桿倍數) 欄位。
      這意味著目前系統可能僅支援 1x 槓桿或全倉模式 (Cross Margin) 且未詳細記錄佔用保證金。
      若要實作強平 (Liquidation) 邏輯，必須追加這些欄位。
*/
package com.exchange.exchange.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ====== 檔案總結 ======
// WalletTransaction 實體對應 `wallet_transactions` 表。
// 紀錄每一筆錢包餘額變動的原因與金額，是系統稽核的重要依據。
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    // 流水紀錄 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 會員 ID
    @Column(name = "member_id")
    private Integer memberId;

    // 幣種 ID
    @Column(name = "coin_id")
    private String coinId;

    // 變動類型
    // 使用 String 而非 Enum，以允許更靈活的業務類型 (如 SPOT_BUY_COST, ADJUSTMENT 等)
    @Column(length = 50) 
    private String type; 

    // 變動金額 (正數為增加，負數為減少)
    @Column(precision = 36, scale = 18)
    private BigDecimal amount;

    // 發生時間 (預設為當前時間)
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // 無參數建構子
    public WalletTransaction() {}

    // 便捷建構子
    public WalletTransaction(Integer memberId, String coinId, String type, BigDecimal amount) {
        this.memberId = memberId;
        this.coinId = coinId;
        this.type = type;
        this.amount = amount;
    }

    // 取得流水 ID
    public Long getId() { return id; }
    
    // 設定流水 ID
    public void setId(Long id) { this.id = id; }
    
    // 取得會員 ID
    public Integer getMemberId() { return memberId; }
    
    // 設定會員 ID
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    
    // 取得幣種 ID
    public String getCoinId() { return coinId; }
    
    // 設定幣種 ID
    public void setCoinId(String coinId) { this.coinId = coinId; }
    
    // 取得變動類型
    public String getType() { return type; }
    
    // 設定變動類型
    public void setType(String type) { this.type = type; }
    
    // 取得變動金額
    public BigDecimal getAmount() { return amount; }
    
    // 設定變動金額
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    // 取得發生時間
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // 設定發生時間
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
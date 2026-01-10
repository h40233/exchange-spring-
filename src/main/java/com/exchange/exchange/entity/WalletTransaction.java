package com.exchange.exchange.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// WalletTransaction 實體類別對應 `wallet_transactions` 表。
// 用於紀錄所有錢包餘額變動的歷史流水 (Audit Log)。
// 每一筆資金的增加或減少都必須在此產生一條紀錄，以供對帳與查詢。
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    // 流水號 ID (Primary Key)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 關聯會員 ID
    @Column(name = "member_id")
    private Integer memberId;

    // 關聯幣種 ID
    @Column(name = "coin_id")
    private String coinId;

    // 交易類型描述 (如 "DEPOSIT", "SPOT_BUY_COST", "FEE")
    // 使用 String 保留擴充彈性
    @Column(length = 50) 
    private String type; 

    // 變動金額 (正數代表增加，負數代表減少)
    @Column(precision = 36, scale = 18)
    private BigDecimal amount;

    // 發生時間
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletTransaction() {}

    // 便捷建構子，方便程式碼中快速建立紀錄
    public WalletTransaction(Integer memberId, String coinId, String type, BigDecimal amount) {
        this.memberId = memberId;
        this.coinId = coinId;
        this.type = type;
        this.amount = amount;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    
    public String getCoinId() { return coinId; }
    public void setCoinId(String coinId) { this.coinId = coinId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
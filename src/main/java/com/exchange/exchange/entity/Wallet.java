package com.exchange.exchange.entity;

// 引入複合主鍵類別
import java.math.BigDecimal;

import com.exchange.exchange.entity.key.WalletId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Wallet 實體類別對應資料庫中的 `wallets` 表。
// 採用複合主鍵 (memberID + coinID) 來唯一識別一個錢包。
// 核心概念：
// - Balance: 總資產 (包含凍結與可用)。
// - Available: 可用資產 (下單時可被圈存的額度)。
// - Frozen (隱含): 凍結資產 = Balance - Available。
@Entity
@Table(name = "wallets")
@IdClass(WalletId.class) // 指定使用 WalletId 作為複合主鍵類別
public class Wallet {

    // 複合主鍵部分 1：會員 ID
    @Id
    @Column(name = "memberID")
    private Integer memberId;

    // 複合主鍵部分 2：幣種 ID (如 "BTC", "USDT")
    @Id
    @Column(name = "coinID", length = 45, columnDefinition = "varchar(45) COLLATE utf8mb3_bin")
    private String coinId;

    // 總餘額 (Total Balance)
    // 代表使用者擁有的該幣種總量，包含掛單中被鎖定的資金
    @Column(precision = 36, scale = 18)
    private BigDecimal balance;

    // 可用餘額 (Available Balance)
    // 代表使用者目前可以自由支配 (提現、下單) 的額度
    @Column(precision = 36, scale = 18)
    private BigDecimal available;

    // JPA 規範要求的無參數建構子
    public Wallet() {
    }

    // --- Getters & Setters ---

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getCoinId() {
        return coinId;
    }

    public void setCoinId(String coinId) {
        this.coinId = coinId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public void setAvailable(BigDecimal available) {
        this.available = available;
    }
}
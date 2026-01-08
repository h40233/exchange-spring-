package com.exchange.exchange.entity;

// 引入複合主鍵類別
import com.exchange.exchange.entity.key.WalletId;
// 引入 JPA 註解
import jakarta.persistence.*;
import java.math.BigDecimal;

// ====== 檔案總結 ======
// Wallet 實體對應 `wallets` 表。
// 使用 @IdClass 指定複合主鍵 (MemberID + CoinID)。
// 定義了使用者的資產狀態：Balance (總資產) 與 Available (可用資產)。
@Entity
@Table(name = "wallets")
@IdClass(WalletId.class)
public class Wallet {

    // 複合主鍵之一：會員 ID
    @Id
    @Column(name = "memberID")
    private Integer memberId;

    // 複合主鍵之二：幣種 ID
    @Id
    @Column(name = "coinID", length = 45, columnDefinition = "varchar(45) COLLATE utf8mb3_bin")
    private String coinId;

    // 總餘額 (Balance) = 可用餘額 (Available) + 凍結餘額 (Frozen)
    @Column(precision = 36, scale = 18)
    private BigDecimal balance;

    // 可用餘額 (Available)
    // 這是下單時能被圈存的最大額度
    @Column(precision = 36, scale = 18)
    private BigDecimal available;

    // 無參數建構子
    public Wallet() {
    }

    // 取得會員 ID
    public Integer getMemberId() {
        return memberId;
    }

    // 設定會員 ID
    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    // 取得幣種 ID
    public String getCoinId() {
        return coinId;
    }

    // 設定幣種 ID
    public void setCoinId(String coinId) {
        this.coinId = coinId;
    }

    // 取得總餘額
    public BigDecimal getBalance() {
        return balance;
    }

    // 設定總餘額
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    // 取得可用餘額
    public BigDecimal getAvailable() {
        return available;
    }

    // 設定可用餘額
    public void setAvailable(BigDecimal available) {
        this.available = available;
    }
}
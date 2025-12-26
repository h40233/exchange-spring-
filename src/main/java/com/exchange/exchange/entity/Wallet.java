package com.exchange.exchange.entity;

import com.exchange.exchange.entity.key.WalletId;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@IdClass(WalletId.class)
public class Wallet {
    @Id
    @Column(name = "memberID")
    private Integer memberId;

    @Id
    @Column(name = "coinID", length = 45, columnDefinition = "varchar(45) COLLATE utf8mb3_bin")
    private String coinId;

    @Column(precision = 36, scale = 18)
    private BigDecimal balance;

    @Column(precision = 36, scale = 18)
    private BigDecimal available;

    public Wallet() {
    }

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
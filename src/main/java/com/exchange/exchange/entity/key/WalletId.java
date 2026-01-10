package com.exchange.exchange.entity.key;

import java.io.Serializable;
import java.util.Objects;

// ====== 檔案總結 ======
// WalletId 類別定義了 `Wallet` 實體的複合主鍵規則。
// 根據 JPA 規範，複合主鍵類別必須：
// 1. 實作 Serializable 介面。
// 2. 擁有無參數建構子。
// 3. 覆寫 equals() 與 hashCode() 方法以確保物件唯一性判斷正確。
public class WalletId implements Serializable {

    // 對應 Wallet.memberId
    private Integer memberId;

    // 對應 Wallet.coinId
    private String coinId;

    public WalletId() {
    }

    public WalletId(Integer memberId, String coinId) {
        this.memberId = memberId;
        this.coinId = coinId;
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

    // 覆寫 equals：比較兩個 WalletId 是否代表同一筆資料
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletId walletId = (WalletId) o;
        return Objects.equals(memberId, walletId.memberId) && Objects.equals(coinId, walletId.coinId);
    }

    // 覆寫 hashCode：生成雜湊值，用於 Map/Set 儲存
    @Override
    public int hashCode() {
        return Objects.hash(memberId, coinId);
    }
}
package com.exchange.exchange.entity.key;

import java.io.Serializable;
import java.util.Objects;

public class WalletId implements Serializable {
    private Integer memberId;
    private String coinId;

    public WalletId() {
    }

    public WalletId(Integer memberId, String coinId) {
        this.memberId = memberId;
        this.coinId = coinId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletId walletId = (WalletId) o;
        return Objects.equals(memberId, walletId.memberId) && Objects.equals(coinId, walletId.coinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, coinId);
    }
}
package com.exchange.exchange.entity.key;

// 引入序列化介面與物件工具
import java.io.Serializable;
import java.util.Objects;

// ====== 檔案總結 ======
// WalletId 用於定義 `Wallet` 實體的複合主鍵 (Composite Key)。
// 根據 JPA 規範，當一個 Entity 有多個 @Id 欄位時，必須定義一個獨立的 ID 類別。
// 唯一識別一個錢包的條件是：特定的會員 (memberId) + 特定的幣種 (coinId)。
public class WalletId implements Serializable {

    // 對應 Wallet 實體中的 memberId 欄位
    private Integer memberId;

    // 對應 Wallet 實體中的 coinId 欄位
    private String coinId;

    // 無參數建構子 (序列化所需)
    public WalletId() {
    }

    // 全參數建構子
    public WalletId(Integer memberId, String coinId) {
        this.memberId = memberId;
        this.coinId = coinId;
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

    // 覆寫 equals 方法
    // 用於 JPA 在 Context 中比較兩個物件是否代表同一筆資料
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletId walletId = (WalletId) o;
        return Objects.equals(memberId, walletId.memberId) && Objects.equals(coinId, walletId.coinId);
    }

    // 覆寫 hashCode 方法
    // 用於雜湊結構 (如 HashMap, HashSet) 中快速定位物件
    @Override
    public int hashCode() {
        return Objects.hash(memberId, coinId);
    }
}
package com.exchange.exchange.entity;

// 引入 JPA 註解
import jakarta.persistence.*;

// ====== 檔案總結 ======
// Coin 實體對應資料庫中的 `coins` 表。
// 用於定義交易所支援的基礎貨幣單位 (如 BTC, ETH, USDT)。
// 關鍵欄位：
// - decimals: 精度設定，決定該幣種在前端顯示或計算時的小數位數 (雖然後端統一用 BigDecimal，但前端顯示需參考此值)。
@Entity
@Table(name = "coins")
public class Coin {

    // 幣種 ID (主鍵，如 "BTC", "USDT")
    // 設定為 varchar(45) 並指定定序 (Collation) 以支援區分大小寫的查詢 (雖然通常幣種代碼是大寫)
    @Id
    @Column(name = "coinID", length = 45, columnDefinition = "varchar(45) COLLATE utf8mb3_bin")
    private String coinId;

    // 幣種全名 (如 "Bitcoin", "Tether")
    @Column(length = 45)
    private String name;

    // 小數點精度 (Decimals)
    // 例如 USDT 通常為 6 或 18，BTC 為 8
    private Float decimals;

    // 無參數建構子 (JPA 規範)
    public Coin() {
    }

    // 取得幣種 ID
    public String getCoinId() {
        return coinId;
    }

    // 設定幣種 ID
    public void setCoinId(String coinId) {
        this.coinId = coinId;
    }

    // 取得幣種全名
    public String getName() {
        return name;
    }

    // 設定幣種全名
    public void setName(String name) {
        this.name = name;
    }

    // 取得精度
    public Float getDecimals() {
        return decimals;
    }

    // 設定精度
    public void setDecimals(Float decimals) {
        this.decimals = decimals;
    }
}
// ====== 備註區 ======
/*
[註1] 精度類型 (Data Type):
      使用 `Float` 來儲存精度 (`decimals`) 可能會有潛在風險，雖然通常精度是整數 (如 8, 18)。
      建議改用 `Integer` 以避免浮點數比較的問題。
*/
package com.exchange.exchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Coin 實體類別對應 `coins` 表。
// 定義系統支援的虛擬貨幣種類 (如 BTC, ETH)。
// 包含精度 (decimals) 設定，用於前端顯示格式化。
@Entity
@Table(name = "coins")
public class Coin {

    // 幣種 ID (如 "BTC")，作為 Primary Key
    @Id
    @Column(name = "coinID", length = 45, columnDefinition = "varchar(45) COLLATE utf8mb3_bin")
    private String coinId;

    // 幣種全名 (如 "Bitcoin")
    @Column(length = 45)
    private String name;

    // 顯示精度 (小數位數)
    // 注意：後端計算統一使用 BigDecimal，此數值主要供前端 UI 決定顯示幾位小數
    private Float decimals;

    public Coin() {
    }

    // --- Getters & Setters ---

    public String getCoinId() {
        return coinId;
    }

    public void setCoinId(String coinId) {
        this.coinId = coinId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getDecimals() {
        return decimals;
    }

    public void setDecimals(Float decimals) {
        this.decimals = decimals;
    }
}
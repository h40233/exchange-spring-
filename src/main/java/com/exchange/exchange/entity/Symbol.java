package com.exchange.exchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ====== 檔案總結 ======
// Symbol 實體類別對應 `symbols` 表。
// 定義交易所支援的交易對 (Trading Pair)，例如 BTC/USDT。
// 關聯兩個幣種：基礎幣 (Base Coin) 與 報價幣 (Quote Coin)。
@Entity
@Table(name = "symbols")
public class Symbol {

    // 交易對 ID (例如 "BTCUSDT")，手動指定
    @Id
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 前端顯示名稱 (例如 "BTC/USDT")
    @Column(length = 45)
    private String name;

    // 基礎幣種 ID (被交易的資產，如 BTC)
    @Column(name = "base_coinID", length = 45)
    private String baseCoinId;

    // 報價幣種 ID (計價單位，如 USDT)
    @Column(name = "quote_coinID", length = 45)
    private String quoteCoinId;

    public Symbol() {
    }

    // --- Getters & Setters ---

    public String getSymbolId() {
        return symbolId;
    }

    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseCoinId() {
        return baseCoinId;
    }

    public void setBaseCoinId(String baseCoinId) {
        this.baseCoinId = baseCoinId;
    }

    public String getQuoteCoinId() {
        return quoteCoinId;
    }

    public void setQuoteCoinId(String quoteCoinId) {
        this.quoteCoinId = quoteCoinId;
    }
}
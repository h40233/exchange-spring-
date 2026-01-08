package com.exchange.exchange.entity;

import jakarta.persistence.*;

// ====== 檔案總結 ======
// Symbol 實體對應 `symbols` 表。
// 定義基礎幣 (Base Coin) 與報價幣 (Quote Coin) 的對應關係。
@Entity
@Table(name = "symbols")
public class Symbol {

    // 交易對 ID (手動指定，如 BTCUSDT)
    @Id
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    // 顯示名稱 (如 BTC/USDT)
    @Column(length = 45)
    private String name;

    // 基礎幣 ID (交易標的)
    @Column(name = "base_coinID", length = 45)
    private String baseCoinId;

    // 報價幣 ID (計價單位)
    @Column(name = "quote_coinID", length = 45)
    private String quoteCoinId;

    // 無參數建構子
    public Symbol() {
    }

    // 取得交易對 ID
    public String getSymbolId() {
        return symbolId;
    }

    // 設定交易對 ID
    public void setSymbolId(String symbolId) {
        this.symbolId = symbolId;
    }

    // 取得顯示名稱
    public String getName() {
        return name;
    }

    // 設定顯示名稱
    public void setName(String name) {
        this.name = name;
    }

    // 取得基礎幣 ID
    public String getBaseCoinId() {
        return baseCoinId;
    }

    // 設定基礎幣 ID
    public void setBaseCoinId(String baseCoinId) {
        this.baseCoinId = baseCoinId;
    }

    // 取得報價幣 ID
    public String getQuoteCoinId() {
        return quoteCoinId;
    }

    // 設定報價幣 ID
    public void setQuoteCoinId(String quoteCoinId) {
        this.quoteCoinId = quoteCoinId;
    }
}
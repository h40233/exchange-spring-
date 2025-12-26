package com.exchange.exchange.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "symbols")
public class Symbol {
    @Id
    @Column(name = "symbolID", length = 45)
    private String symbolId;

    @Column(length = 45)
    private String name;

    @Column(name = "base_coinID", length = 45)
    private String baseCoinId;

    @Column(name = "quote_coinID", length = 45)
    private String quoteCoinId;

    public Symbol() {
    }

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
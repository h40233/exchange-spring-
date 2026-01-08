package com.exchange.exchange.dto;

import java.math.BigDecimal;
import java.util.List;

// ====== 檔案總結 ======
// OrderBookDTO 用於傳輸訂單簿 (Order Book) 的深度資料。
// 包含買盤 (bids) 與賣盤 (asks) 兩個列表。
public class OrderBookDTO {
    private List<Entry> bids;
    private List<Entry> asks;

    // 建構子
    public OrderBookDTO(List<Entry> bids, List<Entry> asks) {
        this.bids = bids;
        this.asks = asks;
    }

    // 取得買盤列表
    public List<Entry> getBids() {
        return bids;
    }

    // 設定買盤列表
    public void setBids(List<Entry> bids) {
        this.bids = bids;
    }

    // 取得賣盤列表
    public List<Entry> getAsks() {
        return asks;
    }

    // 設定賣盤列表
    public void setAsks(List<Entry> asks) {
        this.asks = asks;
    }

    // 靜態內部類別：代表訂單簿中的一個價格檔位
    public static class Entry {
        private BigDecimal price;
        private BigDecimal quantity;

        // 建構子
        public Entry(BigDecimal price, BigDecimal quantity) {
            this.price = price;
            this.quantity = quantity;
        }

        // 取得價格
        public BigDecimal getPrice() {
            return price;
        }

        // 設定價格
        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        // 取得數量
        public BigDecimal getQuantity() {
            return quantity;
        }

        // 設定數量
        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
    }
}
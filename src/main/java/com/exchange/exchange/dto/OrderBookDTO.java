package com.exchange.exchange.dto;

import java.math.BigDecimal;
import java.util.List;

// ====== 檔案總結 ======
// OrderBookDTO 用於傳輸訂單簿 (Order Book) 的深度資料給前端。
// 結構包含兩個列表：買盤 (Bids) 與 賣盤 (Asks)。
// 每個列表中的元素代表一個價格檔位的聚合數據。
public class OrderBookDTO {
    
    // 買單列表 (通常按價格由高到低排序)
    private List<Entry> bids;
    
    // 賣單列表 (通常按價格由低到高排序)
    private List<Entry> asks;

    public OrderBookDTO(List<Entry> bids, List<Entry> asks) {
        this.bids = bids;
        this.asks = asks;
    }

    public List<Entry> getBids() {
        return bids;
    }

    public void setBids(List<Entry> bids) {
        this.bids = bids;
    }

    public List<Entry> getAsks() {
        return asks;
    }

    public void setAsks(List<Entry> asks) {
        this.asks = asks;
    }

    // 靜態內部類別：代表訂單簿中的單一條目 (Price Level)
    public static class Entry {
        // 價格檔位
        private BigDecimal price;
        // 該檔位總掛單數量
        private BigDecimal quantity;

        public Entry(BigDecimal price, BigDecimal quantity) {
            this.price = price;
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
    }
}
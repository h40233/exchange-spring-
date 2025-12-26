package com.exchange.exchange.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderBookDTO {
    private List<Entry> bids;
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

    public static class Entry {
        private BigDecimal price;
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

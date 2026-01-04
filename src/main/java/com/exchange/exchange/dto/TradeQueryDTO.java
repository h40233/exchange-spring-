package com.exchange.exchange.dto;

import com.exchange.exchange.entity.Trade;

public class TradeQueryDTO {
    private Trade trade;
    private Boolean isTakerMine;
    private Boolean isMakerMine;

    public TradeQueryDTO(Trade trade, Boolean isTakerMine, Boolean isMakerMine) {
        this.trade = trade;
        this.isTakerMine = isTakerMine;
        this.isMakerMine = isMakerMine;
    }

    public Trade getTrade() { return trade; }
    public Boolean getIsTakerMine() { return isTakerMine; }
    public Boolean getIsMakerMine() { return isMakerMine; }
}

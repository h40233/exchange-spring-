package com.exchange.exchange.dto;

import com.exchange.exchange.entity.Trade;

// ====== 檔案總結 ======
// TradeQueryDTO 是一個用於接收 JPA 查詢結果的 DTO。
// 它不僅包含成交紀錄 (Trade)，還包含兩個布林值旗標，
// 用於告訴上層調用者：當前使用者在這筆成交中是 Maker 還是 Taker。
public class TradeQueryDTO {
    
    // 原始成交紀錄實體
    private Trade trade;
    
    // 旗標：使用者是否為 Taker
    private Boolean isTakerMine;
    
    // 旗標：使用者是否為 Maker
    private Boolean isMakerMine;

    // 建構子 (必須匹配 JPQL 中的 new 表達式)
    public TradeQueryDTO(Trade trade, Boolean isTakerMine, Boolean isMakerMine) {
        this.trade = trade;
        this.isTakerMine = isTakerMine;
        this.isMakerMine = isMakerMine;
    }

    // --- Getters---

    public Trade getTrade() {
        return trade;
    }

    public Boolean getIsTakerMine() {
        return isTakerMine;
    }

    public Boolean getIsMakerMine() {
        return isMakerMine;
    }

}
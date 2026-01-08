package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// TradeType 定義交易的模式。
// SPOT: 現貨交易 (錢貨兩訖，不開槓桿)。
// CONTRACT: 合約交易 (保證金交易，可開槓桿，有爆倉風險)。
public enum TradeType {
    SPOT,
    CONTRACT
}
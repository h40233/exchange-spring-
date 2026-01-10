package com.exchange.exchange.enums;

// ====== 檔案總結 ======
// TradeType 枚舉定義了交易的商業模式。
// SPOT: 現貨交易 (Spot Trading)。
//       - 錢貨兩訖，不使用槓桿，風險較低。
// CONTRACT: 合約交易 (Contract/Futures Trading)。
//           - 保證金交易，允許開槓桿 (Leverage)，具備爆倉風險。
public enum TradeType {
    SPOT,
    CONTRACT
}
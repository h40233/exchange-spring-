package com.exchange.exchange.dto;

// 引入實體
import com.exchange.exchange.entity.Trade;

// ====== 檔案總結 ======
// TradeQueryDTO 是一個中間層資料傳輸物件。
// 用途：在 Repository 層執行複雜查詢時，將 `Trade` 實體與「動態計算出的布林標記」封裝在一起。
// 解決的問題：在查詢「我的成交紀錄」時，一筆成交紀錄可能「我是買方(Taker)」也可能「我是賣方(Maker)」，
// 此 DTO 用於在 SQL 查詢結果中直接標記出我是哪一方。
public class TradeQueryDTO {

    // 原始成交紀錄實體
    private Trade trade;

    // 標記：當前查詢的使用者是否為該筆交易的 Taker (主動方)
    private Boolean isTakerMine;

    // 標記：當前查詢的使用者是否為該筆交易的 Maker (被動方)
    private Boolean isMakerMine;

    // 全參數建構子 (供 JPQL 查詢使用：SELECT new com...TradeQueryDTO(...) FROM ...)
    public TradeQueryDTO(Trade trade, Boolean isTakerMine, Boolean isMakerMine) {
        this.trade = trade;
        this.isTakerMine = isTakerMine;
        this.isMakerMine = isMakerMine;
    }

    // 取得成交紀錄
    public Trade getTrade() { return trade; }

    // 判斷是否為我的 Taker 單
    public Boolean getIsTakerMine() { return isTakerMine; }

    // 判斷是否為我的 Maker 單
    public Boolean getIsMakerMine() { return isMakerMine; }
}
package com.exchange.exchange.repository;

// 引入實體
import com.exchange.exchange.entity.Trade;
// 引入 Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ====== 檔案總結 ======
// TradeRepository 負責成交紀錄的資料存取。
// 除了基本的 CRUD，還包含了複雜的歷史紀錄查詢，需要判斷使用者在交易中是 Taker 還是 Maker。
@Repository
public interface TradeRepository extends JpaRepository<Trade, Integer> {
    
    // 查詢特定交易對的「最新一筆」成交，用於行情價格顯示 (Ticker)
    Optional<Trade> findTopBySymbolIdOrderByTradeIdDesc(String symbolId);

    // 查詢某會員參與的所有成交紀錄 (無論是 Taker 還是 Maker)
    // JPQL 解析：找出 (Taker訂單屬於該會員) 或 (Maker訂單屬於該會員) 的成交紀錄
    @Query("SELECT t FROM Trade t WHERE " +
           "t.takerOrderId IN (SELECT o.orderId FROM Order o WHERE o.memberId = :memberId) " +
           "OR " +
           "t.makerOrderId IN (SELECT o.orderId FROM Order o WHERE o.memberId = :memberId) " +
           "ORDER BY t.executedAt DESC")
    List<Trade> findTradesByMemberId(@Param("memberId") Integer memberId);

    // 進階查詢：將成交紀錄包裝為 DTO，並標記使用者角色
    // 這是一個非常巧妙的查詢，它在查詢結果中直接計算出布林值旗標。
    // 建構子表達式：new TradeQueryDTO(Trade物件, 是否為我的Taker單, 是否為我的Maker單)
    @Query("SELECT new com.exchange.exchange.dto.TradeQueryDTO(t, " +
           "(CASE WHEN oT.memberId = :memberId THEN true ELSE false END), " +
           "(CASE WHEN oM.memberId = :memberId THEN true ELSE false END)) " +
           "FROM Trade t " +
           "LEFT JOIN Order oT ON t.takerOrderId = oT.orderId " + // 關聯 Taker 訂單表
           "LEFT JOIN Order oM ON t.makerOrderId = oM.orderId " + // 關聯 Maker 訂單表
           "WHERE oT.memberId = :memberId OR oM.memberId = :memberId " + // 篩選條件
           "ORDER BY t.executedAt DESC")
    List<com.exchange.exchange.dto.TradeQueryDTO> findTradeHistory(@Param("memberId") Integer memberId);
}

// ====== 備註區 ======
/*
[註1] 效能優化 (Performance):
      `findTradesByMemberId` 使用了 `IN (SELECT ...)` 子查詢。
      當 Orders 表數據量巨大時，這種子查詢效率可能較低。
      `findTradeHistory` 使用 `JOIN` 的方式通常效能較好，且能一次帶出更多資訊，推薦優先使用 JOIN 寫法。

[註2] 索引建議 (Indexing):
      為了支援上述查詢，建議在資料庫層面建立以下索引：
      1. `trades(symbolID, tradesID DESC)` -> 用於 Ticker 查詢。
      2. `trades(taker_orderID)` 與 `trades(maker_orderID)` -> 用於 JOIN。
      3. `orders(memberID)` -> 用於篩選使用者訂單。
*/
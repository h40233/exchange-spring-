package com.exchange.exchange.repository;

// 引入 DTO：訂單簿項目 (價格, 數量)
import com.exchange.exchange.dto.OrderBookDTO;
// 引入實體與枚舉
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
// 引入 Spring Data JPA
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

// ====== 檔案總結 ======
// OrderRepository 負責所有與訂單相關的資料庫操作。
// 核心功能：
// 1. 撮合查詢：找出價格符合條件的對手單。
// 2. 訂單簿查詢：聚合計算每個價格檔位的總掛單量。
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    // 查詢某會員的歷史訂單 (依時間倒序)
    List<Order> findByMemberIdOrderByCreatedAtDesc(Integer memberId);

    // === 撮合核心邏輯 (Matching Engine Queries) ===

    // 尋找「賣單」來匹配使用者的「買單」
    // 條件：
    // 1. 同交易對、同類型、狀態為活躍 (NEW/PARTIAL)
    // 2. 賣單價格 <= 我的買單限價 (買方想買便宜) [註1]
    // 排序：
    // 1. 價格由低到高 (優先吃最便宜的賣單)
    // 2. 時間由早到晚 (同價格先到先得 - FIFO)
    @Query("SELECT o FROM Order o WHERE o.symbolId = :symbolId AND o.side = :side " +
           "AND o.tradeType = :tradeType " +
           "AND o.status IN :statuses " +
           "AND o.price <= :priceLimit " +
           "ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> findMatchingSellOrders(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, BigDecimal priceLimit);

    // 尋找「買單」來匹配使用者的「賣單」
    // 條件：
    // 1. 同交易對、同類型、狀態為活躍
    // 2. 買單價格 >= 我的賣單限價 (賣方想賣貴)
    // 排序：
    // 1. 價格由高到低 (優先吃最貴的買單)
    // 2. 時間由早到晚 (FIFO)
    @Query("SELECT o FROM Order o WHERE o.symbolId = :symbolId AND o.side = :side " +
           "AND o.tradeType = :tradeType " +
           "AND o.status IN :statuses " +
           "AND o.price >= :priceLimit " +
           "ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> findMatchingBuyOrders(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, BigDecimal priceLimit);

    // === 訂單簿聚合邏輯 (Order Book Aggregation) ===

    // 查詢買盤 (Bids) - 綠色
    // 邏輯：將相同價格的訂單分組 (GROUP BY price)，並加總剩餘數量 (quantity - filledQuantity)
    // 排序：價格由高到低 (買盤上方顯示最高價)
    // 使用 DTO 投影 (Projection) 直接回傳輕量物件
    @Query("SELECT new com.exchange.exchange.dto.OrderBookDTO$Entry(o.price, SUM(o.quantity - o.filledQuantity)) " +
           "FROM Order o " +
           "WHERE o.symbolId = :symbolId AND o.side = :side AND o.tradeType = :tradeType AND o.status IN :statuses " +
           "GROUP BY o.price " +
           "ORDER BY o.price DESC")
    List<OrderBookDTO.Entry> findOrderBookBids(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, Pageable pageable);

    // 查詢賣盤 (Asks) - 紅色
    // 邏輯：同上，加總剩餘數量
    // 排序：價格由低到高 (賣盤下方顯示最低價)
    @Query("SELECT new com.exchange.exchange.dto.OrderBookDTO$Entry(o.price, SUM(o.quantity - o.filledQuantity)) " +
           "FROM Order o " +
           "WHERE o.symbolId = :symbolId AND o.side = :side AND o.tradeType = :tradeType AND o.status IN :statuses " +
           "GROUP BY o.price " +
           "ORDER BY o.price ASC")
    List<OrderBookDTO.Entry> findOrderBookAsks(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, Pageable pageable);
}

// ====== 備註區 ======
/*
[註1] 撮合效能 (Matching Performance):
      使用資料庫進行撮合 (In-Database Matching) 在資料量大時會遇到鎖競爭與 IO 瓶頸。
      專業交易所通常使用「記憶體撮合引擎」(In-Memory Matching Engine)，如 LMAX Disruptor 架構。
      但在教學專案中，使用 SQL 排序能最直觀地展示「價格優先、時間優先」的原則。

[註2] 索引優化 (Index Optimization):
      為了支援上述查詢，資料庫必須建立複合索引：
      `orders(symbolID, side, status, price, created_at)`
      若無此索引，每次撮合都會全表掃描，系統會瞬間卡死。
*/
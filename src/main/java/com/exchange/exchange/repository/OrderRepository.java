package com.exchange.exchange.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.dto.OrderBookDTO;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;

// ====== 檔案總結 ======
// OrderRepository 負責與資料庫中的 `orders` 表進行交互。
// 核心功能：
// 1. 撮合查詢：提供「買單找賣單」與「賣單找買單」的查詢邏輯。
// 2. 訂單簿聚合：提供計算買賣盤深度 (Group by Price) 的查詢。
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    // 查詢特定會員的歷史訂單，按建立時間倒序
    List<Order> findByMemberIdOrderByCreatedAtDesc(Integer memberId);

    // === 撮合核心邏輯 (Matching Engine Queries) ===

    // 尋找「賣單」來匹配使用者的「買單」 (Buy Taker -> Sell Makers)
    // 條件：同幣對、同類型、狀態活躍、且賣價 <= 我的買價 (願意低賣)
    // 排序：價格由低到高 (最便宜的優先吃)，時間由早到晚 (先到先得)
    @Query("SELECT o FROM Order o WHERE o.symbolId = :symbolId AND o.side = :side " +
           "AND o.tradeType = :tradeType " +
           "AND o.status IN :statuses " +
           "AND o.price <= :priceLimit " +
           "AND o.memberId <> :takerMemberId " +
           "ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> findMatchingSellOrders(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, BigDecimal priceLimit, Integer takerMemberId);

    // 尋找「買單」來匹配使用者的「賣單」 (Sell Taker -> Buy Makers)
    // 條件：同幣對、同類型、狀態活躍、且買價 >= 我的賣價 (願意高買)
    // 排序：價格由高到低 (最貴的優先吃)，時間由早到晚
    @Query("SELECT o FROM Order o WHERE o.symbolId = :symbolId AND o.side = :side " +
           "AND o.tradeType = :tradeType " +
           "AND o.status IN :statuses " +
           "AND o.price >= :priceLimit " +
           "AND o.memberId <> :takerMemberId " +
           "ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> findMatchingBuyOrders(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, BigDecimal priceLimit, Integer takerMemberId);

    // === 訂單簿聚合邏輯 (Order Book Aggregation) ===

    // 查詢買盤 (Bids)
    // 邏輯：將活躍買單按價格分組，加總剩餘數量，按價格降序排列 (高價在前)
    @Query("SELECT new com.exchange.exchange.dto.OrderBookDTO$Entry(o.price, SUM(o.quantity - o.filledQuantity)) " +
           "FROM Order o " +
           "WHERE o.symbolId = :symbolId AND o.side = :side AND o.tradeType = :tradeType AND o.status IN :statuses " +
           "GROUP BY o.price " +
           "ORDER BY o.price DESC")
    List<OrderBookDTO.Entry> findOrderBookBids(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, Pageable pageable);

    // 查詢賣盤 (Asks)
    // 邏輯：將活躍賣單按價格分組，加總剩餘數量，按價格升序排列 (低價在前)
    @Query("SELECT new com.exchange.exchange.dto.OrderBookDTO$Entry(o.price, SUM(o.quantity - o.filledQuantity)) " +
           "FROM Order o " +
           "WHERE o.symbolId = :symbolId AND o.side = :side AND o.tradeType = :tradeType AND o.status IN :statuses " +
           "GROUP BY o.price " +
           "ORDER BY o.price ASC")
    List<OrderBookDTO.Entry> findOrderBookAsks(String symbolId, OrderSide side, com.exchange.exchange.enums.TradeType tradeType, List<OrderStatus> statuses, Pageable pageable);
}
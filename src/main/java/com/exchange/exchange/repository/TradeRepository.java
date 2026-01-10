package com.exchange.exchange.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.entity.Trade;

// ====== 檔案總結 ======
// TradeRepository 負責對 `trades` 表進行查詢。
// 包含 Ticker 價格查詢以及複雜的使用者成交歷史查詢。
@Repository
public interface TradeRepository extends JpaRepository<Trade, Integer> {
    
    // 獲取某個交易對的最新一筆成交 (用於顯示當前市價)
    Optional<Trade> findTopBySymbolIdOrderByTradeIdDesc(String symbolId);

    // 查詢某會員的成交紀錄 (簡單查詢)
    // 邏輯：只要該會員是 Taker 或 Maker 之一，即視為相關紀錄
    @Query("SELECT t FROM Trade t WHERE " +
           "t.takerOrderId IN (SELECT o.orderId FROM Order o WHERE o.memberId = :memberId) " +
           "OR " +
           "t.makerOrderId IN (SELECT o.orderId FROM Order o WHERE o.memberId = :memberId) " +
           "ORDER BY t.executedAt DESC")
    List<Trade> findTradesByMemberId(@Param("memberId") Integer memberId);

    // 查詢某會員的成交紀錄 (進階 DTO 投影)
    // 邏輯：同時查詢出該會員在該筆成交中是 "Taker" 還是 "Maker"，並封裝進 TradeQueryDTO
    @Query("SELECT new com.exchange.exchange.dto.TradeQueryDTO(t, " +
           "(CASE WHEN oT.memberId = :memberId THEN true ELSE false END), " +
           "(CASE WHEN oM.memberId = :memberId THEN true ELSE false END)) " +
           "FROM Trade t " +
           "LEFT JOIN Order oT ON t.takerOrderId = oT.orderId " +
           "LEFT JOIN Order oM ON t.makerOrderId = oM.orderId " +
           "WHERE oT.memberId = :memberId OR oM.memberId = :memberId " +
           "ORDER BY t.executedAt DESC")
    List<com.exchange.exchange.dto.TradeQueryDTO> findTradeHistory(@Param("memberId") Integer memberId);
}
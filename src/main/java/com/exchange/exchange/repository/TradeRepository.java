package com.exchange.exchange.repository;

import com.exchange.exchange.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Integer> {
    Optional<Trade> findTopBySymbolIdOrderByTradeIdDesc(String symbolId);

    @Query("SELECT t FROM Trade t WHERE " +
           "t.takerOrderId IN (SELECT o.orderId FROM Order o WHERE o.memberId = :memberId) " +
           "OR " +
           "t.makerOrderId IN (SELECT o.orderId FROM Order o WHERE o.memberId = :memberId) " +
           "ORDER BY t.executedAt DESC")
    List<Trade> findTradesByMemberId(@Param("memberId") Integer memberId);

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

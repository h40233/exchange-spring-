package com.exchange.exchange.repository;

import com.exchange.exchange.dto.OrderBookDTO;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByMemberIdOrderByCreatedAtDesc(Integer memberId);

    // Find SELL orders that match a BUY order (SellPrice <= BuyLimitPrice)
    @Query("SELECT o FROM Order o WHERE o.symbolId = :symbolId AND o.side = :side " +
           "AND o.status IN :statuses " +
           "AND o.price <= :priceLimit " +
           "ORDER BY o.price ASC, o.createdAt ASC")
    List<Order> findMatchingSellOrders(String symbolId, OrderSide side, List<OrderStatus> statuses, BigDecimal priceLimit);

    // Find BUY orders that match a SELL order (BuyPrice >= SellLimitPrice)
    @Query("SELECT o FROM Order o WHERE o.symbolId = :symbolId AND o.side = :side " +
           "AND o.status IN :statuses " +
           "AND o.price >= :priceLimit " +
           "ORDER BY o.price DESC, o.createdAt ASC")
    List<Order> findMatchingBuyOrders(String symbolId, OrderSide side, List<OrderStatus> statuses, BigDecimal priceLimit);

    // Order Book Queries
    @Query("SELECT new com.exchange.exchange.dto.OrderBookDTO$Entry(o.price, SUM(o.quantity - o.filledQuantity)) " +
           "FROM Order o " +
           "WHERE o.symbolId = :symbolId AND o.side = :side AND o.status IN :statuses " +
           "GROUP BY o.price " +
           "ORDER BY o.price DESC")
    List<OrderBookDTO.Entry> findOrderBookBids(String symbolId, OrderSide side, List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT new com.exchange.exchange.dto.OrderBookDTO$Entry(o.price, SUM(o.quantity - o.filledQuantity)) " +
           "FROM Order o " +
           "WHERE o.symbolId = :symbolId AND o.side = :side AND o.status IN :statuses " +
           "GROUP BY o.price " +
           "ORDER BY o.price ASC")
    List<OrderBookDTO.Entry> findOrderBookAsks(String symbolId, OrderSide side, List<OrderStatus> statuses, Pageable pageable);
}

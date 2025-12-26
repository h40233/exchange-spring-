package com.exchange.exchange.service;

import com.exchange.exchange.entity.Order;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.entity.Trade;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.repository.OrderRepository;
import com.exchange.exchange.repository.SymbolRepository;
import com.exchange.exchange.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MatchingService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private WalletService walletService;
    
    @Autowired
    private SymbolRepository symbolRepository;

    @Transactional
    public void matchOrder(Order takerOrder) {
        if (takerOrder.getStatus() == OrderStatus.FILLED || takerOrder.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED);
        List<Order> matchingOrders;

        // 1. Find Matching Orders
        if (takerOrder.getSide() == OrderSide.BUY) {
            // Taker BUY -> Look for SELLs (Price <= TakerPrice)
            matchingOrders = orderRepository.findMatchingSellOrders(
                    takerOrder.getSymbolId(), OrderSide.SELL, activeStatuses, takerOrder.getPrice());
        } else {
            // Taker SELL -> Look for BUYs (Price >= TakerPrice)
            matchingOrders = orderRepository.findMatchingBuyOrders(
                    takerOrder.getSymbolId(), OrderSide.BUY, activeStatuses, takerOrder.getPrice());
        }

        if (matchingOrders.isEmpty()) return;

        // 2. Iterate and Match
        Symbol symbol = symbolRepository.findById(takerOrder.getSymbolId()).orElseThrow();
        
        for (Order makerOrder : matchingOrders) {
            if (takerOrder.getStatus() == OrderStatus.FILLED) break;

            // Calculate Match Quantity
            BigDecimal takerRemaining = takerOrder.getQuantity().subtract(takerOrder.getFilledQuantity());
            BigDecimal makerRemaining = makerOrder.getQuantity().subtract(makerOrder.getFilledQuantity());
            BigDecimal matchQty = takerRemaining.min(makerRemaining);

            if (matchQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Match Price is always MAKER's price
            BigDecimal matchPrice = makerOrder.getPrice();

            // 3. Create Trade
            Trade trade = new Trade();
            trade.setSymbolId(takerOrder.getSymbolId());
            trade.setTakerOrderId(takerOrder.getOrderId());
            trade.setMakerOrderId(makerOrder.getOrderId());
            trade.setPrice(matchPrice);
            trade.setQuantity(matchQty);
            trade.setTakerSide(takerOrder.getSide());
            trade.setExecutedAt(LocalDateTime.now());
            // Fee logic can be added later
            tradeRepository.save(trade);

            // 4. Update Orders
            updateOrder(takerOrder, matchQty, matchPrice);
            updateOrder(makerOrder, matchQty, matchPrice);
            
            // 5. Refund Excess Margin for Taker BUY
            // If Taker BUY limit is 50000, but matched at 49000. 
            // We locked 50000 * Qty. Actual cost 49000 * Qty. Refund 1000 * Qty.
            if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getPrice().compareTo(matchPrice) > 0) {
                BigDecimal diff = takerOrder.getPrice().subtract(matchPrice);
                BigDecimal refund = diff.multiply(matchQty);
                walletService.unfreezeFunds(takerOrder.getMemberId(), symbol.getQuoteCoinId(), refund);
            }
        }
        
        // Save final state of Taker Order
        orderRepository.save(takerOrder);
    }

    private void updateOrder(Order order, BigDecimal matchQty, BigDecimal matchPrice) {
        order.setFilledQuantity(order.getFilledQuantity().add(matchQty));
        
        // Update Cumulative Quote Quantity (Total Trade Value)
        BigDecimal tradeValue = matchPrice.multiply(matchQty);
        BigDecimal currentCum = order.getCumQuoteQty() != null ? order.getCumQuoteQty() : BigDecimal.ZERO;
        order.setCumQuoteQty(currentCum.add(tradeValue));

        if (order.getFilledQuantity().compareTo(order.getQuantity()) >= 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIAL_FILLED);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}

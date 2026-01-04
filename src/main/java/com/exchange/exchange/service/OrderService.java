package com.exchange.exchange.service;

import com.exchange.exchange.dto.OrderBookDTO;
import com.exchange.exchange.dto.OrderRequest;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.OrderType;
import com.exchange.exchange.repository.OrderRepository;
import com.exchange.exchange.repository.SymbolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private MatchingService matchingService;

    public OrderBookDTO getOrderBook(String symbolId, com.exchange.exchange.enums.TradeType tradeType) {
        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED);
        PageRequest limit = PageRequest.of(0, 10);

        List<OrderBookDTO.Entry> bids = orderRepository.findOrderBookBids(symbolId, OrderSide.BUY, tradeType, activeStatuses, limit);
        List<OrderBookDTO.Entry> asks = orderRepository.findOrderBookAsks(symbolId, OrderSide.SELL, tradeType, activeStatuses, limit);

        return new OrderBookDTO(bids, asks);
    }

    @Transactional
    public Order createOrder(Integer memberId, OrderRequest request) {
        // 1. Basic Validation
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (request.getType() == OrderType.LIMIT && 
            (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Price must be positive for Limit orders");
        }

        // 2. Validate Symbol
        Symbol symbol = symbolRepository.findById(request.getSymbolId())
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + request.getSymbolId()));

        // 3. Calculate Required Funds & Freeze
        com.exchange.exchange.enums.TradeType tradeType = request.getTradeType();
        if (tradeType == null) {
            tradeType = com.exchange.exchange.enums.TradeType.SPOT; // Default to SPOT for demo
        }

        String currencyToFreeze;
        BigDecimal amountToFreeze;

        if (tradeType == com.exchange.exchange.enums.TradeType.SPOT && request.getSide() == OrderSide.SELL) {
            // Spot Sell: Freeze Base Coin (e.g. BTC)
            currencyToFreeze = symbol.getBaseCoinId();
            amountToFreeze = request.getQuantity();
        } else {
            // Spot Buy OR Contract (Buy/Sell): Freeze Quote Coin (e.g. USDT)
            currencyToFreeze = symbol.getQuoteCoinId();
            
            if (request.getType() == OrderType.MARKET) {
                 throw new UnsupportedOperationException("Market orders not yet fully supported for funds freezing");
            }
            // Calculate total value: Price * Quantity
            amountToFreeze = request.getPrice().multiply(request.getQuantity());
        }

        // Freeze logic (will throw if insufficient funds)
        walletService.freezeFunds(memberId, currencyToFreeze, amountToFreeze);

        // 4. Create Order Entity
        Order order = new Order();
        order.setMemberId(memberId);
        order.setSymbolId(request.getSymbolId());
        order.setSide(request.getSide());
        order.setType(request.getType());
        order.setTradeType(tradeType);
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setFilledQuantity(BigDecimal.ZERO);
        order.setStatus(OrderStatus.NEW);
        order.setPostOnly(false); // Default
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        order = orderRepository.save(order);
        
        // 5. Trigger Matching
        matchingService.matchOrder(order);
        
        return order;
    }

    public java.util.List<Order> getOrders(Integer memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional
    public Order cancelOrder(Integer memberId, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.PARTIAL_FILLED) {
            throw new IllegalArgumentException("Order cannot be canceled in state: " + order.getStatus());
        }

        // Calculate refund amount
        // Limit Order: Refund (Price * Remaining Quantity)
        BigDecimal remainingQty = order.getQuantity().subtract(order.getFilledQuantity());
        
        // Retrieve Symbol to know Quote/Base Coin
        Symbol symbol = symbolRepository.findById(order.getSymbolId())
                 .orElseThrow(() -> new IllegalStateException("Symbol missing for existing order"));

        String currencyToUnfreeze;
        BigDecimal refundAmount;
        
        // Default to CONTRACT if null (legacy support)
        com.exchange.exchange.enums.TradeType tradeType = order.getTradeType();
        if (tradeType == null) {
            tradeType = com.exchange.exchange.enums.TradeType.CONTRACT;
        }

        if (tradeType == com.exchange.exchange.enums.TradeType.SPOT && order.getSide() == OrderSide.SELL) {
             // Spot Sell: Refund Base Coin (e.g. BTC)
             currencyToUnfreeze = symbol.getBaseCoinId();
             refundAmount = remainingQty;
        } else {
             // Spot Buy OR Contract: Refund Quote Coin (e.g. USDT)
             currencyToUnfreeze = symbol.getQuoteCoinId();
             refundAmount = order.getPrice().multiply(remainingQty);
        }

        // Update status
        order.setStatus(OrderStatus.CANCELED);
        order.setUpdatedAt(LocalDateTime.now());
        
        // Unfreeze
        walletService.unfreezeFunds(memberId, currencyToUnfreeze, refundAmount);

        return orderRepository.save(order);
    }
}

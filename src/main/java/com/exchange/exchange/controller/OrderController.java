package com.exchange.exchange.controller;

import com.exchange.exchange.dto.OrderRequest;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.exchange.exchange.enums.TradeType;

import com.exchange.exchange.repository.TradeRepository;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TradeRepository tradeRepository;

    private Integer getMemberId(HttpSession session) {
        return (Integer) session.getAttribute("memberId");
    }

    @GetMapping
    public ResponseEntity<?> getOrders(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<Order> orders = orderService.getOrders(memberId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/trades")
    public ResponseEntity<?> getMyTrades(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<com.exchange.exchange.dto.TradeQueryDTO> rawTrades = tradeRepository.findTradeHistory(memberId);
        List<com.exchange.exchange.dto.TradeRecordDTO> history = new java.util.ArrayList<>();
        
        for (com.exchange.exchange.dto.TradeQueryDTO item : rawTrades) {
            com.exchange.exchange.entity.Trade t = item.getTrade();
            
            // If I am Taker -> Record 1
            if (Boolean.TRUE.equals(item.getIsTakerMine())) {
                 com.exchange.exchange.dto.TradeRecordDTO dto = new com.exchange.exchange.dto.TradeRecordDTO();
                 dto.setTradeId(t.getTradeId());
                 dto.setSymbolId(t.getSymbolId());
                 dto.setSide(t.getTakerSide()); // Taker Side is MY side
                 dto.setPrice(t.getPrice());
                 dto.setQuantity(t.getQuantity());
                 dto.setExecutedAt(t.getExecutedAt());
                 dto.setTradeType(t.getTradeType());
                 dto.setRole("TAKER");
                 history.add(dto);
            }
            
            // If I am Maker -> Record 2 (Can exist simultaneously with Taker if self-trade)
            if (Boolean.TRUE.equals(item.getIsMakerMine())) {
                 com.exchange.exchange.dto.TradeRecordDTO dto = new com.exchange.exchange.dto.TradeRecordDTO();
                 dto.setTradeId(t.getTradeId());
                 dto.setSymbolId(t.getSymbolId());
                 // Maker Side is OPPOSITE of Taker Side
                 dto.setSide(t.getTakerSide() == com.exchange.exchange.enums.OrderSide.BUY ? 
                             com.exchange.exchange.enums.OrderSide.SELL : com.exchange.exchange.enums.OrderSide.BUY);
                 dto.setPrice(t.getPrice());
                 dto.setQuantity(t.getQuantity());
                 dto.setExecutedAt(t.getExecutedAt());
                 dto.setTradeType(t.getTradeType());
                 dto.setRole("MAKER");
                 history.add(dto);
            }
        }
        
        // Sort by time descending
        history.sort((a,b) -> b.getExecutedAt().compareTo(a.getExecutedAt()));
        
        return ResponseEntity.ok(history);
    }


    @GetMapping("/book/{symbolId}")
    public ResponseEntity<?> getOrderBook(@PathVariable String symbolId,
                                          @RequestParam(required = false, defaultValue = "SPOT") TradeType type) {
        return ResponseEntity.ok(orderService.getOrderBook(symbolId, type));
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Order order = orderService.createOrder(memberId, request);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Order creation failed: " + e.toString() + " | " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Order order = orderService.cancelOrder(memberId, orderId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

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

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

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

    @GetMapping("/book/{symbolId}")
    public ResponseEntity<?> getOrderBook(@PathVariable String symbolId) {
        return ResponseEntity.ok(orderService.getOrderBook(symbolId));
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
            return ResponseEntity.internalServerError().body("Order creation failed");
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

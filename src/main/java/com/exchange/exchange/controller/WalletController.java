package com.exchange.exchange.controller;

import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    // DTO for deposit request
    public static class DepositRequest {
        public String coinId;
        public BigDecimal amount;
    }

    // Helper to get logged-in user ID
    private Integer getMemberId(HttpSession session) {
        return (Integer) session.getAttribute("memberId");
    }

    // 1. 取得所有錢包
    @GetMapping
    public ResponseEntity<?> myWallets(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Wallet> wallets = walletService.getWallets(memberId);
        return ResponseEntity.ok(wallets);
    }

    // 2. 取得特定幣種錢包 (篩選)
    @GetMapping("/{coinId}")
    public ResponseEntity<?> myWallet(@PathVariable String coinId, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Wallet wallet = walletService.getWallet(memberId, coinId);
        return ResponseEntity.ok(wallet);
    }

    // 3. 模擬儲值
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest request, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // DEBUG LOGGING
        System.out.printf("DEBUG: 儲值請求 - 會員: %d, 幣種: [%s], 金額: %s%n", memberId, request.coinId, request.amount);
        if (request.coinId != null) {
            System.out.print("DEBUG: 幣種字元編碼: ");
            for (char c : request.coinId.toCharArray()) {
                System.out.print((int)c + " ");
            }
            System.out.println();
        }

        try {
            String cleanCoinId = request.coinId != null ? request.coinId.trim() : null;
            Wallet wallet = walletService.deposit(memberId, cleanCoinId, request.amount);
            return ResponseEntity.ok(wallet);
        } catch (IllegalArgumentException e) {
            System.err.println("儲值錯誤: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. 一鍵重置
    @PostMapping("/reset")
    public ResponseEntity<?> reset(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        walletService.resetWallets(memberId);
        return ResponseEntity.ok().build();
    }
}

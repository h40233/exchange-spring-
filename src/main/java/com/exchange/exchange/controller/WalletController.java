package com.exchange.exchange.controller;

// 引入實體與服務
import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.service.WalletService;
// 引入 Spring Web 與 Session
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// ====== 檔案總結 ======
// WalletController 提供錢包與資產相關的 API。
// 核心功能：
// 1. 查詢所有錢包或特定幣種錢包。
// 2. 查詢資金流水 (Transactions)。
// 3. 執行模擬儲值 (Deposit) 與 重置資產 (Reset)。
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    // 內部 DTO 類別：用於接收儲值請求的 JSON Body [註1]
    public static class DepositRequest {
        public String coinId;
        public BigDecimal amount;
    }

    // 私有輔助方法：從 Session 獲取當前用戶 ID
    private Integer getMemberId(HttpSession session) {
        return (Integer) session.getAttribute("memberId");
    }

    // API：取得我的所有錢包
    // GET /api/wallets
    @GetMapping
    public ResponseEntity<?> myWallets(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Wallet> wallets = walletService.getWallets(memberId);
        return ResponseEntity.ok(wallets);
    }

    // API：取得特定幣種的錢包 (若無則自動建立)
    // GET /api/wallets/{coinId}
    @GetMapping("/{coinId}")
    public ResponseEntity<?> myWallet(@PathVariable String coinId, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Wallet wallet = walletService.getWallet(memberId, coinId);
        return ResponseEntity.ok(wallet);
    }

    // API：模擬儲值 (Deposit)
    // POST /api/wallets/deposit
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest request, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 偵錯日誌：記錄儲值請求細節 (生產環境應改用 Logger)
        System.out.printf("DEBUG: 儲值請求 - 會員: %d, 幣種: [%s], 金額: %s%n", memberId, request.coinId, request.amount);
        
        // 偵錯：印出幣種字串的 ASCII 碼，排查不可見字元問題
        if (request.coinId != null) {
            System.out.print("DEBUG: 幣種字元編碼: ");
            for (char c : request.coinId.toCharArray()) {
                System.out.print((int)c + " ");
            }
            System.out.println();
        }

        try {
            // 清理輸入並呼叫 Service
            String cleanCoinId = request.coinId != null ? request.coinId.trim() : null;
            Wallet wallet = walletService.deposit(memberId, cleanCoinId, request.amount);
            return ResponseEntity.ok(wallet);
        } catch (IllegalArgumentException e) {
            // 幣種不存在或金額錯誤
            System.err.println("儲值錯誤: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API：一鍵重置所有資產 (開發測試用)
    // POST /api/wallets/reset
    @PostMapping("/reset")
    public ResponseEntity<?> reset(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        walletService.resetWallets(memberId);
        return ResponseEntity.ok().build();
    }

    // API：取得資金流水紀錄
    // GET /api/wallets/transactions
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 直接回傳 Entity 列表
        List<com.exchange.exchange.entity.WalletTransaction> list = walletService.getTransactions(memberId);
        return ResponseEntity.ok(list);
    }
}

// ====== 備註區 ======
/*
[註1] DTO 管理 (DTO Management):
      將 `DepositRequest` 定義為 Controller 的內部靜態類別是小型專案的常見做法。
      但在大型專案中，建議將所有 DTO 移至獨立的 `com.exchange.exchange.dto` 套件中，方便重複使用與管理。

[註2] 浮點數精度 (Floating Point Precision):
      `BigDecimal` 在 JSON 序列化時通常沒問題，但若前端傳送的是浮點數 (如 100.0000001)，
      需確保 JSON 解析器 (Jackson) 配置正確，避免精度丟失。
*/
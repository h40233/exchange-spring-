package com.exchange.exchange.controller;

// 引入實體與服務
import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.service.WalletService;
// 引入 Spring Web 與 Session 工具
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// ====== 檔案總結 ======
// WalletController 提供使用者資產管理的 RESTful API。
// 核心功能：
// 1. 查詢資產：列出所有幣種的錢包餘額或查詢特定錢包。
// 2. 資金操作：模擬儲值 (Deposit) 與 重置資產 (Reset)。
// 3. 歷史紀錄：查詢資金流水 (Transactions)。
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    // 注入錢包服務
    @Autowired
    private WalletService walletService;

    // 注入幣種儲存庫 (用於查詢系統支援的幣種)
    @Autowired
    private com.exchange.exchange.repository.CoinRepository coinRepository;

    // API：取得系統支援的所有幣種清單
    // 路徑：GET /api/wallets/coins
    // 用途：前端下拉選單使用
    @GetMapping("/coins")
    public ResponseEntity<List<String>> getAllCoins() {
        // 使用 Stream API 提取所有 Coin 實體的 coinId
        List<String> coins = coinRepository.findAll().stream()
                .map(com.exchange.exchange.entity.Coin::getCoinId)
                .toList();
        return ResponseEntity.ok(coins);
    }

    // 內部類別 (DTO)：定義儲值請求的 JSON 結構 [註1]
    public static class DepositRequest {
        public String coinId;
        public BigDecimal amount;
    }

    // 私有輔助方法：從 Session 獲取當前登入的會員 ID
    private Integer getMemberId(HttpSession session) {
        return (Integer) session.getAttribute("memberId");
    }

    // API：取得我的所有錢包餘額
    // 路徑：GET /api/wallets
    @GetMapping
    public ResponseEntity<?> myWallets(HttpSession session) {
        Integer memberId = getMemberId(session);
        // 權限檢查
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 呼叫 Service 查詢該會員名下所有錢包
        List<Wallet> wallets = walletService.getWallets(memberId);
        return ResponseEntity.ok(wallets);
    }

    // API：取得特定幣種的錢包 (若不存在則自動建立)
    // 路徑：GET /api/wallets/{coinId}
    @GetMapping("/{coinId}")
    public ResponseEntity<?> myWallet(@PathVariable String coinId, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Wallet wallet = walletService.getWallet(memberId, coinId);
        return ResponseEntity.ok(wallet);
    }

    // API：模擬儲值 (Deposit)
    // 路徑：POST /api/wallets/deposit
    // 注意：此接口僅供開發測試，生產環境應串接區塊鏈節點
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest request, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 偵錯日誌：記錄詳細的儲值參數
        System.out.printf("DEBUG: 儲值請求 - 會員: %d, 幣種: [%s], 金額: %s%n", memberId, request.coinId, request.amount);
        
        // 偵錯邏輯：檢查幣種字串是否包含不可見字元 (如 BOM 或 空格)
        if (request.coinId != null) {
            System.out.print("DEBUG: 幣種字元編碼: ");
            for (char c : request.coinId.toCharArray()) {
                System.out.print((int)c + " ");
            }
            System.out.println();
        }

        try {
            // 資料清洗：去除幣種 ID 的前後空白
            String cleanCoinId = request.coinId != null ? request.coinId.trim() : null;
            // 呼叫 Service 執行入金邏輯
            Wallet wallet = walletService.deposit(memberId, cleanCoinId, request.amount);
            return ResponseEntity.ok(wallet);
        } catch (IllegalArgumentException e) {
            // 捕捉幣種不存在或金額為負數等錯誤
            System.err.println("儲值錯誤: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API：重置所有資產 (歸零)
    // 路徑：POST /api/wallets/reset
    // 用途：方便測試重複下單或清空環境
    @PostMapping("/reset")
    public ResponseEntity<?> reset(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        walletService.resetWallets(memberId);
        return ResponseEntity.ok().build();
    }

    // API：取得資金流水紀錄
    // 路徑：GET /api/wallets/transactions
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 查詢該會員的所有歷史資金變動
        List<com.exchange.exchange.entity.WalletTransaction> list = walletService.getTransactions(memberId);
        return ResponseEntity.ok(list);
    }
}

// ====== 備註區 ======
/*
[註1] DTO 架構 (DTO Architecture):
      將 `DepositRequest` 作為內部靜態類別定義在 Controller 中，適合小型專案快速開發。
      隨著專案變大，建議將所有 Request/Response 物件移至獨立的 `dto` 套件中 (如 `com.exchange.exchange.dto.DepositRequest`)，
      以提升程式碼的可維護性與重用性。

[註2] 數值精度 (Numerical Precision):
      使用 `BigDecimal` 處理金額是正確的。
      但在 JSON 序列化/反序列化過程中，需確保前端傳遞的數值格式正確 (例如字串形式的數字 "100.00")，
      以避免 JavaScript 浮點數運算帶來的精度丟失問題。
*/
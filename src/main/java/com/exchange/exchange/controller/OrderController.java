package com.exchange.exchange.controller;

// 引入 DTO：接收前端下單請求
import com.exchange.exchange.dto.OrderRequest;
// 引入實體：訂單
import com.exchange.exchange.entity.Order;
// 引入服務層與儲存庫
import com.exchange.exchange.service.OrderService;
import com.exchange.exchange.repository.TradeRepository;
// 引入枚舉：交易類型 (現貨/合約)
import com.exchange.exchange.enums.TradeType;

// 引入 Spring Web 與 Session 工具
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ====== 檔案總結 ======
// OrderController 提供交易相關的 RESTful API。
// 核心功能：
// 1. 查詢訂單 (歷史委託) 與 成交紀錄 (歷史成交)。
// 2. 查詢訂單簿 (深度圖數據)。
// 3. 下單 (Create Order) 與 撤單 (Cancel Order)。
// 所有的寫入操作 (POST) 都需要驗證使用者登入狀態 (Session)。
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TradeRepository tradeRepository;

    // 私有輔助方法：從 Session 中提取當前登入的 Member ID
    // 若未登入 (Session 中無 memberId)，則回傳 null
    private Integer getMemberId(HttpSession session) {
        return (Integer) session.getAttribute("memberId");
    }

    // API：獲取我的歷史訂單
    // GET /api/orders
    @GetMapping
    public ResponseEntity<?> getOrders(HttpSession session) {
        Integer memberId = getMemberId(session);
        // 權限驗證：未登入則回傳 401 Unauthorized
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<Order> orders = orderService.getOrders(memberId);
        return ResponseEntity.ok(orders);
    }

    // API：獲取我的成交紀錄 (包含 Taker 與 Maker 的成交) [註1]
    // GET /api/orders/trades
    @GetMapping("/trades")
    public ResponseEntity<?> getMyTrades(HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        // 從 Repository 獲取原始查詢結果 (TradeQueryDTO)
        // 這些結果包含了成交資訊以及 "我是 Taker 嗎?" "我是 Maker 嗎?" 的標記
        List<com.exchange.exchange.dto.TradeQueryDTO> rawTrades = tradeRepository.findTradeHistory(memberId);
        
        // 準備一個列表來存放轉換後的 DTO (TradeRecordDTO) 給前端顯示
        List<com.exchange.exchange.dto.TradeRecordDTO> history = new java.util.ArrayList<>();
        
        // 遍歷每一筆原始成交資料進行邏輯處理
        for (com.exchange.exchange.dto.TradeQueryDTO item : rawTrades) {
            com.exchange.exchange.entity.Trade t = item.getTrade();
            
            // 情境 A：我在這筆成交中擔任 Taker (主動吃單)
            // 需要產生一條 "TAKER" 視角的紀錄
            if (Boolean.TRUE.equals(item.getIsTakerMine())) {
                 com.exchange.exchange.dto.TradeRecordDTO dto = new com.exchange.exchange.dto.TradeRecordDTO();
                 dto.setTradeId(t.getTradeId());
                 dto.setSymbolId(t.getSymbolId());
                 dto.setSide(t.getTakerSide()); // Taker 的方向就是訂單上的方向
                 dto.setPrice(t.getPrice());
                 dto.setQuantity(t.getQuantity());
                 dto.setExecutedAt(t.getExecutedAt());
                 dto.setTradeType(t.getTradeType());
                 dto.setRole("TAKER"); // 標記角色
                 history.add(dto);
            }
            
            // 情境 B：我在這筆成交中擔任 Maker (被動掛單)
            // 需要產生一條 "MAKER" 視角的紀錄
            // 注意：若發生 "自成交" (Self-Trade)，同一筆 Trade 可能同時進入 A 和 B 兩個區塊
            if (Boolean.TRUE.equals(item.getIsMakerMine())) {
                 com.exchange.exchange.dto.TradeRecordDTO dto = new com.exchange.exchange.dto.TradeRecordDTO();
                 dto.setTradeId(t.getTradeId());
                 dto.setSymbolId(t.getSymbolId());
                 // Maker 的方向與 Taker 相反 (Taker 買，Maker 就是賣)
                 dto.setSide(t.getTakerSide() == com.exchange.exchange.enums.OrderSide.BUY ? 
                             com.exchange.exchange.enums.OrderSide.SELL : com.exchange.exchange.enums.OrderSide.BUY);
                 dto.setPrice(t.getPrice());
                 dto.setQuantity(t.getQuantity());
                 dto.setExecutedAt(t.getExecutedAt());
                 dto.setTradeType(t.getTradeType());
                 dto.setRole("MAKER"); // 標記角色
                 history.add(dto);
            }
        }
        
        // 將整理好的列表按時間倒序排列 (最新的在前)
        history.sort((a,b) -> b.getExecutedAt().compareTo(a.getExecutedAt()));
        
        return ResponseEntity.ok(history);
    }


    // API：獲取公開訂單簿 (Order Book)
    // GET /api/orders/book/{symbolId}?type=SPOT
    @GetMapping("/book/{symbolId}")
    public ResponseEntity<?> getOrderBook(@PathVariable String symbolId,
                                          @RequestParam(required = false, defaultValue = "SPOT") TradeType type) {
        // 此 API 為公開資訊，無需登入驗證
        return ResponseEntity.ok(orderService.getOrderBook(symbolId, type));
    }

    // API：建立新訂單 (下單)
    // POST /api/orders
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 呼叫 Service 執行下單邏輯 (包含凍結資金與撮合)
            Order order = orderService.createOrder(memberId, request);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            // 參數錯誤 (如餘額不足、參數為負數) -> 回傳 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UnsupportedOperationException e) {
            // 功能未實作 (如市價單) -> 回傳 501 Not Implemented
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(e.getMessage());
        } catch (Exception e) {
            // 未知錯誤 -> 回傳 500 Internal Server Error 並印出堆疊
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Order creation failed: " + e.toString() + " | " + e.getMessage());
        }
    }

    // API：取消訂單 (撤單)
    // POST /api/orders/{orderId}/cancel
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId, HttpSession session) {
        Integer memberId = getMemberId(session);
        if (memberId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            // 呼叫 Service 執行撤單邏輯 (包含解凍資金)
            Order order = orderService.cancelOrder(memberId, orderId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            // 訂單不存在或不屬於該用戶 -> 回傳 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

// ====== 備註區 ======
/*
[註1] 邏輯分層 (Layering Violation):
      `getMyTrades` 方法中包含了大量的資料轉換與業務邏輯 (判斷 Maker/Taker, 翻轉方向)。
      這屬於 Service 層的職責。Controller 應專注於請求分發。
      建議將這段 `for` 迴圈邏輯移至 `TradeService` 或 `OrderService` 中。

[註2] 異常處理 (Global Exception Handling):
      目前在 `createOrder` 中使用了 try-catch 區塊。
      建議使用 @ControllerAdvice 與 @ExceptionHandler 來進行全域異常處理，
      這樣可以讓 Controller 程式碼更乾淨，並統一錯誤回傳格式。

[註3] DTO 轉換 (DTO Mapping):
      目前直接回傳 Entity (`Order`) 給前端。
      雖然方便，但暴露了資料庫結構 (如 `memberID`)。
      建議轉換為 `OrderResponseDTO` 再回傳，僅暴露必要的欄位。
*/
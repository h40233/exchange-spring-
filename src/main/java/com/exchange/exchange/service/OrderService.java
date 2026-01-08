package com.exchange.exchange.service;

// 引入 DTO：訂單請求與訂單簿結構
import com.exchange.exchange.dto.OrderBookDTO;
import com.exchange.exchange.dto.OrderRequest;
// 引入實體：訂單與交易對
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.entity.Symbol;
// 引入枚舉：方向、狀態、類型
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.OrderType;
// 引入資料存取層
import com.exchange.exchange.repository.OrderRepository;
import com.exchange.exchange.repository.SymbolRepository;
// 引入 Spring 相關工具
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

// ====== 檔案總結 ======
// OrderService 負責訂單生命週期的管理。
// 核心功能包括：
// 1. 查詢訂單簿 (Order Book)
// 2. 建立新訂單 (下單 -> 驗證 -> 凍結資金 -> 呼叫撮合)
// 3. 取消訂單 (驗證 -> 解凍資金 -> 更新狀態)
@Service
public class OrderService {

    // 注入訂單資料庫操作介面
    @Autowired
    private OrderRepository orderRepository;

    // 注入錢包服務，用於資金凍結與解凍
    @Autowired
    private WalletService walletService;

    // 注入交易對設定儲存庫
    @Autowired
    private SymbolRepository symbolRepository;

    // 注入撮合服務，當新訂單產生時觸發撮合
    @Autowired
    private MatchingService matchingService;

    // 方法：獲取訂單簿資料 (買賣盤)
    // 參數：symbolId (如 BTCUSDT), tradeType (如 SPOT)
    public OrderBookDTO getOrderBook(String symbolId, com.exchange.exchange.enums.TradeType tradeType) {
        // 定義哪些狀態的訂單屬於「活躍掛單」(Active Orders)
        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED);
        // 設定分頁限制，僅取前 10 檔報價 (Top 10) [註1]
        PageRequest limit = PageRequest.of(0, 10);

        // 查詢買單 (Bids)：價格由高到低排序 (買方希望越便宜越好，但對市場來說高價買單優先)
        List<OrderBookDTO.Entry> bids = orderRepository.findOrderBookBids(symbolId, OrderSide.BUY, tradeType, activeStatuses, limit);
        // 查詢賣單 (Asks)：價格由低到高排序 (賣方希望越貴越好，但對市場來說低價賣單優先)
        List<OrderBookDTO.Entry> asks = orderRepository.findOrderBookAsks(symbolId, OrderSide.SELL, tradeType, activeStatuses, limit);

        // 封裝成 DTO 返回給前端
        return new OrderBookDTO(bids, asks);
    }

    // 方法：建立新訂單
    // 使用 @Transactional 確保下單流程 (驗證、凍結、存檔) 的一致性
    @Transactional
    public Order createOrder(Integer memberId, OrderRequest request) {
        // 1. 基礎參數驗證 (Basic Validation)
        // 檢查數量必須大於 0
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        // 若為限價單 (LIMIT)，價格必須存在且大於 0
        if (request.getType() == OrderType.LIMIT && 
            (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Price must be positive for Limit orders");
        }

        // 2. 驗證交易對 (Validate Symbol)
        // 確認該 Symbol 是否存在於系統中，若無則拋出異常
        Symbol symbol = symbolRepository.findById(request.getSymbolId())
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + request.getSymbolId()));

        // 3. 計算所需資金並凍結 (Calculate Required Funds & Freeze)
        // 獲取交易類型，若未傳入則預設為現貨 (SPOT)
        com.exchange.exchange.enums.TradeType tradeType = request.getTradeType();
        if (tradeType == null) {
            tradeType = com.exchange.exchange.enums.TradeType.SPOT; // Default to SPOT for demo
        }

        String currencyToFreeze;
        BigDecimal amountToFreeze;

        // 判斷凍結邏輯 (現貨交易)
        if (tradeType == com.exchange.exchange.enums.TradeType.SPOT && request.getSide() == OrderSide.SELL) {
            // 情境：現貨賣出 (Spot Sell) -> 例如賣 BTC 換 USDT
            // 需凍結：基礎幣 (Base Coin，如 BTC)
            // 凍結金額：賣出數量 (Quantity)
            currencyToFreeze = symbol.getBaseCoinId();
            amountToFreeze = request.getQuantity();
        } else {
            // 情境：現貨買入 (Spot Buy) 或 合約交易 (Contract)
            // 需凍結：報價幣 (Quote Coin，如 USDT)
            currencyToFreeze = symbol.getQuoteCoinId();
            
            // 目前系統尚未完全支援市價單的預凍結邏輯 (因為市價單價格未定)
            if (request.getType() == OrderType.MARKET) {
                 throw new UnsupportedOperationException("Market orders not yet fully supported for funds freezing");
            }
            // 限價單凍結金額 = 價格 * 數量
            amountToFreeze = request.getPrice().multiply(request.getQuantity());
        }

        // 執行資金凍結邏輯 (若餘額不足，WalletService 會拋出異常中斷交易)
        walletService.freezeFunds(memberId, currencyToFreeze, amountToFreeze);

        // 4. 建立並保存訂單實體 (Create Order Entity)
        Order order = new Order();
        order.setMemberId(memberId);
        order.setSymbolId(request.getSymbolId());
        order.setSide(request.getSide());
        order.setType(request.getType());
        order.setTradeType(tradeType);
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setFilledQuantity(BigDecimal.ZERO); // 初始已成交量為 0
        order.setStatus(OrderStatus.NEW);         // 初始狀態為 NEW
        order.setPostOnly(false);                 // 預設關閉 Post Only
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 將訂單存入資料庫以獲取 Order ID
        order = orderRepository.save(order);
        
        // 5. 觸發撮合引擎 (Trigger Matching)
        // 注意：這裡是同步呼叫，意味著下單請求會等待撮合完成才返回 [註2]
        matchingService.matchOrder(order);
        
        return order;
    }

    // 方法：獲取指定會員的所有訂單 (依時間倒序)
    public java.util.List<Order> getOrders(Integer memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    // 方法：取消訂單
    @Transactional
    public Order cancelOrder(Integer memberId, Integer orderId) {
        // 根據 ID 查找訂單，若無則拋出異常
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // 權限驗證：確保只能取消自己的訂單
        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        // 狀態驗證：只能取消「新訂單」或「部分成交」的訂單
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.PARTIAL_FILLED) {
            throw new IllegalArgumentException("Order cannot be canceled in state: " + order.getStatus());
        }

        // 計算應退還的資金 (Calculate refund amount)
        // 需退還金額 = 價格 * 剩餘未成交數量
        BigDecimal remainingQty = order.getQuantity().subtract(order.getFilledQuantity());
        
        // 重新獲取 Symbol 資訊以確定幣種
        Symbol symbol = symbolRepository.findById(order.getSymbolId())
                 .orElseThrow(() -> new IllegalStateException("Symbol missing for existing order"));

        String currencyToUnfreeze;
        BigDecimal refundAmount;
        
        // 處理遺留資料的相容性 (若 tradeType 為空則預設為合約)
        com.exchange.exchange.enums.TradeType tradeType = order.getTradeType();
        if (tradeType == null) {
            tradeType = com.exchange.exchange.enums.TradeType.CONTRACT;
        }

        // 判斷解凍幣種邏輯
        if (tradeType == com.exchange.exchange.enums.TradeType.SPOT && order.getSide() == OrderSide.SELL) {
             // 現貨賣單取消：退還基礎幣 (如 BTC)
             currencyToUnfreeze = symbol.getBaseCoinId();
             refundAmount = remainingQty;
        } else {
             // 現貨買單或合約單取消：退還報價幣 (如 USDT)
             // 退還金額 = 凍結單價 * 剩餘數量
             currencyToUnfreeze = symbol.getQuoteCoinId();
             refundAmount = order.getPrice().multiply(remainingQty);
        }

        // 更新訂單狀態為 CANCELED
        order.setStatus(OrderStatus.CANCELED);
        order.setUpdatedAt(LocalDateTime.now());
        
        // 執行資金解凍
        walletService.unfreezeFunds(memberId, currencyToUnfreeze, refundAmount);

        // 保存狀態變更
        return orderRepository.save(order);
    }
}

// ====== 備註區 ======
/*
[註1] 深度合併 (Depth Aggregation):
      目前的實作僅依賴 PageRequest(0, 10)，這可能只是抓取了前 10 筆「訂單」，而非前 10 個「價格檔位」。
      真正的訂單簿通常需要將相同價格的訂單數量加總 (Group By Price)，這部分邏輯似乎已在 Repository 中透過 JPQL 實作，值得嘉許。

[註2] 同步撮合風險 (Synchronous Matching Risk):
      在 createOrder 事務中直接呼叫 matchingService.matchOrder(order) 會導致交易時間拉長。
      若撮合邏輯複雜或對手單眾多，使用者介面會卡住。
      改進建議：使用事件驅動 (Event-Driven) 或消息隊列 (如 RabbitMQ/Kafka) 非同步觸發撮合。

[註3] 異常處理 (Exception Handling):
      目前大量使用 IllegalArgumentException。建議建立自定義異常類別 (例如 InsufficientFundsException, OrderNotFoundException)，
      以便在 Controller 層統一處理錯誤訊息與 HTTP 狀態碼。
*/
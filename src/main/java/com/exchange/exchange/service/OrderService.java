package com.exchange.exchange.service;

// 引入 DTO：用於回傳訂單簿數據與接收下單請求
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchange.exchange.dto.OrderBookDTO;
import com.exchange.exchange.dto.OrderRequest;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.OrderType;
import com.exchange.exchange.repository.OrderRepository;
import com.exchange.exchange.repository.SymbolRepository;

// ====== 檔案總結 ======
// OrderService 負責管理訂單的生命週期 (Lifecycle Management)。
// 主要功能：
// 1. 訂單簿查詢：聚合買賣盤深度。
// 2. 下單處理：包含參數驗證、資金凍結 (Pre-Trade Risk Check)、訂單持久化、觸發撮合。
// 3. 撤單處理：包含狀態檢查、資金解凍 (Refund)。
@Service
public class OrderService {

    // 注入訂單儲存庫
    @Autowired
    private OrderRepository orderRepository;

    // 注入錢包服務：負責資金的凍結與解凍
    @Autowired
    private WalletService walletService;

    // 注入交易對儲存庫：用於驗證交易對有效性
    @Autowired
    private SymbolRepository symbolRepository;

    // 注入撮合服務：下單成功後立即觸發撮合
    @Autowired
    private MatchingService matchingService;

    // 方法：查詢訂單簿 (Order Book)
    // 用於前端顯示深度圖 (Depth Chart) 或買賣盤列表
    public OrderBookDTO getOrderBook(String symbolId, com.exchange.exchange.enums.TradeType tradeType) {
        // 定義哪些訂單狀態應被納入訂單簿 (僅 NEW 和 PARTIAL_FILLED)
        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED);
        // 設定查詢限制：只取最優的 10 檔報價 (Top 10)
        PageRequest limit = PageRequest.of(0, 10);

        // 查詢買盤 (Bids)：價格由高到低排序
        // 買方希望買低，但市場上價格最高的買單最容易成交，排在最上面 (Best Bid)
        List<OrderBookDTO.Entry> bids = orderRepository.findOrderBookBids(symbolId, OrderSide.BUY, tradeType, activeStatuses, limit);
        
        // 查詢賣盤 (Asks)：價格由低到高排序
        // 賣方希望賣高，但市場上價格最低的賣單最容易成交，排在最上面 (Best Ask)
        List<OrderBookDTO.Entry> asks = orderRepository.findOrderBookAsks(symbolId, OrderSide.SELL, tradeType, activeStatuses, limit);

        // 將結果封裝為 DTO 回傳
        return new OrderBookDTO(bids, asks);
    }

    // 方法：建立新訂單 (Create Order)
    // 標註 @Transactional 確保整個下單流程 (驗證->凍結->存檔) 為原子操作
    @Transactional
    public Order createOrder(Integer memberId, OrderRequest request) {
        // 臨時限制：暫時禁用合約交易功能，僅開放現貨
        if (request.getTradeType() == com.exchange.exchange.enums.TradeType.CONTRACT) {
            throw new UnsupportedOperationException("Contract trading is temporarily disabled.");
        }

        // 步驟 1：基礎參數驗證 (Validation)
        // 數量必須為正數
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        // 若為限價單，價格必須為正數
        if (request.getType() == OrderType.LIMIT && 
            (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Price must be positive for Limit orders");
        }

        // 步驟 2：驗證交易對是否存在
        Symbol symbol = symbolRepository.findById(request.getSymbolId())
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + request.getSymbolId()));

        // 步驟 3：計算並凍結所需資金 (Freeze Funds)
        com.exchange.exchange.enums.TradeType tradeType = request.getTradeType();
        if (tradeType == null) {
            tradeType = com.exchange.exchange.enums.TradeType.SPOT; // 若未指定，預設為現貨
        }

        String currencyToFreeze; // 需凍結的幣種
        BigDecimal amountToFreeze; // 需凍結的金額

        if (request.getType() == OrderType.MARKET) {
            // --- 市價單 (Market Order) 處理邏輯 ---
            if (request.getSide() == OrderSide.SELL) {
                // 市價賣出：凍結基礎幣 (Base Coin)
                // 例如：賣出 1 BTC，需凍結 1 BTC
                currencyToFreeze = symbol.getBaseCoinId();
                amountToFreeze = request.getQuantity();
                // 市價單沒有指定價格，設為 0 (撮合時會忽略此價格)
                request.setPrice(BigDecimal.ZERO);
            } else {
                // 市價買入：凍結報價幣 (Quote Coin)
                // 困難點：因為不知道最終成交價，需預估凍結金額
                currencyToFreeze = symbol.getQuoteCoinId();
                
                // 策略：查詢當前市場「最佳賣價」(Best Ask)
                PageRequest top1 = PageRequest.of(0, 1);
                List<OrderBookDTO.Entry> asks = orderRepository.findOrderBookAsks(
                        symbol.getSymbolId(), OrderSide.SELL, tradeType, 
                        Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED), top1);
                
                BigDecimal estimatedPrice;
                if (asks.isEmpty()) {
                    // 若市場無賣單，無法評估價格，拒絕市價買入
                    throw new IllegalArgumentException("No asks available for market buy");
                } else {
                    estimatedPrice = asks.get(0).getPrice();
                }
                
                // 計算凍結金額：預估價格 * 數量 * 1.05 (多凍結 5% 作為價格滑點緩衝) [註1]
                amountToFreeze = estimatedPrice.multiply(request.getQuantity())
                                               .multiply(new BigDecimal("1.05"));
                
                // 設定一個極高的價格作為市價買單的「虛擬限價」，確保能吃掉所有賣單
                // 使用 long 的最大值，確保比任何賣單都高
                request.setPrice(new BigDecimal("1000000000")); 
            }
        } else {
            // --- 限價單 (Limit Order) 處理邏輯 ---
            if (tradeType == com.exchange.exchange.enums.TradeType.SPOT && request.getSide() == OrderSide.SELL) {
                // 限價賣出：凍結基礎幣 (數量)
                currencyToFreeze = symbol.getBaseCoinId();
                amountToFreeze = request.getQuantity();
            } else {
                // 限價買入：凍結報價幣 (價格 * 數量)
                currencyToFreeze = symbol.getQuoteCoinId();
                amountToFreeze = request.getPrice().multiply(request.getQuantity());
            }
        }

        // 呼叫 WalletService 執行資金凍結
        // 若餘額不足，此處會拋出異常，交易將回滾 (Rollback)
        walletService.freezeFunds(memberId, currencyToFreeze, amountToFreeze);

        // 步驟 4：建立並保存訂單實體
        Order order = new Order();
        order.setMemberId(memberId);
        order.setSymbolId(request.getSymbolId());
        order.setSide(request.getSide());
        order.setType(request.getType());
        order.setTradeType(tradeType);
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setFilledQuantity(BigDecimal.ZERO); // 初始成交量為 0
        order.setStatus(OrderStatus.NEW);         // 初始狀態為 NEW
        order.setPostOnly(false);                 // 預設關閉 Post-Only
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 寫入資料庫，獲取生成的 Order ID
        order = orderRepository.save(order);
        
        // 步驟 5：觸發撮合引擎
        // 這是同步呼叫，使用者需等待撮合完成才會收到 API 回應
        matchingService.matchOrder(order);
        
        return order;
    }

    // 方法：查詢某會員的歷史訂單
    public java.util.List<Order> getOrders(Integer memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    // 方法：取消訂單 (Cancel Order)
    @Transactional
    public Order cancelOrder(Integer memberId, Integer orderId) {
        // 查詢訂單，若不存在則拋出異常
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // 權限驗證：確保只能取消自己的訂單
        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        // 狀態驗證：只能取消「新訂單」或「部分成交」的訂單
        // 若訂單已完成或已取消，則無法再次取消
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.PARTIAL_FILLED) {
            throw new IllegalArgumentException("Order cannot be canceled in state: " + order.getStatus());
        }

        // 計算剩餘未成交數量 (Only refund remaining part)
        BigDecimal remainingQty = order.getQuantity().subtract(order.getFilledQuantity());
        
        // 獲取 Symbol 資訊以確定幣種
        Symbol symbol = symbolRepository.findById(order.getSymbolId())
                 .orElseThrow(() -> new IllegalStateException("Symbol missing for existing order"));

        String currencyToUnfreeze;
        BigDecimal refundAmount;
        
        // 處理舊資料相容性
        com.exchange.exchange.enums.TradeType tradeType = order.getTradeType();
        if (tradeType == null) {
            tradeType = com.exchange.exchange.enums.TradeType.CONTRACT;
        }

        // 判斷解凍邏輯
        if (tradeType == com.exchange.exchange.enums.TradeType.SPOT && order.getSide() == OrderSide.SELL) {
             // 現貨賣單取消：退還基礎幣 (Base Coin) 的剩餘數量
             currencyToUnfreeze = symbol.getBaseCoinId();
             refundAmount = remainingQty;
        } else {
             // 現貨買單取消：退還報價幣 (Quote Coin)
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
[註1] 市價單滑點 (Market Order Slippage):
      目前採用固定 5% (1.05) 的緩衝係數。在劇烈波動的市場中，5% 可能不足以覆蓋價格滑動，導致下單失敗。
      更好的做法是讓使用者在前端設定「最大滑點容忍度」(Slippage Tolerance)。

[註2] 撮合解耦 (Decoupling Matching):
      `matchingService.matchOrder(order)` 是同步呼叫。
      這會導致 `createOrder` 的回應時間取決於撮合引擎的處理速度。
      建議引入 Message Queue (如 RabbitMQ, Kafka)，將下單事件發送至佇列，由撮合引擎非同步消費處理，提升下單 API 的吞吐量。
*/
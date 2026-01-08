package com.exchange.exchange.service;

// 引入實體層：訂單、交易對、成交紀錄
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.entity.Trade;
// 引入枚舉層：訂單方向、狀態
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
// 引入資料存取層
import com.exchange.exchange.repository.OrderRepository;
import com.exchange.exchange.repository.SymbolRepository;
import com.exchange.exchange.repository.TradeRepository;
// 引入 Spring 框架依賴注入與服務標記
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 引入高精度數學運算與時間處理
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

// ====== 檔案總結 ======
// 此類別 (Service) 實現了交易所的核心撮合引擎邏輯。
// 主要職責是接收一個新建立的訂單 (Taker)，並在資料庫中尋找符合條件的掛單 (Maker) 進行成交。
// 包含成交後的資金結算與訂單狀態更新。
@Service
public class MatchingService {

    // 注入 OrderRepository，用於查詢掛單與更新訂單狀態
    @Autowired
    private OrderRepository orderRepository;

    // 注入 TradeRepository，用於保存成交紀錄
    @Autowired
    private TradeRepository tradeRepository;

    // 注入 WalletService，用於處理成交後的資金劃轉與解凍
    @Autowired
    private WalletService walletService;
    
    // 注入 SymbolRepository，用於獲取幣對的詳細設定（如基礎幣與報價幣）
    @Autowired
    private SymbolRepository symbolRepository;
    
    // 注入 PositionService，用於處理合約交易的倉位變化（目前程式碼中部分被註解掉）
    @Autowired
    private PositionService positionService;

    // 核心撮合方法：接收一個主動單 (Taker Order) 並嘗試撮合
    // 使用 @Transactional 確保撮合過程中的資料庫操作（訂單更新、成交紀錄、資金變動）具有原子性 [註1]
    @Transactional
    public void matchOrder(Order takerOrder) {
        // 檢查防禦：若訂單狀態已經是完成或取消，則不進行任何操作直接返回
        if (takerOrder.getStatus() == OrderStatus.FILLED || takerOrder.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        // 定義哪些狀態的訂單可以作為對手單（Maker）：新訂單或部分成交的訂單
        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED);
        // 宣告一個列表來存儲匹配到的對手單
        List<Order> matchingOrders;

        // 1. 尋找匹配的訂單 (Find Matching Orders)
        // 判斷 Taker 是買方還是賣方
        if (takerOrder.getSide() == OrderSide.BUY) {
            // 若 Taker 是買入 (BUY)，則需尋找賣出 (SELL) 的掛單
            // 條件：對手單價格 <= Taker 出價 (買得更便宜或相等)
            matchingOrders = orderRepository.findMatchingSellOrders(
                    takerOrder.getSymbolId(), OrderSide.SELL, takerOrder.getTradeType(), activeStatuses, takerOrder.getPrice());
        } else {
            // 若 Taker 是賣出 (SELL)，則需尋找買入 (BUY) 的掛單
            // 條件：對手單價格 >= Taker 出價 (賣得更貴或相等)
            matchingOrders = orderRepository.findMatchingBuyOrders(
                    takerOrder.getSymbolId(), OrderSide.BUY, takerOrder.getTradeType(), activeStatuses, takerOrder.getPrice());
        }

        // 若沒有找到任何匹配的訂單，撮合結束，訂單將掛在 OrderBook 上等待被撮合
        if (matchingOrders.isEmpty()) return;

        // 2. 迭代並執行撮合 (Iterate and Match)
        // 獲取該交易對的詳細資訊 (例如 BTCUSDT -> Base: BTC, Quote: USDT)
        // 若找不到該幣對 ID，則拋出異常 (NoSuchElementException)
        Symbol symbol = symbolRepository.findById(takerOrder.getSymbolId()).orElseThrow();
        
        // 遍歷所有符合條件的 Maker 訂單
        for (Order makerOrder : matchingOrders) {
            // 迴圈檢查：若 Taker 訂單已完全成交，則提前跳出迴圈
            if (takerOrder.getStatus() == OrderStatus.FILLED) break;

            // 計算可成交數量 (Match Quantity)
            // Taker 剩餘未成交量 = 總量 - 已成交量
            BigDecimal takerRemaining = takerOrder.getQuantity().subtract(takerOrder.getFilledQuantity());
            // Maker 剩餘未成交量 = 總量 - 已成交量
            BigDecimal makerRemaining = makerOrder.getQuantity().subtract(makerOrder.getFilledQuantity());
            // 本次成交量取兩者之最小值 (min)
            BigDecimal matchQty = takerRemaining.min(makerRemaining);

            // 邊界檢查：若計算出的成交量小於等於 0，則跳過此筆 (理論上不應發生，除非資料異常)
            if (matchQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 確定成交價格：依照交易所慣例，成交價始終以「掛單 (Maker)」的價格為準 [註2]
            BigDecimal matchPrice = makerOrder.getPrice();

            // 3. 建立成交紀錄 (Create Trade)
            // 實例化一個新的 Trade 物件來記錄這次撮合
            Trade trade = new Trade();
            // 設定交易對 ID
            trade.setSymbolId(takerOrder.getSymbolId());
            // 記錄 Taker 與 Maker 的訂單 ID，建立關聯
            trade.setTakerOrderId(takerOrder.getOrderId());
            trade.setMakerOrderId(makerOrder.getOrderId());
            // 設定本次成交的價格與數量
            trade.setPrice(matchPrice);
            trade.setQuantity(matchQty);
            // 記錄主動方 (Taker) 的方向，用於後續判斷主動買入或賣出
            trade.setTakerSide(takerOrder.getSide());
            // 記錄交易類型 (現貨或合約)
            trade.setTradeType(takerOrder.getTradeType());
            // 設定成交時間為當下
            trade.setExecutedAt(LocalDateTime.now());
            // 目前尚未實作手續費邏輯，留空
            tradeRepository.save(trade);

            // 4. 更新訂單狀態 (Update Orders)
            // 更新 Taker 訂單的已成交量與狀態
            updateOrder(takerOrder, matchQty, matchPrice);
            // 更新 Maker 訂單的已成交量與狀態
            updateOrder(makerOrder, matchQty, matchPrice);
            
            // 5. 更新倉位 (僅合約交易) - 目前被註解保留
            /*
            if (takerOrder.getTradeType() == com.exchange.exchange.enums.TradeType.CONTRACT) {
                // 更新 Taker 的倉位
                positionService.processTrade(takerOrder.getMemberId(), takerOrder.getSymbolId(), 
                                             takerOrder.getSide(), matchPrice, matchQty);
                // 更新 Maker 的倉位
                positionService.processTrade(makerOrder.getMemberId(), makerOrder.getSymbolId(), 
                                             makerOrder.getSide(), matchPrice, matchQty);
            } else 
            */
            // 處理現貨交易 (Spot) 的資金結算
            if (takerOrder.getTradeType() == com.exchange.exchange.enums.TradeType.SPOT) {
                // 5b. 現貨交易結算 (Settlement for Spot Trading)
                // 計算本次交易的總金額 (成交價 * 成交量)
                BigDecimal cost = matchPrice.multiply(matchQty);
                
                // 處理 Taker (主動方) 的資金變動
                if (takerOrder.getSide() == OrderSide.BUY) {
                    // Taker 買入：扣除凍結的報價幣 (如 USDT)，獲得基礎幣 (如 BTC)
                    // 注意：這裡直接使用硬編碼字串 "SPOT_BUY_COST" 作為流水類型 [註3]
                    walletService.deductFrozen(takerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_BUY_COST");
                    walletService.addBalance(takerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_BUY_GET");
                } else {
                    // Taker 賣出：扣除凍結的基礎幣 (如 BTC)，獲得報價幣 (如 USDT)
                    walletService.deductFrozen(takerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_SELL_COST");
                    walletService.addBalance(takerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_SELL_GET");
                }

                // 處理 Maker (被動方) 的資金變動
                if (makerOrder.getSide() == OrderSide.BUY) {
                    // Maker 買入 (掛單買)：扣除凍結的報價幣，獲得基礎幣
                    walletService.deductFrozen(makerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_BUY_COST");
                    walletService.addBalance(makerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_BUY_GET");
                } else {
                    // Maker 賣出 (掛單賣)：扣除凍結的基礎幣，獲得報價幣
                    walletService.deductFrozen(makerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_SELL_COST");
                    walletService.addBalance(makerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_SELL_GET");
                }
            }

            // 6. 退還多餘保證金 (Refund Excess Margin) - 針對 Taker 買單
            // 情境：Taker 限價買入出價 50000，但與 Maker 的 49000 成交
            // 系統先前凍結了 50000 * Qty，實際花費 49000 * Qty，需退還差價 (1000 * Qty)
            if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getPrice().compareTo(matchPrice) > 0) {
                // 計算價差
                BigDecimal diff = takerOrder.getPrice().subtract(matchPrice);
                // 計算退款總額
                BigDecimal refund = diff.multiply(matchQty);
                // 呼叫錢包服務解凍資金
                walletService.unfreezeFunds(takerOrder.getMemberId(), symbol.getQuoteCoinId(), refund);
            }
        }
        
        // 迴圈結束後，保存 Taker 訂單的最終狀態
        orderRepository.save(takerOrder);
    }

    // 私有輔助方法：更新訂單的成交數量、累計金額與狀態
    private void updateOrder(Order order, BigDecimal matchQty, BigDecimal matchPrice) {
        // 增加已成交數量
        order.setFilledQuantity(order.getFilledQuantity().add(matchQty));
        
        // 計算本次成交的總金額 (Quote Quantity)
        BigDecimal tradeValue = matchPrice.multiply(matchQty);
        // 獲取當前累計金額，若為 null 則視為 0
        BigDecimal currentCum = order.getCumQuoteQty() != null ? order.getCumQuoteQty() : BigDecimal.ZERO;
        // 更新累計成交金額 (常用於計算平均成交價 = cumQuoteQty / filledQuantity)
        order.setCumQuoteQty(currentCum.add(tradeValue));

        // 判斷訂單狀態
        // 若已成交量 >= 訂單總量，標記為完全成交 (FILLED)
        if (order.getFilledQuantity().compareTo(order.getQuantity()) >= 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            // 否則標記為部分成交 (PARTIAL_FILLED)
            order.setStatus(OrderStatus.PARTIAL_FILLED);
        }
        // 更新最後修改時間
        order.setUpdatedAt(LocalDateTime.now());
        // 保存至資料庫
        orderRepository.save(order);
    }
}

// ====== 備註區 ======
/*
[註1] 事務管理 (@Transactional):
      建議明確指定 rollbackFor 屬性，例如 @Transactional(rollbackFor = Exception.class)。
      預設只會在 RuntimeException 時回滾，若發生 Checked Exception 可能導致資料不一致（例如錢扣了但訂單沒更新）。

[註2] 撮合價格原則:
      這裡實作了標準的價格優先原則。Taker 永遠「吃」Maker 的價格。
      這保證了 Maker 得到他們預期的價格（或更好），而 Taker 則享受到市場當前最優價格。

[註3] 硬編碼字串 (Hardcoded Strings):
      "SPOT_BUY_COST" 等字串散落在程式碼中，維護風險高。
      改進建議：建立一個 TransactionType 枚舉 (Enum) 來管理這些類型，
      例如: walletService.deductFrozen(..., TransactionType.SPOT_BUY_COST);

[註4] 效能隱憂:
      在迴圈內頻繁呼叫 walletService (涉及 DB Update) 與 orderRepository.save 
      會導致大量的資料庫 I/O。
      改進建議：使用 Batch Update (批次更新) 或在迴圈結束後一次性保存所有變更。
*/
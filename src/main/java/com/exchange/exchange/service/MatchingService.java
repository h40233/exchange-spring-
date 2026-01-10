package com.exchange.exchange.service;

// 引入實體：K線、訂單、交易對、成交紀錄
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.entity.Trade;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.Timeframe;
import com.exchange.exchange.repository.CandleRepository;
import com.exchange.exchange.repository.OrderRepository;
import com.exchange.exchange.repository.SymbolRepository;
import com.exchange.exchange.repository.TradeRepository;

// ====== 檔案總結 ======
// MatchingService 實現了交易所的核心撮合引擎邏輯 (Matching Engine)。
// 採用「訂單驅動」模式：當新訂單 (Taker) 進入系統時，主動尋找既有的掛單 (Maker) 進行匹配。
// 職責範圍：
// 1. 搜尋對手單。
// 2. 執行成交 (數量扣除、狀態更新)。
// 3. 產生成交紀錄 (Trades)。
// 4. 觸發資金結算 (錢包餘額變動)。
// 5. 更新市場行情 (K線圖)。
@Service
public class MatchingService {

    // 注入訂單儲存庫：用於查詢對手單與更新訂單狀態
    @Autowired
    private OrderRepository orderRepository;

    // 注入成交紀錄儲存庫：用於保存撮合成功的交易
    @Autowired
    private TradeRepository tradeRepository;

    // 注入錢包服務：用於處理成交後的資產轉移
    @Autowired
    private WalletService walletService;
    
    // 注入交易對儲存庫：用於查詢基礎幣與報價幣設定
    @Autowired
    private SymbolRepository symbolRepository;
    
    // 注入倉位服務：用於合約交易的倉位計算 (目前部分功能保留)
    @Autowired
    private PositionService positionService;

    // 注入 K 線儲存庫：用於更新價格走勢圖
    @Autowired
    private CandleRepository candleRepository;

    // 核心撮合方法：接收一個新進入的訂單 (Taker Order) 並嘗試進行撮合
    // 標註 @Transactional 確保撮合過程中的資料庫變更具有原子性 [註1]
    @Transactional
    public void matchOrder(Order takerOrder) {
        // 防禦性檢查：若傳入的訂單狀態已經結束 (完全成交或已取消)，則不應進行撮合
        if (takerOrder.getStatus() == OrderStatus.FILLED || takerOrder.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        // 定義可作為對手單 (Maker) 的狀態列表
        // 只有「新訂單」或「部分成交」的訂單可以被撮合
        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED);
        // 宣告變數用於存放查詢到的匹配訂單列表
        List<Order> matchingOrders;

        // 步驟 1：根據 Taker 的方向尋找對手單 (Find Matching Orders)
        // 買單 (BUY) -> 尋找賣單 (SELL)
        // 賣單 (SELL) -> 尋找買單 (BUY)
        // 查詢邏輯包含價格優先與時間優先的排序 (由 Repository 實作)
        if (takerOrder.getSide() == OrderSide.BUY) {
            matchingOrders = orderRepository.findMatchingSellOrders(
                    takerOrder.getSymbolId(), OrderSide.SELL, takerOrder.getTradeType(), activeStatuses, takerOrder.getPrice(), takerOrder.getMemberId());
        } else {
            matchingOrders = orderRepository.findMatchingBuyOrders(
                    takerOrder.getSymbolId(), OrderSide.BUY, takerOrder.getTradeType(), activeStatuses, takerOrder.getPrice(), takerOrder.getMemberId());
        }

        // 若沒有找到任何符合價格條件的對手單，撮合結束，該訂單將轉為掛單 (Maker)
        if (matchingOrders.isEmpty()) return;

        // 步驟 2：準備交易對資訊，用於後續的資金結算
        // 使用 orElseThrow 確保資料一致性，若交易對不存在則拋出異常
        Symbol symbol = symbolRepository.findById(takerOrder.getSymbolId()).orElseThrow();
        
        // 步驟 3：遍歷對手單列表，逐一執行撮合
        for (Order makerOrder : matchingOrders) {
            // 若 Taker 訂單已完全成交，則跳出迴圈
            if (takerOrder.getStatus() == OrderStatus.FILLED) break;

            // 計算 Taker 剩餘未成交數量
            BigDecimal takerRemaining = takerOrder.getQuantity().subtract(takerOrder.getFilledQuantity());
            // 計算 Maker 剩餘未成交數量
            BigDecimal makerRemaining = makerOrder.getQuantity().subtract(makerOrder.getFilledQuantity());
            
            // 本次撮合數量 = 兩者剩餘數量的最小值
            BigDecimal matchQty = takerRemaining.min(makerRemaining);

            // 防禦檢查：若計算出的撮合量小於等於 0，跳過此筆 (理論上不應發生)
            if (matchQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 成交價格以 Maker (掛單) 的價格為準 [註2]
            BigDecimal matchPrice = makerOrder.getPrice();

            // 步驟 4：建立並儲存成交紀錄 (Trade)
            Trade trade = new Trade();
            trade.setSymbolId(takerOrder.getSymbolId());
            trade.setTakerOrderId(takerOrder.getOrderId());
            trade.setMakerOrderId(makerOrder.getOrderId());
            trade.setPrice(matchPrice);
            trade.setQuantity(matchQty);
            trade.setTakerSide(takerOrder.getSide()); // 記錄是誰主動發起的方向
            trade.setTradeType(takerOrder.getTradeType());
            trade.setExecutedAt(LocalDateTime.now());
            // 待辦事項：此處尚未實作手續費 (Fee) 的計算邏輯
            tradeRepository.save(trade);

            // 步驟 5：更新 K 線數據 (即時反映最新成交價)
            updateCandles(trade);

            // 步驟 6：更新雙方訂單的狀態與成交量
            updateOrder(takerOrder, matchQty, matchPrice);
            updateOrder(makerOrder, matchQty, matchPrice);
            
            // (保留區塊) 合約交易的倉位處理邏輯
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
            
            // 步驟 7：現貨交易 (Spot) 的資金結算 (Settlement)
            // 根據「一手交錢，一手交貨」原則更新雙方錢包
            if (takerOrder.getTradeType() == com.exchange.exchange.enums.TradeType.SPOT) {
                // 計算總成交金額 (Cost) = 價格 * 數量
                BigDecimal cost = matchPrice.multiply(matchQty);
                
                // 處理 Taker 的資產變動
                if (takerOrder.getSide() == OrderSide.BUY) {
                    // Taker 買入：扣除凍結的報價幣 (USDT)，增加基礎幣 (BTC)
                    walletService.deductFrozen(takerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_BUY_COST");
                    walletService.addBalance(takerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_BUY_GET");
                } else {
                    // Taker 賣出：扣除凍結的基礎幣 (BTC)，增加報價幣 (USDT)
                    walletService.deductFrozen(takerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_SELL_COST");
                    walletService.addBalance(takerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_SELL_GET");
                }

                // 處理 Maker 的資產變動
                if (makerOrder.getSide() == OrderSide.BUY) {
                    // Maker 買入 (掛買單被吃)：扣除凍結的報價幣，增加基礎幣
                    walletService.deductFrozen(makerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_BUY_COST");
                    walletService.addBalance(makerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_BUY_GET");
                } else {
                    // Maker 賣出 (掛賣單被吃)：扣除凍結的基礎幣，增加報價幣
                    walletService.deductFrozen(makerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_SELL_COST");
                    walletService.addBalance(makerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_SELL_GET");
                }
            }

            // 步驟 8：處理 Taker 的多餘保證金退款 (Refund)
            // 情境：當 Taker 用較高限價買入較低價格的 Maker 單時，會有價差餘額
            // 例如：限價 50,000 買入，但撮合到 49,000 的賣單，需退還 (50,000 - 49,000) * 數量的凍結資金
            if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getPrice().compareTo(matchPrice) > 0) {
                BigDecimal diff = takerOrder.getPrice().subtract(matchPrice);
                BigDecimal refund = diff.multiply(matchQty);
                walletService.unfreezeFunds(takerOrder.getMemberId(), symbol.getQuoteCoinId(), refund);
            }
        }
        
        // 迴圈結束後，保存 Taker 訂單的最終狀態
        orderRepository.save(takerOrder);
    }

    // 私有方法：觸發更新多個時間週期的 K 線
    private void updateCandles(Trade trade) {
        // 更新 1分鐘、1小時、1天 的 K 線數據
        updateCandleForTimeframe(trade, Timeframe._1m);
        updateCandleForTimeframe(trade, Timeframe._1H);
        updateCandleForTimeframe(trade, Timeframe._1D);
    }

    // 私有方法：針對特定週期更新 K 線資料
    private void updateCandleForTimeframe(Trade trade, Timeframe timeframe) {
        // 計算這筆成交屬於哪一個時間區間 (例如 10:05:30 的成交屬於 10:05:00 的 1分K)
        LocalDateTime openTime = truncateTime(trade.getExecutedAt(), timeframe);
        // 建立複合主鍵物件
        com.exchange.exchange.entity.key.CandleId candleId = new com.exchange.exchange.entity.key.CandleId(trade.getSymbolId(), timeframe, openTime);

        // 查詢是否存在該時間點的 K 線
        Optional<Candle> candleOpt = candleRepository.findById(candleId);
        Candle candle;

        if (candleOpt.isPresent()) {
            // 若存在，則更新 High, Low, Close
            candle = candleOpt.get();
            // 如果成交價高於目前最高價，更新 High
            if (trade.getPrice().compareTo(candle.getHigh()) > 0) {
                candle.setHigh(trade.getPrice());
            }
            // 如果成交價低於目前最低價，更新 Low
            if (trade.getPrice().compareTo(candle.getLow()) < 0) {
                candle.setLow(trade.getPrice());
            }
            // 更新收盤價為最新成交價
            candle.setClose(trade.getPrice());
        } else {
            // 若不存在，則建立新的 K 線 (Open, High, Low, Close 皆設為當前成交價)
            candle = new Candle();
            candle.setSymbolId(trade.getSymbolId());
            candle.setTimeframe(timeframe);
            candle.setOpenTime(openTime);
            candle.setOpen(trade.getPrice());
            candle.setHigh(trade.getPrice());
            candle.setLow(trade.getPrice());
            candle.setClose(trade.getPrice());
            
            // 計算該 K 線的結束時間
            candle.setCloseTime(calculateCloseTime(openTime, timeframe));
        }
        // 儲存 K 線資料
        candleRepository.save(candle);
    }

    // 輔助方法：將時間截斷至週期的起始點 (Time Truncation)
    private LocalDateTime truncateTime(LocalDateTime time, Timeframe timeframe) {
        switch (timeframe) {
            case _1m: return time.truncatedTo(ChronoUnit.MINUTES);
            case _5m: 
                // 將分鐘數正規化為 5 的倍數
                int minute5 = time.getMinute() / 5 * 5;
                return time.truncatedTo(ChronoUnit.HOURS).plusMinutes(minute5);
            case _1H: return time.truncatedTo(ChronoUnit.HOURS);
            case _1D: return time.truncatedTo(ChronoUnit.DAYS);
            default: return time.truncatedTo(ChronoUnit.MINUTES);
        }
    }

    // 輔助方法：計算週期的結束時間
    private LocalDateTime calculateCloseTime(LocalDateTime openTime, Timeframe timeframe) {
        switch (timeframe) {
            case _1m: return openTime.plusMinutes(1).minusSeconds(1);
            case _5m: return openTime.plusMinutes(5).minusSeconds(1);
            case _1H: return openTime.plusHours(1).minusSeconds(1);
            case _1D: return openTime.plusDays(1).minusSeconds(1);
            default: return openTime.plusMinutes(1).minusSeconds(1);
        }
    }

    // 私有輔助方法：更新訂單的累積數據與狀態
    private void updateOrder(Order order, BigDecimal matchQty, BigDecimal matchPrice) {
        // 累加已成交數量
        order.setFilledQuantity(order.getFilledQuantity().add(matchQty));
        
        // 計算本次成交的總金額
        BigDecimal tradeValue = matchPrice.multiply(matchQty);
        // 累加訂單的總成交金額 (用於計算均價)
        BigDecimal currentCum = order.getCumQuoteQty() != null ? order.getCumQuoteQty() : BigDecimal.ZERO;
        order.setCumQuoteQty(currentCum.add(tradeValue));

        // 判斷狀態：若已成交數量 >= 委託數量，則標記為 FILLED (完全成交)
        if (order.getFilledQuantity().compareTo(order.getQuantity()) >= 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            // 否則標記為 PARTIAL_FILLED (部分成交)
            order.setStatus(OrderStatus.PARTIAL_FILLED);
        }
        // 更新最後修改時間
        order.setUpdatedAt(LocalDateTime.now());
        // 寫入資料庫
        orderRepository.save(order);
    }
}

// ====== 備註區 ======
/*
[註1] 效能瓶頸 (Performance Bottleneck):
      將撮合邏輯放在 `@Transactional` 內雖然保證了資料一致性，但這意味著撮合期間會鎖定相關的資料庫資源。
      如果 Taker 訂單很大，需要匹配數百個 Maker 訂單，這個事務會執行很久，導致資料庫鎖競爭 (Lock Contention)。
      優化建議：改用記憶體撮合引擎 (In-Memory Matching Engine)，撮合完成後再一次性批量寫入資料庫。

[註2] 價格決定原則 (Price Determination):
      這裡正確實作了交易所的標準規則：成交價由 Maker (先掛單者) 決定。
      這保證了 Taker 永遠是以「優於或等於」預期的價格成交 (買得更便宜，賣得更貴)。
*/
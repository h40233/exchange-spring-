package com.exchange.exchange.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    // 注入 CandleRepository，用於更新 K 線資料
    @Autowired
    private CandleRepository candleRepository;

    // 核心撮合方法：接收一個主動單 (Taker Order) 並嘗試撮合
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
        if (takerOrder.getSide() == OrderSide.BUY) {
            matchingOrders = orderRepository.findMatchingSellOrders(
                    takerOrder.getSymbolId(), OrderSide.SELL, takerOrder.getTradeType(), activeStatuses, takerOrder.getPrice(), takerOrder.getMemberId());
        } else {
            matchingOrders = orderRepository.findMatchingBuyOrders(
                    takerOrder.getSymbolId(), OrderSide.BUY, takerOrder.getTradeType(), activeStatuses, takerOrder.getPrice(), takerOrder.getMemberId());
        }

        // 若沒有找到任何匹配的訂單，撮合結束
        if (matchingOrders.isEmpty()) return;

        // 2. 迭代並執行撮合
        Symbol symbol = symbolRepository.findById(takerOrder.getSymbolId()).orElseThrow();
        
        for (Order makerOrder : matchingOrders) {
            if (takerOrder.getStatus() == OrderStatus.FILLED) break;

            BigDecimal takerRemaining = takerOrder.getQuantity().subtract(takerOrder.getFilledQuantity());
            BigDecimal makerRemaining = makerOrder.getQuantity().subtract(makerOrder.getFilledQuantity());
            BigDecimal matchQty = takerRemaining.min(makerRemaining);

            if (matchQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal matchPrice = makerOrder.getPrice();

            // 3. 建立成交紀錄
            Trade trade = new Trade();
            trade.setSymbolId(takerOrder.getSymbolId());
            trade.setTakerOrderId(takerOrder.getOrderId());
            trade.setMakerOrderId(makerOrder.getOrderId());
            trade.setPrice(matchPrice);
            trade.setQuantity(matchQty);
            trade.setTakerSide(takerOrder.getSide());
            trade.setTradeType(takerOrder.getTradeType());
            trade.setExecutedAt(LocalDateTime.now());
            // 目前尚未實作手續費邏輯
            tradeRepository.save(trade);

            // 更新 K 線 (Candles)
            updateCandles(trade);

            // 4. 更新訂單狀態
            updateOrder(takerOrder, matchQty, matchPrice);
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
                BigDecimal cost = matchPrice.multiply(matchQty);
                
                if (takerOrder.getSide() == OrderSide.BUY) {
                    walletService.deductFrozen(takerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_BUY_COST");
                    walletService.addBalance(takerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_BUY_GET");
                } else {
                    walletService.deductFrozen(takerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_SELL_COST");
                    walletService.addBalance(takerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_SELL_GET");
                }

                if (makerOrder.getSide() == OrderSide.BUY) {
                    walletService.deductFrozen(makerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_BUY_COST");
                    walletService.addBalance(makerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_BUY_GET");
                } else {
                    walletService.deductFrozen(makerOrder.getMemberId(), symbol.getBaseCoinId(), matchQty, "SPOT_SELL_COST");
                    walletService.addBalance(makerOrder.getMemberId(), symbol.getQuoteCoinId(), cost, "SPOT_SELL_GET");
                }
            }

            // 6. 退還多餘保證金
            if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getPrice().compareTo(matchPrice) > 0) {
                BigDecimal diff = takerOrder.getPrice().subtract(matchPrice);
                BigDecimal refund = diff.multiply(matchQty);
                walletService.unfreezeFunds(takerOrder.getMemberId(), symbol.getQuoteCoinId(), refund);
            }
        }
        
        orderRepository.save(takerOrder);
    }

    private void updateCandles(Trade trade) {
        // 更新不同週期的 K 線
        updateCandleForTimeframe(trade, Timeframe._1m);  // 1分鐘
        updateCandleForTimeframe(trade, Timeframe._1H);  // 1小時
        updateCandleForTimeframe(trade, Timeframe._1D);  // 1天
    }

    private void updateCandleForTimeframe(Trade trade, Timeframe timeframe) {
        LocalDateTime openTime = truncateTime(trade.getExecutedAt(), timeframe);
        com.exchange.exchange.entity.key.CandleId candleId = new com.exchange.exchange.entity.key.CandleId(trade.getSymbolId(), timeframe, openTime);

        Optional<Candle> candleOpt = candleRepository.findById(candleId);
        Candle candle;

        if (candleOpt.isPresent()) {
            candle = candleOpt.get();
            // 更新最高價
            if (trade.getPrice().compareTo(candle.getHigh()) > 0) {
                candle.setHigh(trade.getPrice());
            }
            // 更新最低價
            if (trade.getPrice().compareTo(candle.getLow()) < 0) {
                candle.setLow(trade.getPrice());
            }
            // 更新收盤價
            candle.setClose(trade.getPrice());
        } else {
            candle = new Candle();
            candle.setSymbolId(trade.getSymbolId());
            candle.setTimeframe(timeframe);
            candle.setOpenTime(openTime);
            candle.setOpen(trade.getPrice());
            candle.setHigh(trade.getPrice());
            candle.setLow(trade.getPrice());
            candle.setClose(trade.getPrice());
            
            candle.setCloseTime(calculateCloseTime(openTime, timeframe));
        }
        candleRepository.save(candle);
    }

    private LocalDateTime truncateTime(LocalDateTime time, Timeframe timeframe) {
        switch (timeframe) {
            case _1m: return time.truncatedTo(ChronoUnit.MINUTES);
            case _5m: 
                int minute5 = time.getMinute() / 5 * 5;
                return time.truncatedTo(ChronoUnit.HOURS).plusMinutes(minute5);
            case _1H: return time.truncatedTo(ChronoUnit.HOURS);
            case _1D: return time.truncatedTo(ChronoUnit.DAYS);
            default: return time.truncatedTo(ChronoUnit.MINUTES);
        }
    }

    private LocalDateTime calculateCloseTime(LocalDateTime openTime, Timeframe timeframe) {
        switch (timeframe) {
            case _1m: return openTime.plusMinutes(1).minusSeconds(1);
            case _5m: return openTime.plusMinutes(5).minusSeconds(1);
            case _1H: return openTime.plusHours(1).minusSeconds(1);
            case _1D: return openTime.plusDays(1).minusSeconds(1);
            default: return openTime.plusMinutes(1).minusSeconds(1);
        }
    }

    // 私有輔助方法：更新訂單的成交數量、累計金額與狀態
    private void updateOrder(Order order, BigDecimal matchQty, BigDecimal matchPrice) {
        order.setFilledQuantity(order.getFilledQuantity().add(matchQty));
        
        BigDecimal tradeValue = matchPrice.multiply(matchQty);
        BigDecimal currentCum = order.getCumQuoteQty() != null ? order.getCumQuoteQty() : BigDecimal.ZERO;
        order.setCumQuoteQty(currentCum.add(tradeValue));

        if (order.getFilledQuantity().compareTo(order.getQuantity()) >= 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIAL_FILLED);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
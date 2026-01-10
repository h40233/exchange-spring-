package com.exchange.exchange.service;

// 引入實體與存取層
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.SymbolRepository;
import com.exchange.exchange.repository.TradeRepository;

// ====== 檔案總結 ======
// SymbolService 負責管理交易所的「幣種」與「交易對」。
// 核心功能：
// 1. 自動發現交易對 (Auto-Discovery)：根據現有幣種自動建立對應 USDT 的交易對。
// 2. 行情查詢 (Ticker)：查詢所有交易對的最新成交價格。
@Service
public class SymbolService {

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private TradeRepository tradeRepository;

    // 方法：獲取所有可交易的幣種列表，並自動初始化交易對
    // [註1] 注意：此方法包含寫入邏輯 (Side Effect)
    @Transactional
    public List<String> getAllTradableCoins() {
        List<Coin> allCoins = coinRepository.findAll();
        List<String> tradableCoins = new ArrayList<>();

        for (Coin coin : allCoins) {
            String coinId = coin.getCoinId();
            
            // 跳過 USDT 本身，因為我們將其作為計價單位 (Quote Currency)
            if ("USDT".equalsIgnoreCase(coinId)) {
                tradableCoins.add(coinId); 
                continue;
            }

            // 建構交易對 ID，規則為：[幣種]USDT (例如 BTCUSDT)
            String symbolId = coinId + "USDT";
            
            // 自動初始化：若該交易對不存在，則自動建立
            if (!symbolRepository.existsById(symbolId)) {
                Symbol newSymbol = new Symbol();
                newSymbol.setSymbolId(symbolId);
                newSymbol.setName(coinId + "/USDT"); // 顯示名稱
                newSymbol.setBaseCoinId(coinId);     // 基礎幣
                newSymbol.setQuoteCoinId("USDT");    // 報價幣
                symbolRepository.save(newSymbol);
            }
            
            tradableCoins.add(coinId);
        }
        
        return tradableCoins;
    }

    // 方法：獲取所有幣種對 USDT 的最新價格 (Tickers)
    // 回傳 Map: Key=幣種 (e.g. BTC), Value=最新價格
    public Map<String, BigDecimal> getCoinPricesInUsdt() {
        Map<String, BigDecimal> prices = new HashMap<>();
        // 先確保交易對都已初始化
        List<String> coins = getAllTradableCoins();

        for (String coin : coins) {
            // USDT 對 USDT 價格恆為 1
            if ("USDT".equalsIgnoreCase(coin)) {
                prices.put(coin, BigDecimal.ONE);
            } else {
                String symbolId = coin + "USDT";
                // 查詢該交易對的「最新一筆成交紀錄」的價格
                // 若無任何成交紀錄，則價格預設為 0
                prices.put(coin, tradeRepository.findTopBySymbolIdOrderByTradeIdDesc(symbolId)
                        .map(com.exchange.exchange.entity.Trade::getPrice)
                        .orElse(BigDecimal.ZERO));
            }
        }
        return prices;
    }
}

// ====== 備註區 ======
/*
[註1] 職責分離 (Separation of Concerns):
      Getter 方法 (`getAllTradableCoins`) 中包含「建立資料」的邏輯是不推薦的設計。
      這會導致每次查詢都進行不必要的檢查與潛在的寫入。
      改進建議：將初始化邏輯移至 `ExchangeApplication.java` 的啟動腳本或專門的 Admin API 中。

[註2] N+1 查詢效能問題 (N+1 Problem):
      `getCoinPricesInUsdt` 在迴圈中對每個幣種執行一次 SQL 查詢 (`findTopBy...`)。
      若幣種數量增加，效能將急劇下降。
      改進建議：使用 SQL 聚合查詢一次取出所有幣對的最新價格，
      例如: `SELECT symbolId, price FROM trades WHERE (symbolId, tradesID) IN ...`
*/
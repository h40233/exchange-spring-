package com.exchange.exchange.service;

// 引入實體與存取層
import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.SymbolRepository;
import com.exchange.exchange.repository.TradeRepository;
// 引入 Spring 工具
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ====== 檔案總結 ======
// SymbolService 負責管理交易所的「幣種」與「交易對」。
// 包含自動化初始化交易對 (Auto-Discovery) 與查詢最新成交價 (Ticker) 的功能。
@Service
public class SymbolService {

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private TradeRepository tradeRepository;

    // 方法：獲取所有可交易的幣種列表，並自動初始化交易對
    // 此邏輯有點特殊：它假設除了 USDT 以外的所有幣種，都應該有一個對應 USDT 的交易對。
    @Transactional
    public List<String> getAllTradableCoins() {
        List<Coin> allCoins = coinRepository.findAll();
        List<String> tradableCoins = new ArrayList<>();

        for (Coin coin : allCoins) {
            String coinId = coin.getCoinId();
            
            // 跳過 USDT 本身，因為我們是將其作為計價單位 (Quote Currency)
            if ("USDT".equalsIgnoreCase(coinId)) {
                tradableCoins.add(coinId); // 雖然加入列表，但後續邏輯主要針對 Pair
                continue;
            }

            // 建構交易對 ID，規則為：[幣種]USDT (例如 BTCUSDT)
            String symbolId = coinId + "USDT";
            
            // 自動初始化：若該交易對不存在，則自動建立 [註1]
            if (!symbolRepository.existsById(symbolId)) {
                Symbol newSymbol = new Symbol();
                newSymbol.setSymbolId(symbolId);
                newSymbol.setName(coinId + "/USDT"); // 顯示名稱
                newSymbol.setBaseCoinId(coinId);     // 基礎幣 (被交易的商品)
                newSymbol.setQuoteCoinId("USDT");    // 報價幣 (計價單位)
                symbolRepository.save(newSymbol);
            }
            
            tradableCoins.add(coinId);
        }
        
        return tradableCoins;
    }

    // 方法：獲取所有幣種對 USDT 的最新價格 (Tickers)
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
      Getter 方法 (`getAllTradableCoins`) 中包含「寫入/建立」邏輯 (Side Effect) 是不推薦的設計。
      這會導致每次查詢都進行不必要的檢查與潛在的寫入。
      改進建議：將初始化邏輯移至 `ExchangeApplication.java` 的啟動腳本或專門的 Admin API 中。

[註2] N+1 查詢問題:
      `getCoinPricesInUsdt` 在迴圈中對每個幣種執行一次 SQL 查詢 (`findTopBy...`)。
      若幣種數量增加，效能將急劇下降。
      改進建議：使用 SQL 聚合查詢一次取出所有幣對的最新價格，
      例如: SELECT symbolId, price FROM trades WHERE (symbolId, tradesID) IN ...
*/
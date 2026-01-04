package com.exchange.exchange.service;

import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.SymbolRepository;
import com.exchange.exchange.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SymbolService {

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Transactional
    public List<String> getAllTradableCoins() {
        List<Coin> allCoins = coinRepository.findAll();
        List<String> tradableCoins = new ArrayList<>();

        for (Coin coin : allCoins) {
            String coinId = coin.getCoinId();
            
            // Skip USDT itself as we are pairing against USDT
            if ("USDT".equalsIgnoreCase(coinId)) {
                tradableCoins.add(coinId); // Include USDT so it can be used for wallet display if needed, but logic below assumes pairs
                continue;
            }

            String symbolId = coinId + "USDT";
            
            // Check if symbol exists, if not create it
            if (!symbolRepository.existsById(symbolId)) {
                Symbol newSymbol = new Symbol();
                newSymbol.setSymbolId(symbolId);
                newSymbol.setName(coinId + "/USDT");
                newSymbol.setBaseCoinId(coinId);
                newSymbol.setQuoteCoinId("USDT");
                symbolRepository.save(newSymbol);
            }
            
            tradableCoins.add(coinId);
        }
        
        // Ensure USDT is in the list if it wasn't added (though the loop adds it)
        // But for the dropdown "Trade Symbol", we usually filter out USDT in frontend.
        // We will return ALL coins, and let frontend filter.
        return tradableCoins;
    }

    public Map<String, BigDecimal> getCoinPricesInUsdt() {
        Map<String, BigDecimal> prices = new HashMap<>();
        List<String> coins = getAllTradableCoins();

        for (String coin : coins) {
            if ("USDT".equalsIgnoreCase(coin)) {
                prices.put(coin, BigDecimal.ONE);
            } else {
                String symbolId = coin + "USDT";
                prices.put(coin, tradeRepository.findTopBySymbolIdOrderByTradeIdDesc(symbolId)
                        .map(com.exchange.exchange.entity.Trade::getPrice)
                        .orElse(BigDecimal.ZERO));
            }
        }
        return prices;
    }
}

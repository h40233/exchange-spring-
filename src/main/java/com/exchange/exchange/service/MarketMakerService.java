package com.exchange.exchange.service;

import com.exchange.exchange.dto.OrderRequest;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderType;
import com.exchange.exchange.enums.TradeType;
import com.exchange.exchange.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;

import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.SymbolRepository;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.entity.Coin;
import java.util.List;

@Service
public class MarketMakerService {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private WalletService walletService;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private CoinRepository coinRepository;
    
    // 寫死機器人 Member ID = 1 (需與 ExchangeApplication 初始化邏輯一致)
    private static final Integer BOT_MEMBER_ID = 1;

    private final Random random = new Random();

    // 每 5 秒執行一次造市邏輯
    @Scheduled(fixedRate = 5000)
    public void performMarketMaking() {
        try {
            ensureBotFunds(); // 確保機器人有錢
            
            List<Symbol> symbols = symbolRepository.findAll();
            for (Symbol s : symbols) {
                String symbolId = s.getSymbolId();
                
                // 1. 從 Binance 獲取當前價格
                BigDecimal currentPrice = fetchBinancePrice(symbolId);
                if (currentPrice == null) continue;

                // 2. 隨機放置買單與賣單 (模擬掛單)
                placeRandomOrders(symbolId, currentPrice);
            }
        } catch (Exception e) {
            System.err.println("Market Maker Error: " + e.getMessage());
        }
    }

    private void ensureBotFunds() {
        // 機器人每次執行都補滿所有幣種的錢包
        try {
            List<Coin> coins = coinRepository.findAll();
            for (Coin coin : coins) {
                // USDT 給多一點，其他幣種給適量
                BigDecimal amount = coin.getCoinId().equals("USDT") ? 
                                    new BigDecimal("1000000") : new BigDecimal("10000");
                walletService.deposit(BOT_MEMBER_ID, coin.getCoinId(), amount);
            }
        } catch (Exception e) {
            // Ignore errors during funding (e.g. concurrent updates)
        }
    }

    private BigDecimal fetchBinancePrice(String symbol) {
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + symbol;
        try {
            // 回傳格式: {"symbol":"BTCUSDT","price":"95000.00000000"}
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("price")) {
                return new BigDecimal(response.get("price"));
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch price from Binance for " + symbol);
        }
        return null;
    }

    private void placeRandomOrders(String symbol, BigDecimal centerPrice) {
        // 價格波動範圍 0.1% ~ 0.5%
        
        // 放置 2 個買單 (低於市價)
        for (int i = 0; i < 2; i++) {
            BigDecimal price = centerPrice.multiply(BigDecimal.ONE.subtract(randomPct(0.001, 0.005)));
            BigDecimal qty = randomQty(centerPrice);
            placeOrder(symbol, OrderSide.BUY, price, qty);
        }

        // 放置 2 個賣單 (高於市價)
        for (int i = 0; i < 2; i++) {
            BigDecimal price = centerPrice.multiply(BigDecimal.ONE.add(randomPct(0.001, 0.005)));
            BigDecimal qty = randomQty(centerPrice);
            placeOrder(symbol, OrderSide.SELL, price, qty);
        }
    }

    private BigDecimal randomPct(double min, double max) {
        double val = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(val);
    }
    
    // 動態計算下單數量：目標每單價值約 10 ~ 100 USDT
    private BigDecimal randomQty(BigDecimal currentPrice) {
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE;
        
        // 隨機目標價值: 10 ~ 100 USDT
        double targetValue = 10 + (90 * random.nextDouble());
        
        // 數量 = 目標價值 / 當前價格
        BigDecimal qty = BigDecimal.valueOf(targetValue).divide(currentPrice, 4, RoundingMode.HALF_UP);
        
        // 確保最小數量不為 0
        if (qty.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("0.0001");
        
        return qty;
    }

    private void placeOrder(String symbolId, OrderSide side, BigDecimal price, BigDecimal quantity) {
        OrderRequest req = new OrderRequest();
        req.setSymbolId(symbolId);
        req.setSide(side);
        req.setType(OrderType.LIMIT);
        req.setTradeType(TradeType.SPOT);
        req.setPrice(price.setScale(8, RoundingMode.HALF_UP)); // 價格取小數 8 位，避免低價幣精度不足
        req.setQuantity(quantity);

        try {
            orderService.createOrder(BOT_MEMBER_ID, req);
            // System.out.println("Bot placed order: " + side + " " + symbolId + " @ " + price);
        } catch (Exception e) {
            // System.err.println("Bot order failed: " + e.getMessage());
        }
    }
}

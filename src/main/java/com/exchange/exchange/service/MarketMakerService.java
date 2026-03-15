package com.exchange.exchange.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.exchange.exchange.dto.OrderRequest;
import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Order;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderStatus;
import com.exchange.exchange.enums.OrderType;
import com.exchange.exchange.enums.TradeType;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.OrderRepository;
import com.exchange.exchange.repository.SymbolRepository;

@Service
public class MarketMakerService {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private WalletService walletService;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private CoinRepository coinRepository;
    
    private static final Integer BOT_MEMBER_ID = 1;

    private final Random random = new Random();

    @Scheduled(fixedRate = 5000)
    public void performMarketMaking() {
        try {
            // 步驟 1：確保機器人帳戶有足夠資金進行掛單
            ensureBotFunds(); 
            
            // 步驟 2：獲取系統中所有啟用的交易對
            List<Symbol> symbols = symbolRepository.findAll();
            
            // 步驟 3：遍歷每個交易對，分別進行造市
            for (Symbol s : symbols) {
                String symbolId = s.getSymbolId();
                
                // 步驟 4：呼叫外部 API 獲取該幣對的當前市價
                BigDecimal currentPrice = fetchBinancePrice(symbolId);
                
                // 若無法獲取價格，則跳過此幣對
                if (currentPrice == null) continue;

                // 步驟 5：管理現有訂單 (保留最接近市價的 5 檔，撤銷其餘)
                manageBotOrders(symbolId);

                // 步驟 6：根據當前價格，計算並放置隨機的買單與賣單
                placeRandomOrders(symbolId, currentPrice);
            }
        } catch (Exception e) {
            System.err.println("Market Maker Error: " + e.getMessage());
        }
    }

    // 管理機器人訂單：保留最優的 5 筆，其餘取消
    private void manageBotOrders(String symbolId) {
        try {
            List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.PARTIAL_FILLED);
            // 查詢該幣對下機器人的所有活躍訂單
            List<Order> orders = orderRepository.findByMemberIdAndSymbolIdAndStatusIn(BOT_MEMBER_ID, symbolId, activeStatuses);
            
            List<Order> bids = new ArrayList<>();
            List<Order> asks = new ArrayList<>();
            
            for (Order o : orders) {
                if (o.getSide() == OrderSide.BUY) bids.add(o);
                else if (o.getSide() == OrderSide.SELL) asks.add(o);
            }
            
            // 買單排序：價格由高到低 (越高越接近市價/越優)
            bids.sort((o1, o2) -> o2.getPrice().compareTo(o1.getPrice()));
            
            // 賣單排序：價格由低到高 (越低越接近市價/越優)
            asks.sort((o1, o2) -> o1.getPrice().compareTo(o2.getPrice()));
            
            // 保留前 5 筆，撤銷其餘
            removeExtraOrders(bids, 5);
            removeExtraOrders(asks, 5);
            
        } catch (Exception e) {
            System.err.println("Failed to manage bot orders for " + symbolId + ": " + e.getMessage());
        }
    }

    private void removeExtraOrders(List<Order> sortedOrders, int keepCount) {
        if (sortedOrders.size() <= keepCount) return;
        
        for (int i = keepCount; i < sortedOrders.size(); i++) {
            Order order = sortedOrders.get(i);
            try {
                orderService.cancelOrder(BOT_MEMBER_ID, order.getOrderId());
            } catch (Exception e) {
                // 忽略取消失敗 (可能已成交)
            }
        }
    }

    private void ensureBotFunds() {
        try {
            List<Coin> coins = coinRepository.findAll();
            for (Coin coin : coins) {
                BigDecimal amount = coin.getCoinId().equals("USDT") ? 
                                    new BigDecimal("1000000") : new BigDecimal("10000");
                walletService.deposit(BOT_MEMBER_ID, coin.getCoinId(), amount);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private BigDecimal fetchBinancePrice(String symbol) {
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + symbol;
        try {
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
        // 放置 5 筆買單
        for (int i = 0; i < 5; i++) {
            BigDecimal price = centerPrice.multiply(BigDecimal.ONE.subtract(randomPct(0.0001, 0.001)));
            BigDecimal qty = randomQty(centerPrice);
            placeOrder(symbol, OrderSide.BUY, price, qty);
        }

        // 放置 5 筆賣單
        for (int i = 0; i < 5; i++) {
            BigDecimal price = centerPrice.multiply(BigDecimal.ONE.add(randomPct(0.0001, 0.001)));
            BigDecimal qty = randomQty(centerPrice);
            placeOrder(symbol, OrderSide.SELL, price, qty);
        }
    }

    private BigDecimal randomPct(double min, double max) {
        double val = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(val);
    }
    
    private BigDecimal randomQty(BigDecimal currentPrice) {
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE;
        double targetValue = 10 + (90 * random.nextDouble());
        BigDecimal qty = BigDecimal.valueOf(targetValue).divide(currentPrice, 4, RoundingMode.HALF_UP);
        if (qty.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("0.0001");
        return qty;
    }

    private void placeOrder(String symbolId, OrderSide side, BigDecimal price, BigDecimal quantity) {
        OrderRequest req = new OrderRequest();
        req.setSymbolId(symbolId);
        req.setSide(side);
        req.setType(OrderType.LIMIT); 
        req.setTradeType(TradeType.SPOT); 
        req.setPrice(price.setScale(8, RoundingMode.HALF_UP)); 
        req.setQuantity(quantity);

        try {
            orderService.createOrder(BOT_MEMBER_ID, req);
        } catch (Exception e) {
            // ignore
        }
    }
}

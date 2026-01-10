package com.exchange.exchange.service;

// 引入 DTO，用於封裝下單請求的參數
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.exchange.exchange.dto.OrderRequest;
import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.enums.OrderSide;
import com.exchange.exchange.enums.OrderType;
import com.exchange.exchange.enums.TradeType;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.SymbolRepository;

// ====== 檔案總結 ======
// MarketMakerService 扮演自動化造市機器人 (Market Maker Bot) 的角色。
// 核心職責：
// 1. 定時從外部交易所 (如 Binance) 獲取最新參考價格。
// 2. 確保機器人帳戶擁有足夠的資金。
// 3. 在參考價格上下隨機掛出買單與賣單，為交易所提供流動性與深度。
@Service
public class MarketMakerService {

    // 注入訂單服務，用於執行下單操作
    @Autowired
    private OrderService orderService;
    
    // 注入 HTTP 客戶端工具，用於呼叫外部行情 API
    @Autowired
    private RestTemplate restTemplate;
    
    // 注入錢包服務，用於管理機器人的資金 (存款/重置)
    @Autowired
    private WalletService walletService;

    // 注入交易對儲存庫，用於獲取系統支援的所有交易對
    @Autowired
    private SymbolRepository symbolRepository;

    // 注入幣種儲存庫，用於遍歷所有幣種進行資金補充
    @Autowired
    private CoinRepository coinRepository;
    
    // 定義機器人的會員 ID 常數
    // 注意：此 ID 必須與系統初始化時建立的機器人帳號一致
    private static final Integer BOT_MEMBER_ID = 1;

    // 實例化隨機數產生器，用於價格波動與數量的隨機化
    private final Random random = new Random();

    // 定時任務方法：執行造市邏輯
    // 設定為每 5000 毫秒 (5秒) 執行一次
    @Scheduled(fixedRate = 5000)
    public void performMarketMaking() {
        try {
            // 步驟 1：確保機器人帳戶有足夠資金進行掛單 [註1]
            ensureBotFunds(); 
            
            // 步驟 2：獲取系統中所有啟用的交易對
            List<Symbol> symbols = symbolRepository.findAll();
            
            // 步驟 3：遍歷每個交易對，分別進行造市
            for (Symbol s : symbols) {
                // 取得交易對 ID (例如 "BTCUSDT")
                String symbolId = s.getSymbolId();
                
                // 步驟 4：呼叫外部 API 獲取該幣對的當前市價
                BigDecimal currentPrice = fetchBinancePrice(symbolId);
                
                // 若無法獲取價格 (可能因為網絡問題或幣對不存在於 Binance)，則跳過此幣對
                if (currentPrice == null) continue;

                // 步驟 5：根據當前價格，計算並放置隨機的買單與賣單
                placeRandomOrders(symbolId, currentPrice);
            }
        } catch (Exception e) {
            // 捕捉並記錄執行過程中的任何未預期錯誤，避免整個排程崩潰
            System.err.println("Market Maker Error: " + e.getMessage());
        }
    }

    // 私有方法：確保機器人資金充裕
    // 每次執行造市前都會無條件補充資金，確保不會因餘額不足導致下單失敗
    private void ensureBotFunds() {
        try {
            // 獲取所有幣種列表
            List<Coin> coins = coinRepository.findAll();
            for (Coin coin : coins) {
                // 根據幣種判斷補充金額
                // 若為 USDT (計價幣)，給予較大額度 (1,000,000) 以支援買單
                // 其他幣種 (基礎幣)，給予適量額度 (10,000) 以支援賣單
                BigDecimal amount = coin.getCoinId().equals("USDT") ? 
                                    new BigDecimal("1000000") : new BigDecimal("10000");
                                    
                // 呼叫錢包服務進行入金 (Deposit)
                walletService.deposit(BOT_MEMBER_ID, coin.getCoinId(), amount);
            }
        } catch (Exception e) {
            // 忽略資金補充過程中的錯誤 (例如併發更新導致的樂觀鎖異常)
            // 這是為了保證造市主流程不被中斷
        }
    }

    // 私有方法：從 Binance API 獲取指定幣對的最新價格
    // 輸入：symbol (例如 "BTCUSDT")
    // 輸出：最新價格 (BigDecimal)，若失敗則回傳 null
    private BigDecimal fetchBinancePrice(String symbol) {
        // 建構 Binance API URL
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + symbol;
        try {
            // 發送 GET 請求，預期回傳 JSON 格式: {"symbol":"BTCUSDT","price":"95000.00000000"}
            // 使用 Map.class 簡單接收 JSON 物件 [註2]
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            
            // 檢查回應是否包含價格欄位
            if (response != null && response.containsKey("price")) {
                // 將字串格式的價格轉換為 BigDecimal
                return new BigDecimal(response.get("price"));
            }
        } catch (Exception e) {
            // 若發生網路錯誤或解析錯誤，印出錯誤訊息並回傳 null
            System.err.println("Failed to fetch price from Binance for " + symbol);
        }
        return null;
    }

    // 私有方法：在參考價格附近放置隨機訂單
    // 策略：以參考價格為中心，分別在下方掛買單，上方掛賣單，形成價差 (Spread)
    private void placeRandomOrders(String symbol, BigDecimal centerPrice) {
        // 定義價格波動範圍：0.1% ~ 0.5%
        
        // 迴圈：放置 2 筆買單 (Bid)
        for (int i = 0; i < 2; i++) {
            // 計算買入價格：中心價 * (1 - 隨機跌幅)
            // 這樣保證買單價格低於市價
            BigDecimal price = centerPrice.multiply(BigDecimal.ONE.subtract(randomPct(0.001, 0.005)));
            
            // 計算下單數量
            BigDecimal qty = randomQty(centerPrice);
            
            // 執行下單
            placeOrder(symbol, OrderSide.BUY, price, qty);
        }

        // 迴圈：放置 2 筆賣單 (Ask)
        for (int i = 0; i < 2; i++) {
            // 計算賣出價格：中心價 * (1 + 隨機漲幅)
            // 這樣保證賣單價格高於市價
            BigDecimal price = centerPrice.multiply(BigDecimal.ONE.add(randomPct(0.001, 0.005)));
            
            // 計算下單數量
            BigDecimal qty = randomQty(centerPrice);
            
            // 執行下單
            placeOrder(symbol, OrderSide.SELL, price, qty);
        }
    }

    // 輔助方法：產生指定範圍內的隨機百分比
    // 例如 min=0.001, max=0.005 -> 回傳 0.001 到 0.005 之間的 BigDecimal
    private BigDecimal randomPct(double min, double max) {
        double val = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(val);
    }
    
    // 輔助方法：動態計算下單數量
    // 目標：讓每筆訂單的總價值落在 10 ~ 100 USDT 之間
    private BigDecimal randomQty(BigDecimal currentPrice) {
        // 防止除以零
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE;
        
        // 隨機生成目標總價值 (10 ~ 100)
        double targetValue = 10 + (90 * random.nextDouble());
        
        // 數量 = 目標價值 / 單價
        // 使用 4 位小數精度，捨入模式為四捨五入
        BigDecimal qty = BigDecimal.valueOf(targetValue).divide(currentPrice, 4, RoundingMode.HALF_UP);
        
        // 確保計算出的數量不為 0 (避免無效訂單)
        if (qty.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("0.0001");
        
        return qty;
    }

    // 私有方法：封裝下單請求並呼叫 OrderService
    private void placeOrder(String symbolId, OrderSide side, BigDecimal price, BigDecimal quantity) {
        // 建立下單請求 DTO
        OrderRequest req = new OrderRequest();
        req.setSymbolId(symbolId);
        req.setSide(side);
        req.setType(OrderType.LIMIT); // 造市單通常為限價單 (Limit Order)
        req.setTradeType(TradeType.SPOT); // 目前僅支援現貨
        
        // 設定價格：保留 8 位小數，避免因浮點數精度導致的 API 錯誤
        req.setPrice(price.setScale(8, RoundingMode.HALF_UP)); 
        req.setQuantity(quantity);

        try {
            // 呼叫 OrderService 執行下單
            // 使用機器人 ID (BOT_MEMBER_ID)
            orderService.createOrder(BOT_MEMBER_ID, req);
            // (選擇性) 可以在此處記錄下單成功的日誌
            // System.out.println("Bot placed order: " + side + " " + symbolId + " @ " + price);
        } catch (Exception e) {
            // 捕捉下單失敗異常 (例如餘額不足、參數錯誤)，防止中斷迴圈
            // System.err.println("Bot order failed: " + e.getMessage());
        }
    }
}

// ====== 備註區 ======
/*
[註1] 資金控管優化 (Fund Management):
      目前 `ensureBotFunds` 在每次迴圈都會執行 `deposit`，這會導致 `wallet_transactions` 表迅速膨脹，
      且頻繁寫入資料庫會影響效能。
      建議邏輯：先檢查餘額 (`walletService.getWallet(...)`)，只有當餘額低於特定閾值時才進行儲值。

[註2] 型別安全 (Type Safety):
      使用 `Map.class` 接收 API 回應雖然方便，但缺乏型別檢查。
      建議定義一個內部的 DTO 類別 (例如 `BinanceTickerResponse`) 來映射 JSON 回應，
      這樣可以避免 `String` 到 `BigDecimal` 轉換時的潛在錯誤，並提高程式碼可讀性。
*/
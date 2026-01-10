package com.exchange.exchange;

// 引入實體與資料存取層
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Member;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.MemberRepository;
import com.exchange.exchange.repository.SymbolRepository;

// ====== 檔案總結 ======
// ExchangeApplication 是整個 Spring Boot 應用程式的啟動入口。
// 標註 @EnableScheduling 以啟用定時任務 (用於 MarketMakerService)。
// 包含 `initData` Bean，用於在系統啟動時自動檢查並建立預設的資料 (種子數據)。
@SpringBootApplication
@EnableScheduling
public class ExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeApplication.class, args);
    }

    // 定義 CommandLineRunner，應用程式啟動後自動執行此方法
    // 使用 @Transactional 確保整個初始化過程具有原子性 (要嘛全成功，要嘛全失敗)
    @Bean
    @Transactional
    public CommandLineRunner initData(CoinRepository coinRepository, SymbolRepository symbolRepository, MemberRepository memberRepository) {
        return args -> {
            System.out.println("------------------------------------------------");
            System.out.println("正在初始化資料...");
            
            // 0. 初始化機器人帳號 (Initialize Bot)
            // 假設 ID 1 為機器人，若不存在則建立
            // 這對應 MarketMakerService 中的 BOT_MEMBER_ID = 1
            if (!memberRepository.existsById(1)) {
                 System.out.println("建立造市機器人帳號...");
                 Member bot = new Member();
                 bot.setAccount("MarketMakerBot");
                 bot.setPassword("bot_secret_password"); 
                 bot.setName("Market Maker");
                 bot.setNumber("0000000000");
                 memberRepository.save(bot);
            }
            
            // 1. 初始化幣種 (Initialize Coins)
            List<String> defaultCoins = Arrays.asList("USDT", "BTC", "ETH", "BNB", "SOL", "XRP", "ADA", "DOGE", "DOT");
            for (String coinId : defaultCoins) {
                // 若幣種不存在則建立
                if (!coinRepository.existsById(coinId)) {
                    System.out.println("正在建立幣種: " + coinId);
                    Coin coin = new Coin();
                    coin.setCoinId(coinId);
                    coin.setName(coinId + " Token");
                    coin.setDecimals(8.0f); // 預設精度
                    coinRepository.save(coin);
                }
            }

            // 2. 初始化交易對 (Initialize Symbols)
            // 格式：{基礎幣, 報價幣} (例如 BTC/USDT)
            String[][] defaultSymbols = {
                {"BTC", "USDT"},
                {"ETH", "USDT"},
                {"BNB", "USDT"},
                {"SOL", "USDT"},
                {"XRP", "USDT"},
                {"ADA", "USDT"},
                {"DOGE", "USDT"},
                {"DOT", "USDT"}
            };

            for (String[] pair : defaultSymbols) {
                String base = pair[0];
                String quote = pair[1];
                String symbolId = base + quote; // 例如 BTCUSDT

                // 若交易對不存在則建立
                if (!symbolRepository.existsById(symbolId)) {
                    System.out.println("正在建立交易對: " + symbolId);
                    Symbol symbol = new Symbol();
                    symbol.setSymbolId(symbolId);
                    symbol.setName(base + "/" + quote);
                    symbol.setBaseCoinId(base);
                    symbol.setQuoteCoinId(quote);
                    symbolRepository.save(symbol);
                }
            }

            System.out.println("資料初始化完成。");
            System.out.println("------------------------------------------------");
        };
    }
}
package com.exchange.exchange;

import com.exchange.exchange.entity.Coin;
import com.exchange.exchange.entity.Symbol;
import com.exchange.exchange.repository.CoinRepository;
import com.exchange.exchange.repository.SymbolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class ExchangeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExchangeApplication.class, args);
	}

	@Bean
	@Transactional
	public CommandLineRunner initData(CoinRepository coinRepository, SymbolRepository symbolRepository) {
		return args -> {
			System.out.println("------------------------------------------------");
			System.out.println("正在初始化資料...");
			
			// 1. Initialize Coins
			List<String> defaultCoins = Arrays.asList("USDT", "BTC", "ETH", "BNB");
			for (String coinId : defaultCoins) {
				if (!coinRepository.existsById(coinId)) {
					System.out.println("正在建立幣種: " + coinId);
					Coin coin = new Coin();
					coin.setCoinId(coinId);
					coin.setName(coinId + " Token");
					coin.setDecimals(8.0f);
					coinRepository.save(coin);
				}
			}

			// 2. Initialize Symbols
			// Format: BaseCoin/QuoteCoin (e.g., BTC/USDT)
			String[][] defaultSymbols = {
				{"BTC", "USDT"},
				{"ETH", "USDT"},
				{"BNB", "USDT"}
			};

			for (String[] pair : defaultSymbols) {
				String base = pair[0];
				String quote = pair[1];
				String symbolId = base + quote; // e.g., BTCUSDT

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
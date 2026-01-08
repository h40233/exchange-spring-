package com.exchange.exchange.controller;

import com.exchange.exchange.service.SymbolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// ====== 檔案總結 ======
// SymbolController 提供市場基礎資訊的 API (公開數據，不需登入)。
// 功能：
// 1. 取得可交易幣種列表。
// 2. 取得所有幣種對 USDT 的最新價格 (Ticker)。
@RestController
@RequestMapping("/api/symbols")
public class SymbolController {

    @Autowired
    private SymbolService symbolService;

    // API: 獲取所有可交易幣種 (例如 ["BTC", "ETH", "BNB"])
    // GET /api/symbols/coins
    @GetMapping("/coins")
    public ResponseEntity<List<String>> getTradableCoins() {
        // 呼叫 Service 獲取幣種列表
        return ResponseEntity.ok(symbolService.getAllTradableCoins());
    }

    // API: 獲取全市場最新報價 (以 USDT 計價)
    // GET /api/symbols/tickers
    // 回傳格式範例: {"BTC": 50000.00, "ETH": 3000.00}
    @GetMapping("/tickers")
    public ResponseEntity<Map<String, BigDecimal>> getTickers() {
        return ResponseEntity.ok(symbolService.getCoinPricesInUsdt());
    }
}
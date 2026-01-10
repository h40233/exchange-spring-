package com.exchange.exchange.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exchange.exchange.service.SymbolService;

// ====== 檔案總結 ======
// SymbolController 提供市場基礎資訊的 RESTful API。
// 這些接口通常是公開的 (Public Endpoints)，不需要登入即可訪問。
// 用途：前端首頁顯示幣價、交易對選單等。
@RestController
@RequestMapping("/api/symbols")
public class SymbolController {

    @Autowired
    private SymbolService symbolService;

    // API: 獲取所有可交易幣種列表 (例如 ["BTC", "ETH", "BNB"])
    // GET /api/symbols/coins
    @GetMapping("/coins")
    public ResponseEntity<List<String>> getTradableCoins() {
        // 呼叫 Service 獲取幣種列表 (同時會觸發自動初始化邏輯)
        return ResponseEntity.ok(symbolService.getAllTradableCoins());
    }

    // API: 獲取全市場最新報價 (以 USDT 計價)
    // GET /api/symbols/tickers
    // 回傳格式範例: {"BTC": 50000.00, "ETH": 3000.00}
    // 用於首頁行情列表
    @GetMapping("/tickers")
    public ResponseEntity<Map<String, BigDecimal>> getTickers() {
        return ResponseEntity.ok(symbolService.getCoinPricesInUsdt());
    }
}
package com.exchange.exchange.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.service.CandleService;

// ====== 檔案總結 ======
// CandleController 提供 K 線數據的查詢接口。
// 支援從本地資料庫查詢，也提供代理 (Proxy) 功能直接轉發請求至 Binance (用於前端開發測試)。
@RestController
@RequestMapping("/api/candles")
public class CandleController {

    @Autowired
    private CandleService candleService;

    // 注入 RestTemplate 用於代理外部 API 請求
    @Autowired
    private RestTemplate restTemplate;

    // API: 從本地資料庫獲取 K 線數據
    // GET /api/candles/{symbolId}?interval=1m
    @GetMapping("/{symbolId}")
    public ResponseEntity<List<Candle>> getCandles(
            @PathVariable String symbolId,
            @RequestParam(defaultValue = "1m") String interval) {
        // 呼叫 Service 查詢並回傳
        return ResponseEntity.ok(candleService.getCandles(symbolId, interval));
    }

    // API: Binance K 線代理 (Proxy Endpoint)
    // GET /api/candles/proxy/{symbol}?interval=1m
    // 用途：避免前端直接呼叫 Binance API 時遇到的 CORS (跨域資源共享) 問題。
    @GetMapping("/proxy/{symbol}")
    public ResponseEntity<?> getBinanceCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval) {
        
        // 建構 Binance API URL
        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol.toUpperCase() + "&interval=" + interval + "&limit=1000";
        
        try {
            // 直接轉發請求並將原始回應傳回前端
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 若發生錯誤，回傳 500
            return ResponseEntity.status(500).body("Error fetching from Binance: " + e.getMessage());
        }
    }
}
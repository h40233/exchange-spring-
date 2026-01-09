package com.exchange.exchange.controller;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.service.CandleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api/candles")
public class CandleController {

    @Autowired
    private CandleService candleService;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/{symbolId}")
    public ResponseEntity<List<Candle>> getCandles(
            @PathVariable String symbolId,
            @RequestParam(defaultValue = "1m") String interval) {
        return ResponseEntity.ok(candleService.getCandles(symbolId, interval));
    }

    // Proxy endpoint to fetch data from Binance to avoid CORS issues on frontend
    @GetMapping("/proxy/{symbol}")
    public ResponseEntity<?> getBinanceCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval) {
        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol.toUpperCase() + "&interval=" + interval + "&limit=1000";
        try {
            // Forward the raw response from Binance
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching from Binance: " + e.getMessage());
        }
    }
}

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

@RestController
@RequestMapping("/api/symbols")
public class SymbolController {

    @Autowired
    private SymbolService symbolService;

    @GetMapping("/coins")
    public ResponseEntity<List<String>> getTradableCoins() {
        return ResponseEntity.ok(symbolService.getAllTradableCoins());
    }

    @GetMapping("/tickers")
    public ResponseEntity<Map<String, BigDecimal>> getTickers() {
        return ResponseEntity.ok(symbolService.getCoinPricesInUsdt());
    }
}

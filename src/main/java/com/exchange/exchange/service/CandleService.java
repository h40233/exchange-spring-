package com.exchange.exchange.service;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.enums.Timeframe;
import com.exchange.exchange.repository.CandleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CandleService {

    @Autowired
    private CandleRepository candleRepository;

    public List<Candle> getCandles(String symbolId, String interval) {
        Timeframe timeframe = Timeframe._1m; // Default
        try {
            switch (interval) {
                case "1m": timeframe = Timeframe._1m; break;
                case "5m": timeframe = Timeframe._5m; break;
                case "15m": timeframe = Timeframe._15m; break;
                case "30m": timeframe = Timeframe._30m; break;
                case "1h": timeframe = Timeframe._1H; break; // Note upper case H
                case "1d": timeframe = Timeframe._1D; break; // Note upper case D
                default: 
                    // Try to parse if it matches _1m format directly
                    timeframe = Timeframe.valueOf(interval); 
            }
        } catch (IllegalArgumentException e) {
            // Keep default _1m
        }

        // 取最近 1000 根
        Pageable limit = PageRequest.of(0, 1000);
        List<Candle> candles = candleRepository.findBySymbolIdAndTimeframeOrderByOpenTimeDesc(symbolId, timeframe, limit);
        
        // 資料庫回傳是 Desc (新->舊)，但圖表通常需要 Asc (舊->新)
        Collections.reverse(candles);
        
        return candles;
    }
}
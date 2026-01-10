package com.exchange.exchange.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.enums.Timeframe;
import com.exchange.exchange.repository.CandleRepository;

// ====== 檔案總結 ======
// CandleService 負責提供 K 線圖 (Candlestick Chart) 所需的數據。
// 主要功能是將前端傳來的字串週期 (如 "1h") 轉換為枚舉，並查詢資料庫。
@Service
public class CandleService {

    @Autowired
    private CandleRepository candleRepository;

    // 方法：查詢 K 線數據
    // 參數：symbolId (交易對), interval (週期字串)
    public List<Candle> getCandles(String symbolId, String interval) {
        Timeframe timeframe = Timeframe._1m; // 預設為 1 分鐘
        try {
            // 解析週期字串，映射到 Timeframe 枚舉
            timeframe = switch (interval) {
                case "1m" -> Timeframe._1m;
                case "5m" -> Timeframe._5m;
                case "15m" -> Timeframe._15m;
                case "30m" -> Timeframe._30m;
                case "1h" -> Timeframe._1H; // 注意枚舉命名是大寫 H
                case "1d" -> Timeframe._1D; // 注意枚舉命名是大寫 D
                default -> Timeframe.valueOf(interval); // 嘗試直接轉換，若失敗則進入 catch
            };
        } catch (IllegalArgumentException e) {
            // 解析失敗時保持預設值
        }

        // 設定分頁限制：取最近 1000 根 K 線
        Pageable limit = PageRequest.of(0, 1000);
        
        // 查詢資料庫：依據開盤時間倒序 (最新的在前)
        List<Candle> candles = candleRepository.findBySymbolIdAndTimeframeOrderByOpenTimeDesc(symbolId, timeframe, limit);
        
        // 資料處理：資料庫回傳是 Desc (新->舊)，但前端圖表庫 (如 TradingView) 通常需要 Asc (舊->新)
        Collections.reverse(candles);
        
        return candles;
    }
}
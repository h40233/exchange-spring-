package com.exchange.exchange.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.entity.key.CandleId;
import com.exchange.exchange.enums.Timeframe;

// ====== 檔案總結 ======
// CandleRepository 負責對 `candles` 表進行操作。
// 提供查詢歷史 K 線數據的功能，用於前端繪製 K 線圖。
@Repository
public interface CandleRepository extends JpaRepository<Candle, CandleId> {

    // 查詢特定交易對、特定週期的 K 線列表
    // 依照開盤時間倒序排列 (最新的在前)，配合 Pageable 取出最近 N 根
    List<Candle> findBySymbolIdAndTimeframeOrderByOpenTimeDesc(String symbolId, Timeframe timeframe, Pageable pageable);
}
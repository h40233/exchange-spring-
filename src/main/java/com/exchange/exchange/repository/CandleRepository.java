package com.exchange.exchange.repository;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.entity.key.CandleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ====== 檔案總結 ======
// CandleRepository 負責 K 線資料的存取。
// 主鍵類型為複合鍵 `CandleId`。
// 通常需要擴充方法如 `findBySymbolIdAndTimeframeAndOpenTimeBetween(...)` 來繪製圖表。
@Repository
public interface CandleRepository extends JpaRepository<Candle, CandleId> {
}
package com.exchange.exchange.repository;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.entity.key.CandleId;
import com.exchange.exchange.enums.Timeframe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<Candle, CandleId> {

    List<Candle> findBySymbolIdAndTimeframeOrderByOpenTimeDesc(String symbolId, Timeframe timeframe, Pageable pageable);
}

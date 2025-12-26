package com.exchange.exchange.repository;

import com.exchange.exchange.entity.Candle;
import com.exchange.exchange.entity.key.CandleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CandleRepository extends JpaRepository<Candle, CandleId> {
}

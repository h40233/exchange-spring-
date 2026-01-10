package com.exchange.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.entity.Coin;

// ====== 檔案總結 ======
// CoinRepository 負責對 `coins` 表進行操作。
// 用於管理系統支援的基礎幣種。
@Repository
public interface CoinRepository extends JpaRepository<Coin, String> {
}
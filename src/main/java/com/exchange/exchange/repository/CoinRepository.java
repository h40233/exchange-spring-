package com.exchange.exchange.repository;

// 引入實體與 JPA
import com.exchange.exchange.entity.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ====== 檔案總結 ======
// CoinRepository 提供對 `coins` 表的 CRUD 操作。
// 繼承 JpaRepository 後，自動獲得 findAll, findById, save 等標準方法。
@Repository
public interface CoinRepository extends JpaRepository<Coin, String> {
}
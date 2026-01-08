package com.exchange.exchange.repository;

// 引入實體：交易對
import com.exchange.exchange.entity.Symbol;
// 引入 Spring Data JPA 核心介面
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ====== 檔案總結 ======
// SymbolRepository 負責對 `symbols` 表進行資料存取操作。
// 交易對 (Symbol) 是交易所的基礎設定，定義了哪些幣種可以互換 (例如 BTC 換 USDT)。
// 繼承 JpaRepository 後，直接獲得標準的 CRUD 功能，無需手寫 SQL。
@Repository
public interface SymbolRepository extends JpaRepository<Symbol, String> {
    // 目前使用 Spring Data JPA 預設提供的 findById, findAll, save 等方法即可滿足需求。
    // [註1] 未來擴充性：
    // 若將來需要查詢「所有以 USDT 為計價單位的交易對」，
    // 可以新增方法：List<Symbol> findByQuoteCoinId(String quoteCoinId);
}

// ====== 備註區 ======
/*
[註1] 快取建議 (Caching Strategy):
      交易對資訊 (Symbols) 屬於「讀多寫少」的靜態設定資料。
      在生產環境中，頻繁查詢資料庫來獲取這些設定會造成不必要的負擔。
      建議加上 Spring Cache (@Cacheable) 或使用 Redis 進行快取，提升系統效能。
*/
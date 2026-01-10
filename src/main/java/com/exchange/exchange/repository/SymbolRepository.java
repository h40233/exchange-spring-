package com.exchange.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.entity.Symbol;

// ====== 檔案總結 ======
// SymbolRepository 負責對 `symbols` 表進行 CRUD 操作。
// 用於系統啟動時初始化交易對，或下單時驗證交易對是否存在。
@Repository
public interface SymbolRepository extends JpaRepository<Symbol, String> {
}
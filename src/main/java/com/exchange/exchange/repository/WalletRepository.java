package com.exchange.exchange.repository;

// 引入實體：錢包
import com.exchange.exchange.entity.Wallet;
// 引入複合主鍵類別
import com.exchange.exchange.entity.key.WalletId;
// 引入 Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// ====== 檔案總結 ======
// WalletRepository 負責對 `wallets` 表進行資料存取。
// 核心職責：
// 1. 根據複合主鍵 (MemberID + CoinID) 精確查找單一錢包。
// 2. 查詢特定會員名下的所有錢包資產。
//
// 泛型參數解析：
// - Wallet: 要操作的實體型別。
// - WalletId: 該實體的主鍵型別 (因為是複合鍵，所以填入 WalletId 類別)。
@Repository
public interface WalletRepository extends JpaRepository<Wallet, WalletId> {
    
    // 自定義查詢方法：根據會員 ID 查找該會員擁有的所有錢包
    // Spring Data JPA 會自動解析方法名稱 (Method Name Parsing) 生成 SQL：
    // SELECT * FROM wallets WHERE member_id = ?
    List<Wallet> findByMemberId(Integer memberId);
}

// ====== 備註區 ======
/*
[註1] 索引效能 (Indexing):
      雖然 `memberId` 是複合主鍵的一部分，但資料庫通常只會對複合索引的第一個欄位 (假設是 memberId) 進行最佳化。
      若複合主鍵定義順序是 (memberId, coinId)，則此查詢效能良好。
      若定義順序是 (coinId, memberId)，則單獨查詢 `findByMemberId` 可能無法完全利用索引，需檢查資料庫 Schema 定義。
      
[註2] 鎖機制 (Locking):
      在涉及資金變動的高併發場景 (如撮合成功後扣款)，
      建議使用 `findById` 配合 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 來獲取悲觀鎖，
      防止「更新丟失」(Lost Update) 問題，確保資產安全。
*/
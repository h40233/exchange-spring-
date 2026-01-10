package com.exchange.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.exchange.exchange.entity.WalletTransaction;

// ====== 檔案總結 ======
// WalletTransactionRepository 負責對 `wallet_transactions` 表進行查詢。
// 提供使用者查詢資產變動明細的功能。
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    
    // 查詢會員的所有資金流水，按時間倒序排列
    List<WalletTransaction> findByMemberIdOrderByCreatedAtDesc(Integer memberId);
}
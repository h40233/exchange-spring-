package com.exchange.exchange.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.entity.Wallet;
import com.exchange.exchange.entity.key.WalletId;

// ====== 檔案總結 ======
// WalletRepository 負責對 `wallets` 表進行 CRUD 操作。
// 繼承 JpaRepository，並指定複合主鍵類別 WalletId。
@Repository
public interface WalletRepository extends JpaRepository<Wallet, WalletId> {
    
    // 查詢特定會員名下的所有錢包
    // Spring Data JPA 會自動解析方法名稱生成 SQL
    List<Wallet> findByMemberId(Integer memberId);
}
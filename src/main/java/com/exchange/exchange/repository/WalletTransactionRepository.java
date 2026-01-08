package com.exchange.exchange.repository;

// 引入實體與 JPA
import com.exchange.exchange.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// ====== 檔案總結 ======
// WalletTransactionRepository 提供資金流水的查詢功能。
// 主要用於讓使用者查看自己的資產變動歷史。
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    
    // 查詢某會員的所有流水紀錄，並依照建立時間倒序排列 (最新的在最上面)
    List<WalletTransaction> findByMemberIdOrderByCreatedAtDesc(Integer memberId);
}
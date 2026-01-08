package com.exchange.exchange.repository;

// 引入實體與 JPA
import com.exchange.exchange.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ====== 檔案總結 ======
// MemberRepository 提供會員資料的資料庫操作。
// 核心功能：根據帳號 (Account) 查詢會員，用於登入驗證與註冊檢查。
@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    
    // 透過帳號尋找會員 (回傳 Optional 避免 NullPointerException)
    // 對應 SQL: SELECT * FROM members WHERE account = ?
    Optional<Member> findByAccount(String account);
}
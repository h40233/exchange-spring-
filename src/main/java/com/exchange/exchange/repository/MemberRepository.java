package com.exchange.exchange.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchange.exchange.entity.Member;

// ====== 檔案總結 ======
// MemberRepository 負責對 `members` 表進行操作。
// 主要用於登入驗證 (透過帳號查找會員)。
@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    
    // 根據帳號名稱查找會員
    Optional<Member> findByAccount(String account);
}